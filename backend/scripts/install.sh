#!/bin/bash
set -euo pipefail

# --- Ustawienia domyślne ---
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_FILE="$BASE_DIR/biblioteki.service"
SERVICE_LINK="/etc/systemd/system/biblioteki.service"
ENV_FILE="/etc/biblioteki.env"
NGINX_CONF_SOURCE="$BASE_DIR/biblioteki.conf"
NGINX_CONF_LINK="/etc/nginx/sites-available/biblioteki"
USER="biblioteki"
GROUP="biblioteki"
LOG_DIR="/var/log/biblioteki"

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

# Generuj losowy ciąg znaków (32 bajty hex = 64 znaki)
generate_secret() {
    openssl rand -hex 32
}

# Wyświetl instrukcje
show_usage() {
    cat << EOF
Użycie: $0 [OPCJA]

Opcje:
  --create-db        Utwórz nową bazę danych i użytkownika (wymaga uprawnień do PostgreSQL)
  --existing-db      Użyj istniejącej bazy danych (zapyta o hasło)
  --migration-only   Wykonaj tylko migracje (wymaga istnienia $ENV_FILE)

Przykłady:
  $0 --create-db
  $0 --existing-db
  $0 --migration-only
EOF
    exit 0
}

# --- Sprawdź flagi (nie wymaga root) ---
if [ $# -eq 0 ]; then
    show_usage
fi

CREATE_DB=false
EXISTING_DB=false
MIGRATION_ONLY=false

for arg in "$@"; do
    case "$arg" in
        --create-db)
            CREATE_DB=true
            ;;
        --existing-db)
            EXISTING_DB=true
            ;;
        --migration-only)
            MIGRATION_ONLY=true
            ;;
        *)
            error "Nieznana opcja: $arg"
            ;;
    esac
done

if [ "$CREATE_DB" = true ] && [ "$EXISTING_DB" = true ]; then
    error "Można użyć tylko jednej opcji: --create-db lub --existing-db"
fi

if [ "$CREATE_DB" = true ] && [ "$MIGRATION_ONLY" = true ]; then
    error "Nie można łączyć --create-db z --migration-only"
fi

if [ "$EXISTING_DB" = true ] && [ "$MIGRATION_ONLY" = true ]; then
    error "Nie można łączyć --existing-db z --migration-only"
fi

# --- Sprawdź uprawnienia ---
if [ "$(id -u)" -ne 0 ]; then
    error "Ten skrypt musi być uruchomiony jako root (sudo)."
fi

