#!/bin/bash
# Skrypt uruchamiajacy backend Biblioteki jako usluge

# Ustawienia domyslne
APP_NAME="biblioteki"
JAR_FILE="/opt/${APP_NAME}/${APP_NAME}-0.0.1-SNAPSHOT.jar"
CONFIG_DIR="/etc/${APP_NAME}"
ENV_FILE="${CONFIG_DIR}/application.env"
LOG_DIR="/var/log/${APP_NAME}"
USER="biblioteki"
GROUP="biblioteki"

# Sprawdz czy skrypt jest uruchamiany jako root
if [ "$EUID" -ne 0 ]; then
    echo "Ten skrypt musi byc uruchomiony jako root (sudo)"
    exit 1
fi

# Funkcja do wyswietlania komunikatow
info() {
    echo "[INFO] $1"
}

error() {
    echo "[ERROR] $1" >&2
}

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
mkdir -p "$(dirname "$JAR_FILE")"
mkdir -p "$CONFIG_DIR"
mkdir -p "$LOG_DIR"

# Ustaw wlasciciela katalogow
chown -R "${USER}:${GROUP}" "$(dirname "$JAR_FILE")"
chown -R "${USER}:${GROUP}" "$CONFIG_DIR"
chown -R "${USER}:${GROUP}" "$LOG_DIR"

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

# Utworz usluge systemd
info "Tworze usluge systemd..."
cat > /etc/systemd/system/${APP_NAME}.service << EOL
[Unit]
Description=Biblioteki Backend Service
After=network.target postgresql.service
Requires=postgresql.service

[Service]
User=${USER}
Group=${GROUP}
WorkingDirectory=$(dirname "$JAR_FILE")
EnvironmentFile=${ENV_FILE}
ExecStart=/usr/bin/java -jar ${JAR_FILE}
SuccessExitStatus=143
Restart=always
RestartSec=30
StandardOutput=append:${LOG_DIR}/stdout.log
StandardError=append:${LOG_DIR}/stderr.log
SyslogIdentifier=${APP_NAME}

# Ograniczenia zasobow (opcjonalnie)
MemoryMax=512M
CPUQuota=50%

[Install]
WantedBy=multi-user.target
EOL

# Przeladuj systemd
info "Przeladowuje systemd..."
systemctl daemon-reload

# Wlacz usluge (autostart)
info "Wlaczam usluge ${APP_NAME}..."
systemctl enable ${APP_NAME}.service

# Uruchom usluge
info "Uruchamiam usluge ${APP_NAME}..."
systemctl start ${APP_NAME}.service

# Sprawdz status
info "Sprawdzam status uslugi..."
systemctl status ${APP_NAME}.service

# Wyswietl informacje o usludze
info ""
info "========================================="
info "Usluga ${APP_NAME} zostala zainstalowana!"
info ""
info "Komendy do zarzadzania usluga:"
info "  sudo systemctl start ${APP_NAME}   - Uruchom usluge"
info "  sudo systemctl stop ${APP_NAME}    - Zatrzymaj usluge"
info "  sudo systemctl restart ${APP_NAME} - Zrestartuj usluge"
info "  sudo systemctl status ${APP_NAME} - Sprawdz status"
info "  sudo journalctl -u ${APP_NAME} -f - Logi na zywo"
info ""
info "Plik konfiguracyjny: ${ENV_FILE}"
info "Edytuj go i zrestartuj usluge, zeby zmienic ustawienia."
info ""
info "Aplikacja bedzie dostepna pod adresem:"
info "  http://localhost:8080/api/loans"
info "  http://localhost:8080/swagger-ui.html"
info "========================================="
