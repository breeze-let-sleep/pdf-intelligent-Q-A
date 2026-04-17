# ChatPDF - 与任何 PDF 对话

基于 Vue 3 + Vite 的 PDF 智能问答前端应用。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Vue Router** - 官方路由管理器
- **Pinia** - 状态管理库
- **@vueuse/core** - Vue 组合式函数库
- **marked** - Markdown 解析器
- **DOMPurify** - XSS 攻击防护
- **highlight.js** - 代码语法高亮
- **@heroicons/vue** - Heroicons 图标库

## 功能特性

- 📄 **PDF 上传** - 支持点击上传和拖拽上传
- 💬 **智能对话** - 基于 RAG 的 PDF 问答
- 🌓 **暗色模式** - 支持亮色/暗色主题切换
- 📝 **Markdown 渲染** - 支持代码高亮、表格、列表等
- 🔒 **XSS 防护** - 内容净化，防止脚本注入
- 📱 **响应式设计** - 适配桌面和移动端

## 安装依赖

```bash
npm install
```

## 开发

```bash
npm run dev
```

## 构建

```bash
npm run build
```

## 预览生产构建

```bash
npm run preview
```

## 后端接口

应用需要连接后端 Spring AI 服务，默认地址：`http://localhost:8080`

### 主要接口

- `POST /ai/pdf/upload/{chatId}` - 上传 PDF 文件
- `GET /ai/pdf/chat?prompt=xxx&chatId=xxx` - 发送消息（流式响应）
- `GET /ai/history/pdf` - 获取历史对话列表
- `GET /ai/history/pdf/{chatId}` - 获取指定对话消息
- `GET /ai/pdf/file/{chatId}` - 下载 PDF 文件

## 项目结构

```
rag_qclaw/
├── public/              # 静态资源
├── src/
│   ├── assets/          # 样式文件
│   ├── components/      # 组件
│   │   ├── ChatMessage.vue   # 消息气泡组件
│   │   └── PDFViewer.vue     # PDF 预览组件
│   ├── router/          # 路由配置
│   ├── services/        # API 服务
│   ├── views/           # 页面视图
│   │   └── ChatPDF.vue  # 主页面
│   ├── App.vue          # 根组件
│   └── main.js          # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 许可证

MIT
