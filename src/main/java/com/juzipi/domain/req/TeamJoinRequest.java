package com.juzipi.domain.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 团队加入请求
 *
 * @Author : Juzipi
 * @create 2024/2/6 20:04
 */
@Data
@ApiModel(value = "加入队伍请求")
public class TeamJoinRequest implements Serializable {
    private static final long serialVersionUID = -3755024144750907374L;
    /**
     * id
     */
    @ApiModelProperty(value = "队伍id", required = true)
    private Long teamId;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码")
    private String password;
}
