#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════════════╗"
echo "║     Air Mouse Go Server - COMPLETE TEST SUITE                     ║"
echo "╚═══════════════════════════════════════════════════════════════════╝"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Start server
echo -e "${YELLOW}Starting server...${NC}"
./airmouse-server &
SERVER_PID=$!
sleep 3

echo -e "${GREEN}✓ Server started${NC}"
echo ""

# Run integration tests
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}📦 Running Integration Tests${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
go test -v ./internal/integration/ -timeout 30s
INTEGRATION_RESULT=$?

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}📦 Running Protocol Tests${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
go test -v ./internal/all_tests_test.go
PROTOCOL_RESULT=$?

# Stop server
echo ""
echo -e "${YELLOW}Stopping server...${NC}"
kill $SERVER_PID 2>/dev/null

# Summary
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}📊 FINAL SUMMARY${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ $INTEGRATION_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ Integration Tests: PASSED${NC}"
else
    echo -e "${RED}✗ Integration Tests: FAILED${NC}"
fi

if [ $PROTOCOL_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ Protocol Tests: PASSED${NC}"
else
    echo -e "${RED}✗ Protocol Tests: FAILED${NC}"
fi

echo ""

if [ $INTEGRATION_RESULT -eq 0 ] && [ $PROTOCOL_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ ALL TESTS PASSED!${NC}"
    echo -e "${GREEN}✅ Air Mouse Server is FULLY FUNCTIONAL!${NC}"
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED${NC}"
    exit 1
fi
