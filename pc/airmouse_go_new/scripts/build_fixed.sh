#!/bin/bash

cd /Users/tahamajs/Documents/uni/CPS/Files/ComputerAssignments/CA2/code/pc/airmouse_go_new

# Set environment
export GOPROXY=https://goproxy.io,direct
export GOSUMDB=off
export GO111MODULE=on
export CGO_ENABLED=1

echo "=== Cleaning ==="
go clean -modcache
rm -f go.sum

echo "=== Downloading dependencies ==="
go mod download 2>&1 | head -20

echo "=== Tidying modules ==="
go mod tidy

echo "=== Building ==="
go build -tags noai -v -o airmouse-server ./cmd/airmouse-server

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo "Run: ./airmouse-server"
else
    echo ""
    echo "❌ Build failed"
    exit 1
fi