package config

import (
	"errors"
	"log/slog"
	"testing"

	"github.com/pietro-a/humanize/bytesize"
	"github.com/pietro-a/isp-network-tester/server/internal/e4s"
)

func clearConfigEnv(t *testing.T) {
	t.Helper()
	for _, key := range []string{
		"LISTEN_ADDR", "BUFFER_SIZE", "MAX_DOWNLOAD_SIZE", "MAX_CONCURRENT_DOWNLOADS",
		"READ_HEADER_TIMEOUT", "IDLE_TIMEOUT", "SHUTDOWN_TIMEOUT", "LOG_LEVEL",
	} {
		t.Setenv(key, "")
	}
}

func TestParseConfigReturnsDefaults(t *testing.T) {
	clearConfigEnv(t)

	got, err := ParseConfig()
	if err != nil {
		t.Fatalf("ParseConfig() returned error: %v", err)
	}

	want := &Config{
		ListenAddr:             ":8080",
		BufferSize:             16 * bytesize.MiB,
		MaxDownloadSize:        bytesize.GiB,
		MaxConcurrentDownloads: 4,
		ReadHeaderTimeout:      5,
		IdleTimeout:            30,
		ShutdownTimeout:        10,
		LogLevel:               slog.LevelInfo,
	}
	if *got != *want {
		t.Fatalf("ParseConfig() = %+v, want %+v", got, want)
	}
}

func TestParseConfigReadsEnvironment(t *testing.T) {
	clearConfigEnv(t)
	t.Setenv("LISTEN_ADDR", ":9090")
	t.Setenv("BUFFER_SIZE", "32MiB")
	t.Setenv("MAX_DOWNLOAD_SIZE", "2GiB")
	t.Setenv("MAX_CONCURRENT_DOWNLOADS", "8")
	t.Setenv("READ_HEADER_TIMEOUT", "7s")
	t.Setenv("IDLE_TIMEOUT", "2m")
	t.Setenv("SHUTDOWN_TIMEOUT", "1h")
	t.Setenv("LOG_LEVEL", "DeBuG")

	got, err := ParseConfig()
	if err != nil {
		t.Fatalf("ParseConfig() returned error: %v", err)
	}

	want := &Config{
		ListenAddr:             "0.0.0.0:9090",
		BufferSize:             32 * bytesize.MiB,
		MaxDownloadSize:        2 * bytesize.GiB,
		MaxConcurrentDownloads: 8,
		ReadHeaderTimeout:      7,
		IdleTimeout:            120,
		ShutdownTimeout:        3600,
		LogLevel:               slog.LevelDebug,
	}
	if *got != *want {
		t.Fatalf("ParseConfig() = %+v, want %+v", got, want)
	}
}

func TestParseConfigAcceptsExplicitIPAndPort(t *testing.T) {
	clearConfigEnv(t)
	for _, addr := range []string{"127.0.0.1:9090", "[::1]:9090"} {
		t.Run(addr, func(t *testing.T) {
			t.Setenv("LISTEN_ADDR", addr)

			got, err := ParseConfig()
			if err != nil {
				t.Fatalf("ParseConfig() returned error: %v", err)
			}
			if got.ListenAddr != addr {
				t.Fatalf("ListenAddr = %q, want %q", got.ListenAddr, addr)
			}
		})
	}
}

func TestParseConfigDoesNotRetainPreviousValues(t *testing.T) {
	clearConfigEnv(t)
	t.Setenv("LISTEN_ADDR", ":9090")
	t.Setenv("BUFFER_SIZE", "32MiB")

	first, err := ParseConfig()
	if err != nil {
		t.Fatalf("first ParseConfig() returned error: %v", err)
	}
	if first.ListenAddr != "0.0.0.0:9090" || first.BufferSize != 32*bytesize.MiB {
		t.Fatalf("first ParseConfig() = %+v, unexpected values", first)
	}

	t.Setenv("LISTEN_ADDR", "")
	t.Setenv("BUFFER_SIZE", "")

	second, err := ParseConfig()
	if err != nil {
		t.Fatalf("second ParseConfig() returned error: %v", err)
	}
	if second.ListenAddr != ":8080" || second.BufferSize != 16*bytesize.MiB {
		t.Fatalf("second ParseConfig() retained previous values: %+v", second)
	}
}

