<template>
  <!--
    ChatPDF 主页面组件
    整体布局：左侧边栏（历史记录）+ 右侧主内容区
    主内容区根据状态切换：上传欢迎页 或 分栏视图（PDF预览 + 聊天）
  -->
  <div class="chat-pdf" :class="{ 'dark': isDark }">
    <div class="chat-container">

      <!-- ==================== 左侧边栏 ==================== -->
      <div class="sidebar">
        <!-- 边栏头部：Logo -->
        <div class="sidebar-header">
          <a href="#" class="logo-link" @click="handleLogoClick">
            <DocumentTextIcon class="logo" />
            <h1 class="title">ChatPDF</h1>
          </a>
        </div>

        <!-- 历史记录列表 -->
        <div class="history-list">
          <div class="history-header">
            <span>历史记录</span>
            <!-- 新建对话按钮 -->
            <button class="new-chat-btn" @click="startNewChat">
              <PlusIcon class="icon" />
              新聊天
            </button>
          </div>

          <!-- 历史记录项 -->
          <div
            v-for="chat in chatHistory"
            :key="chat.id"
            class="history-item"
            :class="{ 'active': currentChatId === chat.id }"
            @click="loadChat(chat.id)"
          >
            <DocumentTextIcon class="icon" />
            <span class="title">{{ chat.title || 'PDF对话' }}</span>
          </div>
        </div>
      </div>

      <!-- ==================== 主内容区域 ==================== -->
      <div class="chat-main">

        <!-- 状态 1：未上传文件 - 显示上传欢迎页 -->
        <div v-if="!currentPdfName" class="upload-welcome">
          <h1 class="main-title">
            与任何 <span class="highlight">PDF</span> 对话
          </h1>

          <!-- 拖拽上传区域 -->
          <div
            class="drop-zone"
            @dragover.prevent="handleDragOver"
            @dragleave.prevent="handleDragLeave"
            @drop.prevent="handleDrop"
            :class="{
              'dragging': isDragging,
              'uploading': isUploading
            }"
          >
            <div class="upload-content">
              <!-- 上传中状态 -->
              <div v-if="isUploading" class="upload-status">
                <div class="spinner"></div>
                <div class="upload-progress">
                  <p class="status-text">正在上传文件...</p>
                  <p class="filename">{{ uploadingFileName }}</p>
                </div>
              </div>

              <!-- 默认上传界面 -->
              <template v-else>
                <DocumentArrowUpIcon class="upload-icon" />
                <p class="upload-text">点击上传，或将PDF拖拽到此处</p>
                <input
                  type="file"
                  accept=".pdf"
                  @change="handleFileUpload"
                  :disabled="isUploading"
                  class="file-input"
                >
                <button
                  class="upload-button"
                  :class="{ 'uploading': isUploading }"
                  @click="triggerFileInput"
                >
                  <ArrowUpTrayIcon class="icon" />
                  上传PDF
                </button>
              </template>
            </div>
          </div>
        </div>

        <!-- 状态 2：已上传文件 - 显示分栏视图 -->
        <div v-else class="split-view">
          <!-- 左侧：PDF 预览 -->
          <PDFViewer
            :file="pdfFile"
            :fileName="currentPdfName"
          />

          <!-- 右侧：聊天区域 -->
          <div class="chat-view">
            <!-- 消息列表 -->
            <div class="messages" ref="messagesRef">
              <ChatMessage
                v-for="(message, index) in currentMessages"
                :key="index"
                :message="message"
              />
            </div>

            <!-- 输入区域 -->
            <div class="input-area">
              <textarea
                v-model="userInput"
                @keydown.enter.prevent="sendMessage()"
                placeholder="请输入您的问题..."
                rows="1"
                ref="inputRef"
              ></textarea>
              <button
                class="send-button"
                @click="sendMessage()"
                :disabled="isStreaming || !userInput.trim()"
              >
                <PaperAirplaneIcon class="icon" />
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * ChatPDF 主页面组件
 * 职责：
 *   1. 管理 PDF 上传（拖拽/点击）
 *   2. 显示 PDF 预览
 *   3. 与 AI 进行流式对话
 *   4. 管理对话历史
 *
 * 核心状态：
 *   - currentPdfName: 当前 PDF 文件名（控制视图切换）
 *   - pdfFile: PDF File 对象（传给 PDFViewer）
 *   - currentChatId: 当前对话 ID
 *   - currentMessages: 当前对话消息列表
 *   - chatHistory: 历史对话列表
 *   - isStreaming: 是否正在接收流式响应
 */

