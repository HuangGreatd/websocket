package com.juzipi.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * @author Juzipi
 * @version 1.0
 * @date 2024-03-16 19:31
 */
@Data
public class SeriesData {
    private String name;
    private String type = "bar";
    private List<Integer> data;
}
