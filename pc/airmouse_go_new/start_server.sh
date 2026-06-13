#!/bin/bash

echo "Starting Air Mouse Server..."

# Kill any existing server
pkill -f airmouse-server 2>/dev/null

# Start new server
./airmouse-server &
SERVER_PID=$!

echo "Server started with PID: $SERVER_PID"

# Wait for server to be ready
sleep 3

# Test server
if curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo "✓ Server is running"
    LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)
    echo ""
    echo "=========================================="
    echo "Air Mouse Ready!"
    echo "=========================================="
    echo "Phone IP: $LOCAL_IP"
    echo "Port: 8080"
    echo ""
    echo "Open Air Mouse app and enter:"
    echo "  IP: $LOCAL_IP"
    echo "  Port: 8080"
    echo "=========================================="
else
    echo "✗ Server failed to start"
fi

# Keep server running
wait $SERVER_PID
