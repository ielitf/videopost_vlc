package com.ceiv.videopost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
//import android.util.Log;
import com.ceiv.log4j.Log;

import com.ceiv.communication.utils.SystemInfoUtils;

/**
 * Created by chu on 2018/6/25.
 */

public class MainReceiver extends BroadcastReceiver {

    private final static String TAG = "MainReceiver";

    public final static String START_APK = "android.intent.action.BOOT_COMPLETED";
    public final static String SCREEN_SHOT_FINISH = "com.ceiv.SCREEN_SHOT_FINISH";

    @Override
    public void onReceive (Context context, Intent intent) {
        Log.d(TAG, "receive broadcast");
        if (intent.getAction().equals(START_APK)) {
            Log.d(TAG, "receive action:" + START_APK);
            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d(TAG, "start MainActivity");
            context.startActivity(mainActivityIntent);
        } else if (intent.getAction().equals(SCREEN_SHOT_FINISH)) {
            //通知子线程截图完毕，可以回复上位机了
            Log.d(TAG, "Screen shot finished");
            synchronized (SystemInfoUtils.getMediaOptObject()) {
                SystemInfoUtils.getMediaOptObject().notify();
            }
        } else {
            Log.d(TAG, "receive unknown broadcast");
        }
    }

}

