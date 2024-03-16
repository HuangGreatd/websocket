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
 * 用户队伍关系
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_team")
public class UserTeam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 队伍id
     */
    @TableField("team_id")
    private Long teamId;

    /**
     * 加入时间
     */
    @TableField("join_time")
    private Date joinTime;

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
