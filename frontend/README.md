# Biblioteki Frontend

TypeScript source files for the Biblioteki Spring Boot application.

## Structure

```
frontend/
├── src/                      # TypeScript source
│   ├── loans.ts             # Loans page functionality
│   ├── accounts.ts          # Accounts page functionality
│   └── types.ts             # Type definitions
├── package.json             # Dependencies
├── tsconfig.json            # TypeScript config
├── vite.config.ts           # Vite config
└── build.js                # Build script
```

## Usage

### Install dependencies
```bash
cd frontend
npm install
```

### Development
```bash
npm run dev
```
- Runs Vite dev server on port 3000
- Proxy `/api` requests to Spring Boot on port 8080

### Build for production
```bash
node build.js
```
- Builds TypeScript files
- Outputs JS bundles to `frontend/dist/`
- Copies JS files to `src/main/resources/static/js/`

## Important

- This directory is **NOT** served by Spring Boot
- Only the compiled JS files in `src/main/resources/static/` are served
- HTML and CSS files are in `src/main/resources/static/` and should not be moved
