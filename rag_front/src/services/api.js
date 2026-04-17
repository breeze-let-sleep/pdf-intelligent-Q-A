/**
 * services/api.js
 * API 通信服务层
 * 封装所有与后端 Spring AI 接口的交互逻辑
 * 采用 Fetch API + 流式响应（ReadableStream）实现实时消息推送
 */

// 后端服务地址（开发环境）
const BASE_URL = 'http://localhost:8080'

/**
 * chatAPI - ChatPDF 模块 API 对象
 * 包含以下能力：
 *   1. sendPdfMessage: 发送 PDF 问答消息（流式响应）
 *   2. getChatHistory: 获取对话历史列表
 *   3. getChatMessages: 获取指定对话的消息记录
 */
export const chatAPI = {
  /**
   * sendPdfMessage - 发送 PDF 问答消息
   * @param {string} prompt - 用户输入的问题
   * @param {string|null} chatId - 当前对话 ID（首次为 null，服务端自动生成）
   * @returns {Promise<ReadableStreamDefaultReader>} 返回流式读取器，调用方自行消费
   *
   * 接口: GET /ai/pdf/chat?prompt=xxx&chatId=xxx
   * 后端返回 SSE/流式文本，逐步 yield 每个字符/词元
   */
  async sendPdfMessage(prompt, chatId) {
    try {
      // 构建 URL，可选追加 chatId 参数
      const url = new URL(`${BASE_URL}/ai/pdf/chat`)
      url.searchParams.append('prompt', prompt)
      if (chatId) {
        url.searchParams.append('chatId', chatId)
      }

      const response = await fetch(url, {
        method: 'GET',
        // 30 秒超时，防止后端无响应导致请求挂起
        signal: AbortSignal.timeout(30000)
      })

      if (!response.ok) {
        throw new Error(`API error: ${response.status}`)
      }

      // 返回响应体的可读流读取器，供调用方逐步消费数据块
      return response.body.getReader()
    } catch (error) {
      console.error('API Error:', error)
      throw error
    }
  },

  /**
   * getChatHistory - 获取聊天历史列表
   * @param {string} type - 对话类型（PDF 类型传 'pdf'）
   * @returns {Promise<Array>} 对话历史列表 [{ id, title, fileName }]
   *
   * 接口: GET /ai/history/{type}
   * 后端返回 JSON: [{ id: "xxx", title: "文件名.pdf", fileName: "文件名.pdf" }]
   */
  async getChatHistory(type = 'pdf') {
    try {
      const response = await fetch(`${BASE_URL}/ai/history/${type}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const historyList = await response.json()
      // 直接使用后端返回的数据，包含 id、title、fileName
      return Array.isArray(historyList) ? historyList : []
    } catch (error) {
      console.error('API Error:', error)
      return []
    }
  },

  /**
   * getChatMessages - 获取指定对话的消息历史
   * @param {string} chatId - 对话 ID
   * @param {string} type - 对话类型（PDF 类型传 'pdf'）
   * @returns {Promise<Array>} 消息列表 [{ role, content, timestamp }]
   *
   * 接口: GET /ai/history/{type}/{chatId}
   * 后端返回 JSON 数组
   */
  async getChatMessages(chatId, type = 'pdf') {
    try {
      const response = await fetch(`${BASE_URL}/ai/history/${type}/${chatId}`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const messages = await response.json()
      // 为每条消息补充时间戳（后端未提供则使用客户端当前时间）
      return (Array.isArray(messages) ? messages : []).map(msg => ({
        ...msg,
        timestamp: new Date()
      }))
    } catch (error) {
      console.error('API Error:', error)
      return []
    }
  },

  /**
   * downloadPdf - 下载指定对话的 PDF 文件
   * @param {string} chatId - 对话 ID
   * @returns {Promise<File>} PDF File 对象
   *
   * 接口: GET /ai/pdf/file/{chatId}
   * 后端通过 Content-Disposition 头返回文件名
   */
  async downloadPdf(chatId) {
    const response = await fetch(`${BASE_URL}/ai/pdf/file/${chatId}`)
    if (!response.ok) throw new Error('获取 PDF 失败')

    // 从响应头提取文件名（兼容不同编码格式）
    const contentDisposition = response.headers.get('content-disposition')
    let filename = 'document.pdf'
    if (contentDisposition) {
      const matches = contentDisposition.match(/filename=["']?([^"']+)["']?/)
      if (matches && matches[1]) {
        filename = decodeURIComponent(matches[1])
      }
    }

    // 将响应体转为 Blob，再包装为 File 对象
    const blob = await response.blob()
    return new File([blob], filename, { type: 'application/pdf' })
  }
}
