package com.iisi.cmt.stationObsType.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


@Configuration
@ComponentScan({"com.iisi.cmt.stationObsType.service"
			, "com.iisi.cmt.stationObsType.dao"})
@PropertySource(value="file:config/application.properties")
public class Config {

	@Autowired
	Environment env;
	
	@Bean
	DataSource dataSource() {	
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName(env.getProperty("jdbc.driver"));
		ds.setUrl(env.getProperty("jdbc.url"));
		ds.setUsername(env.getProperty("jdbc.username"));
		ds.setPassword(env.getProperty("jdbc.password"));
		return ds;
	}
	
	@Bean
	JdbcTemplate jdbcTemplate (){
		return new JdbcTemplate(dataSource());
	}
	
}