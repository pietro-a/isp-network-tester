# ISP Network Tester

A lightweight HTTP server for measuring download throughput to an ISP-hosted test endpoint. The server generates a reusable buffer of random data at startup and streams the requested number of bytes to each client.

The server is intended to run inside an ISP network. Measurements therefore reflect the path between the client and this server; they do not measure general Internet transit performance.

## Server

The server is implemented in Go and can be run directly or as a small Docker container.

### Requirements

- Go 1.27 or newer to build and run from source
- Docker, if you want to build and run the container image

### Run from source

From the `server` directory:

```sh
go run .
```

By default, the server listens on `0.0.0.0:8080`.

To build a standalone binary for your current operating system and architecture:

```sh
make build
./ispnettest
```

The Makefile also provides test and static-analysis targets. Run them before building if desired:

```sh
make test
make race
make vet
```

You can override the build target operating system with `GOOS`, for example, `make build GOOS=linux`. The resulting binary is written to `server/ispnettest`.

The server writes structured JSON logs to standard output and handles `SIGINT` and `SIGTERM` for graceful shutdown.

### Run with Docker

Build the image from the `server` directory:

```sh
make docker
```

This builds the image tagged `ispnettest:latest` by default. If the repository has an exact matching Git version tag, that version is used as the image tag (without the leading `v`).

Run it:

```sh
docker run --rm --name ispnettest -p 8080:8080 ispnettest:latest
```

Override configuration with environment variables:

```sh
docker run --rm --name ispnettest \
  -p 8080:8080 \
  -e LISTEN_ADDR=:8080 \
  -e MAX_DOWNLOAD_SIZE=2GiB \
  ispnettest:latest
```

The image uses a non-root user and a distroless runtime image. It does not include a shell or tools such as `curl`; use an HTTP health probe from the container platform (for example, a Kubernetes probe) rather than a shell-based Docker health check.

### Configuration

All settings are optional environment variables. The defaults are:

| Variable | Default | Description |
| --- | --- | --- |
| `LISTEN_ADDR` | `:8080` | HTTP listen address. A value such as `:8080` listens on all interfaces. |
| `BUFFER_SIZE` | `16MiB` | Size of the random data buffer generated at startup. |
| `MAX_DOWNLOAD_SIZE` | `1GiB` | Largest download a client may request. |
| `MAX_CONCURRENT_DOWNLOADS` | `4` | Maximum number of simultaneous download requests. |
| `READ_HEADER_TIMEOUT` | `5s` | Maximum time to read request headers. |
| `IDLE_TIMEOUT` | `30s` | Maximum time to wait for the next request on an idle keep-alive connection. |
| `SHUTDOWN_TIMEOUT` | `10s` | Graceful shutdown timeout. |
| `LOG_LEVEL` | `info` | Log level: `debug`, `info`, `warn`, or `error`. |

Sizes accept human-readable byte units (for example, `16MiB`, `1GiB`). Timeout values accept duration strings (for example, `5s`, `1m`). Invalid values cause the server to exit with a configuration error.

The random buffer is allocated once at startup. Each download streams from that shared buffer, repeating its contents as needed to produce the requested byte count. Increasing `BUFFER_SIZE` increases the server's memory usage.

### HTTP endpoints

#### `GET /healthz`

Returns `204 No Content` when the server is running.

```sh
curl -i http://localhost:8080/healthz
```

#### `GET /download?size=<bytes>`

Streams exactly the requested number of bytes, subject to `MAX_DOWNLOAD_SIZE` and the concurrency limit.

For example, download 100 MiB and discard the response body:

```sh
curl --fail --output /dev/null \
  "http://localhost:8080/download?size=104857600"
```

The response uses `application/octet-stream`, includes `Content-Length`, and disables caching and transformation. The `size` parameter is a positive integer number of bytes (decimal units can be calculated as 1 MB = 1,000,000 bytes).

Possible error responses include:

- `400 Bad Request` — missing, invalid, or non-positive `size`.
- `413 Content Too Large` — requested size exceeds `MAX_DOWNLOAD_SIZE`.
- `503 Service Unavailable` — all download slots are in use. The response includes `Retry-After: 5`.

### Deployment notes

- Restrict access to the download endpoint with network policy, firewall rules, or ingress controls. It is designed to transfer large amounts of data and should not be exposed as an unrestricted public bandwidth endpoint.
- If deployed behind a proxy or ingress, disable response compression and caching for `/download`.
- Configure platform-level HTTP liveness and readiness probes against `/healthz`. For Kubernetes, use an HTTP `httpGet` probe on port `8080`.
- The server's throughput result measures the network path to this server only.
