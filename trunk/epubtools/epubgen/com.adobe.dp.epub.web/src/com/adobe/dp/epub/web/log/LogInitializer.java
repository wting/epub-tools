package com.adobe.dp.epub.web.log;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public class LogInitializer {

	static File home;
	
	static {
		try {
			home = new File("/home/soroto2");
			if (!home.isDirectory())
				home = new File(System.getProperty("user.home"));
			RollingFileAppender appender = new RollingFileAppender();
			appender.setFile(home + File.separator + "logs" + File.separator
					+ "epubconv.log");
			appender.setBufferedIO(false);
			String pattern = "%d{DATE} %-5p [%c@%t]: %m%n";
			appender.setLayout(new PatternLayout(pattern));
			appender.setMaxFileSize("1Mb");
			appender.setMaxBackupIndex(3);
			appender.activateOptions();
			BasicConfigurator.configure(appender);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static File getHome() {
		return home;
	}
}
