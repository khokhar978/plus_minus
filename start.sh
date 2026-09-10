#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "============================================================"
echo "               PlusMinus Game Server (Linux/Mac)"
echo "============================================================"
echo "  Public URL:    https://game.khokhar.in.net"
echo "  Local UI:      http://localhost:7000"
echo "  WebSocket:     ws://localhost:8887"
echo "============================================================"
echo ""

# ------------------------------------------------------------
# 1. Requirements Check
# ------------------------------------------------------------
echo "[1/5] Checking requirements..."

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in PATH!"
    echo "Please install JDK 17 or higher (e.g. from https://adoptium.net/)"
    exit 1
fi

if ! command -v javac &> /dev/null; then
    echo "[ERROR] javac not found! You need a JDK, not just a JRE."
    echo "Please install JDK 17 or higher."
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "[ERROR] Node.js is not installed or not in PATH!"
    echo "Please install Node.js (v18+) from https://nodejs.org/"
    exit 1
fi

CLOUDFLARED_CMD=""
if command -v cloudflared &> /dev/null; then
    CLOUDFLARED_CMD="cloudflared"
elif [ -f "./cloudflared" ]; then
    CLOUDFLARED_CMD="./cloudflared"
elif [ -f "$HOME/bin/cloudflared" ]; then
    CLOUDFLARED_CMD="$HOME/bin/cloudflared"
else
    echo "[ERROR] cloudflared was not found!"
    echo "Please run ./setup-tunnel.sh first to set up the tunnel."
    exit 1
fi

echo "[OK] Requirements verified."
echo ""

# ------------------------------------------------------------
# 2. Port Cleanup
# ------------------------------------------------------------
echo "[2/5] Cleaning up ports (8887, 7000)..."
if command -v lsof &> /dev/null; then
    lsof -ti:8887,7000 | xargs kill -9 2>/dev/null || true
elif command -v fuser &> /dev/null; then
    fuser -k 8887/tcp 7000/tcp 2>/dev/null || true
fi
echo "[OK] Ports ready."
echo ""

# ------------------------------------------------------------
# 3. Compile Java Backend
# ------------------------------------------------------------
echo "[3/5] Compiling Java backend..."
mkdir -p Server/target/classes

# Note: Java classpath separator is ":" on Unix
javac -cp "Server/lib/*" -d Server/target/classes Server/src/main/java/com/khokhar/game/*.java
echo "[OK] Java backend compiled successfully."
echo ""

# ------------------------------------------------------------
# 4. Install Frontend Dependencies (if needed)
# ------------------------------------------------------------
echo "[4/5] Checking React frontend dependencies..."
if [ ! -d "Client/node_modules" ]; then
    echo "[INFO] Installing npm packages (first-time run)..."
    (cd Client && npm install)
fi
echo "[OK] Frontend ready."
echo ""

# ------------------------------------------------------------
# 5. Launch Servers & Trap for Cleanup
# ------------------------------------------------------------
JAVA_PID=""
VITE_PID=""

cleanup() {
    echo ""
    echo "Shutting down PlusMinus services..."
    [ -n "$JAVA_PID" ] && kill "$JAVA_PID" 2>/dev/null || true
    [ -n "$VITE_PID" ] && kill "$VITE_PID" 2>/dev/null || true
    if command -v lsof &> /dev/null; then
        lsof -ti:8887,7000 | xargs kill -9 2>/dev/null || true
    elif command -v fuser &> /dev/null; then
        fuser -k 8887/tcp 7000/tcp 2>/dev/null || true
    fi
    echo "[OK] All servers stopped."
}

trap cleanup EXIT INT TERM

echo "[5/5] Starting services..."

# Start Java server
echo "  - Starting Java Server on port 8887..."
(cd Server && java -cp "target/classes:lib/*" com.khokhar.game.GameServer) &
JAVA_PID=$!

# Start Vite client
echo "  - Starting React Dev Server on port 7000..."
(cd Client && npm run dev) &
VITE_PID=$!

sleep 3

echo ""
echo "============================================================"
echo "                  ALL SYSTEMS GO!"
echo "============================================================"
echo "  Website:    https://game.khokhar.in.net"
echo "  Local:      http://localhost:7000"
echo ""
echo "  Press Ctrl+C at any time in this window to STOP."
echo "============================================================"
echo ""
echo "Connecting Cloudflare Tunnel to https://game.khokhar.in.net ..."
echo ""

"$CLOUDFLARED_CMD" tunnel --protocol http2 run --url http://localhost:7000 game-tunnel
