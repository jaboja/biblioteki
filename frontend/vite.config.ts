import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  root: resolve(__dirname, '.'),
  publicDir: resolve(__dirname, 'static'),

  build: {
    outDir: resolve(__dirname, '../backend/src/main/resources/static'),
    emptyOutDir: true,
    minify: true,

    rollupOptions: {
      input: {
        loans: resolve(__dirname, 'src/loans.ts'),
        accounts: resolve(__dirname, 'src/accounts.ts'),
        login: resolve(__dirname, 'src/login.ts'),
      },
      output: {
        entryFileNames: '[name].js',
        assetFileNames: '[name].[ext]'
      }
    }
  },

  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});