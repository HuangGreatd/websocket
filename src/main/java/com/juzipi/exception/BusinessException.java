package com.juzipi.exception;

import com.juzipi.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * @Author : Juzipi
 * @create 2024/1/31 14:12
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     * -- GETTER --
     *  获取代码
     *
     * @return int

     */
    private final int code;

    /**
     * 描述
     * -- GETTER --
     *  得到描述
     *
     * @return {@link String}

     */
    private final  String description;

    /**
     * 业务异常
     *
     * @param message     消息
     * @param code        代码
     * @param description 描述
     */
    public BusinessException(String message,int code,String description){
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    /**
     * 业务异常
     *
     * @param errorCode   错误代码
     * @param description 描述
     */
    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

}
