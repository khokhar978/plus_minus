#!/usr/bin/env bash

echo "============================================================"
echo "               PlusMinus: Stopping All Services"
echo "============================================================"
echo ""

echo "Stopping Cloudflare tunnel..."
killall cloudflared 2>/dev/null || true

echo "Stopping Java and React servers..."
if command -v lsof &> /dev/null; then
    lsof -ti:8887,7000 | xargs kill -9 2>/dev/null || true
elif command -v fuser &> /dev/null; then
    fuser -k 8887/tcp 7000/tcp 2>/dev/null || true
fi

echo ""
echo "[OK] All PlusMinus services have been stopped."
