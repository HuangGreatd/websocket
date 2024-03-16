package com.juzipi.service;

import com.juzipi.domain.entity.Chat;
import com.baomidou.mybatisplus.extension.service.IService;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.ChatRequest;
import com.juzipi.domain.vo.ChatMessageVO;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 聊天消息表 服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-06
 */
public interface ChatService  {

    List<ChatMessageVO> getPrivateChat(ChatRequest chatRequest, int privateChat, User loginUser);

    ChatMessageVO chatResult(Long userId, Long toId, String text, Integer chatType, Date createTime);

    List<ChatMessageVO> getCache(String redisKey, String id);

    void saveCache(String redisKey, String id, List<ChatMessageVO> chatMessageVOS);

    List<ChatMessageVO> getTeamChat(ChatRequest chatRequest, int teamChat, User loginUser);

    void deleteKey(String cacheChatHall, String s);

    List<ChatMessageVO> getHallChat(int hallChat, User loginUser);
}
