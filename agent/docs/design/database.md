# 数据库设计文档

## 1. 数据库概述

- 数据库类型: MySQL 8.0
- 字符集: utf8mb4
- 排序规则: utf8mb4_unicode_ci
- 事务隔离级别: READ COMMITTED

## 2. 表结构设计

### 2.1 聊天会话表 (chat_session)

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | varchar | 64 | 否 | 无 | 主键，会话ID |
| user_id | varchar | 64 | 否 | 无 | 用户ID |
| created_at | datetime | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | datetime | - | 否 | CURRENT_TIMESTAMP | 更新时间 |
| status | tinyint | - | 否 | 1 | 状态：1-活跃，0-关闭 |

### 2.2 聊天消息表 (chat_message)

| 字段名 | 类型 | 长度 | 允许空 | 默认值 | 说明 |
|--------|------|------|--------|--------|------|
| id | bigint | - | 否 | 无 | 主键，自增 |
| session_id | varchar | 64 | 否 | 无 | 会话ID |
| message_type | tinyint | - | 否 | 1 | 消息类型：1-用户消息，2-AI响应 |
| content | text | - | 否 | 无 | 消息内容 |
| created_at | datetime | - | 否 | CURRENT_TIMESTAMP | 创建时间 |

## 3. 索引设计

### 3.1 聊天会话表索引

- 主键索引: id
- 普通索引: user_id
- 普通索引: created_at

### 3.2 聊天消息表索引

- 主键索引: id
- 普通索引: session_id
- 普通索引: created_at

## 4. 分库分表策略

1. 按用户ID哈希分库
2. 按时间范围分表（每月一张表）
3. 冷热数据分离（3个月以上数据归档）

## 5. 数据安全

1. 敏感字段加密存储
2. 定期数据备份
3. 数据访问权限控制
4. 操作日志记录

## 6. 性能优化

1. 使用连接池
2. 合理设置索引
3. 定期统计信息更新
4. 查询优化器提示 