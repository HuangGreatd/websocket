package com.juzipi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.juzipi.common.ErrorCode;
import com.juzipi.dao.FollowDao;
import com.juzipi.dao.UserDao;
import com.juzipi.domain.entity.Follow;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.mapper.FollowMapper;
import com.juzipi.service.FollowService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.juzipi.constants.PageConstants.PAGE_SIZE;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper,Follow> implements FollowService {
    @Resource
    private FollowDao followDao;

    @Resource
    private UserDao userDao;
    @Override
    public List<UserVO> listFans(Long userId) {
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getFollowUserId,userId);
        List<Follow> list = followDao.list(followLambdaQueryWrapper);
        if (list == null || list.isEmpty()){
            return new ArrayList<>();
        }
        List<User> userList = list.stream().map((follow -> userDao.getById(follow.getUserId()))).filter(Objects::nonNull).collect(Collectors.toList());
        return userList.stream().map((item) -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(item, userVO);
            LambdaQueryWrapper<Follow> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, item.getId());
            int count = followDao.count(lambdaQueryWrapper);
            userVO.setIsFollow(count > 0);
            return userVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void followUser(Long followUserId, Long userId) {
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getFollowUserId,followUserId).eq(Follow::getUserId,userId);
        int count = this.count(followLambdaQueryWrapper);
        if (count == 0){
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            this.save(follow);
        }else {
            this.remove(followLambdaQueryWrapper);
        }
    }


    @Override
    public Page<UserVO> pageMyFollow(Long userId, String currentPage) {
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getUserId,userId);
        Page<Follow> followPage = this.page(new Page<>(Long.parseLong(currentPage), PAGE_SIZE), followLambdaQueryWrapper);
        if (followPage == null || followPage.getSize() == 0){
            return new Page<>();
        }
        Page<UserVO> userVOPage = new Page<>();
        List<User> userList
                = followPage.getRecords().stream().map((follow -> userDao.getById(follow.getFollowUserId()))).collect(Collectors.toList());
        List<UserVO> userVoList = userList.stream().map((user) -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            userVO.setIsFollow(true);
            return userVO;
        }).collect(Collectors.toList());
        return userVOPage.setRecords(userVoList);
    }

    @Override
    public Page<UserVO> pageFans(Long userId, String currentPage) {
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getFollowUserId, userId);
        Page<Follow> followPage = this.page(new Page<>(Long.parseLong(currentPage), PAGE_SIZE), followLambdaQueryWrapper);
        if (followPage == null || followPage.getSize() == 0) {
            return new Page<>();
        }
        Page<UserVO> userVoPage = new Page<>();
        BeanUtils.copyProperties(followPage,userVoPage);
        List<User> userList = followPage.getRecords().stream().map((follow -> userDao.getById(follow.getUserId()))).filter(Objects::nonNull).collect(Collectors.toList());
        List<UserVO> userVOList = userList.stream().map((item) -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(item, userVO);
            LambdaQueryWrapper<Follow> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, item.getId());
            long count = this.count(lambdaQueryWrapper);
            userVO.setIsFollow(count > 0);
            return userVO;
        }).collect(Collectors.toList());
        userVoPage.setRecords(userVOList);
        return userVoPage;

    }
}
