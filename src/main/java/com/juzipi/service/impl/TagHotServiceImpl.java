package com.juzipi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.juzipi.common.ErrorCode;
import com.juzipi.domain.entity.TagHot;
import com.juzipi.domain.req.WatchHotTagsRequest;
import com.juzipi.domain.vo.ChartData;
import com.juzipi.exception.BusinessException;
import com.juzipi.mapper.TagHotMapper;
import com.juzipi.service.TagHotService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Juzipi
 * @version 1.0
 * @date 2024-03-16 17:22
 */
@Service
public class TagHotServiceImpl extends ServiceImpl<TagHotMapper, TagHot> implements TagHotService {

    @Resource
    private TagHotMapper tagHotMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrUpdateTags(List<String> tagNameList) {
        for (String tagName : tagNameList) {

            TagHot tagHot = tagHotMapper.getTagByName(tagName);

            if (tagHot != null) {
//                tagHotMapper.update
                System.out.println("tagId = " + tagHot.getId());

                //获取当前的版本号
                Integer versionId = tagHot.getVersion();

                boolean result = tagHotMapper.updateTagCount(tagHot.getId(), versionId);
                //是否更新了一行数据，如果版本号发生冲突，这里 result 将为 false
                if (!result) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "系统出现错误啦!");
                }
            } else {
                TagHot newTagHot = new TagHot();
                tagHot.setTagName(tagName);
                // 新纪录的初始版本号设置为 1
                tagHot.setVersion(1);
                this.save(newTagHot);
            }
        }
    }

    @Override
    public ChartData watchHotTags(WatchHotTagsRequest watchHotTagsRequest) {
        ChartData chartData = new ChartData();
        if (watchHotTagsRequest.getTime() != null){
            List<String> tagNames = tagHotMapper.selectByTagNames();
            chartData.setYAxisData(tagNames);
        }
        return null;
    }
}
