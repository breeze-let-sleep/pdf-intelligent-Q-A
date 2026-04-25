package com.hyltest.rag_practice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RAG 实践项目启动类
 * 使用 @MapperScan 扫描 MyBatis Mapper 接口
 */
@SpringBootApplication
@MapperScan("com.hyltest.rag_practice.entity.mapper")
public class RagPracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagPracticeApplication.class, args);
	}

}
