# Biblioteki

Aplikacja do zarządzania wypożyczeniami bibliotecznymi z systemów Ex Libris Primo i Alma.
Umożliwia użytkownikom przeglądanie swoich wypożyczeń, sprawdzanie terminów zwrotu oraz prolongatę książek przez internet.

## Opis projektu

**Biblioteki** to system składający się z trzech głównych komponentów:

- **Backend** - Serwer REST API i konektor MCP. Napisany w Spring Boot (Java)
- **Frontend** - Interfejs webowy (TypeScript)
- **Widget** - Widget macOS (Swift)

Wszystkie komponenty współpracują z systemami bibliotecznymi Ex Libris Alma, umożliwiając:
- Logowanie użytkowników
- Pobieranie listy wypożyczeń
- Prolongatę wypożyczeń
- Wyświetlanie informacji o terminach zwrotu

## Architektura

```
biblioteki/
├── backend/           # Serwer Spring Boot (API)
│   ├── src/main/java/pl/jaboja/biblioteki/
│   │   ├── BibliotekiApplication.java     # Główna klasa aplikacji
│   │   ├── auth/                          # Autentykacja JWT
│   │   │   ├── AuthController.java        # Endpointy autentykacji
│   │   │   ├── JwtService.java            # Obsługa tokenów JWT
│   │   │   ├── SecurityConfig.java        # Konfiguracja Spring Security
│   │   │   ├── User.java                  # Model użytkownika
│   │   │   └── UserService.java           # Serwis użytkowników
│   │   ├── loans/                         # Obsługa wypożyczeń
│   │   │   ├── LoansController.java       # Endpointy wypożyczeń
│   │   │   ├── LoansService.java          # Logika biznesowa
│   │   │   ├── Loan.java                  # Model wypożyczenia
│   │   │   └── LoansResponse.java         # Odpowiedź API
│   │   └── primo/                         # Integracja z Primo
│   │       ├── PrimoAuthService.java      # Autentykacja Primo
│   │       └── PrimoLoansService.java     # Pobieranie wypożyczeń
│   └── pom.xml                            # Konfiguracja Maven
│
├── frontend/          # Interfejs webowy (TypeScript + Vite)
│   ├── src/
│   │   ├── loans.ts                       # Obsługa strony wypożyczeń
│   │   ├── accounts.ts                    # Obsługa kont
│   │   └── types.ts                       # Definicje typów
│   ├── package.json                       # Zależności npm
│   ├── tsconfig.json                      # Konfiguracja TypeScript
│   └── vite.config.ts                     # Konfiguracja Vite
│
└── widget/            # Widget macOS (Swift)
    ├── LibraryWidget/                     # Główna aplikacja widget
    │   └── LibraryWidgetApp.swift         # Konfiguracja widget
    ├── LibraryWidgetExtension/            # Rozszerzenie widget
    │   ├── AppIntent.swift                # Intencje widget
    │   └── LoanWidgetView.swift           # Widok widget
    ├── Models/
    │   └── Loan.swift                     # Model wypożyczenia
    ├── Network/
    │   ├── PrimoClient.swift              # Klient HTTP dla Primo
    │   ├── LoanFetcher.swift              # Pobieranie wypożyczeń
    │   └── RenewClient.swift              # Obsługa prolongaty
    └── Config/
        ├── LibraryAccount.swift           # Konfiguracja konta
        └── AccountStore.swift             # Przechowywanie kont
```

## Komponenty

### Backend (Spring Boot)

Serwer API odpowiedzialny za:
- Logowanie użytkowników (JWT)
- Komunikację z API Primo/Alma
- Przetwarzanie i cache'owanie danych
- Udostępnianie endpointów REST
- Udostępnianie konektora MCP

**Technologie:**
- Java 21
- Spring Boot 4.1.1
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- SpringDoc OpenAPI (dokumentacja API)
- Spring AI MCP Server

**Główne endpointy:**
- `POST /api/auth/login` - Logowanie
- `POST /api/auth/logout` - Wylogowanie
- `GET /api/loans` - Lista wypożyczeń
- `POST /api/loans/refresh` - Odświeżenie danych
- `POST /api/loans/{id}/renew` - Prolongata wypożyczenia
- `POST /mcp` - Konektor MCP

### Frontend (TypeScript + Vite)

Interfejs webowy do zarządzania wypożyczeniami przez przeglądarkę.

**Funkcjonalności:**
- Logowanie i wylogowywanie
- Wyświetlanie listy wypożyczeń
- Prolongata wypożyczeń
- Wyświetlanie terminów zwrotu z kolorowym oznaczeniem statusu

**Technologie:**
- TypeScript
- Vite (bundler)

### Widget macOS (Swift)

Aplikacja macOS w postaci widgetu, która umożliwia szybkie sprawdzenie wypożyczeń bezpośrednio z pulpitu macOS.

**Funkcjonalności:**
- Definiowanie kont bibliotecznych
- Wyświetlanie aktywnych wypożyczeń w widget'cie
- Prolongata wypożyczeń

**Technologie:**
- Swift 5+
- WidgetKit
- AppIntents
- URLSession (komunikacja HTTP)

**Struktura:**
- `PrimoClient.swift` - Klient HTTP do komunikacji z Primo API
- `Loan.swift` - Model danych wypożyczenia
- `LoanWidgetView.swift` - widok widget wyświetlający listę wypożyczeń
- `AccountStore.swift` - Przechowywanie konfiguracji kont

## Integracja

Wszystkie trzy komponenty korzystają z systemu Ex Libris Alma:

1. **Backend** komunikuje się bezpośrednio z API Primo za pomocą zarejestrowanych klientów HTTP.
2. **Frontend** korzysta z endpointów backendu.
3. **Widget** ma własną implementację klienta Primo. Działa niezależne od backendu.

## Uruchomienie pełnego systemu

1. **Aplikacja webowa (backend+frontend):**

```bash
cd backend
./mvnw spring-boot:run
```

2. **Widget:**

Otwórz projekt w Xcode i uruchom
