#!/bin/bash

# Lấy thư mục chứa script này
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "==================================================="
echo "   KHOI DONG HE THONG WMS CHO MACOS (BACKEND & FRONTEND)"
echo "==================================================="

echo "[1/2] Dang chay Backend (Spring Boot)..."
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR/backend' && mvn spring-boot:run\""

echo "[2/2] Dang chay Frontend (Vite/React)..."
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR/frontend' && npm run dev\""

echo "==================================================="
echo "Backend va Frontend dang duoc khoi dong trong 2 cua so Terminal moi."
echo "Vui long kiem tra cac cua so Terminal moi mo de xem log chi tiet."
echo "==================================================="