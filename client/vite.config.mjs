import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(() => {
  return {
    base: '/ui',
    build: {
      outDir: 'build',
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('react')) {
              return 'react';
            }
            if (id.includes('joi')) {
              return 'joi';
            }
          }
        }
      }
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
    preview: {
      port: 4000,
      open: false,
      proxy: {
        '/api': {
          target: 'http://akhq:8080'
        }
      }
    },
    plugins: [react()],
    test: {
      environment: 'jsdom'
    }
  };
});
