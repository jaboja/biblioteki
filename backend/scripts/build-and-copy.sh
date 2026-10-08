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

# --- Funkcje pomocnicze ---
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

info() {
    echo -e "${GREEN}[OK]${NC} $1"
}

error() {
    echo -e "${RED}[BŁĄD]${NC} $1" >&2
    exit 1
}

# Sprawdź czy mvnw istnieje
if [ ! -f "./mvnw" ]; then
    error "Nie znaleziono pliku ./mvnw w bieżącym katalogu."
fi

# Buduj JAR
./mvnw clean package -q

# Znajdź plik JAR
JAR_FILE=$(find target -maxdepth 1 -name "biblioteki-*.jar" -type f | head -1)
if [ -z "$JAR_FILE" ]; then
    error "Nie znaleziono pliku JAR w katalogu target/"
fi

info "Znaleziono plik JAR: $JAR_FILE"

# Kopiuj pliki na serwer
mv "$JAR_FILE" ./target/biblioteki.jar
scp ./target/biblioteki.jar "$DEST/"
mv ./target/biblioteki.jar "$JAR_FILE"
scp ./biblioteki.service "$DEST/"
scp ./scripts/install.sh "$DEST/"
scp ./nginx/biblioteki.conf "$DEST/"

info "Pliki zostały skopiowane pomyślnie na $DEST"
