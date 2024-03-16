package com.juzipi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.common.ErrorCode;
import com.juzipi.dao.FollowDao;
import com.juzipi.dao.TeamDao;
import com.juzipi.dao.UserDao;
import com.juzipi.dao.UserTeamDao;
import com.juzipi.domain.entity.Follow;
import com.juzipi.domain.entity.Team;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.entity.UserTeam;
import com.juzipi.domain.enums.TeamStatusEnum;
import com.juzipi.domain.req.*;
import com.juzipi.domain.vo.TeamVO;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.mapper.TeamMapper;
import com.juzipi.service.TeamService;
import com.juzipi.service.UserService;
import com.juzipi.service.UserTeamService;
import com.juzipi.utils.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.juzipi.constants.PageConstants.PAGE_SIZE;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/2/6 17:36
 */
@Service
public class TeamServiceImpl implements TeamService {
    @Resource
    private UserDao userDao;

    @Resource
    private TeamDao teamDao;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private UserTeamDao userTeamDao;

    @Resource
    private UserService userService;

    @Resource
    private FollowDao followDao;


    @Value("/file/upload")
    private String QINIU_URL;

    @Resource
    private FileUtils fileUtils;

    @Resource
    private UserTeamService userTeamService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTeam(Team team, User loginUser) {
        //1.请求参数是否为空
        if (ObjectUtils.isEmpty(team)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (team.getExpireTime() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(team.getExpireTime());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        } else {
            team.setExpireTime(null);
        }
        final long userId = loginUser.getId();
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Team::getUserId, userId);
        long hasTeamNum = teamDao.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
        // 1. 队伍人数 > 1 且 <= 20
        Integer maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        // 2.队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        // 3.描述 <= 1024
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || description.length() > 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //   4. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        // 6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
        // 8. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = teamDao.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 9. 插入队伍 -》 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamDao.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    public void updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = teamDao.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员或者队伍的创建者可以修改
        if (!oldTeam.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须要设置密码");
            }
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        boolean result = teamDao.updateById(updateTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "更新队伍失败");
        }
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 该用户已加入的队伍数量
        long userId = loginUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("user_id", userId);
        long hasJoinNum = userTeamDao.count(userTeamQueryWrapper);
        if (hasJoinNum > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
        }
        // 不能重复加入已加入的队伍
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("user_id", userId);
        userTeamQueryWrapper.eq("team_id", teamId);
        long hasUserJoinTeam = userTeamDao.count(userTeamQueryWrapper);
        if (hasUserJoinTeam > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }
        // 已加入队伍的人数
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
        }
        // 修改队伍信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamDao.save(userTeam);
    }

    @Override
    public TeamVO getTeam(Long teamId, Long userId) {
        Team team = teamDao.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        TeamVO teamVO = new TeamVO();
        BeanUtils.copyProperties(team, teamVO);
        LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userTeamLambdaQueryWrapper.eq(UserTeam::getTeamId, teamId);
        long count = userTeamDao.count(userTeamLambdaQueryWrapper);
        teamVO.setHasJoinNum(count);
        userTeamLambdaQueryWrapper.eq(UserTeam::getUserId, userId);
        long userJoin = userTeamDao.count(userTeamLambdaQueryWrapper);
        teamVO.setHasJoin(userJoin > 0);
        User leader = userDao.getById(team.getUserId());
        teamVO.setLeaderName(leader.getUsername());
        return teamVO;
    }

    @Override
    public Page<TeamVO> listTeams(long currentPage, TeamQueryRequest teamQuery, boolean isAdmin) {

        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("max_num", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("user_id", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expire_time", new Date()).or().isNull("expire_time"));
        Page<Team> teamPage = teamDao.page(new Page<>(currentPage, PAGE_SIZE), queryWrapper);
        if (CollectionUtils.isEmpty(teamPage.getRecords())) {
            return new Page<>();
        }
        Page<TeamVO> teamVOPage = new Page<>();
        // 关联查询创建人的用户信息
        BeanUtils.copyProperties(teamPage, teamVOPage, "records");
        List<Team> teamPageRecords = teamPage.getRecords();
        ArrayList<TeamVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamPageRecords) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userDao.getById(userId);
            TeamVO teamUserVO = new TeamVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        teamVOPage.setRecords(teamUserVOList);
        return teamVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        Team team = teamDao.getById(teamId);
        Long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setTeamId(teamId);
        queryUserTeam.setUserId(userId);
        LambdaQueryWrapper<UserTeam> queryWrapper = new LambdaQueryWrapper<>();
        int count = userTeamDao.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        //队伍只剩1人，解散
        if (teamHasJoinNum == 1) {
            //删除队伍
            teamDao.removeById(teamId);
        } else {
            //队伍至少还剩两人
            //1. 查询已加入队伍的所有用户和加入时间
            LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userTeamLambdaQueryWrapper.eq(UserTeam::getTeamId, teamId);
            userTeamLambdaQueryWrapper.last("order by id desc limit 2");
            List<UserTeam> userTeamList = userTeamDao.list(userTeamLambdaQueryWrapper);
            if (ObjectUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            UserTeam nextUserTean = userTeamList.get(1);
            Long nextUserTeanUserId = nextUserTean.getUserId();
            //更新当前队伍的队长
            Team updateTeam = new Team();
            updateTeam.setId(teamId);
            updateTeam.setUserId(nextUserTeanUserId);
            boolean result = teamDao.updateById(updateTeam);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
            }
        }
        //移除关系
        return userTeamDao.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser, boolean admin) {
        //校验队伍是否存在
        Team team = teamDao.getById(id);
        Long teamId = team.getId();
        //校验是不是读物的队长
        if (admin) {
            //移除所有加入队伍的关联信息
            LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userTeamLambdaQueryWrapper.eq(UserTeam::getTeamId, teamId);
            boolean remove = userTeamDao.remove(userTeamLambdaQueryWrapper);
            if (!remove) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
            }
            return teamDao.removeById(teamId);
        }
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限删除队伍");
        }
        //移除所有加入队伍的关联信息
        LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userTeamLambdaQueryWrapper.eq(UserTeam::getTeamId, teamId);
        boolean result = userTeamDao.remove(userTeamLambdaQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        return teamDao.removeById(teamId);
    }

    @Override
    public Page<TeamVO> listMyCreate(long currentPage, Long userId) {
        LambdaQueryWrapper<Team> teamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teamLambdaQueryWrapper.eq(Team::getUserId, userId);
        Page<Team> teamPage = teamDao.page(new Page<>(currentPage, PAGE_SIZE), teamLambdaQueryWrapper);
        List<TeamVO> teamVOList = teamPage.getRecords().stream().map((team) -> this.getTeam(team.getId(), userId)).collect(Collectors.toList());
        Page<TeamVO> teamVOPage = new Page<>();
        BeanUtils.copyProperties(teamPage, teamVOPage);
        teamVOPage.setRecords(teamVOList);

        return teamVOPage;
    }

    @Override
    public List<UserVO> getTeamMember(Long teamId, Long userId) {
        Team team = teamDao.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userTeamLambdaQueryWrapper.eq(UserTeam::getTeamId, teamId);
        List<UserTeam> userTeamList = userTeamDao.list(userTeamLambdaQueryWrapper);
        List<Long> userIdList = userTeamList.stream().map(UserTeam::getUserId).filter(id -> Objects.equals(id, userId))
                .collect(Collectors.toList());
        if (userIdList.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.in(User::getId, userIdList);
        List<User> userList = userDao.list(userLambdaQueryWrapper);
        return userList.stream().map((user) -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
            followLambdaQueryWrapper.eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, userId);
            int count = followDao.count(followLambdaQueryWrapper);
            userVO.setIsFollow(count > 0);
            return userVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void changeCoverImage(TeamCoverUpdateRequest request, Long userId, boolean admin) {
        MultipartFile image = request.getFile();
        if (image == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = request.getId();
        if (teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamDao.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        if (!team.getUserId().equals(userId) && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限修改队伍封面");
        }
        String fileName = fileUtils.uploadFile(image);
        Team temp = new Team();
        temp.setId(team.getId());
        temp.setCoverImage(QINIU_URL + fileName);
        teamDao.updateById(temp);
    }

    @Override
    public void kickTeamMember(Long teamId, Long userId, Long loginUserId, boolean admin) {
        if (teamId == null || teamId <=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userId == null || userId <=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userId.equals(loginUserId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不能将自己踢出");
        }
        Team team = teamDao.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        if (!team.getUserId().equals(loginUserId) && !admin){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userTeamLambdaQueryWrapper.eq(UserTeam::getTeamId,teamId).eq(UserTeam::getUserId,userId);
        userTeamDao.remove(userTeamLambdaQueryWrapper);

    }

    @Override
    public List<TeamVO> listAllMyJoin(Long id) {
        LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userTeamLambdaQueryWrapper.eq(UserTeam::getUserId, id);
        List<Long> teamIds = userTeamService.list(userTeamLambdaQueryWrapper).stream().map(UserTeam::getTeamId).collect(Collectors.toList());
        if (teamIds.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Team> teamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teamLambdaQueryWrapper.in(Team::getId, teamIds);
        List<Team> teamList = teamDao.list(teamLambdaQueryWrapper);
        return teamList.stream().map((team) -> {
            TeamVO teamVO = new TeamVO();
            BeanUtils.copyProperties(team, teamVO);
            teamVO.setHasJoin(true);
            return teamVO;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<TeamVO> listMyJoin(long currentPage, TeamQueryRequest teamQuery) {
        List<Long> idList = teamQuery.getIdList();
        LambdaQueryWrapper<Team> teamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teamLambdaQueryWrapper.in(Team::getId, idList);
        Page<Team> teamPage = teamDao.page(new Page<>(currentPage, PAGE_SIZE), teamLambdaQueryWrapper);
        if (CollectionUtils.isEmpty(teamPage.getRecords())) {
            return new Page<>();
        }
        Page<TeamVO> teamVOPage = new Page<>();
        // 关联查询创建人的用户信息
        BeanUtils.copyProperties(teamPage, teamVOPage, "records");
        List<Team> teamPageRecords = teamPage.getRecords();
        ArrayList<TeamVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamPageRecords) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userDao.getById(userId);
            TeamVO teamUserVO = new TeamVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        teamVOPage.setRecords(teamUserVOList);
        return teamVOPage;
    }

    private long countTeamUserByTeamId(Long teamId) {
        LambdaQueryWrapper<UserTeam> userTeamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userTeamLambdaQueryWrapper.eq(UserTeam::getTeamId, teamId);
        return userTeamDao.count(userTeamLambdaQueryWrapper);
    }

    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamDao.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;

    }
}
