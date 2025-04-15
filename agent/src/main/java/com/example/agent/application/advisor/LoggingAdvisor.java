package com.example.agent.application.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 请求响应日志Advisor
 * 用于记录所有API请求的入参和出参
 */
@Component
public class LoggingAdvisor extends NameMatchMethodPointcutAdvisor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAdvisor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoggingAdvisor() {
        // 设置拦截器
        setAdvice(new LoggingInterceptor());
        // 设置匹配的方法名模式
        setMappedNames("*");
    }

    /**
     * 日志拦截器实现
     */
    private class LoggingInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            long startTime = System.currentTimeMillis();
            
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return invocation.proceed();
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            // 记录请求日志
            logger.info("API请求开始: URL={}, Method={}, IP={}, Args={}",
                    request.getRequestURL().toString(),
                    request.getMethod(),
                    request.getRemoteAddr(),
                    Arrays.toString(invocation.getArguments()));
            
            try {
                // 执行目标方法
                Object result = invocation.proceed();
                
                // 记录响应日志
                long executionTime = System.currentTimeMillis() - startTime;
                logger.info("API请求结束: 耗时={}ms, 响应={}",
                        executionTime,
                        objectMapper.writeValueAsString(result));
                
                return result;
            } catch (Exception e) {
                // 记录异常日志
                logger.error("API请求异常: ", e);
                throw e;
            }
        }
    }
} 