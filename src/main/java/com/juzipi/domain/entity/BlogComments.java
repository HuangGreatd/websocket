package com.juzipi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_comments")
public class BlogComments implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 博文id
     */
    @TableField("blog_id")
    private Long blogId;

    /**
     * 关联的1级评论id，如果是一级评论，则值为0
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 回复的评论id
     */
    @TableField("answer_id")
    private Long answerId;

    /**
     * 回复的内容
     */
    @TableField("content")
    private String content;

    /**
     * 点赞数
     */
    @TableField("liked_num")
    private Integer likedNum;

    /**
     * 状态，0：正常，1：被举报，2：禁止查看
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;


}
