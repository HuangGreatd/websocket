package com.juzipi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.juzipi.domain.entity.CommentLike;
import com.juzipi.mapper.CommentLikeMapper;
import com.juzipi.service.CommentLikeService;
import org.springframework.stereotype.Service;

@Service
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike> implements CommentLikeService {
}
