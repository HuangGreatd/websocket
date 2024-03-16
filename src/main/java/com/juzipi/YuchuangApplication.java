package com.juzipi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.juzipi.mapper")
public class YuchuangApplication {

    public static void main(String[] args) {
        System.out.println("程序启动");
        SpringApplication.run(YuchuangApplication.class, args);
    }

}