import { ref, onMounted, nextTick } from 'vue'
import { useDark } from '@vueuse/core'
import { useRouter } from 'vue-router'
import {
  DocumentArrowUpIcon,
  DocumentTextIcon,
  PaperAirplaneIcon,
  ArrowUpTrayIcon,
  PlusIcon
} from '@heroicons/vue/24/outline'
import ChatMessage from '../components/ChatMessage.vue'
import PDFViewer from '../components/PDFViewer.vue'
import { chatAPI } from '../services/api'

// ==================== 响应式数据 ====================
const isDark = useDark()
const router = useRouter()

// DOM 引用
const messagesRef = ref(null)
const inputRef = ref(null)

// 用户输入
const userInput = ref('')

// 状态标志
const isStreaming = ref(false)
const isUploading = ref(false)
const isDragging = ref(false)
const uploadingFileName = ref('')

// 当前对话
const currentChatId = ref(null)
const currentMessages = ref([])
const currentPdfName = ref('')
const pdfFile = ref(null)

// 历史记录
const chatHistory = ref([])

// 后端地址
const BASE_URL = 'http://localhost:8080'

// ==================== 生命周期 ====================

/**
 * onMounted - 页面加载时初始化
 * 加载历史对话列表，如果有历史记录则自动加载第一个
 */
onMounted(() => {
  loadChatHistory()
})

// ==================== 核心方法 ====================

/**
 * loadChatHistory - 加载聊天历史列表
 * 从后端获取所有 PDF 类型对话的 ID 列表
 */
const loadChatHistory = async () => {
  try {
    const history = await chatAPI.getChatHistory('pdf')
    chatHistory.value = history || []
    // 如果有历史记录，自动加载第一个
    if (history && history.length > 0) {
      await loadChat(history[0].id)
    }
  } catch (error) {
    console.error('加载聊天历史失败:', error)
    chatHistory.value = []
  }
}

/**
 * loadChat - 加载指定对话
 * @param {string} chatId - 对话 ID
 * 流程：
 *   1. 清理当前状态
 *   2. 加载消息历史
 *   3. 从服务器下载 PDF 文件
 */
const loadChat = async (chatId) => {
  if (!chatId) return

  cleanupResources()
  currentChatId.value = chatId

  try {
    // 加载消息历史
    const messages = await chatAPI.getChatMessages(chatId, 'pdf')
    currentMessages.value = messages.map(msg => ({
      ...msg,
      isMarkdown: msg.role === 'assistant'
    }))

    // 从服务器获取 PDF
    const file = await chatAPI.downloadPdf(chatId)

    // 更新文件名
    currentPdfName.value = file.name

    // 更新历史记录中的标题
    const chatIndex = chatHistory.value.findIndex(c => c.id === chatId)
    if (chatIndex !== -1) {
      chatHistory.value[chatIndex].title = file.name
    }

    pdfFile.value = file

  } catch (error) {
    console.error('加载失败:', error)
    currentMessages.value.push({
      role: 'assistant',
      content: '加载失败，请重试。',
      timestamp: new Date(),
      isMarkdown: true
    })
  }
}

/**
 * cleanupResources - 清理资源
 * 重置所有状态，用于新建对话或切换对话
 */
const cleanupResources = () => {
  currentPdfName.value = ''
  currentMessages.value = []
  pdfFile.value = null
  currentChatId.value = null
  isUploading.value = false
  uploadingFileName.value = ''
  userInput.value = ''
  isStreaming.value = false

  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
}

/**
 * startNewChat - 开始新对话
 * 清理所有状态，回到上传界面
 */
const startNewChat = () => {
  cleanupResources()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = 0
  }
}

/**
 * handleLogoClick - 点击 Logo 返回首页
 */
const handleLogoClick = (event) => {
  event.preventDefault()
  cleanupResources()
  router.push('/')
}

// ==================== 文件上传 ====================

/**
 * triggerFileInput - 触发文件选择框
 */
const triggerFileInput = () => {
  const fileInput = document.querySelector('.file-input')
  fileInput.click()
}

/**
 * handleFileUpload - 处理文件选择上传
 * @param {Event} event - 文件选择事件
 */
