package com.juzipi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.BlogAddRequest;
import com.juzipi.domain.req.BlogUpdateRequest;
import com.juzipi.domain.vo.BlogVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-16
 */
public interface BlogService {

    Page<BlogVO> pageBlog(long currentPage, String title, Long id);

    Long addBlog(BlogAddRequest blogAddRequest, User loginUser);

    Page<BlogVO> listMyBlogPages(long currentPage, Long id);

    void likeBlog(long id, Long id1);

    BlogVO getBlogById(Long id, Long id1);

    void deleteBlogById(Long blogId, Long userId, boolean isAdmin);

    void updateBlog(BlogUpdateRequest blogUpdateRequest, Long userId, boolean admin);
}
