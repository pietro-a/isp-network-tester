package server

import (
	"log/slog"
	"math"

	"github.com/pietro-a/isp-network-tester/server/internal/e4s"
	"github.com/pietro-a/isp-network-tester/server/internal/randbuf"
)

type Server struct {
	logger          *slog.Logger
	buf             *randbuf.Buffer
	maxDownloadSize int64
	downloads       chan struct{}
}

func New(size, maxDownloadSize int64, maxConcurrentDownloads int, logger *slog.Logger) (*Server, error) {
	if size <= 0 || size > math.MaxInt {
		return nil, e4s.ErrInvalidBufSize
	}
	if maxDownloadSize <= 0 {
		return nil, e4s.ErrInvalidMaxDownloadSize
	}
	if maxConcurrentDownloads <= 0 {
		return nil, e4s.ErrInvalidMaxConcurrentDownloads
	}

	buf, err := randbuf.NewBuffer(int(size))
	if err != nil {
		return nil, err
	}

	if logger == nil {
		logger = slog.Default()
	}

	srv := &Server{
		logger:          logger,
		buf:             buf,
		maxDownloadSize: maxDownloadSize,
		downloads:       make(chan struct{}, maxConcurrentDownloads),
	}

	return srv, nil
}
