package com.juzipi.service;

import com.juzipi.domain.entity.Friends;
import com.baomidou.mybatisplus.extension.service.IService;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.FriendAddRequest;
import com.juzipi.domain.vo.FriendsRecordVO;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 好友申请管理表 服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-22
 */
public interface FriendsService extends IService<Friends> {

    boolean addFriendRecords(User loginUser, FriendAddRequest friendAddRequest);

    List<FriendsRecordVO> obtainFriendApplicationRecords(User loginUser);

    int getRecordCount(User loginUser);

    List<FriendsRecordVO> getMyRecords(User loginUser);

    boolean agreeToApply(User loginUser, Long fromId);

    boolean toRead(User loginUser, Set<Long> ids);
}
