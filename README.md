# AI Agent

基于 Spring Boot 和 Vue.js 的 AI 助手应用，集成了通义千问的文本对话和图片生成功能。

## 功能特性

- 文本对话：支持流式响应的自然语言对话
- 图片生成：基于文本描述生成图片
- 现代化 UI：Apple 风格的界面设计
- 实时响应：流式文本显示，图片生成进度提示

## 技术栈

### 后端
- Spring Boot
- Spring Cloud Alibaba
- 通义千问 API

### 前端
- Vue 3
- TypeScript
- Vite

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- Maven 3.6+

### 后端启动
```bash
cd agent
mvn spring-boot:run
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

## 配置说明

1. 在 `application.yml` 中配置通义千问 API 密钥：
```yaml
spring:
  cloud:
    alibaba:
      ai:
        dashscope:
          api-key: your-api-key
```

2. 确保前端开发服务器代理配置正确（vite.config.ts）

## 许可证

MIT License 