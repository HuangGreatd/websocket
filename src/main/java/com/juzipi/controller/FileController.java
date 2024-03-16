package com.juzipi.controller;

import com.juzipi.common.BaseResponse;
import com.juzipi.common.ErrorCode;
import com.juzipi.common.ResultUtis;
import com.juzipi.dao.UserDao;
import com.juzipi.domain.entity.User;
import com.juzipi.exception.BusinessException;
import com.juzipi.service.UserService;
import com.juzipi.utils.AliOssUtil;
import com.juzipi.utils.FileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/common")
@CrossOrigin("http://localhost:5173")
@Api(tags = "文件管理模块")
@Slf4j
public class FileController {
    /**
     * 基本路径
     */
    @Value("${yuchuang.img}")
    private String basePath;

    @Resource
    private AliOssUtil aliOssUtil;


    /**
     * 用户服务
     */
    @Resource
    private UserService userService;

    @Resource
    private UserDao userDao;

    @Value("${yuchuang.qiniu.url}")
    private String QINIU_URL;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public BaseResponse<String> upload(MultipartFile file){
        log.info("文件上传：{}",file);

        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            //截取原始文件名的后缀   dfdfdf.png
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //构造新文件名称
            String objectName = UUID.randomUUID().toString() + extension;

            //文件的请求路径
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            return ResultUtis.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
        }

        return ResultUtis.success("null");
    }


    /**
     * 下载
     *
     * @param name     名字
     * @param response 响应
     */
    @GetMapping("/download")
    @ApiOperation(value = "文件下载")
    @ApiImplicitParams(
            {@ApiImplicitParam(name = "name", value = "文件名"),
                    @ApiImplicitParam(name = "request", value = "request请求")})
    public void download(String name, HttpServletResponse response) {
        try {
            // 获取指定文件
            File img = new File(System.getProperty("user.dir") + basePath + name);
            //获取输入流
            FileInputStream inputStream = new FileInputStream(img);
            //获取输出流
            ServletOutputStream outputStream = response.getOutputStream();
            //指定response类型
            response.setContentType("image/jpeg");
            int len = 0;
            byte[] bytes = new byte[1024];
            //将文件从输入流读到缓冲区，输出流读取缓冲内容
            while ((len = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
                outputStream.flush();
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
