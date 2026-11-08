package middleware

import (
	"bytes"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func testLogger(buf *bytes.Buffer) *slog.Logger {
	return slog.New(slog.NewJSONHandler(buf, &slog.HandlerOptions{
		Level: slog.LevelDebug,
	}))
}

func TestRecoverWritesInternalServerErrorWhenPanicBeforeResponse(t *testing.T) {
	var logs bytes.Buffer
	logger := testLogger(&logs)

	handler := RequestLogger(logger)(Recover(logger)(http.HandlerFunc(func(http.ResponseWriter, *http.Request) {
		panic("something went wrong")
	})))

	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, httptest.NewRequest(http.MethodGet, "/test", nil))

	if recorder.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusInternalServerError)
	}
	if got, want := recorder.Header().Get("Content-Type"), "application/json"; got != want {
		t.Errorf("Content-Type = %q, want %q", got, want)
	}
	if got, want := recorder.Body.String(), "{\"message\":\"Internal Server Error\"}\n"; got != want {
		t.Errorf("body = %q, want %q", got, want)
	}

	logOutput := logs.String()
	for _, want := range []string{"recovered from panic", "something went wrong", "stack", "HTTP request", "\"status\":500"} {
		if !strings.Contains(logOutput, want) {
			t.Errorf("logs do not contain %q:\n%s", want, logOutput)
		}
	}
}

func TestRecoverDoesNotAppendErrorAfterResponseStarted(t *testing.T) {
	var logs bytes.Buffer
	logger := testLogger(&logs)

	handler := RequestLogger(logger)(Recover(logger)(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/octet-stream")
		// The first body write implicitly commits a 200 response.
		if _, err := w.Write([]byte("partial-download")); err != nil {
			t.Fatalf("write response: %v", err)
		}
		panic("stream failed")
	})))

	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, httptest.NewRequest(http.MethodGet, "/download", nil))

	if recorder.Code != http.StatusOK {
		t.Errorf("status = %d, want %d", recorder.Code, http.StatusOK)
	}
	if got, want := recorder.Body.String(), "partial-download"; got != want {
		t.Errorf("body = %q, want %q (recovery must not append JSON)", got, want)
	}
	if got, want := recorder.Header().Get("Content-Type"), "application/octet-stream"; got != want {
		t.Errorf("Content-Type = %q, want %q", got, want)
	}

	logOutput := logs.String()
	for _, want := range []string{"recovered from panic", "stream failed", "HTTP request", "\"status\":200", "\"bytes\":16"} {
		if !strings.Contains(logOutput, want) {
			t.Errorf("logs do not contain %q:\n%s", want, logOutput)
		}
	}
}

func TestRequestLoggerRecordsResponseStatusAndBytes(t *testing.T) {
	var logs bytes.Buffer
	logger := testLogger(&logs)

	handler := RequestLogger(logger)(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte("created"))
	}))

	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, httptest.NewRequest(http.MethodPost, "/resource", nil))

	if recorder.Code != http.StatusCreated {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusCreated)
	}
	for _, want := range []string{"HTTP request", "\"method\":\"POST\"", "\"path\":\"/resource\"", "\"status\":201", "\"bytes\":7"} {
		if !strings.Contains(logs.String(), want) {
			t.Errorf("logs do not contain %q:\n%s", want, logs.String())
		}
	}
}
