# ============================================================================
# Air Mouse Go Server - Complete Makefile
# ============================================================================

# Project information
PROJECT_NAME := airmouse-server
VERSION := 3.0.0
BUILD_TIME := $(shell date -u '+%Y-%m-%d_%H:%M:%S')
GIT_COMMIT := $(shell git rev-parse --short HEAD 2>/dev/null || echo "unknown")
GO_VERSION := $(shell go version | awk '{print $$3}')

# Build configuration
BINARY_NAME := airmouse-server
PKG := ./cmd/airmouse-server
GO_FILES := $(shell find . -name '*.go' -type f -not -path "./vendor/*" -not -path "./.git/*")

# Directories
BUILD_DIR := build
DIST_DIR := dist
GO_CACHE_DIR := /private/tmp/airmouse-go-build-cache
GO_MOD_CACHE_DIR := /private/tmp/airmouse-go-mod-cache

# Go environment
GO_ENV := GOCACHE=$(GO_CACHE_DIR) GOMODCACHE=$(GO_MOD_CACHE_DIR) CGO_ENABLED=1
GO_BUILD := go build -v -p 1
GO_TEST := go test -v -p 1
GO_CLEAN := go clean -cache -testcache -modcache

# Build flags
LDFLAGS := -ldflags "-s -w \
	-X main.Version=$(VERSION) \
	-X main.BuildTime=$(BUILD_TIME) \
	-X main.GitCommit=$(GIT_COMMIT) \
	-X main.GoVersion=$(GO_VERSION)"

# ============================================================================
# Colors for output
# ============================================================================
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
CYAN := \033[0;36m
NC := \033[0m # No Color

# ============================================================================
# Help - Default target
# ============================================================================
.PHONY: help
help: ## Show this help message
	@echo "$(CYAN)╔═══════════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(CYAN)║     Air Mouse Go Server - Makefile Commands                         ║$(NC)"
	@echo "$(CYAN)╚═══════════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(GREEN)Available targets:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(CYAN)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(YELLOW)Examples:$(NC)"
	@echo "  make build      - Build for current OS"
	@echo "  make run        - Build and run"
	@echo "  make test       - Run all tests"
	@echo "  make build-all  - Build for all platforms"

# ============================================================================
# Development targets
# ============================================================================
.PHONY: all
all: clean deps lint test build ## Clean, install deps, lint, test, and build

.PHONY: deps
deps: ## Install dependencies
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)📦 Installing dependencies...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	mkdir -p $(GO_CACHE_DIR) $(GO_MOD_CACHE_DIR)
	$(GO_ENV) go mod download
	$(GO_ENV) go mod tidy
	@echo "$(GREEN)✓ Dependencies installed$(NC)"

.PHONY: build
build: deps ## Build for current OS
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🔨 Building $(PROJECT_NAME) v$(VERSION)...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	$(GO_ENV) $(GO_BUILD) $(LDFLAGS) -o $(BINARY_NAME) $(PKG)
	@echo "$(GREEN)✓ Build complete: $(BINARY_NAME)$(NC)"
	@ls -lh $(BINARY_NAME)

.PHONY: run
run: build ## Build and run the server
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🚀 Starting Air Mouse Server...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	./$(BINARY_NAME)

.PHONY: dev
dev: ## Run with hot reload (requires air)
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🔄 Running in development mode...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	air

.PHONY: test
test: ## Run all tests
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🧪 Running tests...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	$(GO_ENV) $(GO_TEST) ./...
	@echo "$(GREEN)✓ All tests passed$(NC)"

.PHONY: test-cover
test-cover: ## Run tests with coverage report
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)📊 Running tests with coverage...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	$(GO_ENV) go test -coverprofile=coverage.out ./...
	$(GO_ENV) go tool cover -html=coverage.out -o coverage.html
	@echo "$(GREEN)✓ Coverage report: coverage.html$(NC)"

.PHONY: test-race
test-race: ## Run tests with race detector
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🏃 Running tests with race detector...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	$(GO_ENV) go test -race ./...

.PHONY: lint
lint: ## Run linter
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🔍 Running linter...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@if command -v golangci-lint >/dev/null 2>&1; then \
		golangci-lint run --timeout=5m; \
		echo "$(GREEN)✓ Linting complete$(NC)"; \
	else \
		echo "$(YELLOW)⚠ golangci-lint not installed. Run: go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest$(NC)"; \
	fi

.PHONY: fmt
fmt: ## Format code
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)📝 Formatting code...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	go fmt ./...
	@echo "$(GREEN)✓ Code formatted$(NC)"

.PHONY: vet
vet: ## Run go vet
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🔬 Running go vet...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	go vet ./...
	@echo "$(GREEN)✓ go vet complete$(NC)"

# ============================================================================
# Clean targets
# ============================================================================
.PHONY: clean
clean: ## Clean build artifacts
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🧹 Cleaning build artifacts...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	rm -f $(BINARY_NAME)
	rm -f $(BINARY_NAME).exe
	rm -f $(BINARY_NAME)-linux
	rm -f $(BINARY_NAME)-mac
	rm -f $(BINARY_NAME)-windows.exe
	rm -rf $(BUILD_DIR)
	rm -rf $(DIST_DIR)
	rm -f coverage.out coverage.html
	$(GO_CLEAN)
	@echo "$(GREEN)✓ Clean complete$(NC)"

.PHONY: clean-cache
clean-cache: ## Clean all caches
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🗑️ Cleaning caches...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	rm -rf $(GO_CACHE_DIR)
	rm -rf $(GO_MOD_CACHE_DIR)
	go clean -cache -testcache -modcache -fuzzcache
	@echo "$(GREEN)✓ Caches cleaned$(NC)"

