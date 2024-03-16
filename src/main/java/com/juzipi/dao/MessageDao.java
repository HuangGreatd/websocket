package com.juzipi.dao;

import com.juzipi.domain.entity.Message;
import com.juzipi.mapper.MessageMapper;
import com.juzipi.service.MessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-17
 */
@Service
public class MessageDao extends ServiceImpl<MessageMapper, Message> {

}
