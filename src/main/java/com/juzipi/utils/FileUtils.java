package com.juzipi.utils;

import com.juzipi.common.ErrorCode;
import com.juzipi.exception.BusinessException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;

/**
 * 文件工具
 */
@Component
public class FileUtils {
    private static String basePath;

    @Resource
    private AliOssUtil aliOssUtil;


    public  String uploadFile(MultipartFile file) {
        //获取园文件名
        String originalFilename = file.getOriginalFilename();
        if (Strings.isEmpty(originalFilename)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取后缀名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (Strings.isEmpty(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        File dir = new File(System.getProperty("user.dir") + basePath);
        //如果文件夹不存在则创建新文件夹
        if (!dir.exists()){
            dir.mkdir();
        }
        File localFile = new File(System.getProperty("user.dir") + basePath + originalFilename);
        try {
            //将文件从tomcat 临时目录转移到指定的目录
            file.transferTo(localFile);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        byte[] imageStream = getImageStream(localFile);
        String fileName = aliOssUtil.upload(imageStream,originalFilename);
        localFile.delete();
        //上传七牛云
        return fileName;
//        return null;
    }

    private static byte[] getImageStream(File imageFile) {
        byte[] buffer = null;
        FileInputStream fis;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            fis = new FileInputStream(imageFile);
            byte[] b = new byte[1024];
            int n;
            while((n = fis.read(b)) != -1){
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    @Value("${yuchuang.img}")
    public void initBasePath(String b){
        basePath=b;
    }
}

