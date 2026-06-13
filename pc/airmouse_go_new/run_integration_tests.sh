#!/bin/bash

echo "Starting Air Mouse Server..."
./airmouse-server &
SERVER_PID=$!

echo "Waiting for server to be ready..."
sleep 3

echo ""
echo "Running Integration Tests..."
echo "============================"
go test -v ./internal/integration/...

TEST_RESULT=$?

echo ""
echo "Stopping server..."
kill $SERVER_PID

if [ $TEST_RESULT -eq 0 ]; then
    echo "✅ All tests passed!"
else
    echo "❌ Some tests failed!"
fi

exit $TEST_RESULT
