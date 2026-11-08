package randbuf

import (
	"bytes"
	"errors"
	"fmt"
	"testing"

	"github.com/pietro-a/isp-network-tester/server/internal/e4s"
)

func TestNewBufferRejectsInvalidBufferSizes(t *testing.T) {
	for _, size := range []int{0, -1, -1024} {
		t.Run(fmt.Sprintf("size_%d", size), func(t *testing.T) {
			buf, err := NewBuffer(size)
			if buf != nil {
				t.Fatalf("NewBuffer(%d) returned a non-nil buffer", size)
			}
			if !errors.Is(err, e4s.ErrInvalidBufSize) {
				t.Fatalf("NewBuffer(%d) error = %v, want %v", size, err, e4s.ErrInvalidBufSize)
			}
		})
	}
}

func TestNewBufferCreatesReaderForPositiveBufferSize(t *testing.T) {
	buf, err := NewBuffer(8)
	if err != nil {
		t.Fatalf("NewBuffer(8) returned error: %v", err)
	}
	if buf == nil {
		t.Fatal("NewBuffer(8) returned a nil buffer")
	}
	if got := len(buf.data); got != 8 {
		t.Fatalf("buffer length = %d, want 8", got)
	}
}

func TestReadReturnsRequestedBytesAndRepeatsBuffer(t *testing.T) {
	buf, err := NewBuffer(7)
	if err != nil {
		t.Fatalf("NewBuffer(7) returned error: %v", err)
	}

	wantCycle := append([]byte(nil), buf.data...)
	got := make([]byte, len(wantCycle)*3+2)
	reader := buf.NewReader()

	n, err := reader.Read(got)
	if err != nil {
		t.Fatalf("Read() returned error: %v", err)
	}
	if n != len(got) {
		t.Fatalf("Read() = %d bytes, want %d", n, len(got))
	}

	want := make([]byte, len(got))
	for i := range want {
		want[i] = wantCycle[i%len(wantCycle)]
	}
	if !bytes.Equal(got, want) {
		t.Fatalf("Read() did not repeat the buffer correctly")
	}
}

func TestReadSupportsDifferentChunkSizes(t *testing.T) {
	buf, err := NewBuffer(5)
	if err != nil {
		t.Fatalf("NewBuffer(5) returned error: %v", err)
	}
	wantCycle := append([]byte(nil), buf.data...)

	chunkSizes := []int{2, 1, 7, 0, 3, 9}
	var got []byte
	reader := buf.NewReader()

	for _, size := range chunkSizes {
		chunk := make([]byte, size)
		n, err := reader.Read(chunk)
		if err != nil {
			t.Fatalf("Read(%d-byte slice) returned error: %v", size, err)
		}
		if n != size {
			t.Fatalf("Read(%d-byte slice) = %d, want %d", size, n, size)
		}
		got = append(got, chunk...)
	}

	want := make([]byte, len(got))
	for i := range want {
		want[i] = wantCycle[i%len(wantCycle)]
	}
	if !bytes.Equal(got, want) {
		t.Fatalf("successive reads did not preserve stream position")
	}
}

func TestReadWithEmptySliceDoesNotAdvance(t *testing.T) {
	buf, err := NewBuffer(4)
	if err != nil {
		t.Fatalf("NewBuffer(4) returned error: %v", err)
	}

	reader := buf.NewReader()

	first := make([]byte, 2)
	if _, err := reader.Read(first); err != nil {
		t.Fatalf("initial Read() returned error: %v", err)
	}

	n, err := reader.Read(nil)
	if err != nil {
		t.Fatalf("Read(nil) returned error: %v", err)
	}
	if n != 0 {
		t.Fatalf("Read(nil) = %d, want 0", n)
	}

	next := make([]byte, 2)
	if _, err := reader.Read(next); err != nil {
		t.Fatalf("subsequent Read() returned error: %v", err)
	}
	if !bytes.Equal(next, buf.data[2:4]) {
		t.Fatalf("Read(nil) advanced the reader position")
	}
}

func TestReadFromTwoReaders(t *testing.T) {
	buf, err := NewBuffer(4)
	if err != nil {
		t.Fatalf("NewBuffer(4) returned error: %v", err)
	}

	reader1 := buf.NewReader()
	reader2 := buf.NewReader()

	buf1 := make([]byte, 2)
	if _, err := reader1.Read(buf1); err != nil {
		t.Fatalf("Read() to the first buffer returned error: %v", err)
	}
	if !bytes.Equal(buf1, buf.data[:2]) {
		t.Fatalf("first read did not preserve stream position")
	}

	buf2 := make([]byte, 3)
	if _, err := reader2.Read(buf2); err != nil {
		t.Fatalf("Read() to the second buffer returned error: %v", err)
	}
	if !bytes.Equal(buf2, buf.data[:3]) {
		t.Fatalf("second read did not preserve stream position")
	}
}
