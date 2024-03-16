package com.juzipi.interceptor;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/2/5 13:00
 */
public class RequestContext {
    private static ThreadLocal<String> currentClientIP = new ThreadLocal<>();

    public static String getCurrentClientIP() {
        return currentClientIP.get();
    }

    public static void setCurrentClientIP(String clientIP) {
        currentClientIP.set(clientIP);
    }

    public static void remove() {
        currentClientIP.remove();
    }
}
