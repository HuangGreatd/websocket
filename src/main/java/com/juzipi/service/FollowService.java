package com.juzipi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.domain.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.juzipi.domain.vo.UserVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-16
 */
public interface FollowService  {

    List<UserVO> listFans(Long userId);

    void followUser(Long followUserId, Long userId);

    Page<UserVO> pageMyFollow(Long userId,String currentPage);

    Page<UserVO> pageFans(Long userId, String currentPage);
}
