import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(() => {
  return {
    base: '/ui',
    build: {
      outDir: 'build'
    },
    define: {
      global: 'window'
    },
    server: {
      port: 3000,
      open: true,
      proxy: {
        '/api': {
          target: 'http://localhost:8080'
        }
      }
    },
    plugins: [react()],
    test: {
      environment: 'jsdom'
    }
  };
});
