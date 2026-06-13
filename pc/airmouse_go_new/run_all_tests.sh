#!/bin/bash

echo "=========================================="
echo "Air Mouse Go Server - Complete Test Suite"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if server is running
if ! curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo -e "${YELLOW}Starting server...${NC}"
    ./airmouse-server &
    SERVER_PID=$!
    sleep 3
else
    SERVER_PID=""
fi

# Run all tests with coverage
echo -e "${GREEN}Running Unit Tests...${NC}"
go test -v -cover ./internal/protocol/... 2>&1 | tee test_results.txt

echo ""
echo -e "${GREEN}Running Integration Tests...${NC}"
go test -v -timeout 60s ./internal/integration/... 2>&1 | tee -a test_results.txt

echo ""
echo -e "${GREEN}Running Benchmarks...${NC}"
go test -bench=. -benchmem ./internal/benchmark/... 2>&1 | tee -a test_results.txt

echo ""
echo -e "${GREEN}Generating Coverage Report...${NC}"
go test -coverprofile=coverage.out ./...
go tool cover -html=coverage.out -o coverage.html

# Stop server if we started it
if [ ! -z "$SERVER_PID" ]; then
    echo ""
    echo -e "${YELLOW}Stopping server...${NC}"
    kill $SERVER_PID
fi

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}Test Complete!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo "Coverage report: coverage.html"
echo "Test results: test_results.txt"
