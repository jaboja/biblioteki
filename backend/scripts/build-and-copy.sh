#!/bin/bash
set -euo pipefail

# Sprawdź argumenty
if [ $# -ne 1 ]; then
    echo "Użycie: $0 <sciezka_docelowa_na_serwerze>"
    echo ""
    echo "  <ścieżka_docelowa_na_serwerze> - Ścieżka docelowa (np. user@192.168.1.100:/opt/biblioteki)"
    exit 1
fi

DEST="$1"

# Sprawdź czy mvnw istnieje
if [ ! -f "./mvnw" ]; then
    echo "[BŁĄD] Nie znaleziono pliku ./mvnw w bieżącym katalogu."
    exit 1
fi

# Buduj JAR
./mvnw clean package -q

# Znajdź plik JAR
JAR_FILE=$(find target -maxdepth 1 -name "biblioteki-*.jar" -type f | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "[BŁĄD] Nie znaleziono pliku JAR w katalogu target/"
    exit 1
fi

echo "[OK] Znaleziono plik JAR: $JAR_FILE"

# Kopiuj pliki na serwer
cp "$JAR_FILE" target/biblioteki.jar
scp target/biblioteki.jar "$DEST/"
scp backend/biblioteki.service "$DEST/"
scp backend/scripts/install.sh "$DEST/"
scp backend/nginx/biblioteki.conf "$DEST/"

echo "[OK] Pliki zostały skopiowane pomyślnie na $DEST"
