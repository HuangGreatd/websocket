package com.juzipi.dao;

import com.juzipi.domain.entity.Blog;
import com.juzipi.mapper.BlogMapper;
import com.juzipi.service.BlogService;
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
public class BlogDao extends ServiceImpl<BlogMapper, Blog> {

}
