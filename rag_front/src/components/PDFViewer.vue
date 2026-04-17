<template>
  <!--
    PDFViewer 组件
    使用 iframe + Blob URL 实现 PDF 预览
    优点：无需额外 PDF 库，浏览器原生支持，支持暗色模式背景
  -->
  <div class="pdf-view">
    <!-- PDF 头部：显示文件名 -->
    <div class="pdf-header">
      <DocumentTextIcon class="icon" />
      <span class="filename">{{ fileName }}</span>
    </div>

    <!-- PDF 内容区域 -->
    <div class="pdf-content">
      <!-- 加载状态：上传或加载 PDF 时显示 -->
      <div v-if="isLoading" class="pdf-loading">
        <div class="loading-spinner"></div>
        <p class="loading-text">正在加载 PDF...</p>
      </div>

      <!-- PDF 容器：iframe 挂载点 -->
      <div class="pdf-container" ref="viewerRef"></div>
    </div>
  </div>
</template>

<script setup>
/**
 * PDFViewer 组件
 * 职责：渲染 PDF 文件预览
 * 实现方式：File → Blob URL → iframe src
 * 支持：文件切换、暗色模式背景、资源清理
 */

import { ref, watch, onMounted, onUnmounted } from 'vue'
import { DocumentTextIcon } from '@heroicons/vue/24/outline'
import { useDark } from '@vueuse/core'

// ==================== 响应式数据 ====================
// useDark: 监听系统/用户暗色模式偏好
const isDark = useDark()

// Props 定义
const props = defineProps({
  file: {
    type: [File, null],
    default: null
  },
  fileName: {
    type: String,
    default: ''
  }
})

// 加载状态
const isLoading = ref(false)
// PDF 容器 DOM 引用
const viewerRef = ref(null)
// iframe 实例引用（用于清理）
let instance = null

// ==================== 核心方法 ====================

/**
 * createIframe - 创建 iframe 并设置 Blob URL
 * @param {File} file - PDF 文件对象
 * @returns {Object} { iframe, url } iframe 元素和 Blob URL
 *
 * 原理：File 对象通过 URL.createObjectURL 转为浏览器可访问的临时 URL
 * iframe 加载该 URL 实现 PDF 预览（浏览器内置 PDF 阅读器）
 */
const createIframe = (file) => {
  const iframe = document.createElement('iframe')
  iframe.style.width = '100%'
  iframe.style.height = '100%'
  iframe.style.border = 'none'

  // 创建 Blob URL（临时内存地址，无需上传到服务器）
  const url = URL.createObjectURL(file)

  // 根据当前主题设置 iframe 背景色（浏览器 PDF 阅读器支持）
  if (isDark.value) {
    iframe.style.backgroundColor = '#1a1a1a'
  } else {
    iframe.style.backgroundColor = '#ffffff'
  }

  iframe.src = url
  return { iframe, url }
}

// ==================== 生命周期钩子 ====================

/**
 * onMounted - 组件挂载时初始化
 * 如果传入 file，立即渲染 PDF
 */
onMounted(() => {
  if (viewerRef.value && props.file) {
    try {
      isLoading.value = true

      // 创建 iframe
      const { iframe, url } = createIframe(props.file)

      // 清空容器并添加 iframe
      viewerRef.value.innerHTML = ''
      viewerRef.value.appendChild(iframe)

      // 监听 iframe 加载完成事件
      iframe.onload = () => {
        isLoading.value = false
      }

      // 保存实例用于后续清理
      instance = { url, iframe }

    } catch (error) {
      console.error('PDF 查看器初始化失败:', error)
      isLoading.value = false
    }
  }
})

/**
 * onUnmounted - 组件卸载时清理资源
 * 必须释放 Blob URL，防止内存泄漏
 */
onUnmounted(() => {
  if (instance?.url) {
    URL.revokeObjectURL(instance.url)
  }
})

// ==================== 监听器 ====================

/**
 * 监听 file 变化：切换 PDF 文件时重新渲染
 */
watch(() => props.file, (newFile) => {
  if (newFile) {
    // 清理旧资源
    if (instance?.url) {
      URL.revokeObjectURL(instance.url)
    }

    try {
      isLoading.value = true

      const { iframe, url } = createIframe(newFile)

      if (viewerRef.value) {
        viewerRef.value.innerHTML = ''
        viewerRef.value.appendChild(iframe)
      }

      iframe.onload = () => {
        isLoading.value = false
      }

      instance = { url, iframe }

    } catch (error) {
      console.error('加载 PDF 失败:', error)
      isLoading.value = false
    }
  }
})

/**
 * 监听暗色模式变化：动态调整 iframe 背景色
 * 注意：部分浏览器 PDF 阅读器可能不支持动态背景色切换
 */
watch(() => isDark.value, (newIsDark) => {
  if (instance?.iframe) {
    if (newIsDark) {
      instance.iframe.style.backgroundColor = '#1a1a1a'
    } else {
      instance.iframe.style.backgroundColor = '#ffffff'
    }
  }
})
</script>

<style scoped lang="scss">
.pdf-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(0, 0, 0, 0.1);
  background: #fff;

  /* PDF 头部：文件名显示 */
  .pdf-header {
    padding: 1rem;
    display: flex;
    align-items: center;
    gap: 1rem;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    background: rgba(255, 255, 255, 0.98);
    z-index: 1;

    .icon {
      width: 1.5rem;
      height: 1.5rem;
      color: #666;
    }

    .filename {
      flex: 1;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  /* PDF 内容区域 */
  .pdf-content {
    flex: 1;
    position: relative;
    overflow: hidden;

    .pdf-container {
      width: 100%;
      height: 100%;
    }

    /* 加载状态遮罩 */
    .pdf-loading {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      background: rgba(255, 255, 255, 0.9);
      padding: 2rem;
      border-radius: 1rem;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      z-index: 2;

      .loading-spinner {
        width: 48px;
        height: 48px;
        border: 4px solid rgba(0, 124, 240, 0.1);
        border-left-color: #007CF0;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }

      .loading-text {
        color: #666;
        font-size: 1rem;
        font-weight: 500;
      }
    }
  }
}

/* 旋转动画 */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 暗色模式样式 */
.dark {
  .pdf-view {
    background: #1a1a1a;
    border-right-color: rgba(255, 255, 255, 0.1);

    .pdf-header {
      background: rgba(30, 30, 30, 0.98);
      border-bottom-color: rgba(255, 255, 255, 0.1);

      .icon { color: #999; }
      .filename { color: #fff; }
    }

    .pdf-content {
      background: #0d0d0d;

      .pdf-loading {
        background: rgba(30, 30, 30, 0.9);

        .loading-spinner {
          border-color: rgba(0, 124, 240, 0.2);
          border-left-color: #007CF0;
        }

        .loading-text { color: #999; }
      }
    }
  }
}
</style>
