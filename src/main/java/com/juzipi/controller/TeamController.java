package com.juzipi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.common.BaseResponse;
import com.juzipi.common.ErrorCode;
import com.juzipi.common.ResultUtis;
import com.juzipi.dao.UserTeamDao;
import com.juzipi.domain.entity.Team;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.entity.UserTeam;
import com.juzipi.domain.req.*;
import com.juzipi.domain.vo.TeamVO;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.service.TeamService;
import com.juzipi.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/2/6 18:28
 */
@RestController
@RequestMapping("/team")
@Api(tags = "队伍管理模块")
@Log4j2
public class TeamController {
    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;


    @Resource
    private UserTeamDao userTeamDao;

    /**
     * 创建团队
     *
     * @param teamAddRequest 团队添加请求
     * @param request        请求
     * @return {@link BaseResponse}<{@link Long}>
     */
    @PostMapping("/add")
    @ApiOperation(value = "创建队伍")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "teamAddRequest", value = "队伍添加请求参数"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        Long teamId = teamService.addTeam(team, loginUser);
        return ResultUtis.success(teamId);
    }

    /**
     * 更新团队
     *
     * @param teamUpdateRequest 团队更新请求
     * @param request           请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/update")
    @ApiOperation(value = "更新队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamUpdateRequest", value = "队伍更新请求参数"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        teamService.updateTeam(teamUpdateRequest, loginUser);
        return ResultUtis.success(true);
    }

    /**
     * 通过id获取团队
     *
     * @param id      id
     * @param request 请求
     * @return {@link BaseResponse}<{@link TeamVO}>
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", value = "队伍id")})
    public BaseResponse<TeamVO> getTeamById(@PathVariable Long id, HttpServletRequest request) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return ResultUtis.success(teamService.getTeam(id, loginUser.getId()));
    }

    /**
     * 团队名单
     *
     * @param currentPage      当前页面
     * @param teamQueryRequest 团队查询请求
     * @param request          请求
     * @return {@link BaseResponse}<{@link Page}<{@link TeamVO}>>
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取队伍列表")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamQueryRequest", value = "队伍查询请求参数"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Page<TeamVO>> listTeams(long currentPage, TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Page<TeamVO> teamVOPage = teamService.listTeams(currentPage, teamQueryRequest, userService.isAdmin(loginUser));
        Page<TeamVO> finalPage = getTeamHasJoinNum(teamVOPage);
        return getUserJoinedList(loginUser, finalPage);
    }


    /**
     * 加入团队
     *
     * @param teamJoinRequest 团队加入请求
     * @param request         请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/join")
    @ApiOperation(value = "加入队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamJoinRequest", value = "加入队伍请求参数"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtis.success(result);
    }

    /**
     * 退出团队
     *
     * @param teamQuitRequest 团队辞职请求
     * @param request         请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/quit")
    @ApiOperation(value = "退出队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamQuitRequest", value = "退出队伍请求参数"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtis.success(result);
    }

    /**
     * 删除团队
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/delete")
    @ApiOperation(value = "解散队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "deleteRequest", value = "解散队伍请求参数"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        long id = deleteRequest.getId();
        if (deleteRequest == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean admin = userService.isAdmin(loginUser);
        boolean result = teamService.deleteTeam(id, loginUser, admin);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtis.success(true);
    }

    /**
     * 我创建团队名单
     *
     * @param currentPage 当前页面
     * @param teamQuery   团队查询
     * @param request     请求
     * @return {@link BaseResponse}<{@link Page}<{@link TeamVO}>>
     */
    @GetMapping("/list/my/create")
    @ApiOperation(value = "获取我创建的队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamQuery", value = "获取队伍请求参数"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Page<TeamVO>> listMyCreateTeams(long currentPage, TeamQueryRequest teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        Page<TeamVO> teamVOPage = teamService.listMyCreate(currentPage, loginUser.getId());
        Page<TeamVO> finalPage = getTeamHasJoinNum(teamVOPage);
        return getUserJoinedList(loginUser, finalPage);
    }

    /**
     * 我加入团队名单
     *
     * @param currentPage 当前页面
     * @param teamQuery   团队查询
     * @param request     请求
     * @return {@link BaseResponse}<{@link Page}<{@link TeamVO}>>
     */
    @GetMapping("/list/my/join")
    @ApiOperation(value = "获取我加入的队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamQuery", value = "获取队伍请求参数"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Page<TeamVO>> listMyJoinTeams(long currentPage, TeamQueryRequest teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userTeamLambdaQueryWrapper.eq(UserTeam::getUserId, loginUser.getId());
        List<UserTeam> userTeamList = userTeamDao.list(userTeamLambdaQueryWrapper);
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        if (idList.isEmpty()) {
            return ResultUtis.success(new Page<TeamVO>());
        }
        teamQuery.setIdList(idList);
        Page<TeamVO> teamVOPage = teamService.listMyJoin(currentPage, teamQuery);
        Page<TeamVO> finalPage = getTeamHasJoinNum(teamVOPage);
        return getUserJoinedList(loginUser, finalPage);
    }

    /**
     * 列出所有我加入团队
     *
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link TeamVO}>>
     */
    @GetMapping("/list/my/join/all")
    @ApiOperation(value = "获取我加入的队伍")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamQuery", value = "获取队伍请求参数"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<TeamVO>> listAllMyJoinTeams(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<TeamVO> teamVOList = teamService.listAllMyJoin(loginUser.getId());
        return ResultUtis.success(teamVOList);
    }

    /**
     * 获取已加入的用户
     *
     * @param loginUser 登录用户
     * @param teamPage  团队页面
     * @return {@link BaseResponse}<{@link Page}<{@link TeamVO}>>
     */
    private BaseResponse<Page<TeamVO>> getUserJoinedList(User loginUser, Page<TeamVO> teamPage) {
        try {
            List<TeamVO> teamList = teamPage.getRecords();
            List<Long> teamIdList = teamList.stream().map(TeamVO::getId).collect(Collectors.toList());
            //判断当前用户已加入的队伍
            LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userTeamLambdaQueryWrapper.eq(UserTeam::getUserId, loginUser.getId())
                    .in(UserTeam::getTeamId, teamIdList);
            List<UserTeam> userTeamList = userTeamDao.list(userTeamLambdaQueryWrapper);
            Set<Long> joinTeamIdList = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                team.setHasJoin(joinTeamIdList.contains(team.getId()));
            });
            teamPage.setRecords(teamList);
        } catch (Exception ignored) {

        }
        return ResultUtis.success(teamPage);
    }

    /**
     * 获取队伍已加入人数
     *
     * @param teamVOPage 团队vopage
     * @return {@link Page}<{@link TeamVO}>
     */
    private Page<TeamVO> getTeamHasJoinNum(Page<TeamVO> teamVOPage) {
        List<TeamVO> teamList = teamVOPage.getRecords();
        teamList.forEach((team) -> {
            LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userTeamLambdaQueryWrapper.eq(UserTeam::getTeamId, team.getId());
            long hasJoinNum = userTeamDao.count(userTeamLambdaQueryWrapper);
            team.setHasJoinNum(hasJoinNum);
        });
        teamVOPage.setRecords(teamList);
        return teamVOPage;
    }

    /**
     * 通过id获取团队成员
     *
     * @param id      id
     * @param request 请求
     * @return {@link BaseResponse}<{@link List}<{@link UserVO}>>
     */
    @GetMapping("/member/{id}")
    @ApiOperation(value = "获取队伍成员")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", value = "队伍id"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<List<UserVO>> getTeamMemberById(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<UserVO> teamMember = teamService.getTeamMember(id, loginUser.getId());
        return ResultUtis.success(teamMember);
    }

    /**
     * 更新封面图片
     *
     * @param teamCoverUpdateRequest 团队包括变更请求
     * @param request                请求
     * @return {@link BaseResponse}<{@link String}>
     */
    @PutMapping("/cover")
    @ApiOperation(value = "更新封面图片")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamCoverUpdateRequest", value = "队伍封面更新请求"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<String> changeCoverImage(TeamCoverUpdateRequest teamCoverUpdateRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean admin = userService.isAdmin(loginUser);
        teamService.changeCoverImage(teamCoverUpdateRequest,loginUser.getId(),admin);
        return ResultUtis.success("ok");
    }

    /**
     * 踢出队员
     *
     * @param teamKickOutRequest 踢出队员请求
     * @param request                请求
     * @return {@link BaseResponse}<{@link String}>
     */
    @PostMapping("/kick")
    @ApiOperation(value = "踢出队员")
    @ApiImplicitParams({@ApiImplicitParam(name = "teamKickOutRequest", value = "踢出队员请求"),
            @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<String> kickTeamMember(@RequestBody TeamKickOutRequest teamKickOutRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long teamId = teamKickOutRequest.getTeamId();
        Long userId = teamKickOutRequest.getUserId();
        boolean admin = userService.isAdmin(loginUser);
        teamService.kickTeamMember(teamId,userId,loginUser.getId(),admin);
        return ResultUtis.success("ok");
    }


}
