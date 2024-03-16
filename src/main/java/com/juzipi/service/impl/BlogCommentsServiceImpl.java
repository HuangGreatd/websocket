package com.juzipi.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.juzipi.common.ErrorCode;
import com.juzipi.dao.BlogDao;
import com.juzipi.dao.MessageDao;
import com.juzipi.dao.UserDao;
import com.juzipi.domain.entity.*;
import com.juzipi.domain.enums.MessageTypeEnum;
import com.juzipi.domain.req.AddCommentRequest;
import com.juzipi.domain.vo.BlogCommentsVO;
import com.juzipi.domain.vo.BlogVO;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.mapper.BlogCommentsMapper;
import com.juzipi.service.BlogCommentsService;
import com.juzipi.service.BlogService;
import com.juzipi.service.CommentLikeService;
import com.juzipi.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.juzipi.constants.RedissonConstant;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.juzipi.constants.PageConstants.PAGE_SIZE;
import static com.juzipi.constants.RedisConstants.MESSAGE_LIKE_NUM_KEY;
import static com.juzipi.constants.RedissonConstant.COMMENTS_LIKE_LOCK;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements BlogCommentsService {
    @Resource
    private CommentLikeService commentLikeService;

    @Resource
    private BlogService blogService;

    @Resource
    private BlogDao blogDao;

    @Resource
    private UserDao userDao;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MessageDao messageDao;

    @Value("${yuchuang.qiniu.url}")
    private String QINIU_URL;
    @Override
    public BlogCommentsVO getComment(long commentId, Long userId) {
        BlogComments comments = this.getById(commentId);
        BlogCommentsVO blogCommentsVO = new BlogCommentsVO();
        BeanUtils.copyProperties(comments, blogCommentsVO);
        LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        commentLikeLambdaQueryWrapper.eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, commentId);
        long count = commentLikeService.count(commentLikeLambdaQueryWrapper);
        blogCommentsVO.setIsLiked(count > 0);
        return blogCommentsVO;
    }

    @Override
    public void addComment(AddCommentRequest addCommentRequest, Long userId) {
        BlogComments blogComments = new BlogComments();
        blogComments.setUserId(userId);
        blogComments.setBlogId(addCommentRequest.getBlogId());
        blogComments.setContent(addCommentRequest.getContent());
        blogComments.setLikedNum(0);
        blogComments.setStatus(0);
        this.save(blogComments);
        Blog blog = blogDao.getById(addCommentRequest.getBlogId());
        blogDao.update().eq("id", addCommentRequest.getBlogId())
                .set("comments_num", blog.getCommentsNum() + 1).update();

    }

    @Override
    public List<BlogCommentsVO> listComments(long blogId, Long id) {
        LambdaQueryWrapper<BlogComments> blogCommentsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogCommentsLambdaQueryWrapper.eq(BlogComments::getBlogId, blogId);
        List<BlogComments> blogCommentsList = this.list(blogCommentsLambdaQueryWrapper);
        return blogCommentsList.stream().map((comment) -> {
            BlogCommentsVO blogCommentsVO = new BlogCommentsVO();
            BeanUtils.copyProperties(comment, blogCommentsVO);
            User user = userDao.getById(comment.getUserId());
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            blogCommentsVO.setCommentUser(userVO);
            LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            commentLikeLambdaQueryWrapper.eq(CommentLike::getCommentId, comment.getId())
                    .eq(CommentLike::getUserId, id);
            int count = commentLikeService.count(commentLikeLambdaQueryWrapper);
            blogCommentsVO.setIsLiked(count > 0);
            return blogCommentsVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        RLock lock = redissonClient.getLock(COMMENTS_LIKE_LOCK + commentId + ":" + userId);
        try {
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                BlogComments comments = this.getById(commentId);
                if (comments == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论不存在");
                }
                LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
                commentLikeLambdaQueryWrapper.eq(CommentLike::getCommentId, commentId)
                        .eq(CommentLike::getUserId, userId);
                int count = commentLikeService.count(commentLikeLambdaQueryWrapper);
                if (count == 0) {
                    CommentLike commentLike = new CommentLike();
                    commentLike.setCommentId(commentId);
                    commentLike.setUserId(userId);
                    commentLikeService.save(commentLike);
                    BlogComments blogComments = this.getById(commentId);
                    this.update().eq("id", commentId)
                            .set("liked_num", blogComments.getLikedNum() + 1).update();
                    String likeNumKey = MESSAGE_LIKE_NUM_KEY + blogComments.getUserId();
                    Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
                    if (Boolean.TRUE.equals(hasKey)) {
                        stringRedisTemplate.opsForValue().increment(likeNumKey);
                    } else {
                        stringRedisTemplate.opsForValue().set(likeNumKey, "1");
                    }
                    Message message = new Message();
                    message.setType(MessageTypeEnum.BLOG_COMMENT_LIKE.getValue());
                    message.setFromId(userId);
                    message.setToId(blogComments.getUserId());
                    message.setData(String.valueOf(blogComments.getId()));
                    messageDao.save(message);
                } else {
                    commentLikeService.remove(commentLikeLambdaQueryWrapper);
                    BlogComments blogComments = this.getById(commentId);
                    this.update().eq("id", commentId)
                            .set("liked_num", blogComments.getLikedNum() - 1)
                            .update();
                    LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
                    messageQueryWrapper
                            .eq(Message::getType, MessageTypeEnum.BLOG_COMMENT_LIKE.getValue())
                            .eq(Message::getFromId, userId)
                            .eq(Message::getToId, blogComments.getUserId())
                            .eq(Message::getData, String.valueOf(blogComments.getId()));
                    messageDao.remove(messageQueryWrapper);
                    String likeNumKey = MESSAGE_LIKE_NUM_KEY + blogComments.getUserId();
                    String upNumStr = stringRedisTemplate.opsForValue().get(likeNumKey);
                    if (!StrUtil.isNullOrUndefined(upNumStr) && Long.parseLong(upNumStr) != 0) {
                        stringRedisTemplate.opsForValue().decrement(likeNumKey);
                    }
                }
            }
        } catch (Exception e) {
            log.error("LikeBlog error", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void deleteComment(Long id, Long userId, boolean isAdmin) {
        BlogComments blogComments = this.getById(id);
        if (isAdmin) {
            this.removeById(id);
            Integer commentsNum = blogDao.getById(blogComments.getBlogId()).getCommentsNum();
            blogDao.update().eq("id", blogComments.getBlogId()).set("comments_num", commentsNum - 1).update();
            return;
        }
        if (blogComments == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!blogComments.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        this.removeById(id);
        Integer commentsNum = blogDao.getById(blogComments.getBlogId()).getCommentsNum();
        blogDao.update().eq("id", blogComments.getBlogId()).set("comments_num", commentsNum - 1).update();
    }

    @Override
    public Page<BlogCommentsVO> pageMyComments(Long id, Long currentPage) {
        LambdaQueryWrapper<BlogComments> blogCommentsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogCommentsLambdaQueryWrapper.eq(BlogComments::getUserId, id);
        Page<BlogComments> blogCommentsPage = this.page(new Page<>(currentPage, PAGE_SIZE), blogCommentsLambdaQueryWrapper);
        if (blogCommentsPage==null || blogCommentsPage.getSize()==0){
            return new Page<>();
        }
        Page<BlogCommentsVO> blogCommentsVOPage = new Page<>();
        BeanUtils.copyProperties(blogCommentsPage,blogCommentsVOPage);
        List<BlogCommentsVO> blogCommentsVOList = blogCommentsPage.getRecords().stream().map((item) -> {
            BlogCommentsVO blogCommentsVO = new BlogCommentsVO();
            BeanUtils.copyProperties(item, blogCommentsVO);
            User user = userDao.getById(item.getUserId());
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            blogCommentsVO.setCommentUser(userVO);

            Long blogId = blogCommentsVO.getBlogId();
            Blog blog = blogDao.getById(blogId);
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            String images = blogVO.getImages();
            if (images == null) {
                blogVO.setCoverImage(null);
            } else {
                String[] imgStr = images.split(",");
                blogVO.setCoverImage(QINIU_URL + imgStr[0]);
            }
            Long authorId = blogVO.getUserId();
            User author = userDao.getById(authorId);
            UserVO authorVO = new UserVO();
            BeanUtils.copyProperties(author, authorVO);
            blogVO.setAuthor(authorVO);

            blogCommentsVO.setBlog(blogVO);

            LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            commentLikeLambdaQueryWrapper.eq(CommentLike::getUserId, id).eq(CommentLike::getCommentId, item.getId());
            long count = commentLikeService.count(commentLikeLambdaQueryWrapper);
            blogCommentsVO.setIsLiked(count > 0);
            return blogCommentsVO;
        }).collect(Collectors.toList());
        blogCommentsVOPage.setRecords(blogCommentsVOList);
        return blogCommentsVOPage;
    }
}
