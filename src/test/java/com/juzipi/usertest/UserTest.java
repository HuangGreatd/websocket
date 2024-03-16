package com.juzipi.usertest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class UserTest {

    @Resource
    private UserService userService;
    @Test
    public void testMatch(){
        User user = new User();
        user.setId(1l);
        user.setUsername("juzipi");
        user.setUserAccount("juzipi");
        user.setPassword("1234567");
        user.setAvatarUrl("http://niu.ochiamalu.xyz/75e31415779979ae40c4c0238aa4c34.jpg");

        Page<UserVO> userVOPage = userService.matchUsers(1l, user);

    }
}
