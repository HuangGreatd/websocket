package com.juzipi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 标签热搜表
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-03-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tag_hot")
public class TagHot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    @TableField("tag_name")
    private String tagName;

    /**
     * 搜索次数
     */
    @TableField("count")
    private Integer count;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;


    @TableField("create_time")
    private Date createTime;


}
