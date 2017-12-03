package com.yanghui.elephant.server.comfig;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.enums.DBType;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;

@Configuration
@MapperScan(basePackages = {"com.yanghui.elephant.store.mapper*"})
public class MybatisPlusConfig {
	
	/**
	 * mybatis-plus分页插件<br>
	 * 文档：http://mp.baomidou.com<br>
	 */
	@Bean
	public PaginationInterceptor paginationInterceptor() {
		PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
		paginationInterceptor.setDialectType(DBType.H2.getDb());
		//paginationInterceptor.setOptimizeType(Optimize.JSQLPARSER.getOptimize());
		return paginationInterceptor;
	}
}
