# Biblioteki Frontend

## Strukturę plików

```
static/
├── index.html              # Główna strona z tabami (wypożyczenia + konta)
├── css/
│   └── styles.css          # Wspólne style CSS
├── js/
│   └── bundle.js           # Zbundled JS (działa od razu)
├── src/                   # Źródła TypeScript (do budowania z Vite)
│   ├── app.ts             # Główny punkt wejścia
│   ├── main.ts            # Logika tabów i inicjalizacja
│   ├── loans.ts           # Logika wypożyczeń
│   ├── accounts.ts        # Logika kont
│   └── types.ts           # Typy TypeScript
├── package.json           # Zależności Vite
├── vite.config.ts         # Konfiguracja Vite
├── tsconfig.json          # Konfiguracja TypeScript
└── build.sh              # Skrypt do budowania
```

## Szybki start

### Opcja 1: Użyj gotowego bundle (działa od razu)
- Plik `js/bundle.js` działa natychmiast bez konieczności budowania
- Po prostu otwórz `http://localhost:8080/` w przeglądarce

### Opcja 2: Buduj z Vite (rekomendowane dla rozwoju)

1. **Zainstaluj Node.js i npm** (jeśli nie masz):
   - Pobierz z https://nodejs.org/ (LTS)

2. **Zainstaluj zależności:**
   ```bash
   cd src/main/resources/static
   npm install
   ```

3. **Uruchom w trybie developerskim:**
   ```bash
   npm run dev
   ```
   - Vite uruchomi serwer na porcie 3000 z proxy do API
   - Otwórz http://localhost:3000
   - Zmiany w plikach TS będą automatycznie ładowane

4. **Buduj dla produkcji:**
   ```bash
   npm run build
   ```
   - Wygeneruje zoptymalizowane pliki w `dist/`
   - Skopiuj zawartość `dist/` do `static/`

## Funkcjonalności

### Taby
- **Wypożyczenia**: Pokazuje listę wypożyczeń z bibliotek
- **Konta Dostępu**: Zarządzanie kontami dostępu do bibliotek

### Funkcje kont
- ✅ Lista wszystkich kont
- ✅ Dodawanie nowych kont
- ✅ Edycja istniejących kont
- ✅ Usuwanie kont
- ✅ Filtrowanie po bibliotece
- ✅ Statystyki (aktywne/nieaktywne)

### Funkcje wypożyczeń
- ✅ Lista wypożyczeń
- ✅ Odświeżanie (z cache)
- ✅ Wymuszanie odświeżenia (ignore cache)
- ✅ Statystyki (przeterminowane, wkrótce, do przedłużenia)
- ✅ Kolorowe oznaczenia statusów

## Użycie w Spring Boot

### Rozwój
1. Uruchom Vite: `npm run dev` (port 3000)
2. Uruchom Spring Boot: `./mvnw spring-boot:run` (port 8080)
3. Vite proxy requesty `/api` do Spring Boot

### Produkcja
1. Zbuduj frontend: `npm run build`
2. Zbuduj Spring Boot: `./mvnw package`
3. Uruchom: `java -jar target/biblioteki-0.0.1-SNAPSHOT.jar`

## Konfiguracja Vite

- `vite.config.ts`: Proxy `/api` do `http://localhost:8080`
- `tsconfig.json`: Konfiguracja TypeScript
- `package.json`: Zależności i skrypty

## Uwagi

- Plik `bundle.js` jest tymczasowy i zostanie zastąpiony przez Vite bundle
- Wszystkie funkcje są dostępne globalnie (`window.functionName`)
- Formularze używają `localStorage` do tymczasowego przechowywania danych
- Style CSS są wspólne dla obu zakładek