package com.juzipi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@TableName(value = "blog")
@ApiModel(value = "博文")
public class Blog implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 用户id
     */
    @ApiModelProperty(value = "用户id")
    private Long userId;

    /**
     * 标题
     */
    @ApiModelProperty(value = "标题")
    private String title;

    /**
     * 图片，最多9张，多张以","隔开
     */
    @ApiModelProperty(value = "图片")
    private String images;

    /**
     * 文章
     */
    @ApiModelProperty(value = "正文")
    private String content;

    /**
     * 点赞数量
     */
    @ApiModelProperty(value = "点赞数量")
    private Integer likedNum;

    /**
     * 评论数量
     */
    @ApiModelProperty(value = "评论数量")
    private Integer commentsNum;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