func TestParseConfigRejectsInvalidValues(t *testing.T) {
	clearConfigEnv(t)
	tests := []struct {
		name    string
		key     string
		value   string
		wantErr error
	}{
		{name: "listen address", key: "LISTEN_ADDR", value: "not-an-address"},
		{name: "hostname address", key: "LISTEN_ADDR", value: "localhost:8080"},
		{name: "buffer size", key: "BUFFER_SIZE", value: "many"},
		{name: "max download size", key: "MAX_DOWNLOAD_SIZE", value: "many"},
		{name: "max concurrent downloads", key: "MAX_CONCURRENT_DOWNLOADS", value: "many"},
		{name: "max concurrent downloads overflow", key: "MAX_CONCURRENT_DOWNLOADS", value: "999999999999999999999999999999"},
		{name: "read header timeout", key: "READ_HEADER_TIMEOUT", value: "soon"},
		{name: "idle timeout", key: "IDLE_TIMEOUT", value: "-1s"},
		{name: "shutdown timeout", key: "SHUTDOWN_TIMEOUT", value: "1h2h"},
		{name: "log level", key: "LOG_LEVEL", value: "trace", wantErr: e4s.ErrUnknownLogLevel},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Setenv(tt.key, tt.value)

			got, err := ParseConfig()
			if err == nil {
				t.Fatalf("ParseConfig() with %s=%q returned nil error", tt.key, tt.value)
			}
			if got != nil {
				t.Fatalf("ParseConfig() with invalid configuration returned non-nil config: %+v", got)
			}
			if tt.wantErr != nil && !errors.Is(err, tt.wantErr) {
				t.Fatalf("ParseConfig() error = %v, want %v", err, tt.wantErr)
			}
		})
	}
}

func TestParseConfigRejectsZeroByteSizes(t *testing.T) {
	clearConfigEnv(t)
	for _, key := range []string{"BUFFER_SIZE", "MAX_DOWNLOAD_SIZE"} {
		t.Run(key, func(t *testing.T) {
			t.Setenv(key, "0")

			got, err := ParseConfig()
			if got != nil {
				t.Fatalf("ParseConfig() returned non-nil config: %+v", got)
			}
			if !errors.Is(err, e4s.ErrZeroByteSize) {
				t.Fatalf("ParseConfig() error = %v, want %v", err, e4s.ErrZeroByteSize)
			}
		})
	}
}

func TestParseConfigRejectsZeroTimeouts(t *testing.T) {
	clearConfigEnv(t)
	for _, key := range []string{"READ_HEADER_TIMEOUT", "IDLE_TIMEOUT", "SHUTDOWN_TIMEOUT"} {
		t.Run(key, func(t *testing.T) {
			t.Setenv(key, "0")

			got, err := ParseConfig()
			if got != nil {
				t.Fatalf("ParseConfig() returned non-nil config: %+v", got)
			}
			if !errors.Is(err, e4s.ErrZeroTimeout) {
				t.Fatalf("ParseConfig() error = %v, want %v", err, e4s.ErrZeroTimeout)
			}
		})
	}
}

func TestParseConfigAcceptsAllLogLevelsCaseInsensitively(t *testing.T) {
	clearConfigEnv(t)
	tests := []struct {
		value string
		want  slog.Level
	}{
		{value: "debug", want: slog.LevelDebug},
		{value: "INFO", want: slog.LevelInfo},
		{value: "Warn", want: slog.LevelWarn},
		{value: "ERROR", want: slog.LevelError},
	}

	for _, tt := range tests {
		t.Run(tt.value, func(t *testing.T) {
			t.Setenv("LOG_LEVEL", tt.value)

			got, err := ParseConfig()
			if err != nil {
				t.Fatalf("ParseConfig() returned error: %v", err)
			}
			if got.LogLevel != tt.want {
				t.Fatalf("LogLevel = %v, want %v", got.LogLevel, tt.want)
			}
		})
	}
}

func TestParseConfigAcceptsMinimumConcurrentDownloads(t *testing.T) {
	clearConfigEnv(t)
	t.Setenv("MAX_CONCURRENT_DOWNLOADS", "1")

	got, err := ParseConfig()
	if err != nil {
		t.Fatalf("ParseConfig() returned error: %v", err)
	}
	if got.MaxConcurrentDownloads != 1 {
		t.Fatalf("MaxConcurrentDownloads = %d, want 1", got.MaxConcurrentDownloads)
	}
}

func TestParseConfigRejectsNonPositiveConcurrentDownloads(t *testing.T) {
	clearConfigEnv(t)
	for _, value := range []string{"0", "-1"} {
		t.Run(value, func(t *testing.T) {
			t.Setenv("MAX_CONCURRENT_DOWNLOADS", value)

			got, err := ParseConfig()
			if got != nil {
				t.Fatalf("ParseConfig() returned non-nil config: %+v", got)
			}
			if !errors.Is(err, e4s.ErrInvalidMaxConcurrentDownloads) {
				t.Fatalf("ParseConfig() error = %v, want %v", err, e4s.ErrInvalidMaxConcurrentDownloads)
			}
		})
	}
}
