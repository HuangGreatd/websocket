package com.juzipi.websotck;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.juzipi.config.HttpSessionConfig;
import com.juzipi.dao.ChatDao;
import com.juzipi.dao.TeamDao;
import com.juzipi.dao.UserDao;
import com.juzipi.domain.entity.Chat;
import com.juzipi.domain.entity.Team;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.MessageRequest;
import com.juzipi.domain.vo.ChatMessageVO;
import com.juzipi.domain.vo.WebSocketVO;
import com.juzipi.service.ChatService;
import com.juzipi.service.TeamService;
import com.juzipi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.juzipi.constants.ChatConstant.*;
import static com.juzipi.constants.UserConstants.ADMIN_ROLE;
import static com.juzipi.constants.UserConstants.USER_LOGIN_STATE;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/1/25 18:33
 */
@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{userId}/{teamId}", configurator = HttpSessionConfig.class)
public class WebSocket {
    /**
     * 保存队伍的连接信息
     */
    private static final Map<String, ConcurrentHashMap<String, WebSocket>> ROOMS = new HashMap<>();

    /**
     * 线程安全的无序的集合
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();

    /**
     * 会话池
     */
    private static final Map<String, Session> SESSION_POOL = new HashMap<>(0);

    /**
     * 用户服务
     */
    private static UserService userService;

    private static UserDao userDao;
    /**
     * 聊天服务
     */
    private static ChatService chatService;

    private static ChatDao chatDao;

    private static TeamDao teamDao;
    /**
     * 团队服务
     */
    private static TeamService teamService;

    /**
     * 房间在线人数
     */
    private static int onlineCount = 0;

    /**
     * 当前信息
     */
    private Session session;

    /**
     * http会话
     */
    private HttpSession httpSession;


    /**
     * 上网数
     *
     * @return
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    /**
     * 添加在线计数
     */
    public static synchronized void addOnlineCount() {
        WebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocket.onlineCount--;
    }

    /**
     * 构造器注入用户服务
     *
     * @param userService
     */
    @Resource
    public void setHeatMapService(UserService userService) {
        WebSocket.userService = userService;
    }

    @Resource
    public void setHeatMapService(UserDao userDao) {
        WebSocket.userDao = userDao;
    }

    @Resource
    public void setHeatMapService(ChatService chatService) {
        WebSocket.chatService = chatService;
    }

    @Resource
    public void setHeatMapService(ChatDao chatDao) {
        WebSocket.chatDao = chatDao;
    }


    @Resource
    private void setHeatMapService(TeamService teamService) {
        WebSocket.teamService = teamService;
    }

    @Resource
    private void setHeatMapService(TeamDao teamDao) {
        WebSocket.teamDao = teamDao;
    }

