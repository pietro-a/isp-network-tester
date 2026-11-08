package randbuf

import (
	"crypto/rand"

	"github.com/pietro-a/isp-network-tester/server/internal/e4s"
)

type Buffer struct {
	data []byte
}

func NewBuffer(size int) (*Buffer, error) {
	if size <= 0 {
		return nil, e4s.ErrInvalidBufSize
	}

	data := make([]byte, size)
	if _, err := rand.Read(data); err != nil {
		return nil, err
	}

	return &Buffer{data: data}, nil
}

func (b *Buffer) NewReader() *Reader {
	return &Reader{data: b.data}
}

type Reader struct {
	data []byte
	pos  int
}

func (r *Reader) Read(p []byte) (int, error) {
	if len(p) == 0 {
		return 0, nil
	}

	n := 0
	for n < len(p) {
		copied := copy(p[n:], r.data[r.pos:])
		n += copied
		r.pos = (r.pos + copied) % len(r.data)
	}

	return n, nil
}
