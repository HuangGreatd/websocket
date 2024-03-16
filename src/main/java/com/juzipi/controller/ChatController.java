package com.juzipi.controller;

import com.juzipi.common.BaseResponse;
import com.juzipi.common.ErrorCode;
import com.juzipi.common.ResultUtis;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.ChatRequest;
import com.juzipi.domain.vo.ChatMessageVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.service.ChatService;
import com.juzipi.service.UserService;
import com.juzipi.websotck.WebSocket;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.juzipi.constants.ChatConstant.*;

/**
 * 聊天控制器
 *
 * @Author : Juzipi
 * @create 2024/1/25 19:43
 */
@RestController
@RequestMapping("/chat")
@Api(tags = "聊天模块")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private UserService userService;


    @Resource
    private WebSocket webSocket;

    @PostMapping("/privateChat")
    @ApiOperation(value = "获取私聊")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "chatRequest", value = "聊天请求"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<ChatMessageVO>> sendWebSocketMessage(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        if (ObjectUtils.isEmpty(chatRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (ObjectUtils.isEmpty(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<ChatMessageVO> privateChat = chatService.getPrivateChat(chatRequest, PRIVATE_CHAT, loginUser);
        return ResultUtis.success(privateChat);
    }

    /**
     * 团队聊天
     *
     * @param chatRequest 聊天请求
     * @param request     请求
     * @return {@link BaseResponse}<{@link List}<{@link ChatMessageVO}>>
     */
    @PostMapping("/teamChat")
    @ApiOperation(value = "获取队伍聊天")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "chatRequest", value = "聊天请求"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<ChatMessageVO>> getTeamChat(@RequestBody ChatRequest chatRequest, HttpServletRequest request) {
        if (chatRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        if (ObjectUtils.isEmpty(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<ChatMessageVO> teamChat = chatService.getTeamChat(chatRequest, TEAM_CHAT, loginUser);
        return ResultUtis.success(teamChat);
    }
    /**
     * 大厅聊天
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link ChatMessageVO}>>
     */
    @GetMapping("/hallChat")
    @ApiOperation(value = "获取大厅聊天")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<ChatMessageVO>> getHallChat(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<ChatMessageVO> hallChat = chatService.getHallChat(HALL_CHAT, loginUser);
        return ResultUtis.success(hallChat);
    }
}
