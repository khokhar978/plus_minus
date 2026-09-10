#!/usr/bin/env bash
set -e

echo "============================================================"
echo "     PlusMinus: Cloudflare Tunnel One-Time Setup (Linux/Mac)"
echo "============================================================"
echo ""
echo "Target Domain:  game.khokhar.in.net"
echo "Tunnel Name:    game-tunnel"
echo ""

# 1. Check for cloudflared
CLOUDFLARED_CMD=""
if command -v cloudflared &> /dev/null; then
    CLOUDFLARED_CMD="cloudflared"
elif [ -f "./cloudflared" ]; then
    CLOUDFLARED_CMD="./cloudflared"
elif [ -f "$HOME/bin/cloudflared" ]; then
    CLOUDFLARED_CMD="$HOME/bin/cloudflared"
else
    echo "[ERROR] cloudflared was NOT found on this machine!"
    echo ""
    echo "Please install cloudflared using one of these commands:"
    echo "  macOS (Homebrew):"
    echo "    brew install cloudflared"
    echo ""
    echo "  Ubuntu/Debian Linux:"
    echo "    curl -fsSL https://pkg.cloudflare.com/cloudflare-main.gpg | sudo tee /usr/share/keyrings/cloudflare-main.gpg >/dev/null"
    echo "    echo 'deb [signed-by=/usr/share/keyrings/cloudflare-main.gpg] https://pkg.cloudflare.com/cloudflared jammy main' | sudo tee /etc/apt/sources.list.d/cloudflared.list"
    echo "    sudo apt update && sudo apt install -y cloudflared"
    echo ""
    echo "  Direct binary download:"
    echo "    https://github.com/cloudflare/cloudflared/releases/latest"
    echo ""
    exit 1
fi

echo "[OK] Found cloudflared: $CLOUDFLARED_CMD"
echo ""

# 2. Cloudflare Login
echo "============================================================"
echo "Step 1: Cloudflare Authorization"
echo "============================================================"
echo "A web browser will open to log into Cloudflare."
echo "Select your domain (khokhar.in.net) and authorize."
echo ""
read -r -p "Press Enter to continue..."
echo ""

$CLOUDFLARED_CMD tunnel login
echo ""
echo "[OK] Cloudflare login successful!"
echo ""

# 3. Create Tunnel
echo "============================================================"
echo "Step 2: Creating Tunnel 'game-tunnel'"
echo "============================================================"
if $CLOUDFLARED_CMD tunnel create game-tunnel; then
    echo "[OK] Tunnel 'game-tunnel' created successfully!"
else
    echo "[NOTE] If it says the tunnel already exists, that is completely fine."
fi
echo ""

# 4. Route DNS
echo "============================================================"
echo "Step 3: Routing DNS (game.khokhar.in.net -> game-tunnel)"
echo "============================================================"
if $CLOUDFLARED_CMD tunnel route dns game-tunnel game.khokhar.in.net; then
    echo "[OK] DNS successfully routed to game.khokhar.in.net!"
else
    echo "[NOTE] If it says the DNS record already exists, that is fine."
fi
echo ""

# 5. Completion
echo "============================================================"
echo "                   SETUP COMPLETE!"
echo "============================================================"
echo "Tunnel 'game-tunnel' is now linked to game.khokhar.in.net."
echo "You DO NOT need to run this setup script again."
echo ""
echo "To start the game anytime:"
echo "  ./start.sh"
echo ""
echo "To stop all game servers:"
echo "  ./stop.sh"
echo "============================================================"
