#!/bin/bash
set -euo pipefail

# --- Ustawienia domyślne ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"
SERVICE_FILE="$BASE_DIR/biblioteki.service"
SERVICE_LINK="/etc/systemd/system/biblioteki.service"
ENV_FILE="/etc/biblioteki.env"
NGINX_CONF_SOURCE="$BASE_DIR/biblioteki.conf"
NGINX_CONF_LINK="/etc/nginx/sites-available/biblioteki"
USER="biblioteki"
GROUP="biblioteki"
LOG_DIR="/var/log/biblioteki"

# --- Funkcje pomocnicze ---
info() {
    echo "[OK] $1"
}

error() {
    echo "[BŁĄD] $1" >&2
    exit 1
}

# Generuj losowy ciąg znaków (32 bajty hex = 64 znaki)
generate_secret() {
    openssl rand -hex 32
}

# Wyświetl instrukcje
show_usage() {
    cat << EOF
Użycie: $0 [OPCJA]

Opcje:
  --create-db    Utwórz nową bazę danych i użytkownika (wymaga uprawnień do PostgreSQL)
  --existing-db  Użyj istniejącej bazy danych (zapyta o hasło)

Przykłady:
  $0 --create-db
  $0 --existing-db
EOF
    exit 0
}

# --- Sprawdź flagi (nie wymaga root) ---
if [ $# -eq 0 ]; then
    show_usage
fi

CREATE_DB=false
EXISTING_DB=false

for arg in "$@"; do
    case "$arg" in
        --create-db)
            CREATE_DB=true
            ;;
        --existing-db)
            EXISTING_DB=true
            ;;
        *)
            error "Nieznana opcja: $arg"
            ;;
    esac
done

if [ "$CREATE_DB" = true ] && [ "$EXISTING_DB" = true ]; then
    error "Można użyć tylko jednej opcji: --create-db lub --existing-db"
fi

# --- Sprawdź uprawnienia (tylko dla działań instalacyjnych) ---
if [ "$CREATE_DB" = true ] || [ "$EXISTING_DB" = true ]; then
    if [ "$(id -u)" -ne 0 ]; then
        error "Ten skrypt musi być uruchomiony jako root (sudo) dla operacji instalacyjnych."
    fi
fi

# --- Sprawdź czy plik ENV już istnieje ---
if [ -f "$ENV_FILE" ]; then
    info "Plik konfiguracyjny $ENV_FILE już istnieje. Nic nie robię."
    exit 0
fi

# --- Utwórz katalogi ---
info "Tworzę katalogi..."
mkdir -p "$LOG_DIR"

# --- Utwórz użytkownika systemowego ---
info "Tworzę użytkownika systemowego $USER..."
if ! id "$USER" &>/dev/null; then
    useradd --system --no-create-home --shell /sbin/nologin "$USER"
    info "Użytkownik $USER utworzony."
else
    info "Użytkownik $USER już istnieje."
fi

# --- Symlinkuj plik service ---
info "Tworzę symlink dla usługi systemd..."
ln -sf "$SERVICE_FILE" "$SERVICE_LINK"

# --- Symlinkuj konfigurację Nginx ---
info "Tworzę symlink dla konfiguracji Nginx..."
mkdir -p /etc/nginx/sites-available
ln -sf "$NGINX_CONF_SOURCE" "$NGINX_CONF_LINK"

# --- Przeładuj systemd i Nginx ---
info "Przeładuję systemd..."
systemctl daemon-reload

info "Przeładuję Nginx..."
if command -v nginx &>/dev/null; then
    systemctl reload nginx || echo "[OSTRZEŻENIE] Nginx nie jest zainstalowany lub nie działa"
fi

# --- Generuj JWT_SECRET ---
JWT_SECRET=$(generate_secret)
info "Wygenerowany JWT_SECRET (zapisz go w bezpiecznym miejscu!):"
echo "  $JWT_SECRET"

# --- Konfiguracja bazy danych ---
if [ "$CREATE_DB" = true ]; then
    # Pytaj o hasło administratora PostgreSQL
    read -sp "Podaj hasło administratora PostgreSQL (postgres): " PG_ADMIN_PASSWORD
    echo ""

    # Użytkownik systemowy i użytkownik bazy danych są tacy sami
    DB_USER="$USER"
    DB_PASSWORD=$(generate_secret)
    DB_NAME="$USER"

    # Wykonaj polecenia PostgreSQL
    info "Tworzę użytkownika i bazę danych..."
    sudo -u postgres PGPASSWORD="$PG_ADMIN_PASSWORD" psql -h localhost -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';" 2>/dev/null || \
        error "Nie udało się utworzyć użytkownika. Sprawdź hasło administratora PostgreSQL."
    sudo -u postgres PGPASSWORD="$PG_ADMIN_PASSWORD" psql -h localhost -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;" 2>/dev/null || \
        error "Nie udało się utworzyć bazy danych."
    sudo -u postgres PGPASSWORD="$PG_ADMIN_PASSWORD" psql -h localhost -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;" 2>/dev/null || \
        error "Nie udało się nadać uprawnień."

    info "Użytkownik i baza danych utworzeni."
    info "Wygenerowane hasło do bazy (zapisz je!): $DB_PASSWORD"
    info "Nazwa użytkownika: $DB_USER"
    info "Nazwa bazy: $DB_NAME"

    DB_URL="jdbc:postgresql://localhost:5432/$DB_NAME"

elif [ "$EXISTING_DB" = true ]; then
    # Pytaj o parametry istniejącej bazy
    read -p "Podaj URL bazy danych [jdbc:postgresql://localhost:5432/$USER]: " DB_URL
    DB_URL=${DB_URL:-jdbc:postgresql://localhost:5432/$USER}

    read -p "Podaj nazwę użytkownika bazy danych [$USER]: " DB_USER
    DB_USER=${DB_USER:-$USER}

    read -sp "Podaj hasło użytkownika bazy danych: " DB_PASSWORD
    echo ""
else
    error "Nie podano opcji. Użyj --create-db lub --existing-db."
fi

# --- Utwórz plik ENV (właściciel: root, grupa: biblioteki, prawo odczytu dla grupy) ---
info "Tworzę plik konfiguracyjny $ENV_FILE..."
cat > "$ENV_FILE" << EOL
# Baza danych
SPRING_DATASOURCE_URL=$DB_URL
SPRING_DATASOURCE_USERNAME=$DB_USER
SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD

# JWT
JWT_SECRET=$JWT_SECRET

# Port
SERVER_PORT=8292

# Flyway
SPRING_FLYWAY_ENABLED=true

# Logowanie
LOGGING_LEVEL_PL_JABOJA=INFO
LOGGING_FILE_NAME=$LOG_DIR/application.log
EOL

chmod 640 "$ENV_FILE"
chown root:$GROUP "$ENV_FILE"
info "Plik konfiguracyjny $ENV_FILE utworzony (właściciel: root, grupa: $GROUP)."

# --- Ustaw uprawnienia do katalogów ---
chown -R "$USER:$GROUP" "$LOG_DIR"
chmod 750 "$LOG_DIR"

# --- Włącz i uruchom usługę ---
info "Włączam usługę biblioteki..."
systemctl enable biblioteki.service
systemctl start biblioteki.service

info ""
info "========================================="
info "Instalacja zakończona pomyślnie!"
info ""
info "Aplikacja jest dostępna pod adresem:"
info "  http://localhost:8292"
info ""
info "API:"
info "  http://localhost:8292/api/loans"
info "  http://localhost:8292/swagger-ui.html"
info ""
info "Logi:"
info "  journalctl -u biblioteki -f"
info "========================================="
