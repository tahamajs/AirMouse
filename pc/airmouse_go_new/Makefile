# Makefile for Air Mouse Pro Server

.PHONY: build build-windows build-linux build-mac clean run test install

# Variables
BINARY_NAME=airmouse-server
VERSION=3.0.0
BUILD_TIME=$(shell date -u +%Y-%m-%dT%H:%M:%SZ)
GIT_COMMIT=$(shell git rev-parse --short HEAD 2>/dev/null || echo "unknown")
LDFLAGS=-ldflags "-X main.version=$(VERSION) -X main.buildTime=$(BUILD_TIME) -X main.gitCommit=$(GIT_COMMIT)"

# Build for current platform
build:
	go build $(LDFLAGS) -o $(BINARY_NAME) ./cmd/airmouse-server

# Build for Windows
build-windows:
	GOOS=windows GOARCH=amd64 go build $(LDFLAGS) -o $(BINARY_NAME).exe ./cmd/airmouse-server

# Build for Linux
build-linux:
	GOOS=linux GOARCH=amd64 go build $(LDFLAGS) -o $(BINARY_NAME)-linux ./cmd/airmouse-server

# Build for macOS
build-mac:
	GOOS=darwin GOARCH=amd64 go build $(LDFLAGS) -o $(BINARY_NAME)-mac ./cmd/airmouse-server

# Build for macOS ARM64 (Apple Silicon)
build-mac-arm:
	GOOS=darwin GOARCH=arm64 go build $(LDFLAGS) -o $(BINARY_NAME)-mac-arm ./cmd/airmouse-server

# Build all platforms
build-all: build-windows build-linux build-mac build-mac-arm

# Run the application
run:
	go run ./cmd/airmouse-server

# Run with debug
run-debug:
	go run ./cmd/airmouse-server --log-level=debug

# Clean build artifacts
clean:
	rm -f $(BINARY_NAME) $(BINARY_NAME).exe $(BINARY_NAME)-* 
	rm -rf dist/

# Run tests
test:
	go test -v ./...

# Run tests with coverage
test-coverage:
	go test -v -coverprofile=coverage.out ./...
	go tool cover -html=coverage.out -o coverage.html

# Run benchmarks
bench:
	go test -bench=. -benchmem ./...

# Install dependencies
deps:
	go mod download
	go mod tidy

# Lint code
lint:
	golangci-lint run

# Format code
fmt:
	go fmt ./...

# Create distribution
dist: clean build-all
	mkdir -p dist
	mv $(BINARY_NAME)-* dist/
	cp README.md dist/
	cp LICENSE dist/

# Install system-wide
install: build
	sudo cp $(BINARY_NAME) /usr/local/bin/

# Uninstall
uninstall:
	sudo rm -f /usr/local/bin/$(BINARY_NAME)

# Help
help:
	@echo "Available targets:"
	@echo "  build          - Build for current platform"
	@echo "  build-windows  - Build for Windows"
	@echo "  build-linux    - Build for Linux"
	@echo "  build-mac      - Build for macOS"
	@echo "  build-mac-arm  - Build for macOS ARM64"
	@echo "  build-all      - Build for all platforms"
	@echo "  run            - Run the application"
	@echo "  run-debug      - Run with debug logging"
	@echo "  clean          - Clean build artifacts"
	@echo "  test           - Run tests"
	@echo "  test-coverage  - Run tests with coverage"
	@echo "  bench          - Run benchmarks"
	@echo "  deps           - Download dependencies"
	@echo "  lint           - Lint code"
	@echo "  fmt            - Format code"
	@echo "  dist           - Create distribution"
	@echo "  install        - Install system-wide"
	@echo "  uninstall      - Uninstall"