# AI聊天服务API文档

## 1. 基础信息

- 基础路径: `/api/v1`
- 认证方式: Bearer Token
- 响应格式: JSON
- 字符编码: UTF-8

## 2. 接口列表

### 2.1 普通聊天接口

#### 接口说明
- 路径: `/chat`
- 方法: POST
- 描述: 发送聊天消息并获取AI响应

#### 请求参数
```json
{
    "sessionId": "string",  // 会话ID，8-64位字母数字下划线
    "message": "string"     // 用户消息，1-4096个字符
}
```

#### 响应参数
```json
{
    "sessionId": "string",  // 会话ID
    "message": "string",    // 用户消息
    "response": "string"    // AI响应
}
```

#### 错误码
- 400: 请求参数错误
- 401: 未授权
- 500: 服务器内部错误

### 2.2 流式聊天接口

#### 接口说明
- 路径: `/stream/chat`
- 方法: POST
- 描述: 发送聊天消息并获取流式AI响应

#### 请求参数
同普通聊天接口

#### 响应格式
SSE (Server-Sent Events) 流式响应

#### 错误码
同普通聊天接口

## 3. 安全说明

1. 所有API请求必须包含有效的认证Token
2. 敏感数据使用AES-256加密存储
3. 用户输入经过严格验证和清理
4. 会话ID使用安全的随机生成算法

## 4. 性能说明

1. 普通聊天接口响应时间 < 5s
2. 流式聊天接口首字节响应时间 < 1s
3. 支持并发请求数 > 1000
4. 消息长度限制为4096字符

## 5. 使用示例

### 5.1 普通聊天
```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test123","message":"你好"}'
```

### 5.2 流式聊天
```bash
curl -X POST http://localhost:8080/api/v1/stream/chat \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test123","message":"你好"}'
``` 