.PHONY: clean-all
clean-all: clean clean-cache ## Clean everything (build + caches)

# ============================================================================
# Cross-platform builds
# ============================================================================
.PHONY: build-windows
build-windows: deps ## Build for Windows (64-bit)
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🪟 Building for Windows...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	$(GO_ENV) GOOS=windows GOARCH=amd64 $(GO_BUILD) $(LDFLAGS) -o $(BINARY_NAME).exe $(PKG)
	@echo "$(GREEN)✓ Windows build: $(BINARY_NAME).exe$(NC)"

.PHONY: build-linux
build-linux: deps ## Build for Linux (64-bit)
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🐧 Building for Linux...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	$(GO_ENV) GOOS=linux GOARCH=amd64 $(GO_BUILD) $(LDFLAGS) -o $(BINARY_NAME)-linux $(PKG)
	@echo "$(GREEN)✓ Linux build: $(BINARY_NAME)-linux$(NC)"

.PHONY: build-mac
build-mac: deps ## Build for macOS (64-bit)
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🍎 Building for macOS...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	$(GO_ENV) GOOS=darwin GOARCH=amd64 $(GO_BUILD) $(LDFLAGS) -o $(BINARY_NAME)-mac $(PKG)
	@echo "$(GREEN)✓ macOS build: $(BINARY_NAME)-mac$(NC)"

.PHONY: build-mac-arm
build-mac-arm: deps ## Build for macOS ARM64 (Apple Silicon)
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🍎 Building for macOS ARM64...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	$(GO_ENV) GOOS=darwin GOARCH=arm64 $(GO_BUILD) $(LDFLAGS) -o $(BINARY_NAME)-mac-arm $(PKG)
	@echo "$(GREEN)✓ macOS ARM64 build: $(BINARY_NAME)-mac-arm$(NC)"

.PHONY: build-all
build-all: build-windows build-linux build-mac build-mac-arm ## Build for all platforms
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)✅ All builds complete!$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@ls -lh $(BINARY_NAME)*

# ============================================================================
# Distribution targets
# ============================================================================
.PHONY: dist
dist: build-all ## Create distribution packages
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)📦 Creating distribution packages...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	mkdir -p $(DIST_DIR)
	# Windows
	zip -j $(DIST_DIR)/$(PROJECT_NAME)-windows-$(VERSION).zip $(BINARY_NAME).exe
	# Linux
	tar -czf $(DIST_DIR)/$(PROJECT_NAME)-linux-$(VERSION).tar.gz $(BINARY_NAME)-linux
	# macOS Intel
	tar -czf $(DIST_DIR)/$(PROJECT_NAME)-mac-$(VERSION).tar.gz $(BINARY_NAME)-mac
	# macOS ARM
	tar -czf $(DIST_DIR)/$(PROJECT_NAME)-mac-arm-$(VERSION).tar.gz $(BINARY_NAME)-mac-arm
	@echo "$(GREEN)✓ Distribution packages created in $(DIST_DIR)/$(NC)"

# ============================================================================
# Installation targets
# ============================================================================
.PHONY: install
install: build ## Install to GOPATH/bin
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)📦 Installing to GOPATH/bin...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	go install $(PKG)
	@echo "$(GREEN)✓ Installed$(NC)"

.PHONY: uninstall
uninstall: ## Remove from GOPATH/bin
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🗑️ Uninstalling...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	rm -f $(GOPATH)/bin/$(BINARY_NAME)
	@echo "$(GREEN)✓ Uninstalled$(NC)"

# ============================================================================
# Docker targets
# ============================================================================
.PHONY: docker-build
docker-build: ## Build Docker image
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🐳 Building Docker image...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	docker build -t $(PROJECT_NAME):$(VERSION) .
	docker tag $(PROJECT_NAME):$(VERSION) $(PROJECT_NAME):latest
	@echo "$(GREEN)✓ Docker image built$(NC)"

.PHONY: docker-run
docker-run: ## Run Docker container
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🐳 Running Docker container...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	docker run -p 8080:8080 -p 8081:8081 -p 8082:8082 $(PROJECT_NAME):latest

# ============================================================================
# Version targets
# ============================================================================
.PHONY: version
version: ## Show version information
	@echo "$(CYAN)Air Mouse Go Server$(NC)"
	@echo "  Version:     $(VERSION)"
	@echo "  Build Time:  $(BUILD_TIME)"
	@echo "  Git Commit:  $(GIT_COMMIT)"
	@echo "  Go Version:  $(GO_VERSION)"

# ============================================================================
# Info targets
# ============================================================================
.PHONY: info
info: ## Show environment information
	@echo "$(CYAN)Environment Information:$(NC)"
	@echo "  OS:           $(shell uname -s)"
	@echo "  Arch:         $(shell uname -m)"
	@echo "  Go version:   $(shell go version)"
	@echo "  Go env:       $(shell go env GOPATH)"
	@echo "  Project dir:  $(shell pwd)"

# ============================================================================
# Run with specific config
# ============================================================================
.PHONY: run-debug
run-debug: build ## Run with debug logging
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)🐛 Running in debug mode...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	DEBUG=true ./$(BINARY_NAME)

# ============================================================================
# Profile targets
# ============================================================================
.PHONY: profile
profile: build ## Run with CPU profiling
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	@echo "$(GREEN)📊 Running with CPU profiling...$(NC)"
	@echo "$(BLUE)━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$(NC)"
	./$(BINARY_NAME) -cpuprofile=cpu.prof

# ============================================================================
# Default target
# ============================================================================
.DEFAULT_GOAL := help