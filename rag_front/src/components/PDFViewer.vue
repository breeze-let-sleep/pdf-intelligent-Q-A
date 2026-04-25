<template>
  <!--
    PDFViewer 组件
    支持两种 PDF 预览模式：
      1. File 对象 → Blob URL → iframe（上传时使用）
      2. URL 直接加载 → iframe（从 OSS 加载历史文件时使用）
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
 * 实现方式：
 *   - File 对象 → Blob URL → iframe src（上传场景）
 *   - URL 直接 → iframe src（OSS URL 场景）
 * 支持：文件切换、暗色模式背景、资源清理
 */

import { ref, watch, onMounted, onUnmounted } from 'vue'
import { DocumentTextIcon } from '@heroicons/vue/24/outline'
import { useDark } from '@vueuse/core'

// ==================== 响应式数据 ====================
const isDark = useDark()

// Props 定义
const props = defineProps({
  /**
   * PDF 文件对象（上传场景使用）
   * 与 fileUrl 二选一
   */
  file: {
    type: [File, null],
    default: null
  },
  /**
   * PDF 文件 URL（OSS 地址，历史记录场景使用）
   * 与 file 二选一
   */
  fileUrl: {
    type: String,
    default: ''
  },
  /**
   * 文件名（显示在头部）
   */
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
 * createIframeFromFile - 通过 File 对象创建 iframe
 * @param {File} file - PDF 文件对象
 * @returns {Object} { iframe, blobUrl } iframe 元素和 Blob URL
 *
 * 原理：File 对象通过 URL.createObjectURL 转为浏览器可访问的临时 URL
 * iframe 加载该 URL 实现 PDF 预览（浏览器内置 PDF 阅读器）
 */
const createIframeFromFile = (file) => {
  const iframe = document.createElement('iframe')
  iframe.style.width = '100%'
  iframe.style.height = '100%'
  iframe.style.border = 'none'

  // 创建 Blob URL（临时内存地址，无需上传到服务器）
  const blobUrl = URL.createObjectURL(file)

  // 根据当前主题设置 iframe 背景色
  iframe.style.backgroundColor = isDark.value ? '#1a1a1a' : '#ffffff'
  iframe.src = blobUrl

  return { iframe, blobUrl, isBlobUrl: true }
}

/**
 * createIframeFromUrl - 通过 URL 创建 iframe（OSS URL 场景）
 * @param {string} url - PDF 文件的 URL 地址
 * @returns {Object} { iframe } iframe 元素
 *
 * 原理：直接将 OSS URL 设置为 iframe 的 src
 * 浏览器内置 PDF 阅读器加载远程 PDF 文件
 * 注意：OSS URL 需要支持跨域访问，否则可能无法预览
 */
const createIframeFromUrl = (url) => {
  const iframe = document.createElement('iframe')
  iframe.style.width = '100%'
  iframe.style.height = '100%'
  iframe.style.border = 'none'

  // 根据当前主题设置 iframe 背景色
  iframe.style.backgroundColor = isDark.value ? '#1a1a1a' : '#ffffff'

  // 使用 Google Docs Viewer 或浏览器原生预览
  // 对于 OSS URL，直接设置 src，浏览器会根据 Content-Type 决定是预览还是下载
  // 如果 OSS 返回的 Content-Type 是 application/pdf 且 Content-Disposition 是 inline，则会预览
  iframe.src = url

  return { iframe, blobUrl: null, isBlobUrl: false }
}

/**
 * renderPdf - 渲染 PDF 到容器中
 * 统一处理 File 对象和 URL 两种预览模式
 *
 * 修复：添加错误处理，当 iframe 加载失败时（如 404、跨域错误）
 * 自动隐藏加载状态，避免一直显示"正在加载 PDF..."
 */
const renderPdf = () => {
  if (!viewerRef.value) return

  let result = null

  if (props.file) {
    // 优先使用 File 对象（上传场景）
    result = createIframeFromFile(props.file)
  } else if (props.fileUrl) {
    // 使用 URL 直接加载（OSS/后端代理场景）
    result = createIframeFromUrl(props.fileUrl)
  } else {
    // 没有可渲染的内容
    return
  }

  isLoading.value = true

  // 清空容器并添加 iframe
  viewerRef.value.innerHTML = ''
  viewerRef.value.appendChild(result.iframe)

  // 监听 iframe 加载完成事件
  result.iframe.onload = () => {
    isLoading.value = false
  }

  // 监听 iframe 加载错误事件
  // 当 URL 返回 404、跨域错误、或 Content-Disposition: attachment 时触发
  result.iframe.onerror = () => {
    isLoading.value = false
    console.error('PDF 加载失败，请检查文件 URL 是否可访问:', props.fileUrl)
  }

  // 设置超时处理：如果 10 秒内没有触发 onload，自动隐藏加载状态
  const loadTimeout = setTimeout(() => {
    if (isLoading.value) {
      isLoading.value = false
      console.warn('PDF 加载超时，请检查网络或文件 URL:', props.fileUrl)
    }
  }, 10000)

  // 加载完成后清除超时定时器
  result.iframe.onload = () => {
    clearTimeout(loadTimeout)
    isLoading.value = false
  }

  // 清理旧的 Blob URL 资源（防止内存泄漏）
  if (instance?.isBlobUrl && instance?.blobUrl) {
    URL.revokeObjectURL(instance.blobUrl)
  }

  // 保存实例用于后续清理
  instance = result
}

// ==================== 生命周期钩子 ====================

/**
 * onMounted - 组件挂载时初始化
 * 如果传入了 file 或 fileUrl，立即渲染 PDF
 */
onMounted(() => {
  renderPdf()
})

/**
 * onUnmounted - 组件卸载时清理资源
 * 必须释放 Blob URL，防止内存泄漏
 */
onUnmounted(() => {
  if (instance?.isBlobUrl && instance?.blobUrl) {
    URL.revokeObjectURL(instance.blobUrl)
  }
})

// ==================== 监听器 ====================

/**
 * 监听 file 变化：上传新文件时重新渲染
 */
watch(() => props.file, () => {
  renderPdf()
})

/**
 * 监听 fileUrl 变化：切换历史记录时重新渲染
 */
watch(() => props.fileUrl, () => {
  renderPdf()
})

/**
 * 监听暗色模式变化：动态调整 iframe 背景色
 */
watch(() => isDark.value, (newIsDark) => {
  if (instance?.iframe) {
    instance.iframe.style.backgroundColor = newIsDark ? '#1a1a1a' : '#ffffff'
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
