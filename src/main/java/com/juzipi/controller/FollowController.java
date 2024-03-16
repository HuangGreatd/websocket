package com.juzipi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.common.BaseResponse;
import com.juzipi.common.ErrorCode;
import com.juzipi.common.ResultUtis;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.service.FollowService;
import com.juzipi.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 关注控制器
 */
@RestController
@RequestMapping("/follow")
@Api(tags = "关注管理模块")
public class FollowController {
    /**
     * 关注服务
     */
    @Resource
    private FollowService followService;

    @Resource
    private UserService userService;

    /**
     * 关注用户
     *
     * @param id      id
     * @param request 请求
     * @return {@link BaseResponse}<{@link String}>
     */
    @PostMapping("/{id}")
    @ApiOperation(value = "关注用户")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "id", value = "关注用户id"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<String> followUser(@PathVariable Long id, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        followService.followUser(id,loginUser.getId());
        return ResultUtis.success("ok");
    }

    /**
     * 获取粉丝
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link UserVO}>>
     */
    @GetMapping("/fans")
    @ApiOperation(value = "获取粉丝")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Page<UserVO>> listFans(HttpServletRequest request, String currentPage){
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Page<UserVO> userVOPage = followService.pageFans(loginUser.getId(), currentPage);
        return ResultUtis.success(userVOPage);
    }

    /**
     * 获取我关注的用户
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link UserVO}>>
     */
    @GetMapping("/my")
    @ApiOperation(value = "获取我关注的用户")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Page<UserVO>> listMyFollow(HttpServletRequest request, String currentPage){
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Page<UserVO> userVoPage = followService.pageMyFollow(loginUser.getId(),currentPage);
        return ResultUtis.success(userVoPage);
    }
}
