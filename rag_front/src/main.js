/**
 * main.js
 * Vue 应用入口文件
 * 负责初始化 Vue 实例、挂载 Pinia 状态管理、注册路由
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Pinia 状态管理插件
app.use(createPinia())

// 注册路由插件
app.use(router)

// 挂载到 #app DOM 节点
app.mount('#app')
