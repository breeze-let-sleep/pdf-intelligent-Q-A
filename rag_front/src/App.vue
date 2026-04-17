/**
 * App.vue
 * Vue 应用根组件
 * 包含顶部导航栏（Logo + 主题切换）和路由视图
 */

<script setup>
/**
 * useDark: 响应式暗色模式状态（监听系统偏好 + 本地存储）
 * useToggle: 切换暗色模式的开关函数
 * @vueuse/core 库提供，基于 prefers-color-scheme媒体查询
 */
import { useDark, useToggle } from '@vueuse/core'
import { SunIcon, MoonIcon } from '@heroicons/vue/24/outline'

// 初始化暗色模式状态
const isDark = useDark()
// 创建切换函数，调用后自动更新状态并同步到 localStorage / CSS 变量
const toggleDark = useToggle(isDark)
</script>

<template>
  <!--
    app 容器：根据 isDark 状态切换 dark 类，
    CSS 变量 --bg-color / --text-color 在 .dark 下自动切换颜色
  -->
  <div class="app" :class="{ 'dark': isDark }">
    <!-- 顶部导航栏 -->
    <nav class="navbar">
      <!-- Logo 链接，点击回到首页（根路径） -->
      <router-link to="/" class="logo">ChatPDF</router-link>

      <!-- 主题切换按钮：夜间显示太阳图标（点击切换到亮色），白天显示月亮图标 -->
      <button @click="toggleDark()" class="theme-toggle" :title="isDark ? '切换到亮色模式' : '切换到暗色模式'">
        <SunIcon v-if="isDark" class="icon" />
        <MoonIcon v-else class="icon" />
      </button>
    </nav>

    <!-- 路由视图：根据 URL 渲染对应页面组件，带淡入淡出过渡 -->
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<style lang="scss">
/* ==================== CSS 变量定义 ==================== */
/* 亮色模式变量（默认） */
:root {
  --bg-color: #f5f5f5;
  --text-color: #333;
}

/* 暗色模式变量（添加 .dark 类时生效） */
.dark {
  --bg-color: #1a1a1a;
  --text-color: #fff;
}

/* ==================== 全局布局 ==================== */
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-color);
  color: var(--text-color);
}

/* ==================== 顶部导航栏 ==================== */
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  /* 毛玻璃效果：背景半透明 + 模糊 */
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);

  /* Logo 文字样式：渐变色文字 */
  .logo {
    font-size: 1.5rem;
    font-weight: bold;
    text-decoration: none;
    color: inherit;
    background: linear-gradient(45deg, #9333ea, #c026d3);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  /* 主题切换按钮 */
  .theme-toggle {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0.5rem;
    border-radius: 50%;
    transition: background-color 0.3s;
    display: flex;
    align-items: center;
    justify-content: center;

    /* 悬停时显示浅色背景 */
    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    .icon {
      width: 24px;
      height: 24px;
      color: var(--text-color);
    }
  }

  /* 暗色模式下的导航栏样式调整 */
  .dark & {
    background: rgba(0, 0, 0, 0.2);
    border-bottom-color: rgba(255, 255, 255, 0.05);
  }
}

/* ==================== 路由过渡动画 ==================== */
/* 淡入淡出效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 768px) {
  .navbar {
    padding: 1rem;
  }
}
</style>
