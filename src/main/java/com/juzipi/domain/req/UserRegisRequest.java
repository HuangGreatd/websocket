package com.juzipi.domain.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/1/31 14:09
 */
@Data
@ApiModel(value = "用户注册请求")
public class UserRegisRequest implements Serializable {
    /**
     * 串行版本uid
     */
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 电话
     */
    @ApiModelProperty(value = "手机号")
    private String phone;

    /**
     * 代码
     */
//    @ApiModelProperty(value = "验证码")
//    private String code;

    /**
     * 用户帐户
     */
    @ApiModelProperty(value = "用户账号")
    private String userAccount;

    /**
     * 用户密码
     */
    @ApiModelProperty(value = "用户密码")
    private String userPassword;

    /**
     * 检查密码
     */
    @ApiModelProperty(value = "校验密码")
    private String checkPassword;



}
