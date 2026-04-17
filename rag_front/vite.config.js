import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * vite.config.js
 * Vite 构建工具配置文件
 * 用于配置项目构建选项、插件、别名等
 */
export default defineConfig({
  // 注册 Vue 插件，支持 .vue 文件解析
  plugins: [vue()],
  // 路径别名配置，方便使用 @/ 引用 src 目录
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
