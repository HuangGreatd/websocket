package com.juzipi.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.domain.entity.User;
import com.juzipi.mapper.UserMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.juzipi.constants.PageConstants.PAGE_SIZE;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-01-31
 */
@Service
public class UserDao extends ServiceImpl<UserMapper, User> {

    public User checkHasRegistered(String phone) {
        return lambdaQuery().eq(User::getPhone, phone).one();
    }


    public User findUserIsHas(String userAccount) {
        return lambdaQuery().eq(User::getUserAccount, userAccount).one();
    }

    public boolean updateUserPassword(String phone, String encryptPassword) {
        return lambdaUpdate().eq(User::getPhone, phone).set(User::getPassword, encryptPassword).update();
    }

    public Page<User> searchUsersByTags(List<String> tagNameList, long currentPage) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.or().like(Strings.isNotEmpty(tagName), User::getTags, tagName);
        }
        return page(new Page<>(currentPage, PAGE_SIZE), queryWrapper);
    }
}
