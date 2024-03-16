package com.juzipi.service;

import com.juzipi.domain.entity.TagHot;
import com.baomidou.mybatisplus.extension.service.IService;
import com.juzipi.domain.req.WatchHotTagsRequest;
import com.juzipi.domain.vo.ChartData;

import java.util.List;

/**
 * <p>
 * 标签热搜表 服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-03-16
 */
public interface TagHotService extends IService<TagHot> {

    void createOrUpdateTags(List<String> tagNameList);

    ChartData watchHotTags(WatchHotTagsRequest watchHotTagsRequest);
}
