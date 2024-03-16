package com.juzipi.dao;

import com.juzipi.domain.entity.Team;
import com.juzipi.mapper.TeamMapper;
import com.juzipi.service.TeamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 队伍 服务实现类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-06
 */
@Service
public class TeamDao extends ServiceImpl<TeamMapper, Team>  {

}
