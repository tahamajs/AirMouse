#!/bin/bash

echo "=========================================="
echo "Air Mouse Server - Integration Tests"
echo "=========================================="

# Start server
echo "Starting server..."
./airmouse-server > /tmp/server.log 2>&1 &
SERVER_PID=$!
sleep 3

# Check if server is running
if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "❌ Failed to start server"
    exit 1
fi
echo "✓ Server started (PID: $SERVER_PID)"

# Run tests
echo ""
echo "Running tests..."
go test -v ./internal/integration/ -timeout 30s
TEST_RESULT=$?

# Stop server
echo ""
echo "Stopping server..."
kill $SERVER_PID 2>/dev/null

if [ $TEST_RESULT -eq 0 ]; then
    echo ""
    echo "✅ ALL TESTS PASSED!"
else
    echo ""
    echo "❌ SOME TESTS FAILED"
fi

exit $TEST_RESULT
