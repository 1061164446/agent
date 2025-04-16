package com.example.agent.domain.chat.model;

/**
 * 思考步骤类型枚举
 */
public enum StepType {
    KEYWORD_EXTRACTION,    // 关键词提取
    DOMAIN_IDENTIFICATION, // 领域识别
    QUESTION_TYPE,         // 问题类型识别
    HISTORY_UPDATE,        // 历史记录更新
    CONTEXT_ANALYSIS,      // 上下文分析
    ANSWER_GENERATION      // 答案生成
} 