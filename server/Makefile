GO := go
DOCKER := docker

VERSION ?= $(shell ../scripts/version.sh)
REVISION ?= $(shell ../scripts/revision.sh)
GOOS ?= $(shell $(GO) env GOOS)
IMAGE_NAME := ispnettest
IMAGE_TAG := $(or $(VERSION:v%=%),latest)

all: build

test:
	$(GO) test ./...

race:
	$(GO) test -race ./...

vet:
	$(GO) vet ./...

build:
	CGO_ENABLED=0 GOOS=$(GOOS) $(GO) build -o ./ispnettest -trimpath -ldflags="-s -w \
		-X 'github.com/pietro-a/isp-network-tester/server/internal/version.Version=$(VERSION)' \
		-X 'github.com/pietro-a/isp-network-tester/server/internal/version.Revision=$(REVISION)'"

docker:
	$(DOCKER) build --build-arg VERSION="$(VERSION)" --build-arg REVISION="$(REVISION)" -t $(IMAGE_NAME):$(IMAGE_TAG) .

clean:
	$(GO) clean ./...
	rm -f ispnettest

.PHONY: all test race vet build docker clean
