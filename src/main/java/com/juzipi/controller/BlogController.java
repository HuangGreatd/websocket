package com.juzipi.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.juzipi.common.BaseResponse;
import com.juzipi.common.ErrorCode;
import com.juzipi.common.ResultUtis;
import com.juzipi.domain.entity.User;
import com.juzipi.domain.req.BlogAddRequest;
import com.juzipi.domain.req.BlogUpdateRequest;
import com.juzipi.domain.vo.BlogVO;
import com.juzipi.exception.BusinessException;
import com.juzipi.service.BlogService;
import com.juzipi.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 博客控制器
 * </p>
 *
 * @author <a href="https://github.com/HuangGreatd">Juzipi</a>
 * @since 2024-02-16
 */
@RestController
@RequestMapping("/blog")
@Api(tags = "博文管理模块")
@Log4j2
public class BlogController {

    /**
     * 博客服务
     */
    @Resource
    private BlogService blogService;

    @Resource
    private UserService userService;

    /**
     * 博客列表页面
     *
     * @param currentPage 当前页面
     * @param request     请求
     * @return {@link BaseResponse}<{@link Page}<{@link BlogVO}>>
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取博文")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "currentPage", value = "当前页"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Page<BlogVO>> listBlogPage(long currentPage, String title, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResultUtis.success(blogService.pageBlog(currentPage, title, null));
        } else {
            return ResultUtis.success(blogService.pageBlog(currentPage, title, loginUser.getId()));
        }
    }

    @PostMapping("/add")
    @ApiOperation(value = "添加博文")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "blogAddRequest", value = "博文添加请求"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<String> addBlog( BlogAddRequest blogAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (StringUtils.isAnyBlank(blogAddRequest.getTitle(), blogAddRequest.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        blogService.addBlog(blogAddRequest, loginUser);
        return ResultUtis.success("添加成功");
    }

    /**
     * 我博客列表
     *
     * @param currentPage 当前页面
     * @param request     请求
     * @return {@link BaseResponse}<{@link Page}<{@link BlogVO}>>
     */
    @GetMapping("/list/my/blog")
    @ApiOperation(value = "获取我写的博文")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "currentPage", value = "当前页"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<Page<BlogVO>> listMyBlogPage(long currentPage, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<BlogVO> blogPage = blogService.listMyBlogPages(currentPage, loginUser.getId());
        return ResultUtis.success(blogPage);
    }

    /**
     * 点赞博客
     *
     * @param id      id
     * @param request 请求
     * @return {@link BaseResponse}<{@link String}>
     */
    @PutMapping("/like/{id}")
    @ApiOperation(value = "点赞博文")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "id", value = "博文id"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<String> likeBlog(@PathVariable long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        blogService.likeBlog(id, loginUser.getId());
        return ResultUtis.success("点赞成功");
    }

    /**
     * 通过id获取博客
     *
     * @param id      id
     * @param request 请求
     * @return {@link BaseResponse}<{@link BlogVO}>
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "根据id获取博文")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "id", value = "博文id"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<BlogVO> getBlogById(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtis.success(blogService.getBlogById(id, loginUser.getId()));
    }

    /**
     * 删除博客通过id
     *
     * @param id      id
     * @param request 请求
     * @return {@link BaseResponse}<{@link String}>
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "根据id删除博文")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "id", value = "博文id"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<String> deleteBlogById(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean admin = userService.isAdmin(loginUser);
        blogService.deleteBlogById(id, loginUser.getId(), admin);
        return ResultUtis.success("删除成功");
    }

    /**
     * 更新博客
     *
     * @param blogUpdateRequest 博客更新请求
     * @param request           请求
     * @return {@link BaseResponse}<{@link String}>
     */
    @PutMapping("/update")
    @ApiOperation(value = "更新博文")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "blogUpdateRequest", value = "博文更新请求"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public BaseResponse<String> updateBlog(BlogUpdateRequest blogUpdateRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean admin = userService.isAdmin(loginUser);
        blogService.updateBlog(blogUpdateRequest,loginUser.getId(),admin);
        return ResultUtis.success("更新成功");
    }

}

