package com.juzipi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.domain.entity.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.*;
import com.juzipi.domain.vo.TeamVO;
import com.juzipi.domain.vo.UserVO;

import java.util.List;

/**
 * <p>
 * 队伍 服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-06
 */
public interface TeamService {

    Long addTeam(Team team, User loginUser);

    void updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    TeamVO getTeam(Long id, Long id1);

    Page<TeamVO> listTeams(long currentPage, TeamQueryRequest teamQueryRequest, boolean admin);

    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    boolean deleteTeam(long id, User loginUser, boolean admin);

    Page<TeamVO> listMyCreate(long currentPage, Long id);

    List<UserVO> getTeamMember(Long id, Long id1);

    void changeCoverImage(TeamCoverUpdateRequest teamCoverUpdateRequest, Long id, boolean admin);

    void kickTeamMember(Long teamId, Long userId, Long id, boolean admin);

    List<TeamVO> listAllMyJoin(Long id);

    Page<TeamVO> listMyJoin(long currentPage, TeamQueryRequest teamQuery);
}
