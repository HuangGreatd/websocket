package com.juzipi.config;

import org.springframework.stereotype.Component;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * http 会话配置
 */
@Component
public class HttpSessionConfig extends ServerEndpointConfig.Configurator implements ServletRequestListener {


    /**
     * 请求初始化
     */
    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        //获取httpSession，将所有request请求都携带上HttpSession
        HttpSession session = ((HttpServletRequest) sre.getServletRequest()).getSession();
    }

    /**
     * 修改握手
     **/
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        if (httpSession != null){
            // session放入serverEndpointConfig
            sec.getUserProperties().put(HttpSession.class.getName(), httpSession);
        }
        super.modifyHandshake(sec, request, response);
    }
}
