# Biblioteki Frontend

Frontend application for the Biblioteki Spring Boot project.

## Projekt Structure

```
frontend/
├── index.html              # Main HTML with tabs
├── css/
│   └── styles.css          # Common CSS styles
├── js/
│   └── bundle.js           # Fallback JS bundle (dev only)
├── src/
│   ├── app.ts             # Entry point
│   ├── main.ts            # Tab logic & initialization
│   ├── loans.ts           # Loans functionality
│   ├── accounts.ts        # Accounts functionality
│   └── types.ts           # TypeScript interfaces
├── package.json           # Dependencies
├── vite.config.ts         # Vite configuration
├── tsconfig.json          # TypeScript configuration
└── build.sh              # Build script
```

## Development

### Option 1: Vite Dev Server
```bash
cd frontend
npm install
npm run dev
```
- Runs on port 3000
- Proxy `/api` requests to Spring Boot on port 8080
- Hot module replacement for TypeScript files

### Option 2: Maven Build (Recommended)
```bash
# Full project build (includes frontend)
mvn clean package
```
- Automatically installs Node.js via frontend-maven-plugin
- Runs `npm install` and `npm run build`
- Outputs compiled files to `src/main/resources/static/`

## Build Commands

```bash
# Manual build (from frontend directory)
cd frontend
npm install
npm run build

# The build outputs to ../src/main/resources/static/
```

## Maven Integration

The `pom.xml` includes `frontend-maven-plugin` which:
1. Downloads Node.js automatically
2. Runs `npm install` 
3. Runs `npm run build`
4. Outputs to Spring Boot's static resources directory

## File Output

After build, the following files appear in `src/main/resources/static/`:
- `index.html` - Main page with tabs
- `css/styles.css` - Common styles
- `js/app.js` - Compiled and bundled JavaScript
- `assets/` - Additional assets if any

## Features

### Tabs
- **Wypożyczenia**: Library loans list with filtering and stats
- **Konta Dostępu**: Account management (CRUD)

### Loans Features
- ✅ List all loans
- ✅ Refresh with cache
- ✅ Force refresh (clear cache)
- ✅ Statistics (total, overdue, due soon, renewable)
- ✅ Color-coded status

### Accounts Features  
- ✅ List all accounts
- ✅ Add new accounts
- ✅ Edit existing accounts
- ✅ Delete accounts
- ✅ Library selection dropdown
- ✅ Statistics (total, enabled, disabled)

## Vite Configuration

- **Proxy**: `/api` → `http://localhost:8080`
- **Output**: `../src/main/resources/static/`
- **Entry**: `index.html`
- **Module**: TypeScript with ES modules

## Notes

- For production, use Maven build: `mvn clean package`
- For development, use Vite dev server: `npm run dev`
- The `bundle.js` file is for development only and will be replaced by the Vite build