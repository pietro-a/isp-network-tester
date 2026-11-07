package e4s

import "errors"

var (
	ErrZeroByteSize                  = errors.New("zero byte size")
	ErrZeroTimeout                   = errors.New("zero timeout")
	ErrUnknownLogLevel               = errors.New("unknown log level")
	ErrInvalidBufSize                = errors.New("invalid buffer size")
	ErrInvalidMaxDownloadSize        = errors.New("invalid maximum download size")
	ErrInvalidMaxConcurrentDownloads = errors.New("invalid maximum concurrent downloads")
)
