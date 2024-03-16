package com.juzipi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.juzipi.common.ErrorCode;
import com.juzipi.dao.BlogCommentsDao;
import com.juzipi.dao.BlogDao;
import com.juzipi.dao.UserDao;
import com.juzipi.domain.entity.Blog;
import com.juzipi.domain.entity.Message;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.enums.MessageTypeEnum;
import com.juzipi.domain.vo.BlogCommentsVO;
import com.juzipi.domain.vo.BlogVO;
import com.juzipi.domain.vo.MessageVO;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.mapper.MessageMapper;
import com.juzipi.service.BlogCommentsService;
import com.juzipi.service.BlogService;
import com.juzipi.service.MessageService;
import com.juzipi.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.juzipi.constants.PageConstants.PAGE_SIZE;
import static com.juzipi.constants.RedisConstants.*;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    @Resource
    @Lazy
    private BlogCommentsDao blogCommentsDao;

    @Resource
    @Lazy
    private BlogDao blogDao;

    @Resource
    private BlogCommentsService blogCommentsService;
    @Resource
    @Lazy
    private BlogService blogService;

    @Resource
    private UserDao userDao;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Boolean hasNewMessage(Long userId) {
        String likeNumKey = MESSAGE_LIKE_NUM_KEY + userId;
        Boolean hasLike = stringRedisTemplate.hasKey(likeNumKey);
        if (Boolean.TRUE.equals(hasLike)) {
            String likeNum = stringRedisTemplate.opsForValue().get(likeNumKey);
            assert likeNum != null;
            if (Long.parseLong(likeNum) > 0) {
                return true;
            }
        }
        String blogNumKey = MESSAGE_BLOG_NUM_KEY + userId;
        Boolean hasBlog = stringRedisTemplate.hasKey(blogNumKey);
        if (Boolean.TRUE.equals(hasBlog)) {
            String blogNum = stringRedisTemplate.opsForValue().get(blogNumKey);
            assert blogNum != null;
            return Long.parseLong(blogNum) > 0;
        }
        return false;
    }

    @Override
    public long getMessageNum(Long userId) {
        LambdaQueryWrapper<Message> messageLambdaQueryWrapper = new LambdaQueryWrapper<>();
        messageLambdaQueryWrapper.eq(Message::getToId, userId)
                .eq(Message::getIsRead, 0);
        return this.count(messageLambdaQueryWrapper);
    }

    @Override
    public long getLikeNum(Long userId) {
        String likeNumKey = MESSAGE_LIKE_NUM_KEY + userId;
        Boolean hasLike = stringRedisTemplate.hasKey(likeNumKey);
        if (Boolean.TRUE.equals(hasLike)) {
            String likeNum = stringRedisTemplate.opsForValue().get(likeNumKey);
            assert likeNum != null;
            return Long.parseLong(likeNum);
        } else {
            return 0;
        }
    }

    @Override
    public List<BlogVO> getUserBlog(Long userId) {
        String key = BLOG_FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, System.currentTimeMillis(), 0, 10);
        if (typedTuples == null || typedTuples.size() == 0) {
            return new ArrayList<>();
        }
        ArrayList<BlogVO> blogVOList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            long blogId = Long.parseLong(Objects.requireNonNull(typedTuple.getValue()));
            BlogVO blogVO = blogService.getBlogById(blogId, userId);
            blogVOList.add(blogVO);
        }
        String likeNumKey = MESSAGE_BLOG_NUM_KEY + userId;
        Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
        if (Boolean.TRUE.equals(hasKey)) {
            stringRedisTemplate.opsForValue().set(likeNumKey, "0");
        }
        return blogVOList;
    }

    @Override
    public Page<MessageVO> pageLike(Long userId, Long currentPage) {
        LambdaQueryWrapper<Message> messageLambdaQueryWrapper = new LambdaQueryWrapper<>();
        messageLambdaQueryWrapper.eq(Message::getToId, userId)
                .and(wp -> wp.eq(Message::getType, 0).or().eq(Message::getType, 1))
                .orderBy(true, false, Message::getCreateTime);
        Page<Message> messagePage = this.page(new Page<>(currentPage, PAGE_SIZE), messageLambdaQueryWrapper);
        if (messagePage.getSize() == 0) {
            return new Page<>();
        }
        Page<MessageVO> messageVOPage = new Page<>();
        BeanUtils.copyProperties(messagePage, messageVOPage);
        LambdaUpdateWrapper<Message> messageLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        messageLambdaUpdateWrapper.eq(Message::getToId, userId)
                .eq(Message::getType, 0)
                .or().eq(Message::getType, 1)
                .set(Message::getIsRead, 1);
        this.update(messageLambdaUpdateWrapper);
        String likeNumKey = MESSAGE_LIKE_NUM_KEY + userId;
        Boolean hasLike = stringRedisTemplate.hasKey(likeNumKey);
        if (Boolean.TRUE.equals(hasLike)) {
            stringRedisTemplate.opsForValue().set(likeNumKey, "0");
        }
        List<MessageVO> messageVOList = messagePage.getRecords().stream().map((item) -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(item, messageVO);
            User user = userDao.getById(messageVO.getFromId());
            if (user == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "发送人不存在");
            }
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            messageVO.setFromUser(userVO);
            if (item.getType() == MessageTypeEnum.BLOG_COMMENT_LIKE.getValue()) {
                BlogCommentsVO commentsVO = blogCommentsService.getComment(Long.parseLong(item.getData()), userId);
                messageVO.setComment(commentsVO);
            }
            if (item.getType() == MessageTypeEnum.BLOG_LIKE.getValue()) {
                BlogVO blogVO = blogService.getBlogById(Long.parseLong(item.getData()), userId);
                messageVO.setBlog(blogVO);
            }
            return messageVO;
        }).collect(Collectors.toList());
        messageVOPage.setRecords(messageVOList);
        return messageVOPage;
    }
}