    /**
     * 队伍内群发信息
     *
     * @param teamId
     * @param msg
     */
    public static void broadcast(String teamId, String msg) {
        ConcurrentHashMap<String, WebSocket> map = ROOMS.get(teamId);
        for (String key : map.keySet()) {
            try {
                WebSocket webSocket = map.get(key);
                webSocket.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 建立连接后调用的方法
     *
     * @param session
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId, @PathParam(value = "teamId") String teamId, EndpointConfig config) {
        try {
            if (StringUtils.isBlank(userId) || "undefined".equals(userId)) {
                sendError(userId, "参数错误");
                return;
            }
            HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
            User user = (User) httpSession.getAttribute(USER_LOGIN_STATE);
            if (user != null) {
                this.session = session;
                this.httpSession = httpSession;
            }
            if (!"NaN".equals(teamId)) {
                if (!ROOMS.containsKey(teamId)) {
                    ConcurrentHashMap<String, WebSocket> room = new ConcurrentHashMap<>(0);
                    room.put(userId, this);
                    ROOMS.put(String.valueOf(teamId), room);
                    //在线数+1
                    addOnlineCount();
                } else {
                    if (!ROOMS.get(teamId).contains(userId)) {
                        ROOMS.get(teamId).put(userId, this);
                        //在线数+1
                        addOnlineCount();
                    }
                }
            } else {
                SESSIONS.add(session);
                SESSION_POOL.put(userId, session);
                sendAllUsers();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送失败
     *
     * @param userId       用户id
     * @param errorMessage 错误消息
     */
    private void sendError(String userId, String errorMessage) {
        JSONObject obj = new JSONObject();
        obj.set("error", errorMessage);
        sendOneMessage(userId, obj.toString());
    }

    /**
     * 发送一个消息
     *
     * @param userId  用户编号
     * @param message 消息
     */
    private void sendOneMessage(String userId, String message) {
        Session session = SESSION_POOL.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 处理客户端失败连接
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId, @PathParam(value = "teamId") String teamId, Session session) {
        try {
            if (!"NaN".equals(teamId)) {
                ROOMS.get(teamId).remove(userId);
                if (getOnlineCount() > 0) {
                    subOnlineCount();
                }
            } else {
                if (!SESSION_POOL.isEmpty()) {
                    SESSION_POOL.remove(userId);
                    SESSIONS.remove(session);
                }
                sendAllUsers();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给所有用户
     */
    private void sendAllUsers() {
        HashMap<String, List<WebSocketVO>> stringListHashMap = new HashMap<>();
        ArrayList<WebSocketVO> webSocketVOS = new ArrayList<>();
        stringListHashMap.put("user", webSocketVOS);
        for (String key : SESSION_POOL.keySet()) {
            User user = userDao.getById(key);
            WebSocketVO webSocketVO = new WebSocketVO();
            BeanUtils.copyProperties(user, webSocketVO);
            webSocketVOS.add(webSocketVO);
        }
        sendAllMessage(JSONUtil.toJsonStr(stringListHashMap));
    }

    /**
     * 连接报错时的处理方式
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("消息发生错误：" + session.getId());
        error.printStackTrace();
    }

    /**
     * 如果需要服务器接受客户端的消息就在这里写了
     *
     * @param message
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        if ("PING".equals(message)) {
            sendOneMessage(userId, "pong");
            return;
        }
        MessageRequest messageRequest = new Gson().fromJson(message, MessageRequest.class);
        Long toId = messageRequest.getToId();
        Long teamId = messageRequest.getTeamId();
        String text = messageRequest.getText();
        Integer chatType = messageRequest.getChatType();
        User fromUser = userDao.getById(userId);
        Team team = teamDao.getById(teamId);
        if (chatType == PRIVATE_CHAT) {
            //私聊
            privateChat(fromUser, toId, text, chatType);

        } else if (chatType == TEAM_CHAT) {
            teamChat(fromUser, text, team, chatType);
        } else {
            //群聊
            hallChat(fromUser, text, chatType);
        }
    }

    /**
     * 私聊
     *
     * @param user     用户
     * @param toId     为id
     * @param text     文本
     * @param chatType 聊天类型
     */
    private void privateChat(User user, Long toId, String text, Integer chatType) {
        ChatMessageVO chatMessageVO = chatService.chatResult(user.getId(), toId, text, chatType, DateUtil.date(System.currentTimeMillis()));
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (loginUser.getId() == user.getId()) {
            chatMessageVO.setIsMy(true);
        }
        String toJson = new Gson().toJson(chatMessageVO);
        sendOneMessage(toId.toString(), toJson);
        saveChat(user.getId(), toId, text, null, chatType);
        chatService.deleteKey(CACHE_CHAT_PRIVATE, user.getId() + "" + toId);
        chatService.deleteKey(CACHE_CHAT_PRIVATE, toId + "" + user.getId());
    }

    /**
     * 保存聊天
     *
     * @param userId   用户id
     * @param toId     为id
     * @param text     文本
     * @param teamId   团队id
     * @param chatType 聊天类型
     */
    private void saveChat(Long userId, Long toId, String text, Long teamId, Integer chatType) {
        Chat chat = new Chat();
        chat.setFromId(userId);
        chat.setText(text);
        chat.setChatType(chatType);
        chat.setCreateTime(new Date());
        if (toId != null && toId > 0) {
            chat.setToId(toId);
        }
        if (teamId != null && teamId > 0) {
            chat.setTeamId(teamId);
        }
        chatDao.save(chat);
    }

    /**
     * 大厅聊天
     *
     * @param user     用户
     * @param text     文本
     * @param chatType 聊天类型
     */
    private void hallChat(User user, String text, Integer chatType) {
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        WebSocketVO fromWebSocketVO = new WebSocketVO();
        BeanUtils.copyProperties(user, fromWebSocketVO);
        chatMessageVO.setFormUser(fromWebSocketVO);
        chatMessageVO.setText(text);
        chatMessageVO.setChatType(chatType);
        chatMessageVO.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (user.getRole() == ADMIN_ROLE) {
            chatMessageVO.setIsAdmin(true);
        }
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (loginUser.getId() == user.getId()) {
            chatMessageVO.setIsMy(true);
        }
        String toJson = new Gson().toJson(chatMessageVO);
        sendAllMessage(toJson);
        saveChat(user.getId(), null, text, null, chatType);
        chatService.deleteKey(CACHE_CHAT_HALL, String.valueOf(user.getId()));

    }

    /**
     * 广播消息
     *
     * @param message 消息
     */
    private void sendAllMessage(String message) {
        for (Session session : SESSIONS) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 队伍聊天
     *
     * @param user
     * @param text
     * @param team
     * @param chatType
     */
    private void teamChat(User user, String text, Team team, Integer chatType) {
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        WebSocketVO fromWebSocketVO = new WebSocketVO();
        BeanUtils.copyProperties(user, fromWebSocketVO);
        chatMessageVO.setFormUser(fromWebSocketVO);
        chatMessageVO.setText(text);
        chatMessageVO.setTeamId(team.getId());
        chatMessageVO.setChatType(chatType);
        chatMessageVO.setCreateTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        if (user.getId() == team.getUserId() || user.getRole() == ADMIN_ROLE) {
            chatMessageVO.setIsAdmin(true);
        }
        User loginUser = (User) this.httpSession.getAttribute(USER_LOGIN_STATE);
        if (loginUser.getId() == user.getId()) {
            chatMessageVO.setIsMy(true);
        }
        String toJson = new Gson().toJson(chatMessageVO);
        try {
            broadcast(String.valueOf(team.getId()), toJson);
            saveChat(user.getId(), null, text, team.getId(), chatType);
            chatService.deleteKey(CACHE_CHAT_TEAM, String.valueOf(team.getId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 推送消息
     * getAsyncRemote获取异步端点(推荐使用这个)
     * getBasicRemote获取阻塞端点（不推荐，如果第一次发送失败，就会一直阻塞，影响后面的进行）
     *
     * @param message
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

}


