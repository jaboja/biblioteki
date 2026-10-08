#!/bin/bash
# Skrypt zatrzymujacy usluge Biblioteki

APP_NAME="biblioteki"

# Sprawdz czy skrypt jest uruchamiany jako root
if [ "$EUID" -ne 0 ]; then
    echo "Ten skrypt musi byc uruchomiony jako root (sudo)"
    exit 1
fi

echo "Zatrzymuje usluge ${APP_NAME}..."
systemctl stop ${APP_NAME}.service

echo "Status uslugi ${APP_NAME}:"
systemctl status ${APP_NAME}.service
