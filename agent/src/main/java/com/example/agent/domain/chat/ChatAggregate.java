package com.example.agent.domain.chat;

/**
 * 聊天聚合对象
 * 包含聊天相关的所有信息和功能
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
public class ChatAggregate {
    /**
     * 用户消息
     */
    private String message;

    /**
     * AI响应
     */
    private String response;

    /**
     * 上下文消息列表
     */
    private String[] contextMessages;

    /**
     * 会话ID
     */
    private String sessionId;

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

    /**
     * 获取用户消息
     * @return 用户消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置用户消息
     * @param message 用户消息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取AI响应
     * @return AI响应
     */
    public String getResponse() {
        return response;
    }

    /**
     * 设置AI响应
     * @param response AI响应
     */
    public void setResponse(String response) {
        this.response = response;
    }

    /**
     * 获取上下文消息列表
     * @return 上下文消息列表
     */
    public String[] getContextMessages() {
        return contextMessages;
    }

    /**
     * 设置上下文消息列表
     * @param contextMessages 上下文消息列表
     */
    public void setContextMessages(String[] contextMessages) {
        this.contextMessages = contextMessages;
    }

    /**
     * 获取会话ID
     * @return 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话ID
     * @param sessionId 会话ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取功能类型
     * @return 功能类型
     */
    public String getFunctionType() {
        return functionType;
    }

    /**
     * 设置功能类型
     * @param functionType 功能类型
     */
    public void setFunctionType(String functionType) {
        this.functionType = functionType;
    }

    /**
     * 获取图片生成参数
     * @return 图片生成参数
     */
    public ImageParams getImageParams() {
        return imageParams;
    }

    /**
     * 设置图片生成参数
     * @param imageParams 图片生成参数
     */
    public void setImageParams(ImageParams imageParams) {
        this.imageParams = imageParams;
    }

    /**
     * 获取语音识别参数
     * @return 语音识别参数
     */
    public SpeechParams getSpeechParams() {
        return speechParams;
    }

    /**
     * 设置语音识别参数
     * @param speechParams 语音识别参数
     */
    public void setSpeechParams(SpeechParams speechParams) {
        this.speechParams = speechParams;
    }

    /**
     * 获取函数调用参数
     * @return 函数调用参数
     */
    public FunctionParams getFunctionParams() {
        return functionParams;
    }

    /**
     * 设置函数调用参数
     * @param functionParams 函数调用参数
     */
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

        /**
         * 获取图片大小
         * @return 图片大小
         */
        public String getSize() {
            return size;
        }

        /**
         * 设置图片大小
         * @param size 图片大小
         */
        public void setSize(String size) {
            this.size = size;
        }

        /**
         * 获取图片数量
         * @return 图片数量
         */
        public Integer getN() {
            return n;
        }

        /**
         * 设置图片数量
         * @param n 图片数量
         */
        public void setN(Integer n) {
            this.n = n;
        }

        /**
         * 获取图片质量
         * @return 图片质量
         */
        public String getQuality() {
            return quality;
        }

        /**
         * 设置图片质量
         * @param quality 图片质量
         */
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

        /**
         * 获取语音文件URL
         * @return 语音文件URL
         */
        public String getAudioUrl() {
            return audioUrl;
        }

        /**
         * 设置语音文件URL
         * @param audioUrl 语音文件URL
         */
        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }

        /**
         * 获取语音文件格式
         * @return 语音文件格式
         */
        public String getFormat() {
            return format;
        }

        /**
         * 设置语音文件格式
         * @param format 语音文件格式
         */
        public void setFormat(String format) {
            this.format = format;
        }

        /**
         * 获取语言
         * @return 语言
         */
        public String getLanguage() {
            return language;
        }

        /**
         * 设置语言
         * @param language 语言
         */
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

        /**
         * 获取函数名称
         * @return 函数名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置函数名称
         * @param name 函数名称
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取函数参数
         * @return 函数参数
         */
        public String getArguments() {
            return arguments;
        }

        /**
         * 设置函数参数
         * @param arguments 函数参数
         */
        public void setArguments(String arguments) {
            this.arguments = arguments;
        }

        /**
         * 获取函数描述
         * @return 函数描述
         */
        public String getDescription() {
            return description;
        }

        /**
         * 设置函数描述
         * @param description 函数描述
         */
        public void setDescription(String description) {
            this.description = description;
        }
    }
} 