# --- Obsługa trybu migration-only ---
if [ "$MIGRATION_ONLY" = true ]; then
    # Wymaga istnienia pliku ENV
    if [ ! -f "$ENV_FILE" ]; then
        error "Plik konfiguracyjny $ENV_FILE nie istnieje. Uruchom najpierw instalację z --create-db lub --existing-db."
    fi
    
    # Wczytaj dane z pliku ENV
    if [ -f "$ENV_FILE" ]; then
        # Wczytaj SPRING_DATASOURCE_URL, USERNAME i PASSWORD
        source "$ENV_FILE"
        
        # Wyodrębnij nazwę bazy z URL
        if [ -n "$SPRING_DATASOURCE_URL" ]; then
            # Usuń prefix jdbc:postgresql://
            temp_url="${SPRING_DATASOURCE_URL#jdbc:postgresql://}"
            # Usuń host:port/ (wszystko do ostatniego /)
            DB_NAME="${temp_url##*/}"
        else
            error "Nie znaleziono SPRING_DATASOURCE_URL w pliku $ENV_FILE"
        fi
        
        DB_USER="$SPRING_DATASOURCE_USERNAME"
        DB_PASSWORD="$SPRING_DATASOURCE_PASSWORD"
        
        if [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
            error "Nie znaleziono wymaganych ustawień bazy danych (SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD) w pliku $ENV_FILE"
        fi
    fi
    
    # Wykonywanie migracji Flyway
    info "Wykonywanie migracji Flyway (tryb migration-only)..."
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
    
    MIGRATION_DIR="$BACKEND_DIR/target/classes/db/migration"
    if [ -d "$MIGRATION_DIR" ]; then
        for sql_file in "$MIGRATION_DIR"/*.sql; do
            if [ -f "$sql_file" ]; then
                echo "Wykonywanie migracji: $(basename $sql_file)"
                PGPASSWORD="$DB_PASSWORD" psql -d "$DB_NAME" -U "$DB_USER" -f "$sql_file" || \
                    error "Nie udało się wykonać migracji: $sql_file"
            fi
        done
        info "Wszystkie migracje wykonane pomyślnie."
    else
        error "Nie znaleziono katalogu z migracjami: $MIGRATION_DIR. Wykonaj 'mvn compile' przed uruchomieniem install.sh."
    fi
    
    # Restart usługi
    info "Restartuję usługę biblioteki..."
    if [ "$(id -u)" -eq 0 ]; then
        if systemctl is-active --quiet biblioteki.service 2>/dev/null; then
            systemctl restart biblioteki.service
        else
            systemctl start biblioteki.service
        fi
    else
        if sudo systemctl is-active --quiet biblioteki.service 2>/dev/null; then
            sudo systemctl restart biblioteki.service
        else
            sudo systemctl start biblioteki.service
        fi
    fi
    
    # Komunikat końcowy
    info ""
    info "========================================="
    info "Migracje zakończone pomyślnie!"
    info ""
    info "Aplikacja jest dostępna pod adresem:"
    info "  http://127.0.0.1:8292"
    info ""
    info "API:"
    info "  http://127.0.0.1:8292/api/loans"
    info "  http://127.0.0.1:8292/swagger-ui.html"
    info ""
    info "Logi:"
    info "  journalctl -u biblioteki -f"
    info "========================================="
    exit 0
fi

# --- Sprawdź czy plik ENV już istnieje ---
if [ "$MIGRATION_ONLY" = false ] && [ -f "$ENV_FILE" ]; then
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
info "Wygenerowany JWT_SECRET: $JWT_SECRET"

# --- Konfiguracja bazy danych ---
if [ "$CREATE_DB" = true ]; then
    # Użytkownik systemowy i użytkownik bazy danych są tacy sami
    DB_USER="$USER"
    DB_PASSWORD=$(generate_secret)
    DB_NAME="$USER"

    # Wykonaj polecenia PostgreSQL
    info "Tworzę użytkownika i bazę danych..."
    sudo -u postgres psql -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';" || \
        error "Nie udało się utworzyć użytkownika."
    sudo -u postgres psql -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;" || \
        error "Nie udało się utworzyć bazy danych."
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;" || \
        error "Nie udało się nadać uprawnień."

    info "Użytkownik i baza danych utworzeni."
    info "Wygenerowane hasło do bazy: $DB_PASSWORD"
    info "Nazwa użytkownika: $DB_USER"
    info "Nazwa bazy: $DB_NAME"

    DB_URL="jdbc:postgresql://localhost:5432/$DB_NAME"
elif [ "$EXISTING_DB" = true ]; then
    read -p "Podaj nazwę użytkownika bazy danych [$USER]: " DB_USER
    DB_USER=${DB_USER:-$USER}

    read -sp "Podaj hasło użytkownika bazy danych: " DB_PASSWORD
    echo ""

    read -p "Podaj nazwę bazy danych [$DB_USER]: " DB_NAME
    DB_NAME=${DB_NAME:-$DB_USER}

    DB_URL="jdbc:postgresql://localhost:5432/$DB_NAME"
else
    error "Nie podano opcji. Użyj --create-db lub --existing-db."
fi

# Wykonywanie migracji Flyway
info "Wykonywanie migracji Flyway..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"

# Znajdź pliki migracyjne
MIGRATION_DIR="$BACKEND_DIR/target/classes/db/migration"
if [ -d "$MIGRATION_DIR" ]; then
    # Wykonywanie migracji przez psql
    for sql_file in "$MIGRATION_DIR"/*.sql; do
        if [ -f "$sql_file" ]; then
            echo "Wykonywanie migracji: $(basename $sql_file)"
            sudo -u postgres psql -d "$DB_NAME" -U postgres -f "$sql_file" || \
                error "Nie udało się wykonać migracji: $sql_file"
        fi
    done

    # Nadaj uprawnienia na schemacie public
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON SCHEMA public TO $DB_USER;" || \
        error "Nie udało się nadać uprawnień na schemacie public."
    sudo -u postgres psql -d "$DB_NAME" -c "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $DB_USER;" || \
        error "Nie udało się nadać uprawnień na tabelach."
    sudo -u postgres psql -d "$DB_NAME" -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;" || \
        error "Nie udało się ustawić domyślnych uprawnień."

    info "Wszystkie migracje wykonane pomyślnie."
else
    error "Nie znaleziono katalogu z migracjami: $MIGRATION_DIR. Wykonaj 'mvn compile' przed uruchomieniem install.sh."
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
info "  http://127.0.0.1:8292"
info ""
info "API:"
info "  http://127.0.0.1:8292/api/loans"
info "  http://127.0.0.1:8292/swagger-ui.html"
info ""
info "Logi:"
info "  journalctl -u biblioteki -f"
info "========================================="
