package com.juzipi.domain.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/2/8 10:57
 */
@Data
@ApiModel(value = "退出队伍请求")
public class TeamQuitRequest  implements Serializable {
    private static final long serialVersionUID = 1473299551300760408L;
    /**
     * id
     */
    @ApiModelProperty(value = "队伍id")
    private Long teamId;

}
