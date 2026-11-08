package server

import (
	"io"
	"net/http"
	"strconv"
)

func (srv *Server) Health(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNoContent)
}

func (srv *Server) Download(w http.ResponseWriter, r *http.Request) {
	sizeStr := r.URL.Query().Get("size")
	size, err := strconv.ParseInt(sizeStr, 10, 64)
	if err != nil || size <= 0 {
		srv.logger.Info("invalid download size", "size", sizeStr)
		WriteJSONError(w, http.StatusBadRequest, "invalid download size")
		return
	}

	if size > srv.maxDownloadSize {
		srv.logger.Info("maximum download size exceeded", "size", size, "maxDownloadSize", srv.maxDownloadSize)
		WriteJSONError(w, http.StatusRequestEntityTooLarge, "maximum download size exceeded")
		return
	}

	select {
	case srv.downloads <- struct{}{}:
		defer func() {
			<-srv.downloads
		}()
	default:
		w.Header().Set("Retry-After", "5")
		WriteJSONError(w, http.StatusServiceUnavailable, "server is busy, try again later")
		return
	}

	w.Header().Set("Content-Type", "application/octet-stream")
	w.Header().Set("Content-Length", strconv.FormatInt(size, 10))
	w.Header().Set("Cache-Control", "no-store, no-transform")

	_, err = io.CopyN(w, srv.buf.NewReader(), size)
	if err != nil {
		srv.logger.Info("error sending data", "err", err)
		return
	}
}
