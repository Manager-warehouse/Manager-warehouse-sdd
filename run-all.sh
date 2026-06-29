#!/bin/bash

# Lấy thư mục chứa script này
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "==================================================="
echo "   KHOI DONG HE THONG WMS CHO MACOS (BACKEND & FRONTEND)"
echo "==================================================="

echo "[1/2] Dang chay Backend (Spring Boot)..."
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR/backend' && export JAVA_HOME='/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home' && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081\""

echo "[2/2] Dang chay Frontend (Vite/React)..."
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR/frontend' && npm run dev\""

echo "==================================================="
echo "Backend va Frontend dang duoc khoi dong trong 2 cua so Terminal moi."
echo "Vui long kiem tra cac cua so Terminal moi mo de xem log chi tiet."
echo "==================================================="
