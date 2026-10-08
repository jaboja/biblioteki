#!/bin/bash
# Glowny skrypt deployujacy backend Biblioteki
# Uruchom: ./deploy.sh [build|install|start|stop|restart|status]

set -e

# Ustawienia domyslne
APP_NAME="biblioteki"
VERSION="0.0.1-SNAPSHOT"
JAR_NAME="${APP_NAME}-${VERSION}.jar"
SOURCE_JAR="target/${JAR_NAME}"
TARGET_DIR="/opt/${APP_NAME}"
TARGET_JAR="${TARGET_DIR}/${JAR_NAME}"
CONFIG_DIR="/etc/${APP_NAME}"
ENV_FILE="${CONFIG_DIR}/application.env"
LOG_DIR="/var/log/${APP_NAME}"
USER="biblioteki"
GROUP="biblioteki"
SERVICE_FILE="/etc/systemd/system/${APP_NAME}.service"

# Kolory do komunikatow
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Funkcje do wyswietlania komunikatow
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Sprawdz czy skrypt jest uruchamiany jako root (dla operacji instalacji)
needs_root() {
    if [ "$EUID" -ne 0 ]; then
        error "Ten skrypt musi byc uruchomiony jako root (sudo)"
        exit 1
    fi
}

# Buduj aplikacje (nie wymaga root)
build() {
    info "Buduje aplikacje Spring Boot..."
    if [ -f "pom.xml" ]; then
        ./mvnw clean package -q
    else
        error "Nie znaleziono pliku pom.xml w biezacym katalogu"
        exit 1
    fi
    
    if [ ! -f "$SOURCE_JAR" ]; then
        error "Budowanie nie powiodlo sie - plik ${SOURCE_JAR} nie istnieje"
        exit 1
    fi
    
    info "Aplikacja zostaa zbudowana pomyslnie: ${SOURCE_JAR}"
}

# Instaluj usluge (wymaga root)
install() {
    needs_root
    
    # Utworz uzytkownika systemowego
    info "Tworze uzytkownika systemowego ${USER}..."
    if ! id "$USER" &>/dev/null; then
        useradd --system --no-create-home --shell /sbin/nologin "$USER"
        info "Uzytkownik ${USER} utworzony"
    else
        info "Uzytkownik ${USER} juz istnieje"
    fi
    
    # Utworz katalogi
    info "Tworze katalogi..."
    mkdir -p "$TARGET_DIR"
    mkdir -p "$CONFIG_DIR"
    mkdir -p "$LOG_DIR"
    
    # Ustaw wlasciciela katalogow
    chown -R "${USER}:${GROUP}" "$TARGET_DIR"
    chown -R "${USER}:${GROUP}" "$CONFIG_DIR"
    chown -R "${USER}:${GROUP}" "$LOG_DIR"
    
    # Kopiuj plik JAR
    if [ ! -f "$SOURCE_JAR" ]; then
        error "Plik JAR ${SOURCE_JAR} nie istnieje. Uruchom najpierw: ./deploy.sh build"
        exit 1
    fi
    
    info "Kopiuje ${SOURCE_JAR} do ${TARGET_JAR}..."
    cp "$SOURCE_JAR" "$TARGET_JAR"
    chown "${USER}:${GROUP}" "$TARGET_JAR"
    chmod 644 "$TARGET_JAR"
    
    # Kopiuj plik ENV jeśli nie istnieje
    if [ ! -f "$ENV_FILE" ]; then
        info "Tworze domyslny plik konfiguracyjny ${ENV_FILE}..."
        cat > "$ENV_FILE" << 'EOL'
# Baza danych
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/biblioteki
SPRING_DATASOURCE_USERNAME=biblioteki_user
SPRING_DATASOURCE_PASSWORD=biblioteki123

# JWT
JWT_SECRET=zmien_to_swoje_tajne_haslo_minimum_64_znaki

# Port
SERVER_PORT=8080

# Flyway
SPRING_FLYWAY_ENABLED=true

# Logowanie
LOGGING_LEVEL_PL_JABOJA=INFO
LOGGING_FILE_NAME=/var/log/biblioteki/application.log
EOL
        chmod 600 "$ENV_FILE"
        chown "${USER}:${GROUP}" "$ENV_FILE"
        info "Plik konfiguracyjny ${ENV_FILE} utworzony"
    fi
    
    # Kopiuj usluge systemd
    info "Instaluje usluge systemd..."
    cp "biblioteki.service" "$SERVICE_FILE"
    chmod 644 "$SERVICE_FILE"
    
    # Przeladuj systemd
    info "Przeladowuje systemd..."
    systemctl daemon-reload
    
    # Wlacz usluge (autostart)
    info "Wlaczam usluge ${APP_NAME}..."
    systemctl enable "${APP_NAME}.service"
    
    info "Instalacja zakonczona pomyslnie!"
}

# Uruchom usluge (wymaga root)
start_service() {
    needs_root
    info "Uruchamiam usluge ${APP_NAME}..."
    systemctl start "${APP_NAME}.service"
    info "Usluga ${APP_NAME} zostala uruchomiona"
}

# Zatrzymaj usluge (wymaga root)
stop_service() {
    needs_root
    info "Zatrzymuje usluge ${APP_NAME}..."
    systemctl stop "${APP_NAME}.service"
    info "Usluga ${APP_NAME} zostala zatrzymana"
}

# Zrestartuj usluge (wymaga root)
restart_service() {
    needs_root
    info "Zrestartowuje usluge ${APP_NAME}..."
    systemctl restart "${APP_NAME}.service"
    info "Usluga ${APP_NAME} zostala zrestartowana"
}

# Sprawdz status uslugi
status_service() {
    info "Status uslugi ${APP_NAME}:"
    systemctl status "${APP_NAME}.service" --no-pager
    
    echo ""
    info "Ostatnie logi (ostatnie 20 linii):"
    echo "========================================"
    journalctl -u "${APP_NAME}" -n 20 --no-pager 2>/dev/null || echo "Brak logow"
}

# Glowna logika skryptu
case "$1" in
    build)
        build
        ;;
    install)
        install
        ;;
    start)
        start_service
        ;;
    stop)
        stop_service
        ;;
    restart)
        restart_service
        ;;
    status)
        status_service
        ;;
    deploy)
        info "Buduje i instaluje aplikacje..."
        build
        install
        start_service
        ;;
    *)
        echo "Uzycie: $0 {build|install|start|stop|restart|status|deploy}"
        echo ""
        echo "  build     - Buduje aplikacje (mvn package)"
        echo "  install   - Instaluje usluge systemd i kopiuje pliki (wymaga sudo)"
        echo "  start     - Uruchamia usluge (wymaga sudo)"
        echo "  stop      - Zatrzymuje usluge (wymaga sudo)"
        echo "  restart   - Zrestartowuje usluge (wymaga sudo)"
        echo "  status    - Sprawdza status uslugi"
        echo "  deploy    - Buduje, instaluje i uruchamia (wymaga sudo)"
        exit 1
        ;;
esac
