package com.example.agent.domain.chat;

/**
 * 聊天聚合根
 * 包含聊天相关的所有信息和功能
 */
public class ChatAggregate {
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * AI响应内容
     */
    private String response;
    
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 上下文消息列表
     */
    private String[] contextMessages;

    /**
     * 功能类型
     * text: 文本对话
     * image: 图片生成
     * speech: 语音识别
     * function: 函数调用
     */
    private String functionType;

    /**
     * 图片生成参数
     */
    private ImageParams imageParams;

    /**
     * 语音识别参数
     */
    private SpeechParams speechParams;

    /**
     * 函数调用参数
     */
    private FunctionParams functionParams;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String[] getContextMessages() {
        return contextMessages;
    }

    public void setContextMessages(String[] contextMessages) {
        this.contextMessages = contextMessages;
    }

    public String getFunctionType() {
        return functionType;
    }

    public void setFunctionType(String functionType) {
        this.functionType = functionType;
    }

    public ImageParams getImageParams() {
        return imageParams;
    }

    public void setImageParams(ImageParams imageParams) {
        this.imageParams = imageParams;
    }

    public SpeechParams getSpeechParams() {
        return speechParams;
    }

    public void setSpeechParams(SpeechParams speechParams) {
        this.speechParams = speechParams;
    }

    public FunctionParams getFunctionParams() {
        return functionParams;
    }

    public void setFunctionParams(FunctionParams functionParams) {
        this.functionParams = functionParams;
    }

    /**
     * 图片生成参数
     */
    public static class ImageParams {
        /**
         * 图片大小
         * 可选值：256x256, 512x512, 1024x1024
         */
        private String size;

        /**
         * 图片数量
         * 范围：1-4
         */
        private Integer n;

        /**
         * 图片质量
         * 可选值：standard, hd
         */
        private String quality;

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public Integer getN() {
            return n;
        }

        public void setN(Integer n) {
            this.n = n;
        }

        public String getQuality() {
            return quality;
        }

        public void setQuality(String quality) {
            this.quality = quality;
        }
    }

    /**
     * 语音识别参数
     */
    public static class SpeechParams {
        /**
         * 语音文件URL
         */
        private String audioUrl;

        /**
         * 语音文件格式
         * 可选值：mp3, wav, ogg
         */
        private String format;

        /**
         * 语言
         * 可选值：zh, en
         */
        private String language;

        public String getAudioUrl() {
            return audioUrl;
        }

        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    /**
     * 函数调用参数
     */
    public static class FunctionParams {
        /**
         * 函数名称
         */
        private String name;

        /**
         * 函数参数
         */
        private String arguments;

        /**
         * 函数描述
         */
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
} 