package com.juzipi.dao;

import com.juzipi.domain.entity.BlogComments;
import com.juzipi.mapper.BlogCommentsMapper;
import com.juzipi.service.BlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-16
 */
@Service
public class BlogCommentsDao extends ServiceImpl<BlogCommentsMapper, BlogComments>  {

}
