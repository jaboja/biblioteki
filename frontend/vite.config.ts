import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  root: resolve(__dirname, '.'),
  
  build: {
    outDir: resolve(__dirname, 'dist'),
    emptyOutDir: true,
    minify: false,
    
    rollupOptions: {
      input: {
        loans: resolve(__dirname, 'src/loans.ts'),
        accounts: resolve(__dirname, 'src/accounts.ts')
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