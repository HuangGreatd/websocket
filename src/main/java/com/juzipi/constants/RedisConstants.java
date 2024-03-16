package com.juzipi.constants;

/**
 * Redis常量
 *
 * @Author : Juzipi
 * @create 2024/1/31 16:44
 */
public interface RedisConstants {
    /**
     * 注册验证码键
     */
    String REGISTER_CODE_KEY = "yuchuang:register:";

    Long LOGIN_USER_TTL = 15L;

    String LOGIN_USER_KEY = "yuchuang:login:token:";

    /**
     * 用户忘记密码键
     */
    String USER_FORGET_PASSWORD_KEY = "yuchuang:user:forget:";

    /**
     * 新博文消息键
     */
    String MESSAGE_BLOG_NUM_KEY = "yuchuang:message:blog:num:";

    /**
     * 博客推送键
     */
    String BLOG_FEED_KEY = "yuchuang:feed:blog:";

    /**
     * 新点赞消息键
     */
    String MESSAGE_LIKE_NUM_KEY = "yuchuang:message:like:num:";


    /**
     * 用户推荐缓存
     */
    String USER_RECOMMEND_KEY = "yuchuang:recommend:";
}
