package com.juzipi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 队伍
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("team")
public class Team implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 队伍名称
     */
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 封面图片
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 最大人数
     */
    @TableField("max_num")
    private Integer maxNum;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private Date expireTime;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    @TableField("status")
    private Integer status;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField("is_delete")
    private Integer isDelete;


}
