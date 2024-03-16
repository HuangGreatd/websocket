package com.juzipi.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.RequestInfo;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/2/5 12:48
 */
@Slf4j
public class IpInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = request.getRemoteAddr();

        RequestContext.setCurrentClientIP(clientIp);
        return true;
    }
}
