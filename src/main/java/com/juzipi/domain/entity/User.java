package com.juzipi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * <p>
 * 
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-01-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
     */
    @TableField("username")
    private String username;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    @TableField("password")
    private String password;

    /**
     * 用户头像
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 性别 0-女 1-男 2-保密
     */
    @TableField("gender")
    private Long gender;

    /**
     * 描述
     */
    @TableField("profile")
    private String profile;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 用户状态，0为正常 1为拉黑
     */
    @TableField("status")
    private Integer status;

    /**
     * ip地址信息
     */
    @TableField("ip_info")
    private String ipInfo;

    /**
     * 用户角色 0-普通用户,1-管理员
     */
    @TableField("role")
    private Integer role;

    /**
     * 标签列表
     */
    @TableField("tags")
    private String tags;

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

    @ApiModelProperty(value = "好友id")
    private String friendIds;

    /**
     * 是否删除
     */
    @TableField("is_delete")
    private Integer isDelete;


}
