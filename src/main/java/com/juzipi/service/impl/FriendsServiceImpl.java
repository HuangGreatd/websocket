package com.juzipi.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.juzipi.common.ErrorCode;
import com.juzipi.dao.UserDao;
import com.juzipi.domain.entity.Friends;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.FriendAddRequest;
import com.juzipi.domain.vo.FriendsRecordVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.mapper.FriendsMapper;
import com.juzipi.service.FriendsService;
import com.juzipi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.juzipi.constants.FriendConstant.*;
import static com.juzipi.constants.RedissonConstant.APPLY_LOCK;
import static com.juzipi.utils.StringUtils.stringJsonListToLongSet;

@Service
@Slf4j
public class FriendsServiceImpl extends ServiceImpl<FriendsMapper, Friends> implements FriendsService {
    @Resource
    private UserService userService;
    @Resource
    private UserDao userDao;

    @Resource
    private RedissonClient redissonClient;


    @Override
    public boolean addFriendRecords(User loginUser, FriendAddRequest friendAddRequest) {
        if (StringUtils.isNotBlank(friendAddRequest.getRemark()) && friendAddRequest.getRemark().length() > 120) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请备注最多120个字符");
        }
        if (ObjectUtils.anyNull(loginUser.getId(), friendAddRequest.getReceiveId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加失败");
        }
        // 1.添加的不能是自己
        if (Objects.equals(loginUser.getId(), friendAddRequest.getReceiveId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能添加自己为好友");
        }
        RLock lock = redissonClient.getLock(APPLY_LOCK + loginUser.getId());
        try {
            // 抢到锁并执行
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                // 2.条数大于等于1 就不能再添加
                LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
                friendsLambdaQueryWrapper.eq(Friends::getReceiveId, friendAddRequest.getReceiveId());
                friendsLambdaQueryWrapper.eq(Friends::getFromId, loginUser.getId());
                List<Friends> list = this.list(friendsLambdaQueryWrapper);
                list.forEach(friends -> {
                    if (list.size() > 1 && friends.getStatus() == DEFAULT_STATUS) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复申请");
                    }
                });
                Friends newFriend = new Friends();
                newFriend.setFromId(loginUser.getId());
                newFriend.setReceiveId(friendAddRequest.getReceiveId());
                if (StringUtils.isBlank(friendAddRequest.getRemark())) {
                    newFriend.setRemark("我是" + userDao.getById(loginUser.getId()).getUsername());
                } else {
                    newFriend.setRemark(friendAddRequest.getRemark());
                }
                newFriend.setCreateTime(new Date());
                return this.save(newFriend);
            }
        } catch (InterruptedException e) {
            log.error("joinTeam error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
        return false;
    }

    @Override
    public List<FriendsRecordVO> obtainFriendApplicationRecords(User loginUser) {
        LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        friendsLambdaQueryWrapper.eq(Friends::getReceiveId, loginUser.getId());
        return toFriends(friendsLambdaQueryWrapper);
    }

    private List<FriendsRecordVO> toFriends(LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper) {
        List<Friends> friendsList = this.list(friendsLambdaQueryWrapper);
        Collections.reverse(friendsList);
        return friendsList.stream().map(friends -> {
            FriendsRecordVO friendsRecordVO = new FriendsRecordVO();
            BeanUtils.copyProperties(friends, friendsRecordVO);
            User user = userDao.getById(friends.getFromId());
            friendsRecordVO.setApplyUser(user);
            return friendsRecordVO;
        }).collect(Collectors.toList());
    }

    @Override
    public int getRecordCount(User loginUser) {
        LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        friendsLambdaQueryWrapper.eq(Friends::getReceiveId, loginUser.getId());
        List<Friends> friendsList = this.list(friendsLambdaQueryWrapper);
        int count = 0;
        for (Friends friends : friendsList) {
            if (friends.getStatus() == DEFAULT_STATUS && friends.getIsRead() == NOT_READ) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<FriendsRecordVO> getMyRecords(User loginUser) {
        LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        friendsLambdaQueryWrapper.eq(Friends::getFromId, loginUser.getId());
        List<Friends> friendsList = this.list(friendsLambdaQueryWrapper);
        Collections.reverse(friendsList);
        return friendsList.stream().map(friends -> {
            FriendsRecordVO friendsRecordVO = new FriendsRecordVO();
            BeanUtils.copyProperties(friends, friendsRecordVO);
            User user = userDao.getById(friends.getReceiveId());
            friendsRecordVO.setApplyUser(user);
            return friendsRecordVO;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean agreeToApply(User loginUser, Long fromId) {
        LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        friendsLambdaQueryWrapper.eq(Friends::getReceiveId, loginUser.getId());
        friendsLambdaQueryWrapper.eq(Friends::getFromId, fromId);
        List<Friends> recourdCount = this.list(friendsLambdaQueryWrapper);
        List<Friends> collect = recourdCount.stream().filter(f -> f.getStatus() == DEFAULT_STATUS).collect(Collectors.toList());
        // 条数小于1 就不能再同意
        if (collect.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该申请不存在");
        }
        if (collect.size() > 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "操作有误,请重试");
        }
        AtomicBoolean flag = new AtomicBoolean(false);
        collect.forEach(friend -> {
            if (DateUtil.between(new Date(), friend.getCreateTime(), DateUnit.DAY) >= 3 || friend.getStatus() == EXPIRED_STATUS) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该申请已过期");
            }
            // 1. 分别查询receiveId和fromId的用户，更改userIds中的数据
            User receiveUser = userDao.getById(loginUser.getId());
            User fromUser = userDao.getById(fromId);
            Set<Long> receiveUserIds = stringJsonListToLongSet(receiveUser.getFriendIds());
            Set<Long> fromUserUserIds = stringJsonListToLongSet(fromUser.getFriendIds());

            fromUserUserIds.add(receiveUser.getId());
            receiveUserIds.add(fromUser.getId());

            Gson gson = new Gson();
            String jsonFromUserUserIds = gson.toJson(fromUserUserIds);
            String jsonReceiveUserIds = gson.toJson(receiveUserIds);
            receiveUser.setFriendIds(jsonReceiveUserIds);
            fromUser.setFriendIds(jsonFromUserUserIds);
            // 2. 修改状态由0改为1
            friend.setStatus(AGREE_STATUS);
            flag.set(userDao.updateById(fromUser) && userDao.updateById(receiveUser) && this.updateById(friend));
        });
        return flag.get();
    }

    @Override
    public boolean toRead(User loginUser, Set<Long> ids) {

        boolean flag = false;
        for (Long id : ids) {
            Friends friends = this.getById(id);
            if (friends.getStatus() == DEFAULT_STATUS && friends.getIsRead() == NOT_READ) {
                friends.setIsRead(READ);
                flag = this.updateById(friends);
            }
        }
        return flag;
    }
}
