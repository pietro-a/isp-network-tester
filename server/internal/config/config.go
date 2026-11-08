package config

import (
	"fmt"
	"log/slog"
	"net/netip"
	"os"
	"strconv"
	"strings"

	"github.com/pietro-a/humanize/bytesize"
	"github.com/pietro-a/humanize/duration"
	"github.com/pietro-a/isp-network-tester/server/internal/e4s"
)

type Config struct {
	ListenAddr             string
	BufferSize             int64
	MaxDownloadSize        int64
	MaxConcurrentDownloads int
	ReadHeaderTimeout      int
	IdleTimeout            int
	ShutdownTimeout        int
	LogLevel               slog.Level
}

func ParseConfig() (*Config, error) {
	cfg := &Config{
		ListenAddr:             ":8080",
		BufferSize:             16 * bytesize.MiB,
		MaxDownloadSize:        bytesize.GiB,
		MaxConcurrentDownloads: 4,
		ReadHeaderTimeout:      5,
		IdleTimeout:            30,
		ShutdownTimeout:        10,
		LogLevel:               slog.LevelInfo,
	}

	if v, ok := os.LookupEnv("LISTEN_ADDR"); ok && v != "" {
		if strings.HasPrefix(v, ":") {
			v = fmt.Sprintf("0.0.0.0%s", v)
		}
		p, err := netip.ParseAddrPort(v)
		if err != nil {
			return nil, err
		}
		cfg.ListenAddr = p.String()
	}
	if v, ok := os.LookupEnv("BUFFER_SIZE"); ok && v != "" {
		p, err := bytesize.New(v)
		if err != nil {
			return nil, err
		}
		b := p.Bytes()
		// Byte size can't be negative
		if b == 0 {
			return nil, e4s.ErrZeroByteSize
		}
		cfg.BufferSize = b
	}
	if v, ok := os.LookupEnv("MAX_DOWNLOAD_SIZE"); ok && v != "" {
		p, err := bytesize.New(v)
		if err != nil {
			return nil, err
		}
		b := p.Bytes()
		// Byte size can't be negative
		if b == 0 {
			return nil, e4s.ErrZeroByteSize
		}
		cfg.MaxDownloadSize = b
	}
	if v, ok := os.LookupEnv("MAX_CONCURRENT_DOWNLOADS"); ok && v != "" {
		b, err := strconv.Atoi(v)
		if err != nil {
			return nil, err
		}
		if b <= 0 {
			return nil, e4s.ErrInvalidMaxConcurrentDownloads
		}
		cfg.MaxConcurrentDownloads = b
	}
	if v, ok := os.LookupEnv("READ_HEADER_TIMEOUT"); ok && v != "" {
		p, err := duration.New(v)
		if err != nil {
			return nil, err
		}
		b := int(p.Seconds())
		// Duration can't be negative
		if b == 0 {
			return nil, e4s.ErrZeroTimeout
		}
		cfg.ReadHeaderTimeout = b
	}
	if v, ok := os.LookupEnv("IDLE_TIMEOUT"); ok && v != "" {
		p, err := duration.New(v)
		if err != nil {
			return nil, err
		}
		b := int(p.Seconds())
		// Duration can't be negative
		if b == 0 {
			return nil, e4s.ErrZeroTimeout
		}
		cfg.IdleTimeout = b
	}
	if v, ok := os.LookupEnv("SHUTDOWN_TIMEOUT"); ok && v != "" {
		p, err := duration.New(v)
		if err != nil {
			return nil, err
		}
		b := int(p.Seconds())
		// Duration can't be negative
		if b == 0 {
			return nil, e4s.ErrZeroTimeout
		}
		cfg.ShutdownTimeout = b
	}
	if v, ok := os.LookupEnv("LOG_LEVEL"); ok && v != "" {
		switch strings.ToLower(v) {
		case "debug":
			cfg.LogLevel = slog.LevelDebug
		case "info":
			cfg.LogLevel = slog.LevelInfo
		case "warn":
			cfg.LogLevel = slog.LevelWarn
		case "error":
			cfg.LogLevel = slog.LevelError
		default:
			return nil, e4s.ErrUnknownLogLevel
		}
	}

	return cfg, nil
}
