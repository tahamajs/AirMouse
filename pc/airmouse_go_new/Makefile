# Air Mouse Pro Server Makefile

.PHONY: build build-windows build-linux build-mac build-mac-arm build-all clean run run-debug test test-coverage bench deps lint fmt dist install uninstall docker-build docker-run docs help

# Variables
BINARY_NAME=airmouse-server
VERSION=3.0.0
BUILD_TIME=$(shell date -u +%Y-%m-%dT%H:%M:%SZ)
GIT_COMMIT=$(shell git rev-parse --short HEAD 2>/dev/null || echo "unknown")
LDFLAGS=-ldflags "-X main.version=$(VERSION) -X main.buildTime=$(BUILD_TIME) -X main.gitCommit=$(GIT_COMMIT)"

# Go environment
GOPROXY?=https://proxy.golang.org,direct
GOSUMDB?=sum.golang.org
GO111MODULE=on

# Colors
GREEN=\033[0;32m
YELLOW=\033[1;33m
BLUE=\033[0;34m
RED=\033[0;31m
NC=\033[0m

# Default target
all: deps build

# Set up Go environment
setup:
	@echo "$(BLUE)Setting up Go environment...$(NC)"
	@export GOPROXY=$(GOPROXY)
	@export GOSUMDB=$(GOSUMDB)
	@export GO111MODULE=$(GO111MODULE)
	@echo "$(GREEN)Go environment configured$(NC)"

# Install dependencies
deps: setup
	@echo "$(BLUE)Downloading dependencies...$(NC)"
	go mod download
	go mod verify
	go mod tidy
	@echo "$(GREEN)Dependencies installed$(NC)"

# Build for current platform
build: setup
	@echo "$(BLUE)Building for current platform...$(NC)"
	CGO_ENABLED=1 go build $(LDFLAGS) -o $(BINARY_NAME) ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)$(NC)"

# Build without AI (safer for first build)
build-noai: setup
	@echo "$(BLUE)Building without AI features...$(NC)"
	CGO_ENABLED=1 go build -tags noai $(LDFLAGS) -o $(BINARY_NAME)-noai ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-noai$(NC)"

# Build for Windows
build-windows: setup
	@echo "$(BLUE)Building for Windows...$(NC)"
	GOOS=windows GOARCH=amd64 CGO_ENABLED=1 go build $(LDFLAGS) -o $(BINARY_NAME).exe ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME).exe$(NC)"

# Build for Linux (AMD64)
build-linux: setup
	@echo "$(BLUE)Building for Linux AMD64...$(NC)"
	GOOS=linux GOARCH=amd64 CGO_ENABLED=1 go build $(LDFLAGS) -o $(BINARY_NAME)-linux-amd64 ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-linux-amd64$(NC)"

# Build for Linux (ARM64)
build-linux-arm: setup
	@echo "$(BLUE)Building for Linux ARM64...$(NC)"
	GOOS=linux GOARCH=arm64 CGO_ENABLED=1 go build $(LDFLAGS) -o $(BINARY_NAME)-linux-arm64 ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-linux-arm64$(NC)"

# Build for Linux (ARM)
build-linux-arm7: setup
	@echo "$(BLUE)Building for Linux ARMv7...$(NC)"
	GOOS=linux GOARCH=arm GOARM=7 CGO_ENABLED=1 go build $(LDFLAGS) -o $(BINARY_NAME)-linux-armv7 ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-linux-armv7$(NC)"

# Build for macOS (Intel)
build-mac: setup
	@echo "$(BLUE)Building for macOS Intel...$(NC)"
	GOOS=darwin GOARCH=amd64 CGO_ENABLED=1 go build $(LDFLAGS) -o $(BINARY_NAME)-darwin-amd64 ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-darwin-amd64$(NC)"

# Build for macOS (Apple Silicon)
build-mac-arm: setup
	@echo "$(BLUE)Building for macOS Apple Silicon...$(NC)"
	GOOS=darwin GOARCH=arm64 CGO_ENABLED=1 go build $(LDFLAGS) -o $(BINARY_NAME)-darwin-arm64 ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-darwin-arm64$(NC)"

# Build for FreeBSD
build-freebsd: setup
	@echo "$(BLUE)Building for FreeBSD...$(NC)"
	GOOS=freebsd GOARCH=amd64 CGO_ENABLED=1 go build $(LDFLAGS) -o $(BINARY_NAME)-freebsd-amd64 ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-freebsd-amd64$(NC)"

# Build all platforms
build-all: build-windows build-linux build-linux-arm build-linux-arm7 build-mac build-mac-arm build-freebsd
	@echo "$(GREEN)All builds complete$(NC)"

# Run the application
run: setup
	@echo "$(BLUE)Running Air Mouse Pro Server...$(NC)"
	go run ./cmd/airmouse-server

# Run with debug
run-debug: setup
	@echo "$(BLUE)Running with debug logging...$(NC)"
	go run ./cmd/airmouse-server --log-level=debug

# Clean build artifacts
clean:
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	rm -f $(BINARY_NAME) $(BINARY_NAME).exe $(BINARY_NAME)-* 
	rm -rf dist/
	rm -f coverage.out coverage.html
	rm -f airmouse.log
	@echo "$(GREEN)Clean complete$(NC)"

# Run tests
test: setup
	@echo "$(BLUE)Running tests...$(NC)"
	go test -v -race -timeout 30s ./...

# Run tests with coverage
test-coverage: setup
	@echo "$(BLUE)Running tests with coverage...$(NC)"
	go test -v -coverprofile=coverage.out -covermode=atomic ./...
	go tool cover -html=coverage.out -o coverage.html
	@echo "$(GREEN)Coverage report: coverage.html$(NC)"

# Run tests with race detection
test-race: setup
	@echo "$(BLUE)Running tests with race detection...$(NC)"
	go test -v -race -timeout 30s ./...

# Run benchmarks
bench: setup
	@echo "$(BLUE)Running benchmarks...$(NC)"
	go test -bench=. -benchmem -run=^$$ ./...

# Lint code
lint: setup
	@echo "$(BLUE)Linting code...$(NC)"
	@if command -v golangci-lint >/dev/null 2>&1; then \
		golangci-lint run --timeout=5m; \
	else \
		echo "$(YELLOW)golangci-lint not installed, skipping...$(NC)"; \
		echo "Install with: brew install golangci-lint or curl -sSfL https://raw.githubusercontent.com/golangci/golangci-lint/master/install.sh | sh -s -- -b $(go env GOPATH)/bin"; \
	fi
	@echo "$(GREEN)Linting complete$(NC)"

# Format code
fmt: setup
	@echo "$(BLUE)Formatting code...$(NC)"
	go fmt ./...
	@echo "$(GREEN)Formatting complete$(NC)"

# Vet code
vet: setup
	@echo "$(BLUE)Vetting code...$(NC)"
	go vet ./...
	@echo "$(GREEN)Vetting complete$(NC)"

# Create distribution
dist: clean build-all
	@echo "$(BLUE)Creating distribution...$(NC)"
	mkdir -p dist
	mv $(BINARY_NAME)-* dist/
	cp README.md dist/
	cp LICENSE dist/
	cp config.example.json dist/
	cd dist && sha256sum * > checksums.txt
	@echo "$(GREEN)Distribution created in dist/$(NC)"

# Install system-wide
install: build
	@echo "$(BLUE)Installing system-wide...$(NC)"
	sudo cp $(BINARY_NAME) /usr/local/bin/
	@echo "$(GREEN)Installed to /usr/local/bin/$(BINARY_NAME)$(NC)"

# Uninstall
uninstall:
	@echo "$(YELLOW)Removing from system...$(NC)"
	sudo rm -f /usr/local/bin/$(BINARY_NAME)
	@echo "$(GREEN)Removed$(NC)"

# Docker build
docker-build:
	@echo "$(BLUE)Building Docker image...$(NC)"
	docker build -t airmouse/airmouse-server:$(VERSION) -t airmouse/airmouse-server:latest .
	@echo "$(GREEN)Docker image built$(NC)"

# Docker build with no-cache
docker-build-nocache:
	@echo "$(BLUE)Building Docker image (no cache)...$(NC)"
	docker build --no-cache -t airmouse/airmouse-server:$(VERSION) -t airmouse/airmouse-server:latest .
	@echo "$(GREEN)Docker image built$(NC)"

# Docker run
docker-run:
	@echo "$(BLUE)Running Docker container...$(NC)"
	docker run -d \
		--name airmouse-server \
		-p 8080:8080 \
		-p 8081:8081 \
		-p 8082:8082/udp \
		-v airmouse-data:/var/lib/airmouse \
		-v airmouse-logs:/var/log/airmouse \
		airmouse/airmouse-server:latest
	@echo "$(GREEN)Docker container started$(NC)"

# Docker stop and remove
docker-stop:
	@echo "$(YELLOW)Stopping and removing Docker container...$(NC)"
	docker stop airmouse-server 2>/dev/null || true
	docker rm airmouse-server 2>/dev/null || true
	@echo "$(GREEN)Docker container removed$(NC)"

# Docker compose up
docker-compose-up:
	@echo "$(BLUE)Starting Docker Compose...$(NC)"
	docker-compose up -d
	@echo "$(GREEN)Docker Compose started$(NC)"

# Docker compose down
docker-compose-down:
	@echo "$(YELLOW)Stopping Docker Compose...$(NC)"
	docker-compose down
	@echo "$(GREEN)Docker Compose stopped$(NC)"

# Generate documentation
docs:
	@echo "$(BLUE)Generating documentation...$(NC)"
	go doc -all ./...
	@echo "$(GREEN)Documentation generated$(NC)"

# Generate swagger docs
swagger:
	@echo "$(BLUE)Generating Swagger documentation...$(NC)"
	@if command -v swag >/dev/null 2>&1; then \
		swag init -g cmd/airmouse-server/main.go; \
	else \
		echo "$(YELLOW)swag not installed, skipping...$(NC)"; \
		echo "Install with: go install github.com/swaggo/swag/cmd/swag@latest"; \
	fi
	@echo "$(GREEN)Swagger docs generated$(NC)"

# Create release
release: clean test build-all
	@echo "$(BLUE)Creating release...$(NC)"
	mkdir -p release
	@for file in $(BINARY_NAME)-*; do \
		if [ -f "$$file" ]; then \
			zip -r release/$$file.zip $$file; \
		fi \
	done
	cp config.example.json release/
	cp README.md release/
	cp LICENSE release/
	@echo "$(GREEN)Release created in release/$(NC)"

# Quick build (no AI, no CGO)
quick:
	@echo "$(BLUE)Quick build (no AI, no CGO)...$(NC)"
	CGO_ENABLED=0 go build -tags noai -o $(BINARY_NAME)-quick ./cmd/airmouse-server
	@echo "$(GREEN)Quick build complete: $(BINARY_NAME)-quick$(NC)"

# Show version
version:
	@echo "Air Mouse Pro Server v$(VERSION)"
	@echo "Build Time: $(BUILD_TIME)"
	@echo "Git Commit: $(GIT_COMMIT)"

# Check dependencies
check-deps:
	@echo "$(BLUE)Checking dependencies...$(NC)"
	@go mod verify
	@echo "$(GREEN)All dependencies verified$(NC)"

# Update dependencies
update-deps:
	@echo "$(BLUE)Updating dependencies...$(NC)"
	go get -u ./...
	go mod tidy
	@echo "$(GREEN)Dependencies updated$(NC)"

# Show help
help:
	@echo "$(BLUE)Air Mouse Pro Server - Makefile Commands$(NC)"
	@echo ""
	@echo "$(GREEN)Build Commands:$(NC)"
	@echo "  make build           - Build for current platform"
	@echo "  make build-noai      - Build without AI features"
	@echo "  make build-windows   - Build for Windows"
	@echo "  make build-linux     - Build for Linux AMD64"
	@echo "  make build-linux-arm - Build for Linux ARM64"
	@echo "  make build-mac       - Build for macOS Intel"
	@echo "  make build-mac-arm   - Build for macOS Apple Silicon"
	@echo "  make build-all       - Build for all platforms"
	@echo "  make quick           - Quick build (no AI, no CGO)"
	@echo ""
	@echo "$(GREEN)Run Commands:$(NC)"
	@echo "  make run             - Run the application"
	@echo "  make run-debug       - Run with debug logging"
	@echo ""
	@echo "$(GREEN)Test Commands:$(NC)"
	@echo "  make test            - Run tests"
	@echo "  make test-coverage   - Run tests with coverage"
	@echo "  make test-race       - Run tests with race detection"
	@echo "  make bench           - Run benchmarks"
	@echo ""
	@echo "$(GREEN)Code Quality:$(NC)"
	@echo "  make lint            - Lint code"
	@echo "  make fmt             - Format code"
	@echo "  make vet             - Vet code"
	@echo ""
	@echo "$(GREEN)Docker Commands:$(NC)"
	@echo "  make docker-build    - Build Docker image"
	@echo "  make docker-run      - Run Docker container"
	@echo "  make docker-stop     - Stop Docker container"
	@echo "  make docker-compose-up   - Start Docker Compose"
	@echo "  make docker-compose-down - Stop Docker Compose"
	@echo ""
	@echo "$(GREEN)Distribution:$(NC)"
	@echo "  make dist            - Create distribution package"
	@echo "  make release         - Create release packages"
	@echo "  make install         - Install system-wide"
	@echo "  make uninstall       - Uninstall"
	@echo ""
	@echo "$(GREEN)Maintenance:$(NC)"
	@echo "  make clean           - Clean build artifacts"
	@echo "  make deps            - Install dependencies"
	@echo "  make update-deps     - Update dependencies"
	@echo "  make check-deps      - Check dependencies"
	@echo "  make docs            - Generate documentation"
	@echo "  make swagger         - Generate Swagger docs"
	@echo "  make version         - Show version info"
	@echo "  make help            - Show this help"