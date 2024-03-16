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
 * 好友申请管理表
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("friends")
public class Friends implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 好友申请id
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发送申请的用户id
     */
    @TableField("from_id")
    private Long fromId;

    /**
     * 接收申请的用户id 
     */
    @TableField("receive_id")
    private Long receiveId;

    /**
     * 是否已读(0-未读 1-已读)
     */
    @TableField("is_read")
    private Integer isRead;

    /**
     * 申请状态 默认0 （0-未通过 1-已同意 2-已过期 3-已撤销）
     */
    @TableField("status")
    private Integer status;

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

    /**
     * 好友申请备注信息
     */
    @TableField("remark")
    private String remark;


}
