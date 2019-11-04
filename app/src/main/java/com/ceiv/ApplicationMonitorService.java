package com.ceiv;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import com.ceiv.videopost.MainActivity;
import com.ceiv.videopost.R;
import com.ceiv.log4j.Log;

import java.util.List;

/*
*   采用前台服务的方法来保证，该Service不会被轻易杀死
*   该Service目前起到的作用是不断检测MainActivity的运行状态
*   如果检测到MainActivity不在运行了（可能由于错误崩溃了）
*   就重启该APP的MainActivity
* */
public class ApplicationMonitorService extends Service {


    private static final String TAG = "ApkMonitorService";
    private String gloActivityClassName = null;
    private ActivityManager gloManager = null;

    private static String packageName = "com.ceiv.videopost";

    public ApplicationMonitorService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        //启动前台服务
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        Intent nfIntent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("").setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("").setWhen(System.currentTimeMillis());

        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        startForeground(0, notification);

        //先初始化ActivityManager
        gloManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        android.util.Log.d(TAG, "Start Activity Monitor Thread...");
        Log.d(TAG, "Start Activity Monitor Thread...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //周期性检查当前界面运行的Activity是否是最开始的Activity(我们的程序)
                    List<ActivityManager.RunningTaskInfo> list = gloManager.getRunningTasks(1);
                    if (list != null && list.size() > 0) {
                        ComponentName cpn = list.get(0).topActivity;
                        android.util.Log.d(TAG, "TopActivity's PackageName: " + cpn.getPackageName());
                        if (!packageName.equals(cpn.getPackageName())) {
                            //不相等说明apk已经退出，需要重启apk
                            Log.e(TAG, "Now, TopActivity: " + cpn.getClassName() + ", gona to restart Apk!");
                            Intent intent = new Intent(ApplicationMonitorService.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
                        }
                    } else {
                        Log.e(TAG, "Get Activity Info failed");
                    }
                    //10s检查一次apk是否正常运行
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (Exception e) {
                        Log.e(TAG, "Thread sleep error!", e);
                    }
                }
            }
        }).start();
        super.onCreate();
    }





//    private static final String TAG = ApplicationMonitorService.class.getSimpleName();
//    private String gloActivityClassName = null;
//    private ActivityManager gloManager = null;
//
//    private static String packageName = null;
//
//    private static String Test = null;
//
//    public ApplicationMonitorService() {
//    }
//
//    public static void setMonitorActivity(String targetPackageName) {
//        android.util.Log.d(TAG, "targetPackageName: " + targetPackageName);
//        if (packageName == null || "".equals(packageName)) {
//            if (targetPackageName != null && !("".equals(targetPackageName))) {
//                packageName = targetPackageName;
//                Test = "just for test";
//                android.util.Log.d(TAG, "packageName: " + packageName + " for test: " + Test);
//
//            }
//        }
//    }
//
//    @Override
//    public void onCreate() {
//        Log.d(TAG, "onCreate");
//        android.util.Log.d(TAG, "packageName: " + packageName + " for test: " + Test);
//        if (packageName != null && !("".equals(packageName))) {
//            //启动前台服务
//            Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
//            Intent nfIntent = new Intent(this, MainActivity.class);
//            builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0))
//                    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
//                    .setContentTitle("").setSmallIcon(R.mipmap.ic_launcher)
//                    .setContentText("").setWhen(System.currentTimeMillis());
//
//            Notification notification = builder.build();
//            notification.defaults = Notification.DEFAULT_SOUND;
//            startForeground(0, notification);
//
//            //先初始化ActivityManager
//            gloManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//
//            android.util.Log.d(TAG, "Start Activity Monitor Thread...");
//            Log.d(TAG, "Start Activity Monitor Thread...");
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    while (true) {
//                        //周期性检查当前界面运行的Activity是否是最开始的Activity(我们的程序)
//                        List<ActivityManager.RunningTaskInfo> list = gloManager.getRunningTasks(1);
//                        if (list != null && list.size() > 0) {
//                            ComponentName cpn = list.get(0).topActivity;
//                            android.util.Log.e("ActivityTest", "TopActivity's PackageName: " + cpn.getPackageName());
//                            if (!packageName.equals(cpn.getPackageName())) {
//                                //不相等说明apk已经退出，需要重启apk
//                                android.util.Log.e("ActivityTest", "Now, TopActivity: " + cpn.getClassName() + ", gona to restart Apk!");
//                                Intent intent = new Intent(ApplicationMonitorService.this, MainActivity.class);
//                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                startActivity(intent);
//                            }
//                        } else {
//                            Log.e(TAG, "Get Activity Info failed");
//                            android.util.Log.e("ActivityTest", "Get Activity Info failed");
//                        }
//                        //10s检查一次apk是否正常运行
//                        try {
//                            Thread.sleep(10 * 1000);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Thread sleep error!", e);
//                        }
//                    }
//                }
//            }).start();
//        } else {
//            android.util.Log.e(TAG, "must set packageName before startService!");
//            Log.e(TAG, "must set packageName before startService!");
//        }
//        super.onCreate();
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopForeground(true);
        super.onDestroy();
    }
}
