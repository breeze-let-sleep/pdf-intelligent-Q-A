# ChatPDF - 基于 RAG 的 PDF 智能问答系统

[English](#english) | [中文](#中文)

---

## 📖 项目简介

本项目是一个基于 **RAG（检索增强生成）技术** 的 PDF 智能问答系统。用户上传 PDF 文档后，可以针对文档内容进行自然语言问答，系统通过向量检索找到最相关的 PDF 片段，结合大语言模型生成准确答案。

核心技术栈：Vue 3 + Vite 前端 / Spring Boot 3 + Spring AI 后端，对接 **DeepSeek Chat** 大模型和 **阿里云百炼（DashScope）** 向量化服务。

---

## 🗂️ 项目结构

```
pdf-/
├── rag_front/                         # 前端（Vue 3 单页应用）
│   ├── src/
│   │   ├── App.vue                     # 根组件（导航栏 + 暗色模式）
│   │   ├── main.js                     # 应用入口
│   │   ├── views/
│   │   │   └── ChatPDF.vue             # 主页面（侧边栏 + 对话区 + PDF预览）
│   │   ├── components/
│   │   │   ├── ChatMessage.vue          # 消息气泡（Markdown + 代码高亮 + 复制）
│   │   │   └── PDFViewer.vue           # PDF预览（Blob URL + iframe）
│   │   ├── services/
│   │   │   └── api.js                  # API 封装层
│   │   ├── router/
│   │   │   └── index.js               # 路由配置
│   │   └── assets/
│   │       └── main.css               # 全局样式
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
├── rag_end/
│   └── RAG_Practice/                   # 后端（Spring Boot 3 + Spring AI）
│       ├── src/main/java/com/hyltest/rag_practice/
│       │   ├── RagPracticeApplication.java    # 启动类
│       │   ├── config/
│       │   │   ├── CommonConfiguration.java  # ChatMemory Bean 配置
│       │   │   ├── MvcConfiguration.java      # CORS 跨域配置
│       │   │   └── SpringAIConfiguration.java # ChatClient + VectorStore 配置
│       │   ├── controller/
│       │   │   ├── PdfController.java         # PDF上传 / 下载 / RAG问答
│       │   │   └── ChatHistoryController.java # 会话历史管理
│       │   ├── service/
│       │   │   ├── IFileService.java          # 文件服务接口
│       │   │   └── impl/
│       │   │       └── FileServiceImpl.java   # PDF存储 + 向量化核心实现
│       │   ├── repository/
│       │   │   ├── FileRepository.java              # 文件仓储接口
│       │   │   ├── ChatHistoryRepository.java      # 会话历史接口
│       │   │   ├── LocalPdfFileRepository.java      # PDF本地文件存储
│       │   │   └── InMemoryChatHistoryRepository.java # 会话历史内存实现 + 持久化
│       │   ├── entity/
│       │   │   ├── po/Msg.java                     # 消息持久化实体
│       │   │   └── vo/
│       │   │       ├── Result.java                  # 统一响应封装
│       │   │       ├── ChatHistoryVO.java          # 会话历史 VO
│       │   │       └── MessageVO.java              # 消息 VO
│       │   └── utils/
│       │       └── VectorDistanceUtils.java        # 向量距离计算工具
│       └── src/main/resources/
│           └── application.yml                     # 配置文件（API Key 等）
│
└── [报告模板].docx
```

---

## 🛠️ 技术栈

### 前端

| 技术 | 版本 | 作用 |
|------|------|------|
| **Vue 3** | 3.5 | 渐进式前端框架，Composition API |
| **Vite** | 6.3 | 下一代前端构建工具，HMR 热更新 |
| **Pinia** | 3.x | 轻量级状态管理 |
| **Vue Router** | 4.x | 客户端路由 |
| **@vueuse/core** | latest | Vue Composition API 工具集 |
| **marked** | latest | Markdown 解析 |
| **highlight.js** | latest | 代码语法高亮 |
| **DOMPurify** | latest | XSS 安全防护 |

### 后端

| 技术 | 版本 | 作用 |
|------|------|------|
| **Spring Boot** | 3.4.5 | 后端框架 |
| **Spring AI** | 1.0.0 | AI 能力抽象层 |
| **spring-ai-alibaba-starter** | 1.0.0.2 | 阿里云百炼集成 |
| **spring-ai-starter-model-deepseek** | 1.0.0 | DeepSeek 模型集成 |
| **spring-ai-vector-store** | 1.0.0 | 向量存储接口 |
| **spring-ai-pdf-document-reader** | 1.0.0 | PDF 解析 |
| **Lombok** | latest | 简化代码 |

### AI 服务

| 服务 | 用途 |
|------|------|
| **DeepSeek Chat** (`deepseek-chat`) | 对话生成 |
| **DashScope Embedding** (`text-embedding-v4`) | 文本向量化（1024维） |

---

## 🔌 API 接口

### 后端接口（Base URL: `http://localhost:8080`）

| 接口 | 方法 | 说明 | 返回 |
|------|------|------|------|
| `/ai/pdf/upload/{chatId}` | `POST` | 上传 PDF 文件 | `{"ok": 1, "msg": "ok"}` |
| `/ai/pdf/chat` | `GET` | RAG 问答（流式） | `text/html` SSE 流 |
| `/ai/history/pdf` | `GET` | 获取会话历史列表 | `[{"id","title","fileName"}]` |
| `/ai/history/pdf/{chatId}` | `GET` | 获取消息历史 | `[{"role","content"}]` |
| `/ai/pdf/file/{chatId}` | `GET` | 下载 PDF 文件 | `application/octet-stream` |

> ⚠️ **注意**: 生产环境请务必在 `application.yml` 中替换 API Key，并添加身份认证机制。

---

## 🔄 RAG 流程

```
用户上传PDF
    │
    ▼
┌─────────────────────────────────────────┐
│  1. PDF 解析 (PagePdfDocumentReader)    │
│     每页 PDF 作为一个 Document           │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  2. 向量化 (DashScope text-embedding-v4) │
│     1024 维向量，存储到 SimpleVectorStore │
└────────────────┬────────────────────────┘
                 ▼
          用户提问 prompt
                 │
    ┌────────────▼────────────┐
    │  3. 相似性检索           │
    │  topK=2, 阈值0.5         │
    │  按 chat_id 过滤          │
    └────────────┬────────────┘
                 ▼
    ┌────────────────────────────┐
    │  4. 上下文组装 + 记忆注入   │
    │  MessageChatMemoryAdvisor   │
    └────────────┬────────────────┘
                 ▼
    ┌────────────────────────────┐
    │  5. DeepSeek 生成答案        │
    │  流式输出 (SSE)              │
    └────────────────────────────┘
```

### 核心配置（SpringAIConfiguration.java）

```java
@Bean
public ChatClient pdfChatClient(DeepSeekChatModel model, ChatMemory chatMemory, VectorStore vectorStore) {
    return ChatClient.builder(model)
        .defaultAdvisors(
            SimpleLoggerAdvisor.builder().build(),        // 日志调试
            MessageChatMemoryAdvisor.builder(chatMemory).build(),  // 对话记忆
            QuestionAnswerAdvisor.builder(vectorStore)   // RAG 核心
                .searchRequest(SearchRequest.builder()
                    .similarityThreshold(0.5d)
                    .topK(2)
                    .build())
                .build()
        )
        .build();
}
```

---

## 📂 数据持久化

| 文件 | 用途 |
|------|------|
| `chat-pdf.properties` | chatId → PDF 文件名映射 |
| `chat-pdf.json` | 向量数据库序列化（SimpleVectorStore） |
| `chat-history.json` | 会话历史列表 |
| `chat-memory.json` | 对话消息记录 |

> ⚠️ `SimpleVectorStore` 为内存向量库，重启后会丢失。`@PreDestroy` 注解的方法会在应用关闭时自动持久化到 `chat-pdf.json`；`@PostConstruct` 注解的方法会在启动时恢复。

---

## 🚀 快速开始

### 环境要求

- **JDK 17+**
- **Node.js 18+**
- **Maven 3.8+**
- DeepSeek API Key（[获取地址](https://platform.deepseek.com/)）
- 阿里云百炼 API Key（[获取地址](https://dashscope.console.aliyun.com/)）

### 1. 启动后端

```bash
cd rag_end/RAG_Practice

# 配置 API Key 为环境变量
# 编辑 src/main/resources/application.yml
# 修改 deepseek.api-key 和 dashscope.api-key

# 启动（开发模式）
./mvnw spring-boot:run

# 或打包后运行
./mvnw clean package -DskipTests
java -jar target/RAG_Practice-0.0.1-SNAPSHOT.jar
```

后端启动地址: `http://localhost:8080`

### 2. 启动前端

```bash
cd rag_front

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

前端启动地址: `http://localhost:5173`

### 3. 使用

1. 打开浏览器访问 `http://localhost:5173`
2. 点击上传区域或拖拽 PDF 文件
3. 等待向量化和处理完成
4. 在对话框中输入关于 PDF 内容的问题
5. AI 将基于 PDF 内容进行回答

---

## 📝 关键实现

### 前端：流式响应

```javascript
const reader = response.body.getReader();
while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  const chunk = new TextDecoder().decode(value);
  aiMessageContent.value += chunk;  // 逐块追加，打字机效果
}
```

### 前端：PDF 预览（零依赖）

```javascript
const url = URL.createObjectURL(file);  // File → Blob URL
iframe.src = url;                       // iframe 原生渲染 PDF
```

### 后端：PDF 向量化

```java
PagePdfDocumentReader reader = new PagePdfDocumentReader(
    resource,
    PdfDocumentReaderConfig.builder()
        .withPagesPerDocument(1)  // 每页作为一个 Document
        .build()
);
List<Document> documents = reader.read();
documents.forEach(doc -> doc.getMetadata().put("chat_id", chatId));
vectorStore.add(documents);
```

### 后端：流式 RAG 问答

```java
@RequestMapping(value = "/ai/pdf/chat", produces = "text/html;charset=UTF-8")
public Flux<String> chat(String prompt, String chatId) {
    return pdfChatClient
        .prompt(prompt)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
        .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION,
            "chat_id == '" + chatId + "'"))  // 按会话隔离检索
        .stream()
        .content();
}
```

---

## 📊 项目截图

> （请在发布前将截图放置在 docs/screenshots/ 目录下）

---

## 🔧 生产环境注意事项

1. **API Key 安全**: 切勿将 API Key 提交到 Git，添加到 `.gitignore`
2. **CORS 配置**: `MvcConfiguration.java` 中的跨域配置仅适用于开发环境，生产环境请使用 Nginx 反向代理
3. **向量库升级**: 当前使用 `SimpleVectorStore`（内存），生产环境建议迁移到 Milvus、Qdrant 等专业向量数据库
4. **文件存储**: 当前使用本地磁盘，生产环境建议使用 OSS / S3 等对象存储
5. **身份认证**: 建议增加 JWT 认证和用户管理模块

---

## 📚 参考资料

- [Spring AI 官方文档](https://spring.io/projects/spring-ai)
- [DeepSeek API 文档](https://platform.deepseek.com/docs)
- [阿里云百炼 DashScope](https://help.aliyun.com/zh/dashscope/)
- [Vue 3 官方文档](https://vuejs.org/)
- [Vite 构建工具](https://vitejs.dev/)

---

## 📄 License

MIT License

---

## English

### ChatPDF - PDF Intelligent Q&A System Based on RAG

This is a **Retrieval-Augmented Generation (RAG)** based PDF intelligent Q&A system built with **Vue 3** (frontend) and **Spring Boot 3 + Spring AI** (backend). Users can upload PDF documents and ask natural language questions about the content.

**Key Features:**
- 📤 PDF upload and preview (zero-dependency, native iframe rendering)
- 🤖 RAG-powered Q&A using DeepSeek + DashScope Embedding
- 💬 Streaming AI responses (typewriter effect)
- 📝 Markdown rendering with syntax highlighting
- 🌙 Dark mode support
- 💾 Persistent chat history and vector store

**Tech Stack:** Vue 3 / Vite / Pinia / Spring Boot 3 / Spring AI / DeepSeek / DashScope
