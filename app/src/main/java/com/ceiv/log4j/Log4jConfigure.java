package com.ceiv.log4j;

/**
 * Created by dong on 2018.12.20.
 */

import android.os.Environment;

import org.apache.log4j.Level;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class Log4jConfigure {

    private static final int MAX_FILE_SIZE = 1024 * 1024 * 5;
    private static final int MAX_FILE_NUM = 10;

    private static final String DEFAULT_LOG_FILE_NAME = "videopost.log";

    private static final String TAG = "Log4jConfigure";
    // 对应AndroidManifest文件中的package
    private static final String PACKAGE_NAME = "com.ceiv.linepost";

    public static void configure() {
        final LogConfigurator logConfigurator = new LogConfigurator();
        try {
            String fileName = Environment.getExternalStorageDirectory() +
                    File.separator + "ceiv" + File.separator + "log" +
                    File.separator + DEFAULT_LOG_FILE_NAME;

            //以下为通用配置
            logConfigurator.setFileName(fileName);
            //设置最大文件大小 5M
            logConfigurator.setMaxFileSize(MAX_FILE_SIZE);
            //设置最大产生的文件个数 10个
            logConfigurator.setMaxBackupSize(MAX_FILE_NUM);
            //设置使用文件
            logConfigurator.setUseFileAppender(true);
            //设置文件输出的格式
            /*
             *   记录格式的例子：
             *       2019-01-23 10:50:26.123 DEBUG/SystemInitThread:消息体\n
             * */
            logConfigurator.setFilePattern("%d{yyyy-MM-dd HH:mm:ss.SSS}\t%p/%c:\t%m%n");
            //设置控制台输出
            logConfigurator.setUseLogCatAppender(true);
            //设置控制台输出格式
            logConfigurator.setLogCatPattern("%d{HH:mm:ss.SSS}\t%p/%c:\t%m%n");
            //设置立即输出到文件中
            logConfigurator.setImmediateFlush(true);
            //设置DEBUG以上级别的日志都输出
            logConfigurator.setRootLevel(Level.DEBUG);

            logConfigurator.configure();
            android.util.Log.e(TAG, "Log4j config finish");
        } catch (Throwable throwable) {
            logConfigurator.setResetConfiguration(true);
            android.util.Log.e(TAG, "Log4j config error, use default config. Error:" + throwable);
        }
    }

}
