#!/bin/bash
# Skrypt sprawdzajacy status uslugi Biblioteki

APP_NAME="biblioteki"

echo "Status uslugi ${APP_NAME}:"
systemctl status ${APP_NAME}.service

echo ""
echo "Ostatnie logi (ostatnie 20 linii):"
echo "========================================"
sudo journalctl -u ${APP_NAME} -n 20 --no-pager
