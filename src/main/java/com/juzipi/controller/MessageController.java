package com.juzipi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.common.BaseResponse;
import com.juzipi.common.ErrorCode;
import com.juzipi.common.ResultUtis;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.vo.BlogVO;
import com.juzipi.domain.vo.MessageVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.service.MessageService;
import com.juzipi.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.juzipi.constants.RedisConstants.MESSAGE_BLOG_NUM_KEY;

@RestController
@RequestMapping("/message")
@Api(tags = "消息管理模块")
public class MessageController {

    @Resource
    private MessageService messageService;

    /**
     * redis
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserService userService ;

    /**
     * 用户是否有新消息
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @GetMapping
    @ApiOperation(value = "用户是否有新消息")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Boolean> userHasNewMessage(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResultUtis.success(false);
        }
        Boolean hasNewMessage = messageService.hasNewMessage(loginUser.getId());
        return ResultUtis.success(hasNewMessage);
    }

    /**
     * 获取用户新消息数量
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link Long}>
     */
    @GetMapping("/num")
    @ApiOperation(value = "获取用户新消息数量")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Long> getUserMessageNum(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResultUtis.success(0L);
        }
        long messageNum = messageService.getMessageNum(loginUser.getId());
        return ResultUtis.success(messageNum);
    }

    /**
     * 获取用户点赞消息数量
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link Long}>
     */
    @GetMapping("/like/num")
    @ApiOperation(value = "获取用户点赞消息数量")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Long> getUserLikeMessageNum(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long messageNum = messageService.getLikeNum(loginUser.getId());
        return ResultUtis.success(messageNum);
    }

    /**
     * 获取用户点赞消息
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link MessageVO}>>
     */
    @GetMapping("/like")
    @ApiOperation(value = "获取用户点赞消息")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Page<MessageVO>> getUserLikeMessage(HttpServletRequest request, Long currentPage) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Page<MessageVO> messageVOPage = messageService.pageLike(loginUser.getId(), currentPage);
        return ResultUtis.success(messageVOPage);
    }

    /**
     * 获取用户博客消息数量
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link String}>
     */
    @GetMapping("/blog/num")
    @ApiOperation(value = "获取用户博客消息数量")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<String> getUserBlogMessageNum(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String likeNumKey = MESSAGE_BLOG_NUM_KEY + loginUser.getId();
        Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
        if (Boolean.TRUE.equals(hasKey)) {
            String num = stringRedisTemplate.opsForValue().get(likeNumKey);
            return ResultUtis.success(num);
        } else {
            return ResultUtis.success("0");
        }
    }

    /**
     * 获取用户博客消息
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link BlogVO}>>
     */
    @GetMapping("/blog")
    @ApiOperation(value = "获取用户博客消息")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<BlogVO>> getUserBlogMessage(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<BlogVO> blogVOList = messageService.getUserBlog(loginUser.getId());
        return ResultUtis.success(blogVOList);
    }
}
