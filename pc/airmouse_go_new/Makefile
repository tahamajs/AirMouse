.PHONY: build run test clean deps lint build-windows build-linux build-mac build-all

BINARY_NAME=airmouse-server
GO_FILES=$(shell find . -name '*.go' -type f -not -path "./vendor/*")
GO_CACHE_DIR=/private/tmp/airmouse-go-build-cache
GO_MOD_CACHE_DIR=/private/tmp/airmouse-go-mod-cache
GO_ENV=GOCACHE=$(GO_CACHE_DIR) GOMODCACHE=$(GO_MOD_CACHE_DIR)
GO_BUILD=go build -p 1
GO_TEST=go test -p 1

# Install dependencies
deps:
	mkdir -p $(GO_CACHE_DIR) $(GO_MOD_CACHE_DIR)
	$(GO_ENV) go mod download
	$(GO_ENV) go mod tidy

# Build for current OS
build: deps
	$(GO_ENV) $(GO_BUILD) -o $(BINARY_NAME) ./cmd/airmouse-server

# Run the server
run: build
	./$(BINARY_NAME)

# Run tests
test:
	$(GO_ENV) $(GO_TEST) -v ./...

# Clean build artifacts
clean:
	rm -f $(BINARY_NAME)
	rm -rf $(GO_CACHE_DIR) $(GO_MOD_CACHE_DIR)
	go clean -cache

# Lint with golangci-lint (install first: go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest)
lint:
	golangci-lint run

# Cross‑platform builds
build-windows:
	$(GO_ENV) GOOS=windows GOARCH=amd64 $(GO_BUILD) -o $(BINARY_NAME).exe ./cmd/airmouse-server

build-linux:
	$(GO_ENV) GOOS=linux GOARCH=amd64 $(GO_BUILD) -o $(BINARY_NAME)-linux ./cmd/airmouse-server

build-mac:
	$(GO_ENV) GOOS=darwin GOARCH=amd64 $(GO_BUILD) -o $(BINARY_NAME)-mac ./cmd/airmouse-server

# Build all platforms at once
build-all: build-windows build-linux build-mac
