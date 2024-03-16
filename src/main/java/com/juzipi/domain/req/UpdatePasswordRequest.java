package com.juzipi.domain.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/2/2 10:34
 */
@Data
@ApiModel(value = "密码更新请求")
public class UpdatePasswordRequest {
    /**
     * 电话
     */
    @ApiModelProperty(value = "电话")
    private String phone;
    /**
     * 验证码
     */
//    @ApiModelProperty(value = "验证码")
//    private String code;
    /**
     * 密码
     */
    @ApiModelProperty(value = "密码")
    private String password;
    /**
     * 确认密码
     */
    @ApiModelProperty(value = "确认密码")
    private String confirmPassword;
}
