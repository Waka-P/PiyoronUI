// Web 版のビルドと開発用サーバーの設定
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';

export default defineConfig({
  plugins: [react()],
  server: {
    open: '/demo/index.html',
    fs: { allow: ['..'] },
  },
  build: {
    lib: {
      entry: { piyoron: resolve(import.meta.dirname, 'src/index.js'), react: resolve(import.meta.dirname, 'src/react/index.js') },
      formats: ['es'],
    },
    rollupOptions: {
      external: [/^react($|\/)/, /^react-dom($|\/)/, /^@radix-ui\//],
    },
  },
});
