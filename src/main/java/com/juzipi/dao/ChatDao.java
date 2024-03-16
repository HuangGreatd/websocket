package com.juzipi.dao;

import com.juzipi.domain.entity.Chat;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.ChatRequest;
import com.juzipi.domain.vo.ChatMessageVO;
import com.juzipi.mapper.ChatMapper;
import com.juzipi.service.ChatService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 聊天消息表 服务实现类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-06
 */
@Service
public class ChatDao extends ServiceImpl<ChatMapper, Chat>  {



}
