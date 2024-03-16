package com.juzipi.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yuchuang.qiniu")
public class QiNiuProperties {
    private String accessKey;

    private String secretKey;

    private String bucket;

    private String url;
}
