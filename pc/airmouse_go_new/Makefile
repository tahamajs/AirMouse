.PHONY: build run test clean deps

BINARY_NAME=airmouse-server
GO_FILES=$(shell find . -name '*.go' -type f -not -path "./vendor/*")

deps:
	go mod download
	go mod tidy

build: deps
	go build -o $(BINARY_NAME) ./cmd/airmouse-server

run: build
	./$(BINARY_NAME)

test:
	go test -v ./...

clean:
	rm -f $(BINARY_NAME)
	go clean -cache

lint:
	golangci-lint run

# Cross‑platform builds (optional)
build-windows:
	GOOS=windows GOARCH=amd64 go build -o $(BINARY_NAME).exe ./cmd/airmouse-server

build-linux:
	GOOS=linux GOARCH=amd64 go build -o $(BINARY_NAME)-linux ./cmd/airmouse-server

build-mac:
	GOOS=darwin GOARCH=amd64 go build -o $(BINARY_NAME)-mac ./cmd/airmouse-server