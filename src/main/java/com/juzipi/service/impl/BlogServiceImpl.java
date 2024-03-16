package com.juzipi.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.common.ErrorCode;
import com.juzipi.dao.*;
import com.juzipi.domain.entity.*;
import com.juzipi.domain.enums.MessageTypeEnum;
import com.juzipi.domain.req.BlogAddRequest;
import com.juzipi.domain.req.BlogUpdateRequest;
import com.juzipi.domain.vo.BlogVO;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.service.BlogService;
import com.juzipi.service.FollowService;
import com.juzipi.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.juzipi.constants.PageConstants.PAGE_SIZE;
import static com.juzipi.constants.RedisConstants.*;
import static com.juzipi.constants.RedissonConstant.BLOG_LIKE_LOCK;

@Service
@Slf4j
public class BlogServiceImpl implements BlogService {

    @Value("${yuchuang.qiniu.url}")
    private String QINIU_URL;
    @Resource
    private BlogDao blogDao;

    @Resource
    private FollowService followService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BlogLikeDao blogLikeDao;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserDao userDao;

    @Resource
    private FollowDao followDao;

    @Resource
    private MessageDao messageDao;
    @Resource
    private FileUtils fileUtils;

    @Override
    public Page<BlogVO> pageBlog(long currentPage, String title, Long userId) {
        LambdaQueryWrapper<Blog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.like(StringUtils.isNotBlank(title), Blog::getTitle, title);
        blogLambdaQueryWrapper.orderBy(true, false, Blog::getCreateTime);
        Page<Blog> blogPage = blogDao.page(new Page<>(currentPage, PAGE_SIZE), blogLambdaQueryWrapper);
        Page<BlogVO> blogVoPage = new Page<>();
        BeanUtils.copyProperties(blogVoPage, blogVoPage);

        List<BlogVO> blogVoList = blogPage.getRecords().stream().map((blog) -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            if (userId != null) {
                LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
                blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blog.getId()).eq(BlogLike::getUserId, userId);
                int count = blogLikeDao.count(blogLikeLambdaQueryWrapper);
                blogVO.setIsLike(count > 0);
            }
            return blogVO;
        }).collect(Collectors.toList());
        for (BlogVO blogVO : blogVoList) {
            String images = blogVO.getImages();
            if (images == null) {
                continue;
            }
            String[] imgStrs = images.split(",");
            blogVO.setCoverImage(imgStrs[0]);
        }
        blogVoPage.setRecords(blogVoList);
        return blogVoPage;
    }

    @Override
    public Long addBlog(BlogAddRequest blogAddRequest, User loginUser) {
        Blog blog = new Blog();
        ArrayList<String> imageNameList = new ArrayList<>();
        try {
            MultipartFile[] images = blogAddRequest.getImages();
            if (images != null) {
                for (MultipartFile image : images) {
                    String fileName = fileUtils.uploadFile(image);
                    imageNameList.add(fileName);
                }
                String imageStr = StringUtils.join(imageNameList, ",");
                blog.setImages(imageStr);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
        blog.setUserId(loginUser.getId());
        blog.setTitle(blogAddRequest.getTitle());
        blog.setContent(blogAddRequest.getContent());

        boolean result = blogDao.save(blog);
        if (result) {
            List<UserVO> userVOList = followService.listFans(loginUser.getId());
            if (!userVOList.isEmpty()) {
                for (UserVO userVO : userVOList) {
                    String key = BLOG_FEED_KEY + userVO.getId();
                    stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
                    String likeNumKey = MESSAGE_BLOG_NUM_KEY + userVO.getId();
                    Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
                    if (Boolean.TRUE.equals(hasKey)) {
                        stringRedisTemplate.opsForValue().increment(likeNumKey);
                    } else {
                        stringRedisTemplate.opsForValue().set(likeNumKey, "1");
                    }
                }
            }
        }
        return blog.getId();
    }

    @Override
    public Page<BlogVO> listMyBlogPages(long currentPage, Long id) {
        if (currentPage <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<Blog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.eq(Blog::getUserId, id);
        Page<Blog> blogPage = blogDao.page(new Page<>(currentPage, PAGE_SIZE), blogLambdaQueryWrapper);
        Page<BlogVO> blogVOPage = new Page<>();
        BeanUtils.copyProperties(blogPage, blogVOPage);

        List<BlogVO> blogVOList = blogPage.getRecords().stream().map((blog) -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            System.out.println("blogVO = " + blogVO);
            return blogVO;
        }).collect(Collectors.toList());
        System.out.println(blogVOList);
        for (BlogVO blogVO : blogVOList) {
            String images = blogVO.getImages();
            if (images == null) {
                continue;
            }
            String[] imgStr = images.split(",");
            blogVO.setCoverImage( imgStr[0]);
        }
        blogVOPage.setRecords(blogVOList);
        return blogVOPage;
    }

    @Override
    public void likeBlog(long blogId, Long userId) {
        RLock lock = redissonClient.getLock(BLOG_LIKE_LOCK + blogId + ":" + userId);
        try {
            if (lock.tryLock(0, -1, TimeUnit.MINUTES)) {
                Blog blog = blogDao.getById(blogId);
                if (blog == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "博文不存在");
                }
                LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
                blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blogId);
                blogLikeLambdaQueryWrapper.eq(BlogLike::getUserId, userId);
                long isLike = blogLikeDao.count(blogLikeLambdaQueryWrapper);
                if (isLike > 0) {
                    blogLikeDao.remove(blogLikeLambdaQueryWrapper);
                    int newNum = blog.getLikedNum() - 1;
                    blogDao.update().eq("id", blogId).set("like_num", newNum).update();
                    LambdaQueryWrapper<Message> messageQueryWrapper = new LambdaQueryWrapper<>();
                    messageQueryWrapper.eq(Message::getType, MessageTypeEnum.BLOG_LIKE.getValue()).eq(Message::getFromId, userId).eq(Message::getToId, blog.getUserId()).eq(Message::getData, String.valueOf(blog.getId()));
                    messageDao.remove(messageQueryWrapper);
                    String likeNumKey = MESSAGE_LIKE_NUM_KEY + blog.getUserId();
                    String upNumStr = stringRedisTemplate.opsForValue().get(likeNumKey);
                    if (!StrUtil.isNullOrUndefined(upNumStr) && Long.parseLong(upNumStr) != 0) {
                        stringRedisTemplate.opsForValue().decrement(likeNumKey);
                    }
                } else {
                    BlogLike blogLike = new BlogLike();
                    blogLike.setBlogId(blogId);
                    blogLike.setUserId(userId);
                    blogLikeDao.save(blogLike);
                    int newNum = blog.getLikedNum() + 1;
                    blogDao.update().eq("id", blogId).set("like_num", newNum).update();
                    Message message = new Message();
                    message.setType(MessageTypeEnum.BLOG_LIKE.getValue());
                    message.setFromId(userId);
                    message.setToId(blog.getUserId());
                    message.setData(String.valueOf(blog.getId()));
                    messageDao.save(message);
                    String likeNumKey = MESSAGE_LIKE_NUM_KEY + blog.getUserId();
                    Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
                    if (Boolean.TRUE.equals(hasKey)) {
                        stringRedisTemplate.opsForValue().increment(likeNumKey);
                    } else {
                        stringRedisTemplate.opsForValue().set(likeNumKey, "1");
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
    public BlogVO getBlogById(Long blogId, Long userId) {
        Blog blog = blogDao.getById(blogId);
        if (blog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有该博文！");
        }
        BlogVO blogVO = new BlogVO();
        BeanUtils.copyProperties(blog, blogVO);
        LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blogId);
        blogLikeLambdaQueryWrapper.eq(BlogLike::getUserId, userId);
        long isLike = blogLikeDao.count(blogLikeLambdaQueryWrapper);
        blogVO.setIsLike(isLike > 0);
        User author = userDao.getById(blog.getUserId());
        if (author == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有该博主！");
        }
        UserVO authorVO = new UserVO();
        BeanUtils.copyProperties(author, authorVO);
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getFollowUserId, authorVO.getId()).eq(Follow::getUserId, userId);
        long count = followDao.count(followLambdaQueryWrapper);
        authorVO.setIsFollow(count > 0);
        blogVO.setAuthor(authorVO);
        String images = blogVO.getImages();
        if (images == null) {
            return blogVO;
        }
        String[] imgStrs = images.split(",");
        ArrayList<String> imgStrList = new ArrayList<>();
        for (String imgStr : imgStrs) {
            imgStrList.add( imgStr);
        }
        String imgStr = StringUtils.join(imgStrList, ",");
        blogVO.setImages(imgStr);
        blogVO.setCoverImage(imgStrList.get(0));
        return blogVO;
    }

    @Override
    public void deleteBlogById(Long blogId, Long userId, boolean isAdmin) {
        if (ObjectUtils.isEmpty(blogId) || ObjectUtils.isEmpty(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (isAdmin) {
            blogDao.removeById(blogId);
            return;
        }
        Blog blog = blogDao.getById(blogId);
        if (!userId.equals(blog.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        blogDao.removeById(blogId);
    }

    @Override
    public void updateBlog(BlogUpdateRequest blogUpdateRequest, Long userId, boolean admin) {
        if (ObjectUtils.isEmpty(blogUpdateRequest) || blogUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long createUserId = blogDao.getById(blogUpdateRequest.getId()).getUserId();
        if (!createUserId.equals(userId) && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH, "没有权限");
        }
        String title = blogUpdateRequest.getTitle();
        String content = blogUpdateRequest.getContent();
        if (StringUtils.isAnyBlank(title, content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Blog blog = new Blog();
        blog.setId(blogUpdateRequest.getId());
        ArrayList<String> imageNameList = new ArrayList<>();
        if (StringUtils.isNotBlank(blogUpdateRequest.getImgStr())) {
            String imgStr = blogUpdateRequest.getImgStr();
            String[] imgs = imgStr.split(",");
            for (String img : imgs) {
                imageNameList.add(img.substring(25));
            }
        }
        if (blogUpdateRequest.getImages() != null) {
            MultipartFile[] images = blogUpdateRequest.getImages();
            for (MultipartFile image : images) {
                String fileName = fileUtils.uploadFile(image);
                imageNameList.add(fileName);
            }
        }
        if (!imageNameList.isEmpty()) {
            String imageStr = StringUtils.join(imageNameList, ",");
            blog.setImages(imageStr);
        }
        blog.setTitle(blogUpdateRequest.getTitle());
        blog.setContent(blogUpdateRequest.getContent());
        blogDao.updateById(blog);

    }

}
