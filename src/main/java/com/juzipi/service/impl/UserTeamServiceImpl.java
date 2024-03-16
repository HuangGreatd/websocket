package com.juzipi.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.juzipi.domain.entity.UserTeam;
import com.juzipi.mapper.UserTeamMapper;
import com.juzipi.service.UserTeamService;
import org.springframework.stereotype.Service;

@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam> implements UserTeamService {
}
