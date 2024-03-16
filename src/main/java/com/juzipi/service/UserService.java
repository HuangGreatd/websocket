package com.juzipi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.domain.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.juzipi.domain.req.UserRegisRequest;
import com.juzipi.domain.req.UserUpdateRequest;
import com.juzipi.domain.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-01-31
 */
public interface UserService {

    String userRegister(UserRegisRequest userRegisRequest, HttpServletRequest request);

    String userLogin(String userAccount, String userPassword, HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    boolean isAdmin(User loginUser);

    void updatePassword(String phone, String password, String confirmPassword);
    
    User getSafetyUser(User originUser);

    User getLoginUser(HttpServletRequest request);

    void updateUser(UserUpdateRequest updateRequest, HttpServletRequest request);

    UserVO getUserById(Long id, Long id1);

    Page<User> searchUsersByTags(List<String> tagNameList, long currentPage);

    void updateTags(List<String> tags, Long id);

    Page<UserVO> userPage(long currentPage);

    Page<UserVO> preMatchUsers(long currentPage, String username, User loginUser);

    Page<UserVO> matchUsers(long currentPage,User loginUser);

    Page<UserVO> getRandomUser();
}
