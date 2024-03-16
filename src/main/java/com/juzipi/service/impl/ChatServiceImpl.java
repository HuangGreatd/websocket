package com.juzipi.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.juzipi.common.ErrorCode;
import com.juzipi.dao.ChatDao;
import com.juzipi.dao.TeamDao;
import com.juzipi.dao.UserDao;
import com.juzipi.domain.entity.Chat;
import com.juzipi.domain.entity.Team;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.ChatRequest;
import com.juzipi.domain.vo.ChatMessageVO;
import com.juzipi.domain.vo.WebSocketVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.mapper.TeamMapper;
import com.juzipi.service.ChatService;
import com.juzipi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.juzipi.constants.ChatConstant.*;
import static com.juzipi.constants.UserConstants.ADMIN_ROLE;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/2/6 12:37
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ChatDao chatDao;

    @Resource
    private UserService userService;

    @Resource
    private UserDao userDao;

    @Resource
    private TeamDao teamDao;

    @Resource
    private TeamMapper teamMapper;

    @Override
    public List<ChatMessageVO> getPrivateChat(ChatRequest chatRequest, int chatType, User loginUser) {
        Long toId = chatRequest.getToId();
        if (ObjectUtils.isEmpty(toId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<ChatMessageVO> chatRecords = getCache(CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId));
        if (chatRecords != null) {
            saveCache(CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId), chatRecords);
            return chatRecords;
        }
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper
                .and(privateChat -> privateChat.eq(Chat::getFromId, loginUser.getId()).eq(Chat::getToId, toId)
                        .or()
                        .eq(Chat::getToId, loginUser.getId()).eq(Chat::getFromId, toId)
                ).eq(Chat::getChatType, chatType);
        // 两方共有聊天
        List<Chat> list = chatDao.list(chatLambdaQueryWrapper);
        List<ChatMessageVO> chatMessageVOList = list.stream().map(chat -> {
            ChatMessageVO chatMessageVO = chatResult(loginUser.getId(), toId, chat.getText(), chatType, chat.getCreateTime());
            if (chat.getFromId().equals(loginUser.getId())) {
                chatMessageVO.setIsMy(true);
            }
            return chatMessageVO;
        }).collect(Collectors.toList());
        saveCache(CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId), chatMessageVOList);
        return chatMessageVOList;
    }

    private ChatMessageVO chatResult(Long userId, String text) {
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        User fromUser = userDao.getById(userId);
        WebSocketVO fromWebSocketVo = new WebSocketVO();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        chatMessageVO.setFormUser(fromWebSocketVo);
        chatMessageVO.setText(text);
        return chatMessageVO;
    }

    @Override
    public ChatMessageVO chatResult(Long userId, Long toId, String text, Integer chatType, Date createTime) {
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        User fromUser = userDao.getById(userId);
        User toUser = userDao.getById(toId);
        WebSocketVO fromWebSocketVo = new WebSocketVO();
        WebSocketVO toWebSocketVo = new WebSocketVO();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        BeanUtils.copyProperties(toUser, toWebSocketVo);
        chatMessageVO.setFormUser(fromWebSocketVo);
        chatMessageVO.setToUser(toWebSocketVo);
        chatMessageVO.setText(text);
        chatMessageVO.setChatType(chatType);
        chatMessageVO.setIsAdmin(false);
        chatMessageVO.setCreateTime(DateUtil.format(createTime, "yyyy-MM-dd HH:mm:ss"));
        return chatMessageVO;
    }

    @Override
    public List<ChatMessageVO> getCache(String redisKey, String id) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<ChatMessageVO> chatRecords;
        if (redisKey.equals(CACHE_CHAT_HALL)) {
            chatRecords = (List<ChatMessageVO>) valueOperations.get(redisKey);
        } else {
            chatRecords = (List<ChatMessageVO>) valueOperations.get(redisKey + id);
        }
        return chatRecords;
    }

    @Override
    public void saveCache(String redisKey, String id, List<ChatMessageVO> chatMessageVOS) {
        try {
            ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
            //解决缓存雪崩
            int i = RandomUtil.randomInt(2, 3);
            if (redisKey.equals(CACHE_CHAT_HALL)) {
                valueOperations.set(redisKey, chatMessageVOS, 2 + i / 10, TimeUnit.MINUTES);
            } else {
                valueOperations.set(redisKey + id, chatMessageVOS, 2 + i / 10, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("redis set key error");
        }
    }

    @Override
    public List<ChatMessageVO> getTeamChat(ChatRequest chatRequest, int chatType, User loginUser) {
        Long teamId = chatRequest.getTeamId();
        if (ObjectUtils.isEmpty(teamId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        List<ChatMessageVO> chatRecords = getCache(CACHE_CHAT_TEAM, String.valueOf(teamId));
        if (chatRecords != null) {
            List<ChatMessageVO> chatMessageVOS = checkIsMyMessage(loginUser, chatRecords);
            saveCache(CACHE_CHAT_TEAM, String.valueOf(teamId), chatMessageVOS);
            return chatMessageVOS;
        }
        Team team = teamDao.getById(teamId);
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType).eq(Chat::getTeamId, teamId);
        List<ChatMessageVO> chatMessageVOS = returnMessage(loginUser, team.getUserId(), chatLambdaQueryWrapper);
        saveCache(CACHE_CHAT_TEAM, String.valueOf(teamId), chatMessageVOS);
        return chatMessageVOS;
    }

    @Override
    public void deleteKey(String key, String id) {
        if (key.equals(CACHE_CHAT_HALL)){
            redisTemplate.delete(key);
        }else {
            redisTemplate.delete(key + id);
        }
    }

    @Override
    public List<ChatMessageVO> getHallChat(int chatType, User loginUser) {
        List<ChatMessageVO> chatRecords = getCache(CACHE_CHAT_HALL, String.valueOf(loginUser.getId()));
        if (chatRecords != null) {
            List<ChatMessageVO> chatMessageVOS = checkIsMyMessage(loginUser, chatRecords);
            saveCache(CACHE_CHAT_HALL, String.valueOf(loginUser.getId()), chatMessageVOS);
            return chatMessageVOS;
        }
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType);
        List<ChatMessageVO> chatMessageVOS = returnMessage(loginUser, null, chatLambdaQueryWrapper);
        saveCache(CACHE_CHAT_HALL, String.valueOf(loginUser.getId()), chatMessageVOS);
        return chatMessageVOS;
    }

    private List<ChatMessageVO> returnMessage(User loginUser, Long userId, LambdaQueryWrapper<Chat> chatLambdaQueryWrapper) {
        List<Chat> chatList = chatDao.list(chatLambdaQueryWrapper);
        return chatList.stream().map(chat -> {
            ChatMessageVO chatMessageVO = chatResult(chat.getFromId(), chat.getText());
            boolean isGroup = userId != null && userId.equals(chat.getFromId());
            if (userDao.getById(chat.getFromId()).getRole() == ADMIN_ROLE || isGroup) {
                chatMessageVO.setIsAdmin(true);
            }
            if (chat.getFromId().equals(loginUser.getId())) {
                chatMessageVO.setIsMy(true);
            }
            chatMessageVO.setCreateTime(DateUtil.format(chat.getCreateTime(), "yyyy年MM月dd日 HH:mm:ss"));
            return chatMessageVO;
        }).collect(Collectors.toList());
    }

    private List<ChatMessageVO> checkIsMyMessage(User loginUser, List<ChatMessageVO> chatRecords) {
        return chatRecords.stream().peek(chat -> {
            if (chat.getFormUser().getId() != loginUser.getId() && chat.getIsMy()) {
                chat.setIsMy(false);
            }
            if (chat.getFormUser().getId() == loginUser.getId() && !chat.getIsMy()) {
                chat.setIsMy(true);
            }
        }).collect(Collectors.toList());
    }

}
