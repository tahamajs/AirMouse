# Air Mouse Pro Server Makefile

.PHONY: build build-windows build-linux build-mac clean run test install deps lint fmt help

# Variables
BINARY_NAME=airmouse-server
VERSION=3.0.0
BUILD_TIME=$(shell date -u +%Y-%m-%dT%H:%M:%SZ)
GIT_COMMIT=$(shell git rev-parse --short HEAD 2>/dev/null || echo "unknown")
LDFLAGS=-ldflags "-X main.version=$(VERSION) -X main.buildTime=$(BUILD_TIME) -X main.gitCommit=$(GIT_COMMIT)"

# Colors
GREEN=\033[0;32m
YELLOW=\033[1;33m
BLUE=\033[0;34m
NC=\033[0m

# Default target
all: deps build

# Build for current platform
build:
	@echo "$(BLUE)Building for current platform...$(NC)"
	go build $(LDFLAGS) -o $(BINARY_NAME) ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)$(NC)"

# Build for Windows
build-windows:
	@echo "$(BLUE)Building for Windows...$(NC)"
	GOOS=windows GOARCH=amd64 go build $(LDFLAGS) -o $(BINARY_NAME).exe ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME).exe$(NC)"

# Build for Linux
build-linux:
	@echo "$(BLUE)Building for Linux...$(NC)"
	GOOS=linux GOARCH=amd64 go build $(LDFLAGS) -o $(BINARY_NAME)-linux ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-linux$(NC)"

# Build for macOS (Intel)
build-mac:
	@echo "$(BLUE)Building for macOS Intel...$(NC)"
	GOOS=darwin GOARCH=amd64 go build $(LDFLAGS) -o $(BINARY_NAME)-mac ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-mac$(NC)"

# Build for macOS (Apple Silicon)
build-mac-arm:
	@echo "$(BLUE)Building for macOS Apple Silicon...$(NC)"
	GOOS=darwin GOARCH=arm64 go build $(LDFLAGS) -o $(BINARY_NAME)-mac-arm ./cmd/airmouse-server
	@echo "$(GREEN)Build complete: $(BINARY_NAME)-mac-arm$(NC)"

# Build all platforms
build-all: build-windows build-linux build-mac build-mac-arm
	@echo "$(GREEN)All builds complete$(NC)"

# Run the application
run:
	@echo "$(BLUE)Running Air Mouse Pro Server...$(NC)"
	go run ./cmd/airmouse-server

# Run with debug
run-debug:
	@echo "$(BLUE)Running with debug logging...$(NC)"
	go run ./cmd/airmouse-server --log-level=debug

# Clean build artifacts
clean:
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	rm -f $(BINARY_NAME) $(BINARY_NAME).exe $(BINARY_NAME)-* 
	rm -rf dist/
	@echo "$(GREEN)Clean complete$(NC)"

# Run tests
test:
	@echo "$(BLUE)Running tests...$(NC)"
	go test -v ./...

# Run tests with coverage
test-coverage:
	@echo "$(BLUE)Running tests with coverage...$(NC)"
	go test -v -coverprofile=coverage.out ./...
	go tool cover -html=coverage.out -o coverage.html
	@echo "$(GREEN)Coverage report: coverage.html$(NC)"

# Run benchmarks
bench:
	@echo "$(BLUE)Running benchmarks...$(NC)"
	go test -bench=. -benchmem ./...

# Install dependencies
deps:
	@echo "$(BLUE)Downloading dependencies...$(NC)"
	go mod download
	go mod tidy
	@echo "$(GREEN)Dependencies installed$(NC)"

# Lint code
lint:
	@echo "$(BLUE)Linting code...$(NC)"
	golangci-lint run
	@echo "$(GREEN)Linting complete$(NC)"

# Format code
fmt:
	@echo "$(BLUE)Formatting code...$(NC)"
	go fmt ./...
	@echo "$(GREEN)Formatting complete$(NC)"

# Create distribution
dist: clean build-all
	@echo "$(BLUE)Creating distribution...$(NC)"
	mkdir -p dist
	mv $(BINARY_NAME)-* dist/
	cp README.md dist/
	cp LICENSE dist/
	cp config.example.json dist/
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

# Docker run
docker-run:
	@echo "$(BLUE)Running Docker container...$(NC)"
	docker run -d --name airmouse-server -p 8080:8080 -p 8081:8081 -p 8082:8082/udp airmouse/airmouse-server:latest
	@echo "$(GREEN)Docker container started$(NC)"

# Generate documentation
docs:
	@echo "$(BLUE)Generating documentation...$(NC)"
	go doc -all ./...
	@echo "$(GREEN)Documentation generated$(NC)"

# Show help
help:
	@echo "$(BLUE)Available targets:$(NC)"
	@echo "  $(GREEN)build$(NC)          - Build for current platform"
	@echo "  $(GREEN)build-windows$(NC)  - Build for Windows"
	@echo "  $(GREEN)build-linux$(NC)    - Build for Linux"
	@echo "  $(GREEN)build-mac$(NC)      - Build for macOS Intel"
	@echo "  $(GREEN)build-mac-arm$(NC)  - Build for macOS Apple Silicon"
	@echo "  $(GREEN)build-all$(NC)      - Build for all platforms"
	@echo "  $(GREEN)run$(NC)            - Run the application"
	@echo "  $(GREEN)run-debug$(NC)      - Run with debug logging"
	@echo "  $(GREEN)clean$(NC)          - Clean build artifacts"
	@echo "  $(GREEN)test$(NC)           - Run tests"
	@echo "  $(GREEN)test-coverage$(NC)  - Run tests with coverage"
	@echo "  $(GREEN)bench$(NC)          - Run benchmarks"
	@echo "  $(GREEN)deps$(NC)           - Install dependencies"
	@echo "  $(GREEN)lint$(NC)           - Lint code"
	@echo "  $(GREEN)fmt$(NC)            - Format code"
	@echo "  $(GREEN)dist$(NC)           - Create distribution"
	@echo "  $(GREEN)install$(NC)        - Install system-wide"
	@echo "  $(GREEN)uninstall$(NC)      - Uninstall"
	@echo "  $(GREEN)docker-build$(NC)   - Build Docker image"
	@echo "  $(GREEN)docker-run$(NC)     - Run Docker container"
	@echo "  $(GREEN)docs$(NC)           - Generate documentation"
	@echo "  $(GREEN)help$(NC)           - Show this help"