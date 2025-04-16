package com.example.agent.domain.chat.model;

/**
 * 思考步骤类型枚举
 */
public enum StepType {
    /**
     * 历史更新
     */
    HISTORY_UPDATE("历史更新"),

    /**
     * 关键词提取
     */
    KEYWORD_EXTRACTION("关键词提取"),

    /**
     * 领域识别
     */
    DOMAIN_IDENTIFICATION("领域识别"),

    /**
     * 问题类型
     */
    QUESTION_TYPE("问题类型"),

    /**
     * 上下文分析
     */
    CONTEXT_ANALYSIS("上下文分析"),

    /**
     * 理解分析
     */
    UNDERSTANDING("理解分析"),

    /**
     * 深入思考
     */
    ANALYZING("深入思考"),

    /**
     * 搜索信息
     */
    SEARCHING("搜索信息"),

    /**
     * 规划方案
     */
    PLANNING("规划方案"),

    /**
     * 组织回答
     */
    ORGANIZING("组织回答"),

    /**
     * AI回答
     */
    RESPONSE("AI回答");

    private final String description;

    StepType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 