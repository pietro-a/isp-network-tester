package server

import (
	"bytes"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strconv"
	"sync"
	"testing"
)

func newHandlerTestServer(t *testing.T, maxConcurrent int) *Server {
	t.Helper()

	srv, err := New(8, 32, maxConcurrent, slog.New(slog.NewTextHandler(io.Discard, nil)))
	if err != nil {
		t.Fatalf("New() returned error: %v", err)
	}
	return srv
}

func TestHealthReturnsNoContent(t *testing.T) {
	srv := newHandlerTestServer(t, 1)
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/healthz", nil)

	srv.Health(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusNoContent)
	}
	if rec.Body.Len() != 0 {
		t.Fatalf("body = %q, want empty", rec.Body.String())
	}
}

func TestDownloadReturnsExactBytesAndHeaders(t *testing.T) {
	srv := newHandlerTestServer(t, 1)
	want := make([]byte, 13)
	if _, err := io.ReadFull(srv.buf.NewReader(), want); err != nil {
		t.Fatalf("reading expected bytes: %v", err)
	}

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/download?size=13", nil)
	srv.Download(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d; body: %s", rec.Code, http.StatusOK, rec.Body)
	}
	if !bytes.Equal(rec.Body.Bytes(), want) {
		t.Fatalf("body = %v, want %v", rec.Body.Bytes(), want)
	}
	if got := rec.Header().Get("Content-Type"); got != "application/octet-stream" {
		t.Errorf("Content-Type = %q, want application/octet-stream", got)
	}
	if got := rec.Header().Get("Content-Length"); got != strconv.Itoa(len(want)) {
		t.Errorf("Content-Length = %q, want %q", got, strconv.Itoa(len(want)))
	}
	if got := rec.Header().Get("Cache-Control"); got != "no-store, no-transform" {
		t.Errorf("Cache-Control = %q, want no-store, no-transform", got)
	}
	if got := len(srv.downloads); got != 0 {
		t.Errorf("occupied download slots after completion = %d, want 0", got)
	}
}

func TestDownloadRejectsInvalidSizes(t *testing.T) {
	srv := newHandlerTestServer(t, 1)
	tests := []struct {
		name string
		url  string
	}{
		{name: "missing", url: "/download"},
		{name: "empty", url: "/download?size="},
		{name: "malformed", url: "/download?size=abc"},
		{name: "zero", url: "/download?size=0"},
		{name: "negative", url: "/download?size=-1"},
		{name: "overflow", url: "/download?size=9223372036854775808"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			rec := httptest.NewRecorder()
			req := httptest.NewRequest(http.MethodGet, tt.url, nil)

			srv.Download(rec, req)

			if rec.Code != http.StatusBadRequest {
				t.Fatalf("status = %d, want %d; body: %s", rec.Code, http.StatusBadRequest, rec.Body)
			}
			if got := rec.Header().Get("Content-Type"); got != "application/json" {
				t.Errorf("Content-Type = %q, want application/json", got)
			}
		})
	}
}

func TestDownloadRejectsSizeAboveMaximum(t *testing.T) {
	srv := newHandlerTestServer(t, 1)
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/download?size=33", nil)

	srv.Download(rec, req)

	if rec.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusRequestEntityTooLarge)
	}
	if got := len(srv.downloads); got != 0 {
		t.Errorf("occupied download slots = %d, want 0", got)
	}
}

func TestDownloadRejectsWhenAtCapacityAndReleasesSlot(t *testing.T) {
	srv := newHandlerTestServer(t, 1)
	blocking := &blockingResponseWriter{
		header:  make(http.Header),
		started: make(chan struct{}),
		release: make(chan struct{}),
	}

	done := make(chan struct{})
	go func() {
		defer close(done)
		req := httptest.NewRequest(http.MethodGet, "/download?size=8", nil)
		srv.Download(blocking, req)
	}()

	<-blocking.started

	busyRec := httptest.NewRecorder()
	srv.Download(busyRec, httptest.NewRequest(http.MethodGet, "/download?size=8", nil))
	if busyRec.Code != http.StatusServiceUnavailable {
		t.Fatalf("busy status = %d, want %d", busyRec.Code, http.StatusServiceUnavailable)
	}
	if got := busyRec.Header().Get("Retry-After"); got != "5" {
		t.Errorf("Retry-After = %q, want 5", got)
	}

	healthRec := httptest.NewRecorder()
	srv.Health(healthRec, httptest.NewRequest(http.MethodGet, "/healthz", nil))
	if healthRec.Code != http.StatusNoContent {
		t.Errorf("health status at download capacity = %d, want %d", healthRec.Code, http.StatusNoContent)
	}

	close(blocking.release)
	<-done

	if got := len(srv.downloads); got != 0 {
		t.Fatalf("occupied download slots after completion = %d, want 0", got)
	}

	nextRec := httptest.NewRecorder()
	srv.Download(nextRec, httptest.NewRequest(http.MethodGet, "/download?size=8", nil))
	if nextRec.Code != http.StatusOK {
		t.Fatalf("status after slot release = %d, want %d", nextRec.Code, http.StatusOK)
	}
}

func TestDownloadValidatesRequestBeforeCheckingCapacity(t *testing.T) {
	srv := newHandlerTestServer(t, 1)
	srv.downloads <- struct{}{}

	rec := httptest.NewRecorder()
	srv.Download(rec, httptest.NewRequest(http.MethodGet, "/download?size=invalid", nil))

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusBadRequest)
	}
	if got := len(srv.downloads); got != 1 {
		t.Fatalf("occupied download slots = %d, want 1", got)
	}
}

func TestDownloadReleasesSlotAfterWriteFailure(t *testing.T) {
	srv := newHandlerTestServer(t, 1)
	writer := &failingResponseWriter{header: make(http.Header)}

	srv.Download(writer, httptest.NewRequest(http.MethodGet, "/download?size=8", nil))

	if got := len(srv.downloads); got != 0 {
		t.Fatalf("occupied download slots after write failure = %d, want 0", got)
	}

	rec := httptest.NewRecorder()
	srv.Download(rec, httptest.NewRequest(http.MethodGet, "/download?size=8", nil))
	if rec.Code != http.StatusOK {
		t.Fatalf("status after failed write = %d, want %d", rec.Code, http.StatusOK)
	}
}

type blockingResponseWriter struct {
	header  http.Header
	started chan struct{}
	release chan struct{}
	once    sync.Once
	status  int
}

func (w *blockingResponseWriter) Header() http.Header { return w.header }

func (w *blockingResponseWriter) WriteHeader(status int) { w.status = status }

func (w *blockingResponseWriter) Write(p []byte) (int, error) {
	w.once.Do(func() { close(w.started) })
	<-w.release
	if w.status == 0 {
		w.status = http.StatusOK
	}
	return len(p), nil
}

type failingResponseWriter struct {
	header http.Header
}

func (w *failingResponseWriter) Header() http.Header { return w.header }

func (w *failingResponseWriter) WriteHeader(int) {}

func (w *failingResponseWriter) Write([]byte) (int, error) {
	return 0, errors.New("simulated client disconnect")
}
