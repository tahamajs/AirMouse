#!/bin/bash

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     Air Mouse Go Server - Complete Test Suite v3.0          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Check if server is running
check_server() {
    if curl -s http://localhost:8080/health > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Start server if not running
if ! check_server; then
    echo -e "${YELLOW}Starting server...${NC}"
    ./airmouse-server &
    SERVER_PID=$!
    echo -e "${YELLOW}Waiting for server to start...${NC}"
    sleep 3
else
    SERVER_PID=""
    echo -e "${GREEN}Server already running${NC}"
fi

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📋 RUNNING TEST SUITES${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run test and count results
run_test() {
    local test_name=$1
    local test_cmd=$2
    
    echo -e "${PURPLE}▶ Running $test_name${NC}"
    
    if $test_cmd > /tmp/test_output_$$.txt 2>&1; then
        echo -e "${GREEN}✓ $test_name passed${NC}"
        ((PASSED_TESTS++))
    else
        echo -e "${RED}✗ $test_name failed${NC}"
        cat /tmp/test_output_$$.txt | head -20
        ((FAILED_TESTS++))
    fi
    ((TOTAL_TESTS++))
    echo ""
}

# Run all test suites
run_test "Unit Tests" "go test -v -cover ./internal/protocol/..."
run_test "Integration Tests" "go test -v -timeout 60s ./internal/integration/..."
run_test "Security Tests" "go test -v -run TestJWTTokenValidation ./internal/integration/..."
run_test "Scenario Tests" "go test -v -run TestFullSessionFlow ./internal/integration/..."
run_test "Performance Benchmarks" "go test -bench=. -benchmem -run=^$ ./internal/benchmark/..."
run_test "Protocol Compliance" "go test -v -run TestProtocolVersionCompliance ./internal/protocol/..."
run_test "Edge Cases" "go test -v -run TestMalformedMessages ./internal/integration/..."

# Generate coverage report
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📊 GENERATING COVERAGE REPORT${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

go test -coverprofile=coverage.out ./... 2>/dev/null
go tool cover -func=coverage.out | tail -1
go tool cover -html=coverage.out -o coverage.html
echo -e "${GREEN}Coverage report: coverage.html${NC}"
echo ""

# Print summary
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📈 TEST SUMMARY${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "Total tests:  ${TOTAL_TESTS}"
echo -e "Passed:       ${GREEN}${PASSED_TESTS}${NC}"
echo -e "Failed:       ${RED}${FAILED_TESTS}${NC}"
echo -e "Success rate: ${GREEN}$(( (PASSED_TESTS * 100) / TOTAL_TESTS ))%${NC}"
echo ""

# Stop server if we started it
if [ ! -z "$SERVER_PID" ]; then
    echo -e "${YELLOW}Stopping server...${NC}"
    kill $SERVER_PID 2>/dev/null
fi

# Exit with appropriate code
if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ ALL TESTS PASSED!${NC}"
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED${NC}"
    exit 1
fi