const handleFileUpload = async (event) => {
  const files = event.target.files
  if (files.length === 0) return

  const file = files[0]

  // 校验文件类型
  if (file.type !== 'application/pdf') {
    alert('请上传 PDF 文件')
    return
  }

  await uploadFile(file)

  // 清空文件输入，允许重新选择同一文件
  event.target.value = ''
}

/**
 * handleDrop - 处理拖拽上传
 * @param {DragEvent} event - 拖拽事件
 */
const handleDrop = async (event) => {
  event.preventDefault()
  isDragging.value = false

  const files = event.dataTransfer.files
  if (files.length === 0) return

  const file = files[0]

  // 校验文件类型
  if (file.type !== 'application/pdf') {
    alert('请上传 PDF 文件')
    return
  }

  await uploadFile(file)
}

/**
 * handleDragOver - 拖拽悬停
 */
const handleDragOver = (event) => {
  event.preventDefault()
  isDragging.value = true
}

/**
 * handleDragLeave - 拖拽离开
 */
const handleDragLeave = (event) => {
  event.preventDefault()
  isDragging.value = false
}

/**
 * uploadFile - 上传文件到服务器
 * @param {File} file - PDF 文件
 *
 * 接口: POST /ai/pdf/upload/{chatId}
 * 后端返回: { chatId }
 */
const uploadFile = async (file) => {
  isUploading.value = true
  uploadingFileName.value = file.name

  try {
    const formData = new FormData()
    formData.append('file', file)

    // 生成临时 chatId
    const uploadChatId = currentChatId.value || `pdf_${Date.now()}`

    const response = await fetch(`${BASE_URL}/ai/pdf/upload/${uploadChatId}`, {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error(`上传失败: ${response.status}`)
    }

    const data = await response.json()

    // 保存状态
    currentChatId.value = data.chatId || uploadChatId
    currentPdfName.value = file.name
    pdfFile.value = file

    // 添加到历史记录
    const newChat = {
      id: currentChatId.value,
      title: file.name
    }

    if (!chatHistory.value.some(chat => chat.id === currentChatId.value)) {
      // 添加新会话到历史记录的头部
      chatHistory.value = [newChat, ...chatHistory.value]
    }

    // 清空消息并添加系统欢迎消息
    currentMessages.value = []
    currentMessages.value.push({
      role: 'assistant',
      content: `已上传 PDF 文件: ${file.name}。您可以开始提问了。`,
      timestamp: new Date(),
      isMarkdown: true
    })

  } catch (error) {
    console.error('上传失败:', error)
    alert('文件上传失败，请重试')
  } finally {
    isUploading.value = false
    uploadingFileName.value = ''
  }
}

// ==================== 消息发送 ====================

/**
 * scrollToBottom - 滚动消息列表到底部
 */
const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

/**
 * sendMessage - 发送消息
 * 流程：
 *   1. 添加用户消息到列表
 *   2. 清空输入框
 *   3. 添加空的 AI 消息容器
 *   4. 发送请求并消费流式响应
 *   5. 逐字符更新 AI 消息内容
 */
const sendMessage = async () => {
  if (!userInput.value.trim() || isStreaming.value) return

  // 添加用户消息
  const userMessage = {
    role: 'user',
    content: userInput.value,
    timestamp: new Date()
  }
  currentMessages.value.push(userMessage)

  // 保存输入并清空
  const input = userInput.value
  userInput.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }

  await scrollToBottom()

  // 添加 AI 消息容器
  const assistantMessageIndex = currentMessages.value.length
  currentMessages.value.push({
    role: 'assistant',
    content: '',
    timestamp: new Date(),
    isMarkdown: true
  })

  try {
    isStreaming.value = true

    // 发送请求获取流式读取器
    const reader = await chatAPI.sendPdfMessage(input, currentChatId.value)
    const decoder = new TextDecoder()
    let result = ''

    // 消费流式响应
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      result += chunk

      // 更新 AI 消息内容（强制触发响应式更新）
      currentMessages.value[assistantMessageIndex] = {
        role: 'assistant',
        content: result,
        timestamp: new Date(),
        isMarkdown: true
      }

      await nextTick()
      await scrollToBottom()
    }

  } catch (error) {
    console.error('发送消息失败:', error)
    currentMessages.value[assistantMessageIndex] = {
      role: 'assistant',
      content: '发送消息失败，请重试。',
      timestamp: new Date(),
      isMarkdown: true
    }
  } finally {
    isStreaming.value = false
    await scrollToBottom()
  }
}
</script>

