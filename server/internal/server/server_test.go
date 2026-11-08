package server

import (
	"errors"
	"log/slog"
	"testing"

	"github.com/pietro-a/isp-network-tester/server/internal/e4s"
)

func TestNewRejectsInvalidConfiguration(t *testing.T) {
	tests := []struct {
		name                string
		size                int64
		maxDownloadSize     int64
		maxConcurrent       int
		wantErr             error
	}{
		{name: "zero buffer size", size: 0, maxDownloadSize: 1, maxConcurrent: 1, wantErr: e4s.ErrInvalidBufSize},
		{name: "negative buffer size", size: -1, maxDownloadSize: 1, maxConcurrent: 1, wantErr: e4s.ErrInvalidBufSize},
		{name: "zero maximum download size", size: 1, maxDownloadSize: 0, maxConcurrent: 1, wantErr: e4s.ErrInvalidMaxDownloadSize},
		{name: "negative maximum download size", size: 1, maxDownloadSize: -1, maxConcurrent: 1, wantErr: e4s.ErrInvalidMaxDownloadSize},
		{name: "zero concurrency", size: 1, maxDownloadSize: 1, maxConcurrent: 0, wantErr: e4s.ErrInvalidMaxConcurrentDownloads},
		{name: "negative concurrency", size: 1, maxDownloadSize: 1, maxConcurrent: -1, wantErr: e4s.ErrInvalidMaxConcurrentDownloads},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := New(tt.size, tt.maxDownloadSize, tt.maxConcurrent, nil)
			if got != nil {
				t.Fatalf("New() returned non-nil server: %+v", got)
			}
			if !errors.Is(err, tt.wantErr) {
				t.Fatalf("New() error = %v, want %v", err, tt.wantErr)
			}
		})
	}
}

func TestNewAcceptsNilLogger(t *testing.T) {
	got, err := New(8, 16, 1, nil)
	if err != nil {
		t.Fatalf("New() returned error: %v", err)
	}
	if got == nil {
		t.Fatal("New() returned nil server")
	}
	if got.logger != slog.Default() {
		t.Fatal("New() did not use slog.Default() for a nil logger")
	}
	if cap(got.downloads) != 1 {
		t.Fatalf("download capacity = %d, want 1", cap(got.downloads))
	}
}
