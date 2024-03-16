package com.juzipi.mapper;

import com.juzipi.domain.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-01-31
 */
public interface UserMapper extends BaseMapper<User> {

    List<User> getRandomUser();
}
