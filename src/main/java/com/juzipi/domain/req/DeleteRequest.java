package com.juzipi.domain.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/2/8 11:14
 */
@Data
@ApiModel(value = "删除请求")
public class DeleteRequest  implements Serializable {
    /**
     * 串行版本uid
     */
    private static final long serialVersionUID = -7428525903309954640L;

    /**
     * id
     */
    @ApiModelProperty(value = "id")
    private long id;
}
