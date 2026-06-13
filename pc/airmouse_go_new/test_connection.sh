#!/bin/bash

# Get local IP
LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)

echo "=========================================="
echo "Air Mouse Server Connection Test"
echo "=========================================="
echo ""
echo "Your PC IP Address: $LOCAL_IP"
echo "Port: 8080"
echo ""
echo "Make sure your phone is connected to the same WiFi network"
echo ""
echo "On your phone, open Air Mouse app and enter:"
echo "  IP: $LOCAL_IP"
echo "  Port: 8080"
echo ""
echo "Testing server..."
if curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo "✓ Server is running locally"
else
    echo "✗ Server is not running"
fi
echo ""
echo "To test from your phone, open this URL in your phone's browser:"
echo "  http://$LOCAL_IP:8080/health"
echo ""
