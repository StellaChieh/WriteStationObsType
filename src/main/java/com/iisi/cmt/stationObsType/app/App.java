package com.iisi.cmt.stationObsType.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import com.iisi.cmt.stationObsType.config.Config;
import com.iisi.cmt.stationObsType.service.WriteProcess;


public class App {

	private static boolean printBean = false;
	private static final Logger resultLogger = LogManager.getLogger("result");
	private static final Logger programLogger = LogManager.getLogger("program");
	private static boolean successStatus = false;
	
	public final static void main(String[] args) {
	
		resultLogger.info("Program starts!");
		try {
			ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
			WriteProcess process = (WriteProcess)ctx.getBean("writeProcess");
			process.run(args[0]);
			successStatus = process.isAllCsvWriteIn(); 
			programLogger.info("User operation success status: " + successStatus);
			
			if(printBean) {
				String[] beanNames = ctx.getBeanDefinitionNames();
				for(String bean : beanNames) {
					System.out.println(bean);
				}
			}
			
			// close spring context
			((AbstractApplicationContext) ctx).close();
			resultLogger.info("Program finished!");
		} catch (Exception e ) {
			resultLogger.error("Program failed...");
			programLogger.error(e);
			System.out.println(false);
		}
		
		System.out.println(successStatus);
		
		programLogger.info("************************************");
		programLogger.info("************************************");
		programLogger.info("************************************");
	}
	
}
