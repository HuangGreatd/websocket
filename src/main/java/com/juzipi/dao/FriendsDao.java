package com.juzipi.dao;

import com.juzipi.domain.entity.Friends;
import com.juzipi.mapper.FriendsMapper;
import com.juzipi.service.FriendsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 好友申请管理表 服务实现类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-22
 */
@Service
public class FriendsDao extends ServiceImpl<FriendsMapper, Friends> {

}
