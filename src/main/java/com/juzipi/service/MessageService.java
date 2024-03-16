package com.juzipi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.domain.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import com.juzipi.domain.vo.BlogVO;
import com.juzipi.domain.vo.MessageVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-17
 */
public interface MessageService extends IService<Message> {

    Boolean hasNewMessage(Long id);

    long getMessageNum(Long id);

    long getLikeNum(Long id);

    List<BlogVO> getUserBlog(Long id);

    Page<MessageVO> pageLike(Long id, Long currentPage);
}
