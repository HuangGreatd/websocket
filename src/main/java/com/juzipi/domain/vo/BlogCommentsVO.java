package com.juzipi.domain.vo;

import com.juzipi.domain.entity.BlogComments;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "博文评论返回")
public class BlogCommentsVO extends BlogComments implements Serializable {
    /**
     * 串行版本uid
     */
    private static final long serialVersionUID = 5695588849785352130L;
    /**
     * 用户评论
     */
    @ApiModelProperty(value = "评论用户")
    private UserVO commentUser;
    /**
     * 是喜欢
     */
    @ApiModelProperty(value = "是否点赞")
    private Boolean isLiked;
    /**
     * 博客
     */
    @ApiModelProperty(value = "博客")
    private BlogVO blog;

}
