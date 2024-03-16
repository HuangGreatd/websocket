package com.juzipi.controller;


import com.juzipi.common.BaseResponse;
import com.juzipi.common.ErrorCode;
import com.juzipi.common.ResultUtis;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.FriendAddRequest;
import com.juzipi.domain.vo.FriendsRecordVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.service.FriendsService;
import com.juzipi.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 好友申请管理表 前端控制器
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-22
 */
@RestController
@RequestMapping("/friends")
@Api(tags = "好友管理模块")
public class FriendsController {

    @Resource
    private FriendsService friendsService;

    @Resource
    private UserService userService;

    /**
     * 添加好友
     *
     * @param friendAddRequest 好友添加请求
     * @param request          请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/add")
    @ApiOperation(value = "添加好友")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "friendAddRequest", value = "好友添加请求"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Boolean> addFriendRecords(@RequestBody FriendAddRequest friendAddRequest, HttpServletRequest request) {
        if (friendAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        User loginUser = userService.getLoginUser(request);
        boolean addStatus = friendsService.addFriendRecords(loginUser, friendAddRequest);
        return ResultUtis.success(addStatus);
    }
    /**
     * 查询记录
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link FriendsRecordVO}>>
     */
    @GetMapping("/getRecords")
    @ApiOperation(value = "查询记录")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<FriendsRecordVO>> getRecords(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<FriendsRecordVO> friendsList = friendsService.obtainFriendApplicationRecords(loginUser);
        return ResultUtis.success(friendsList);
    }

    /**
     * 获取未读记录条数
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link Integer}>
     */
    @GetMapping("/getRecordCount")
    @ApiOperation(value = "查询记录数量")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Integer> getRecordCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        int recordCount = friendsService.getRecordCount(loginUser);
        return ResultUtis.success(recordCount);
    }

    /**
     * 获取我申请的记录
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link FriendsRecordVO}>>
     */
    @GetMapping("/getMyRecords")
    @ApiOperation(value = "获取我申请的记录")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<FriendsRecordVO>> getMyRecords(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<FriendsRecordVO> myFriendsList = friendsService.getMyRecords(loginUser);
        return ResultUtis.success(myFriendsList);
    }

    /**
     * 同意申请
     *
     * @param fromId  从id
     * @param request 请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/agree/{fromId}")
    @ApiOperation(value = "同意申请")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "fromId", value = "申请id"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Boolean> agreeToApply(@PathVariable("fromId") Long fromId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean agreeToApplyStatus = friendsService.agreeToApply(loginUser, fromId);
        return ResultUtis.success(agreeToApplyStatus);
    }


    /**
     * 阅读
     *
     * @param ids     id
     * @param request 请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @GetMapping("/read")
    @ApiOperation(value = "阅读")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "ids", value = "申请id"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Boolean> toRead(@RequestParam(required = false) Set<Long> ids, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(ids)) {
            return ResultUtis.success(false);
        }
        User loginUser = userService.getLoginUser(request);
        boolean isRead = friendsService.toRead(loginUser, ids);
        return ResultUtis.success(isRead);
    }

}

