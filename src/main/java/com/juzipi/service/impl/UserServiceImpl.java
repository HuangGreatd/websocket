package com.juzipi.service.impl;

import java.time.Duration;

import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.juzipi.common.ErrorCode;
import com.juzipi.constants.UserConstants;
import com.juzipi.dao.FollowDao;
import com.juzipi.dao.UserDao;
import com.juzipi.domain.entity.Follow;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.enums.GenderEnum;
import com.juzipi.domain.req.UserRegisRequest;
import com.juzipi.domain.req.UserUpdateRequest;
import com.juzipi.domain.vo.UserVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.mapper.UserMapper;
import com.juzipi.service.TagHotService;
import com.juzipi.service.UserService;
import com.juzipi.utils.AlgorithmUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static com.juzipi.constants.PageConstants.DEFAULT_CACHE_PAGE;
import static com.juzipi.constants.PageConstants.PAGE_SIZE;
import static com.juzipi.constants.RedisConstants.*;
import static com.juzipi.constants.UserConstants.USER_LOGIN_STATE;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/1/31 14:01
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private FollowDao followService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private FollowDao followDao;

    @Resource
    private TagHotService tagHotService;

    private static final String[] avatarUrls = {
            "http://niu.ochiamalu.xyz/12d4949b4009d089eaf071aef0f1f40.jpg",
            "http://niu.ochiamalu.xyz/1bff61de34bdc7bf40c6278b2848fbcf.jpg",
            "http://niu.ochiamalu.xyz/22fe8428428c93a565e181782e97654.jpg",
            "http://niu.ochiamalu.xyz/75e31415779979ae40c4c0238aa4c34.jpg",
            "http://niu.ochiamalu.xyz/905731909dfdafd0b53b3c4117438d3.jpg",
            "http://niu.ochiamalu.xyz/a84b1306e46061c0d664e6067417e5b.jpg",
            "http://niu.ochiamalu.xyz/b93d640cc856cb7035a851029aec190.jpg",
            "http://niu.ochiamalu.xyz/c11ae3862b3ca45b0a6cdff1e1bf841.jpg",
            "http://niu.ochiamalu.xyz/cccfb0995f5d103414bd8a8bd742c34.jpg",
            "http://niu.ochiamalu.xyz/f870176b1a628623fa7fe9918b862d7.jpg"
    };

    @Resource
    private UserDao userDao;

    @Resource
    private UserMapper userMapper;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "juzipi";

    /**
     * 用户注册
     *
     * @param userRegisRequest
     * @param request
     * @return
     */
    @Override
    public String userRegister(UserRegisRequest userRegisRequest, HttpServletRequest request) {
        String phone = userRegisRequest.getPhone();
        String userAccount = userRegisRequest.getUserAccount();
        String userPassword = userRegisRequest.getUserPassword();
        String checkPassword = userRegisRequest.getCheckPassword();
        checkRegisterRequest(phone, userAccount, userPassword, checkPassword);
        checkAccountValid(userAccount);
        checkHasRegistered(phone);
        //todo 验证码
        String key = REGISTER_CODE_KEY + phone;
        checkPassword(checkPassword, userPassword);
        long userId = saveUser(phone, userAccount, userPassword);
        return afterInsertUser(key, userId, request);
    }

    @Override
    public String userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        //账号不能包含特殊字符
        checkAccountValid(userAccount);
        //2.加密
        String encryptPassword = Md5Password(userPassword);
        //查询用户是否存在
        User userIsHas = userDao.findUserIsHas(userAccount);
        if (userIsHas == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (!userIsHas.getPassword().equals(encryptPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        //3.用户脱敏
        User safetyUser = getSafetyUser(userIsHas);
        //4.记录用户的登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        request.getSession().setMaxInactiveInterval(900);
        String token = UUID.randomUUID().toString();
        Gson gson = new Gson();
        String userStr = gson.toJson(safetyUser);
        stringRedisTemplate.opsForValue().set(LOGIN_USER_KEY + token, userStr);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, Duration.ofMillis(15));
        return token;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getRole() == UserConstants.ADMIN_ROLE;
    }

    @Override
    public void updatePassword(String phone, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        //todo 验证码
//        String key = USER_FORGET_PASSWORD_KEY + phone;
//
//        String correctCode = stringRedisTemplate.opsForValue().get(key);
//        if (StringUtils.isEmpty(correctCode)){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先获取验证码");
//        }
//        if ()
        String encryptPassword = Md5Password(password);
        boolean result = userDao.updateUserPassword(phone, encryptPassword);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改密码错误");
        }
    }

    private String Md5Password(String password) {
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    private String afterInsertUser(String key, long userId, HttpServletRequest request) {
        stringRedisTemplate.delete(key);
        User userInDatabase = userDao.getById(userId);
        User safeUser = this.getSafetyUser(userInDatabase);
        String token = UUID.randomUUID().toString();
        Gson gson = new Gson();
        String userStr = gson.toJson(safeUser);
        request.getSession().setAttribute(USER_LOGIN_STATE, userStr);
        request.getSession().setMaxInactiveInterval(900);
        stringRedisTemplate.opsForValue().set(LOGIN_USER_KEY + token, userStr);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, Duration.ofMillis(15));
        return token;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setStatus(originUser.getStatus());
        safetyUser.setRole(originUser.getRole());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        //先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (ObjectUtils.isEmpty(userObj)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User currentUser = new User();
        BeanUtils.copyProperties(userObj, currentUser);
        if (ObjectUtils.isEmpty(currentUser) || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //从数据库查询(追求性能可以注释，直接走缓存)
        Long userId = currentUser.getId();
        currentUser = userDao.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return currentUser;
    }

    @Override
    public void updateUser(UserUpdateRequest updateRequest, HttpServletRequest request) {
        //获取当前登录用户
        User loginUser = this.getLoginUser(request);
        if (ObjectUtils.isEmpty(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, loginUser.getId());
        String username = updateRequest.getUsername();
        String profile = updateRequest.getProfile();
        Integer gender = updateRequest.getGender();
        String phone = updateRequest.getPhone();
        String email = updateRequest.getEmail();
        String tags = updateRequest.getTags();
        if (StringUtils.isNotEmpty(username)) {
            updateWrapper.set(User::getUsername, username);
        }
        if (StringUtils.isNotEmpty(profile)) {
            updateWrapper.set(User::getProfile, profile);
        }
        if (ObjectUtils.isNotEmpty(gender)) {
            updateWrapper.set(User::getGender, gender);
        }
        if (StringUtils.isNotEmpty(phone)) {
            updateWrapper.set(User::getPhone, phone);
        }
        if (StringUtils.isNotEmpty(email)) {
            updateWrapper.set(User::getEmail, email);
        }
        if (StringUtils.isNotEmpty(tags)) {
            updateWrapper.set(User::getTags, tags);
        }
        boolean update = userDao.update(updateWrapper);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败，请重试！");
        }
    }

    @Override
    public UserVO getUserById(Long userId, Long loginUserId) {
        User user = this.getById(userId);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getUserId, loginUserId).eq(Follow::getFollowUserId, userId);
        long count = followService.count(followLambdaQueryWrapper);
        userVO.setIsFollow(count > 0);
        return userVO;
    }

    @Override
    public Page<User> searchUsersByTags(List<String> tagNameList, long currentPage) {
        Page<User> userPage = userDao.searchUsersByTags(tagNameList, currentPage);
        tagHotService.createOrUpdateTags(tagNameList);
        return userPage;
    }

    @Override
    public void updateTags(List<String> tags, Long id) {
        User user = new User();
        Gson gson = new Gson();
        String json = gson.toJson(tags);
        user.setId(id);
        user.setTags(json);
        userDao.updateById(user);

    }

    @Override
    public Page<UserVO> userPage(long currentPage) {
        Page<User> page = userDao.page(new Page<>(currentPage, PAGE_SIZE));
        Page<UserVO> userVOPage = new Page<>();
        BeanUtils.copyProperties(page, userVOPage);
        return userVOPage;
    }

    @Override
    public Page<UserVO> preMatchUsers(long currentPage, String username, User loginUser) {
        Gson gson = new Gson();
        if (loginUser != null) {
            String key = USER_RECOMMEND_KEY + loginUser.getId() + ":" + currentPage;
            if (StringUtils.isNotBlank(username)) {
                LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
                userLambdaQueryWrapper.like(User::getUsername, username);
                Page<User> userPage = this.page(new Page<>(currentPage, PAGE_SIZE), userLambdaQueryWrapper);
                Page<UserVO> userVOPage = new Page<>();
                BeanUtils.copyProperties(userPage, userVOPage);
                List<UserVO> userVOList = userPage.getRecords().stream().map((use) -> this.getUserById(use.getId(), loginUser.getId())).collect(Collectors.toList());
                userVOPage.setRecords(userVOList);
                return userVOPage;
            }
            if (currentPage < DEFAULT_CACHE_PAGE) {
                Boolean hasKey = stringRedisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(hasKey)) {
                    String userVOPageStr = stringRedisTemplate.opsForValue().get(key);
                    return gson.fromJson(userVOPageStr, new TypeToken<Page<UserVO>>() {
                    }.getType());

                } else {
                    Page<UserVO> userVOPage = this.matchUsers(currentPage, loginUser);
                    String userVOPageStr = gson.toJson(userVOPage);
                    stringRedisTemplate.opsForValue().set(key, userVOPageStr);
                    return userVOPage;
                }
            } else {
                Page<UserVO> userVOPage = this.matchUsers(currentPage, loginUser);
                String userVOPageStr = gson.toJson(userVOPage);
                stringRedisTemplate.opsForValue().set(key, userVOPageStr);
                return userVOPage;
            }
        } else {
            if (StringUtils.isNotBlank(username)) {
                throw new BusinessException(ErrorCode.NOT_LOGIN);
            }
            long userNum = this.count();
            if (userNum <= 10) {
                Page<User> userPage = this.page(new Page<>(currentPage, PAGE_SIZE));
                List<UserVO> userVOList = userPage.getRecords().stream().map((user) -> {
                    UserVO userVO = new UserVO();
                    BeanUtils.copyProperties(user, userVO);
                    return userVO;
                }).collect(Collectors.toList());
                Page<UserVO> userVOPage = new Page<>();
                userVOPage.setRecords(userVOList);
                return userVOPage;
            }
            return this.getRandomUser();
        }
    }

    @Override
    public Page<UserVO> matchUsers(long currentPage, User loginUser) {
        String tags = loginUser.getTags();
        if (StringUtils.isEmpty(tags)) {
            return this.userPage(currentPage);
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(User::getId, User::getTags);
        //todo 看看是什么内容
        List<User> userList = this.list(queryWrapper);
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        //用户列表的下标 =》 相似度
        ArrayList<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            //无标签或者为当前用户自己
            if (StringUtils.isBlank(userTags) || Objects.equals(user.getId(), loginUser.getId())) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算相似度
            long distance = AlgorithmUtil.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        //按相似度由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream().sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .collect(Collectors.toList());
        //截取currentPage所需要的List
        ArrayList<Pair<User, Long>> finalUserPairList = new ArrayList<>();
        int begin = (int) ((currentPage - 1) * PAGE_SIZE);
        int end = (int) (((currentPage - 1) * PAGE_SIZE) + PAGE_SIZE) - 1;

        if (topUserPairList.size() < end) {
            //剩余数量
            int temp = (int) (topUserPairList.size() - begin);
            if (temp <= 0) {
                return new Page<>();
            }
            for (int i = 0; i <= begin + temp - 1; i++) {
                finalUserPairList.add(topUserPairList.get(i));
            }
        } else {
            for (int i = begin; i < end; i++) {
                finalUserPairList.add(topUserPairList.get(i));
            }
        }
        //获取排列后的UserId
        List<Long> userIdList = finalUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        String idStr = StringUtils.join(userIdList, "");
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.in(User::getId, userIdList).last("ORDER BY FIELD(id," + idStr + ")");
        List<UserVO> userVOList = this.list(userLambdaQueryWrapper)
                .stream().map(user -> {
                    UserVO userVO = new UserVO();
                    BeanUtils.copyProperties(user, userVO);
                    LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    followLambdaQueryWrapper.eq(Follow::getUserId, loginUser.getId())
                            .eq(Follow::getFollowUserId, userVO.getId());
                    int count = followDao.count(followLambdaQueryWrapper);
                    userVO.setIsFollow(count > 0);
                    return userVO;
                }).collect(Collectors.toList());
        Page<UserVO> userVOPage = new Page<>();
        userVOPage.setRecords(userVOList);
        userVOPage.setCurrent(currentPage);
        userVOPage.setSize(userVOList.size());
        return userVOPage;
    }

    @Override
    public Page<UserVO> getRandomUser() {
        List<User> randomUser = userMapper.getRandomUser();
        List<UserVO> userVOList = randomUser.stream().map((item) -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(item, userVO);
            return userVO;
        }).collect(Collectors.toList());
        Page<UserVO> userVOPage = new Page<>();
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

    private long saveUser(String phone, String userAccount, String userPassword) {
        //加密
        String encryptPassword = Md5Password(userPassword);
        //插入数据
        User user = new User();
        Random random = new Random();
        user.setAvatarUrl(avatarUrls[random.nextInt(avatarUrls.length)]);
        user.setPhone(phone);
        user.setGender(GenderEnum.MAN.getCode());
        user.setUsername(userAccount);
        user.setUserAccount(userAccount);
        user.setPassword(encryptPassword);
        ArrayList<String> tag = new ArrayList<>();
        Gson gson = new Gson();
        String jsonTag = gson.toJson(tag);
        user.setTags(jsonTag);
        boolean saveResult = userDao.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return user.getId();


    }

    private void checkPassword(String checkPassword, String userPassword) {
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不一致");
        }
    }

    private void checkHasRegistered(String phone) {
        User user = userDao.checkHasRegistered(phone);
        if (user != null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该手机号已注册");
        }
    }

    /**
     * 检验账号是否有特殊字符
     *
     * @param userAccount
     */
    private void checkAccountValid(String userAccount) {
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名包含特殊字符");
        }
    }

    /**
     * 校验参数信息
     *
     * @param phone
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     */
    private void checkRegisterRequest(String phone, String userAccount, String userPassword, String checkPassword) {
        if (StringUtils.isAnyBlank(phone, userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "信息不全");
        }
        if (StringUtils.isAnyBlank(phone, userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
    }
}
