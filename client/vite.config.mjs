import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    base: '/ui',
    build: {
      outDir: 'build',
      sourcemap: true,
      rollupOptions: {
        onLog(level, log, handler) {
          if (log.cause && log.cause.message === `Can't resolve original location of error.`) {
            return;
          }
          handler(level, log);
        },
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
          target: env.APP_BASE_URL || 'http://localhost:8080'
        },
        '/login': {
          target: env.APP_BASE_URL || 'http://localhost:8080'
        },
        '/logout': {
          target: env.APP_BASE_URL || 'http://localhost:8080'
        },
        '/oauth': {
          target: env.APP_BASE_URL || 'http://localhost:8080'
        }
      }
    },
    preview: {
      port: 4000,
      open: false,
      proxy: {
        '/api': {
          target: 'http://akhq:8080'
        },
        '/login': {
          target: 'http://akhq:8080'
        },
        '/logout': {
          target: 'http://akhq:8080'
        },
        '/oauth': {
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
