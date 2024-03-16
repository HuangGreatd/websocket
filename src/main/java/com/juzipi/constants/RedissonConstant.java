package com.juzipi.constants;

/**
 * redisson常量
 */
public interface RedissonConstant {
    /**
     * 应用锁
     */
    String APPLY_LOCK = "yuchuang:apply:lock:";
    String DISBAND_EXPIRED_TEAM_LOCK = "yuchuang:disbandTeam:lock";
    String USER_RECOMMEND_LOCK = "yuchuang:user:recommend:lock";
    String BLOG_LIKE_LOCK = "yuchuang:blog:like:lock:";
    String COMMENTS_LIKE_LOCK= "yuchuang:comments:like:lock:";


}
