package com.juzipi.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : Juzipi
 * @create 2024/1/31 16:19
 */
@Getter
@AllArgsConstructor
public enum GenderEnum {
    MAN(1L, "帅哥"),
    WOMAN(2L, "美女");

    private final Long code;
    private final String message;


}
