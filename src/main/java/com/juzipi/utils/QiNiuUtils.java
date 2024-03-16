package com.juzipi.utils;

import com.google.gson.Gson;
import com.juzipi.common.AliOssProperties;
import com.juzipi.common.ErrorCode;
import com.juzipi.exception.BusinessException;
import com.juzipi.properties.QiNiuProperties;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.qiniu.storage.Region;

import java.io.ByteArrayInputStream;

/**
 * 七牛云工具
 */
@Component
public class QiNiuUtils {
    private static QiNiuProperties qiNiuProperties;

    private static AliOssProperties aliOssProperties;

    @Resource
    private QiNiuProperties tempProperties;

    @Resource
    private AliOssProperties tempAliOssProperties;

    public static String upload(byte[] uploadBytes) {
        Configuration cfg = new Configuration(Region.autoRegion());
        UploadManager uploadManager = new UploadManager(cfg);
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(uploadBytes);
        Auth auth = Auth.create(qiNiuProperties.getAccessKey(), qiNiuProperties.getSecretKey());
        String upToken = auth.uploadToken(qiNiuProperties.getBucket());
        try {
            Response response = uploadManager.put(byteInputStream, null, upToken, null, null);
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            return putRet.key;
        } catch (QiniuException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }
    }

    @PostConstruct
    public void initProperties() {
        qiNiuProperties = tempProperties;
        aliOssProperties = tempAliOssProperties;
    }
}
