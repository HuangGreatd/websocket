package com.juzipi.domain.vo;

import lombok.Data;

import java.util.List;

/** echarts 展示数据
 * @author Juzipi
 * @version 1.0
 * @date 2024-03-16 19:29
 */
@Data
public class ChartData {
    private List<String> yAxisData;
    private List<SeriesData> seriesData;

}
