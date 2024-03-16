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
 * @since 2024-02-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型-1 点赞
     */
    @TableField("type")
    private Integer type;

    /**
     * 消息发送的用户id
     */
    @TableField("from_id")
    private Long fromId;

    /**
     * 消息接收的用户id
     */
    @TableField("to_id")
    private Long toId;

    /**
     * 消息内容
     */
    @TableField("data")
    private String data;

    /**
     * 已读-0 未读 ,1 已读
     */
    @TableField("is_read")
    private Integer isRead;

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

    /**
     * 逻辑删除
     */
    @TableField("is_delete")
    private Integer isDelete;


}
