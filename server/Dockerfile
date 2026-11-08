FROM golang:1.27-alpine AS builder
ARG VERSION
ARG REVISION
WORKDIR /build
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN apk --no-cache add make && VERSION="$VERSION" REVISION="$REVISION" make build

FROM gcr.io/distroless/static-debian13:nonroot
COPY --from=builder /build/ispnettest /ispnettest
EXPOSE 8080
ENTRYPOINT ["/ispnettest"]