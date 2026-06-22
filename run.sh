#!/bin/bash
# ============================================================
#  Khoi dong WMS - Backend (8081) + Frontend (3000)
#  Dung lenh: bash run.sh
# ============================================================

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

echo "============================================================"
echo " Khoi dong WMS - Backend (8081) + Frontend (3000)"
echo "============================================================"

# --- 1. Backend: Spring Boot ---
echo "[1/2] Dang khoi dong Backend (Spring Boot :8081)..."
(cd "$BACKEND_DIR" && cmd //c mvnw.cmd spring-boot:run) &
BACKEND_PID=$!
echo "      Backend PID: $BACKEND_PID"

# Doi backend san sang
echo "      Dang cho backend khoi dong (30s)..."
sleep 30

# --- 2. Frontend: Vite ---
echo "[2/2] Dang khoi dong Frontend (Vite :3000)..."
if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo "      Cai dependency lan dau (npm install)..."
    (cd "$FRONTEND_DIR" && npm install)
fi
(cd "$FRONTEND_DIR" && npm run dev) &
FRONTEND_PID=$!
echo "      Frontend PID: $FRONTEND_PID"

echo ""
echo "============================================================"
echo " Ca hai service da duoc khoi dong!"
echo "   Frontend : http://localhost:3000"
echo "   API docs : http://localhost:8081/swagger-ui/index.html"
echo "============================================================"
echo " Nhan Ctrl+C de dung ca hai service."
echo "============================================================"

# Doi ca 2 process
wait $BACKEND_PID $FRONTEND_PID
