#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║     Air Mouse Go Server - Complete Test Suite                 ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Start server
echo "Starting server..."
./airmouse-server &
SERVER_PID=$!
sleep 3

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Test counters
TOTAL=0
PASSED=0
FAILED=0

# Function to run tests
run_test_suite() {
    local name=$1
    local path=$2
    
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}📦 Testing: $name${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    go test -v $path/... 2>&1 | grep -E "PASS|FAIL|---" | head -30
    
    if [ ${PIPESTATUS[0]} -eq 0 ]; then
        echo -e "${GREEN}✓ $name passed${NC}"
        ((PASSED++))
    else
        echo -e "${RED}✗ $name failed${NC}"
        ((FAILED++))
    fi
    ((TOTAL++))
    echo ""
}

# Run all test suites
run_test_suite "Protocol TCP" "./internal/protocol/tcp"
run_test_suite "Protocol WebSocket" "./internal/protocol/websocket"
run_test_suite "Protocol UDP" "./internal/protocol/udp"
run_test_suite "Protocol Main" "./internal/protocol"
run_test_suite "Control" "./internal/control"
run_test_suite "Utils" "./internal/utils"
run_test_suite "Integration" "./internal/integration"

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}📊 TEST SUMMARY${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "Total suites: ${TOTAL}"
echo -e "${GREEN}Passed: ${PASSED}${NC}"
echo -e "${RED}Failed: ${FAILED}${NC}"
echo ""

# Stop server
kill $SERVER_PID 2>/dev/null

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ ALL TESTS PASSED!${NC}"
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED${NC}"
    exit 1
fi
