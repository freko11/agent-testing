import { defineConfig } from 'vitest/config'

// Deliberately separate from vite.config.ts's dev-server proxy setup — this project's first
// test runner (E3-F2-S1), scoped narrowly to pure logic (no DOM/jsdom needed yet).
export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
