# PlusMinus: Easy Hosting & Deployment Guide

This guide explains how to run the **PlusMinus** game on any Windows 10 PC (or Linux/Mac) with a single click.

---

## 1. Requirements on the New PC (One-Time Install)

Before running the game on the new PC, make sure you have these installed:

1. **Java JDK 17 or higher**  
   Download and install from: [adoptium.net](https://adoptium.net/) (Eclipse Temurin JDK 17 or 21 LTS).  
   *Make sure during installation you check the box: "Set JAVA_HOME" and "Add to PATH".*

2. **Node.js (v18 or higher)**  
   Download and install from: [nodejs.org](https://nodejs.org/) (LTS version).

3. **Cloudflared (Cloudflare Tunnel CLI)**  
   - **Option A (Easiest)**: Open PowerShell and run:  
     ```powershell
     winget install --id Cloudflare.cloudflared
     ```
   - **Option B**: Download `cloudflared-windows-amd64.exe` from [Cloudflare Releases](https://github.com/cloudflare/cloudflared/releases/latest), rename it to `cloudflared.exe`, and place it directly inside this `PlusMinus` folder.

> [!NOTE]
> You do **NOT** need Maven installed! All required Java libraries (`Java-WebSocket`, `gson`, `slf4j`) are already bundled in the `Server/lib/` folder.

---

## 2. One-Time Setup (First Time Only)

Run the tunnel setup script once on the new PC to authorize your Cloudflare domain:

### On Windows 10:
Double-click:
```
setup-tunnel.bat
```

### On Linux / macOS:
Open terminal and run:
```bash
chmod +x *.sh
./setup-tunnel.sh
```

**What it does:**
1. Opens your browser to log into your Cloudflare account.
2. Select your domain (`khokhar.in.net`) and authorize.
3. Automatically creates the tunnel named `game-tunnel`.
4. Automatically routes the domain `game.khokhar.in.net` to this tunnel.

---

## 3. Daily Startup (Run Every Time)

Whenever you want to start the game server and host it online:

### On Windows 10:
Double-click:
```
start.bat
```

### On Linux / macOS:
Run in terminal:
```bash
./start.sh
```

**What `start.bat` does automatically:**
1. Checks that Java (JDK) and Node.js are available.
2. Cleans up any leftover processes on ports 8887 and 7000.
3. Compiles the Java backend using `Server/lib/*`.
4. Installs frontend packages if needed (`npm install`).
5. Starts the Java Game Server on port `8887`.
6. Starts the React Vite Dev Server on port `7000`.
7. Connects the Cloudflare tunnel with `--protocol http2` to bypass firewall blocks.
8. Displays your live URL: **https://game.khokhar.in.net**.

---

## 4. Stopping the Game

- **Option 1:** Press `Ctrl + C` in the `start.bat` window. It will cleanly shut down the tunnel and all background servers.
- **Option 2:** If you ever close the window with the `X` button, double-click:
  ```
  stop.bat
  ```
  (or `./stop.sh` on Linux/Mac) to terminate all game processes instantly.

---

## 5. Architecture & Ports

| Component | Port | Description |
|-----------|------|-------------|
| **React Frontend (Vite)** | `7000` | Serves the web UI and proxies `/ws` requests |
| **Java GameServer** | `8887` | WebSocket multiplayer game server |
| **Cloudflare Tunnel** | Edge -> `7000` | Secure public tunnel (`game.khokhar.in.net`) using HTTP/2 |

---

## 6. Multiple Apps on Same Domain

- **PyMentor**: Runs via its tunnel on `khokhar.in.net`
- **PlusMinus Game**: Runs via `game-tunnel` on `game.khokhar.in.net`

Both apps run completely independently without interfering with each other!
