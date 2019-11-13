package com.ceiv;

import android.app.Application;

import com.ceiv.communication.utils.CopyFileFromAssets;
import com.ceiv.log4j.Log;
import com.ceiv.videopost.CrashHandler;
import com.ceiv.videopost.utils.SpUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheEntity;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.MemoryCookieStore;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.OkHttpClient;

public class AutoRestartApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static final String TAG = AutoRestartApplication.class.getSimpleName();
    private static AutoRestartApplication app;
    public static AutoRestartApplication getApp(){
        return app;
    }

    @Override
    public void onCreate() {
        new SpUtils(this);
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        OkGo.getInstance().init(this);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(30000, TimeUnit.MILLISECONDS);      //读取超时时间
        builder.writeTimeout(30000, TimeUnit.MILLISECONDS);     //写入超时时间
        builder.connectTimeout(30000, TimeUnit.MILLISECONDS);   //连接超时时间
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);//日志的打印范围
        loggingInterceptor.setColorLevel(Level.INFO); //在logcat中的颜色
        builder.addInterceptor(loggingInterceptor);//默认是Debug日志类型
        builder.cookieJar(new CookieJarImpl(new MemoryCookieStore()));//使用内存保存cookie,退出后失效
        OkGo.getInstance()
                .setOkHttpClient(builder.build())
                .setCacheMode(CacheMode.FIRST_CACHE_THEN_REQUEST)
                .setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE)
                .setRetryCount(1);
        /*
         *   由于在程序运行过程中可能会发现未知的异常，导致程序崩溃退出
         *   使得用户体验不好，所以在这里设置全局的未捕获异常处理器，
         *   出现的任何未捕获的异常都在这里捕获并记录，之后主动退出程序
         *   同时ApplicationMonitorService会不断检测当前界面的程序，
         *   当检测到当前界面的程序不是我们的程序时（此时我们的程序大概率
         *   已经崩溃退出），会重新把我们的程序启动起来，从而实现崩溃自动重启
         *   这里需要注意的是在AndroidManifest.xml中必须将ApplicationMonitorService
         *   设置在单独的进程中运行，防止主程序崩溃时，监控服务也结束了
         * */
        Thread.setDefaultUncaughtExceptionHandler(this);
        super.onCreate();
        File file = new File("sdcard/Movies/test.mp4");
        if (!file.exists()){
            CopyFileFromAssets.copy(this,"nanning.mp4","/sdcard/Movies/","nanning.mp4");
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e(TAG, "Thread[" + t.getName() + "] UncaughtException", e);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
