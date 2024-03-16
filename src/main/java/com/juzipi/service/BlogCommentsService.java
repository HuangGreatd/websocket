package com.juzipi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.domain.entity.BlogComments;
import com.baomidou.mybatisplus.extension.service.IService;
import com.juzipi.domain.req.AddCommentRequest;
import com.juzipi.domain.vo.BlogCommentsVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-16
 */
public interface BlogCommentsService  {

    BlogCommentsVO getComment(long l, Long userId);

    void addComment(AddCommentRequest addCommentRequest, Long id);

    List<BlogCommentsVO> listComments(long blogId, Long id);

    void likeComment(Long blogId, Long userId);

    void deleteComment(Long blogCommentId, Long userId, boolean isAdmin);

    Page<BlogCommentsVO> pageMyComments(Long userId, Long currentPage);
}
