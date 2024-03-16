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
 * 聊天消息表
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("chat")
public class Chat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 聊天记录id
     */
      @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发送消息id
     */
    @TableField("from_id")
    private Long fromId;

    /**
     * 接收消息id
     */
    @TableField("to_id")
    private Long toId;

    @TableField("text")
    private String text;

    /**
     * 聊天类型 1-私聊 2-群聊
     */
    @TableField("chat_type")
    private Integer chatType;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("team_id")
    private Long teamId;

    @TableField("is_delete")
    private Integer isDelete;


}
