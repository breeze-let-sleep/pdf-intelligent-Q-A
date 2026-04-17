/**
 * router/index.js
 * Vue Router 路由配置文件
 * 定义应用的所有路由规则
 */

import { createRouter, createWebHistory } from 'vue-router'
// ChatPDF 主页面组件（懒加载，访问时才加载，减小首屏体积）
import ChatPDF from '../views/ChatPDF.vue'

/**
 * 路由表
 * path: URL 路径
 * name: 路由名称（用于编程式导航）
 * component: 对应的 Vue 组件
 */
const routes = [
  {
    // 根路径默认显示 ChatPDF 页面
    path: '/',
    name: 'ChatPDF',
    component: ChatPDF
  },
  {
    // /chat-pdf 路径也指向 ChatPDF 组件，保持兼容性
    path: '/chat-pdf',
    name: 'ChatPDFAlias',
    component: ChatPDF
  }
]

// 创建路由实例
const router = createRouter({
  // 使用 HTML5 History 模式（URL 不带 #，需要服务器配合重定向）
  history: createWebHistory(),
  routes
})

export default router
