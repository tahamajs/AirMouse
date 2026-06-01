.PHONY: build run test clean deps lint build-windows build-linux build-mac build-all

BINARY_NAME=airmouse-server
GO_FILES=$(shell find . -name '*.go' -type f -not -path "./vendor/*")

# Install dependencies
deps:
	go mod download
	go mod tidy

# Build for current OS
build: deps
	go build -o $(BINARY_NAME) ./cmd/airmouse-server

# Run the server
run: build
	./$(BINARY_NAME)

# Run tests
test:
	go test -v ./...

# Clean build artifacts
clean:
	rm -f $(BINARY_NAME)
	go clean -cache

# Lint with golangci-lint (install first: go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest)
lint:
	golangci-lint run

# Cross‑platform builds
build-windows:
	GOOS=windows GOARCH=amd64 go build -o $(BINARY_NAME).exe ./cmd/airmouse-server

build-linux:
	GOOS=linux GOARCH=amd64 go build -o $(BINARY_NAME)-linux ./cmd/airmouse-server

build-mac:
	GOOS=darwin GOARCH=amd64 go build -o $(BINARY_NAME)-mac ./cmd/airmouse-server

# Build all platforms at once
build-all: build-windows build-linux build-mac