<style scoped lang="scss">
.chat-pdf {
  position: fixed;
  top: 64px; /* 导航栏高度 */
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  background: var(--bg-color);
  overflow: hidden;

  .chat-container {
    flex: 1;
    display: flex;
    max-width: 1800px;
    width: 100%;
    margin: 0 auto;
    padding: 1.5rem 2rem;
    gap: 1.5rem;
    height: 100%;
    overflow: hidden;
  }

  /* ==================== 左侧边栏 ==================== */
  .sidebar {
    width: 300px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 1rem;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .sidebar-header {
      padding: 1.5rem;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);

      .logo-link {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        text-decoration: none;
        color: inherit;
        transition: opacity 0.2s;

        &:hover { opacity: 0.8; }
      }

      .logo {
        width: 2rem;
        height: 2rem;
        color: #9333ea;
      }

      .title {
        font-size: 1.5rem;
        font-weight: bold;
        background: linear-gradient(120deg, #9333ea 0%, #c026d3 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }

    .history-list {
      flex: 1;
      overflow-y: auto;
      padding: 1rem 0;

      .history-header {
        padding: 0.5rem 1.5rem;
        display: flex;
        align-items: center;
        justify-content: space-between;

        span {
          font-size: 0.875rem;
          font-weight: 500;
          color: #666;
          text-transform: uppercase;
        }

        .new-chat-btn {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          padding: 0.5rem 0.75rem;
          border: none;
          border-radius: 0.5rem;
          background: #9333ea;
          color: white;
          font-size: 0.875rem;
          cursor: pointer;
          transition: all 0.2s;

          &:hover { background: #7e22ce; }

          .icon {
            width: 1rem;
            height: 1rem;
          }
        }
      }

      .history-item {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.75rem 1.5rem;
        cursor: pointer;
        transition: background-color 0.2s;

        &:hover { background: rgba(0, 0, 0, 0.05); }

        &.active {
          background: rgba(147, 51, 234, 0.1);

          .icon { color: #9333ea; }
          .title { color: #9333ea; }
        }

        .icon {
          width: 1.25rem;
          height: 1.25rem;
          color: #666;
        }

        .title {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          color: #333;
        }
      }
    }
  }

  /* ==================== 主内容区 ==================== */
  .chat-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 1rem;
    overflow: hidden;
  }

  /* ==================== 上传欢迎页 ==================== */
  .upload-welcome {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 2rem;
    gap: 3rem;

    .main-title {
      font-size: 3rem;
      font-weight: bold;
      text-align: center;

      .highlight {
        color: #9333ea;
        background: linear-gradient(120deg, #9333ea 0%, #c026d3 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }

    .drop-zone {
      width: 100%;
      max-width: 600px;
      min-height: 300px;
      border: 2px dashed #e5e7eb;
      border-radius: 1rem;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;
      background: rgba(255, 255, 255, 0.5);
      backdrop-filter: blur(10px);

      &.dragging {
        border-color: #9333ea;
        background: rgba(147, 51, 234, 0.05);
      }

      &.uploading {
        border-style: dashed;
        border-color: #007CF0;
        background: rgba(0, 124, 240, 0.05);
      }

      .upload-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
        padding: 2rem;

        .upload-status {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 1.5rem;

          .spinner {
            width: 48px;
            height: 48px;
            border: 4px solid rgba(0, 124, 240, 0.1);
            border-left-color: #007CF0;
            border-radius: 50%;
            animation: spin 1s linear infinite;
          }

          .upload-progress {
            text-align: center;

            .status-text {
              font-size: 1.25rem;
              color: #007CF0;
              margin-bottom: 0.5rem;
            }

            .filename {
              font-size: 0.875rem;
              color: #666;
              max-width: 300px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }
        }

        .upload-icon {
          width: 4rem;
          height: 4rem;
          color: #9333ea;
        }

        .upload-text {
          font-size: 1.25rem;
          color: #666;
        }

        .file-input { display: none; }

        .upload-button {
          background: #9333ea;
          color: white;
          border: none;
          padding: 0.75rem 2rem;
          border-radius: 0.5rem;
          font-size: 1rem;
          display: flex;
          align-items: center;
          gap: 0.5rem;
          cursor: pointer;
          transition: all 0.3s ease;

          &:hover { background: #7e22ce; }

          &.uploading {
            background: #9333ea80;
            cursor: not-allowed;
          }

          .icon {
            width: 1.25rem;
            height: 1.25rem;
          }
        }
      }
    }
  }

  /* ==================== 分栏视图 ==================== */
  .split-view {
    flex: 1;
    display: flex;
    overflow: hidden;

    .chat-view {
      flex: 1;
      min-width: 400px;
      max-width: 50%;
      display: flex;
      flex-direction: column;
      background: #fff;

      .messages {
        flex: 1;
        overflow-y: auto;
        padding: 1.5rem;
      }

      .input-area {
        flex-shrink: 0;
        padding: 1.5rem 2rem;
        background: rgba(255, 255, 255, 0.98);
        border-top: 1px solid rgba(0, 0, 0, 0.05);
        display: flex;
        gap: 1rem;
        align-items: flex-end;

        textarea {
          flex: 1;
          resize: none;
          border: 1px solid rgba(0, 0, 0, 0.1);
          background: white;
          border-radius: 0.75rem;
          padding: 1rem;
          color: inherit;
          font-family: inherit;
          font-size: 1rem;
          line-height: 1.5;
          max-height: 150px;

          &:focus {
            outline: none;
            border-color: #333;
            box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
          }

          &:disabled {
            background: #f5f5f5;
            cursor: not-allowed;
          }
        }

        .send-button {
          background: #333;
          color: white;
          border: none;
          border-radius: 0.5rem;
          width: 2.5rem;
          height: 2.5rem;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: background-color 0.3s;

          &:hover:not(:disabled) { background: #000; }

          &:disabled {
            background: #ccc;
            cursor: not-allowed;
          }

          .icon {
            width: 1.25rem;
            height: 1.25rem;
          }
        }
      }
    }
  }
}

/* ==================== 动画 ==================== */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ==================== 暗色模式 ==================== */
.dark {
  .chat-pdf {
    .sidebar {
      background: rgba(40, 40, 40, 0.95);

      .sidebar-header {
        border-bottom-color: rgba(255, 255, 255, 0.05);
      }

      .history-list {
        .history-header {
          span { color: #999; }

          .new-chat-btn {
            background: rgba(147, 51, 234, 0.8);
            &:hover { background: #9333ea; }
          }
        }

        .history-item {
          &:hover { background: rgba(255, 255, 255, 0.05); }
          &.active { background: rgba(147, 51, 234, 0.15); }
          .icon { color: #999; }
          .title { color: #fff; }
        }
      }
    }

    .chat-main {
      background: rgba(40, 40, 40, 0.95);
    }

    .upload-welcome {
      .drop-zone {
        border-color: #444;
        background: rgba(40, 40, 40, 0.5);

        &.dragging {
          border-color: #9333ea;
          background: rgba(147, 51, 234, 0.1);
        }

        &.uploading {
          border-color: #007CF0;
          background: rgba(0, 124, 240, 0.1);
        }

        .upload-content {
          .upload-status {
            .spinner {
              border-color: rgba(0, 124, 240, 0.2);
              border-left-color: #007CF0;
            }

            .upload-progress {
              .status-text { color: #007CF0; }
              .filename { color: #999; }
            }
          }
        }
      }
    }

    .split-view {
      .chat-view {
        background: #1a1a1a;

        .input-area {
          background: rgba(30, 30, 30, 0.98);
          border-top-color: rgba(255, 255, 255, 0.05);

          textarea {
            background: rgba(50, 50, 50, 0.95);
            border-color: rgba(255, 255, 255, 0.1);
            color: white;

            &:focus {
              border-color: #666;
              box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.1);
            }

            &:disabled {
              background: rgba(30, 30, 30, 0.95);
            }
          }
        }
      }
    }
  }
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 1024px) {
  .chat-pdf {
    .split-view {
      flex-direction: column;

      .chat-view {
        width: 100%;
        min-width: 0;
        max-width: none;
        height: 50vh;
      }
    }
  }
}

@media (max-width: 768px) {
  .chat-pdf {
    .chat-container {
      padding: 0;
    }

    .sidebar {
      display: none;
    }

    .chat-main {
      border-radius: 0;
    }

    .upload-welcome {
      .main-title {
        font-size: 2rem;
      }

      .drop-zone {
        min-height: 200px;
      }
    }
  }
}
</style>
