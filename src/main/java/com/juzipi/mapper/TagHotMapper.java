package com.juzipi.mapper;

import com.juzipi.domain.entity.TagHot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 标签热搜表 Mapper 接口
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-03-16
 */
public interface TagHotMapper extends BaseMapper<TagHot> {

    int isHasTag(String tag);

    boolean updateTagCount(@Param("tagId") Long tagId, @Param("version") Integer version);

    TagHot getTagByName(String tag);

    List<String> selectByTagNames();

    List<Integer> selectCountOrderByDesc();

    List<TagHot> selectTagNameAndCountByCreateTime(String createTime);
}
