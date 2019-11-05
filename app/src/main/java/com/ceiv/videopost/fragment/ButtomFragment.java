package com.ceiv.videopost.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
//import android.util.Log;
import com.ceiv.log4j.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ceiv.View.AutoScrollTextView;
import com.ceiv.View.ScrollTextSurfaceView;
import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.DeviceInfoUtils;
import com.ceiv.communication.utils.SystemInfoUtils;
import com.ceiv.videopost.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by chu on 2018/8/14.
 */

public class ButtomFragment  extends Fragment {

    private final static String TAG = "ButtomFragment";

    public final static String TipsContentFile = "Tips.txt";
    //public final static String defaultTips = "请乘客朋友们注意安全，遵守交通规则，有序乘车，营造良好乘车环境。祝您出行愉快。欢迎大家来到慧视科技参观、学习";
    public final static String defaultTips = "请乘客朋友们注意安全，遵守交通规则，有序乘车，营造良好乘车环境。祝您出行愉快。";
    //public final static String defaultTips = "请乘客朋友们注意安全，遵守交通规则";

    public static String textContent;
    public static int textSize;
    public static int textColor;
    public static String textFont;
    public static int scrollSpeed;

    ScrollTextSurfaceView scrollTextSurfaceView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.buttom_fragment,container,false);
        Log.d(TAG, "ButtomFragment onCreateView");
        scrollTextSurfaceView = view.findViewById(R.id.surfaceView);

        textSize = 50;
        textColor = 0xFFFFFFFF;
        textFont = "heiti";
        scrollSpeed = 90;

        if (DeviceInfoUtils.DeviceInfoUtilsInit(getActivity())) {
            DeviceInfo deviceInfo = DeviceInfoUtils.getDeviceInfoFromFile();
            if (deviceInfo != null) {
                Log.e(TAG, "字体大小:"+deviceInfo.getTextSize()+"/字体颜色："+deviceInfo.getTextColor()+"/字体："+deviceInfo.getTextFont());
                displayParametersInit(deviceInfo.getTextSize(), deviceInfo.getTextColor(),
                        deviceInfo.getTextFont(), deviceInfo.getTextSpeed());
            }
        }
        displayTextInit();
        show();

        return view;
    }

    public void displayParametersInit(int size, int color, String font, int speed) {

        updateTextSize(size);
        updateTextColor(color);
        updateTextFont(font);
        updateTextSpeed(speed);
    }

    public void displayTextInit() {

        File file = null;
        FileInputStream fis = null;
        String content = null;
        try {
            file = new File(Environment.getExternalStorageDirectory() + "/media/text/" + TipsContentFile);
            if (!file.exists() || !file.isFile()) {
                //文件不存在时使用默认Tips内容
                textContent = defaultTips;
            } else {
                byte[] readBuffer = new byte[1024];
                StringBuffer stringBuffer = new StringBuffer();
                fis = new FileInputStream(file);
                while (fis.read(readBuffer) != -1) {
                    stringBuffer.append(new String(readBuffer));
                }
                content = stringBuffer.toString().trim();
                if (content == null || "".equals(content)) {
                    //读到空的数据时也采用默认值
                    content = defaultTips;
                }
                textContent = content;
            }
        } catch (Exception e) {
            Log.e(TAG, "set Tips error!", e);
            //e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close Tips file error!", e);
                //e.printStackTrace();
            }
        }
    }

    private void show() {
        if (scrollTextSurfaceView != null) {
            try {
                scrollTextSurfaceView.displayParametersInit(textContent, textSize, textColor,
                        SystemInfoUtils.getTypeface(getContext(), SystemInfoUtils.fontToName.get(textFont)), scrollSpeed);
                scrollTextSurfaceView.show();
            } catch (Exception e) {
                Log.e(TAG, "show scrollTextSurfaceView error!", e);
                //e.printStackTrace();
            }
        }
    }

    public void updateDisplayContent() {
        displayTextInit();
        show();
    }

    public void updateDisplayParameters() {
        if (DeviceInfoUtils.DeviceInfoUtilsInit(getContext())) {
            DeviceInfo deviceInfo = DeviceInfoUtils.getDeviceInfoFromFile();
            if (deviceInfo != null) {
                displayParametersInit(deviceInfo.getTextSize(), deviceInfo.getTextColor(),
                        deviceInfo.getTextFont(), deviceInfo.getTextSpeed());
            }
        }
        show();
    }

    private void updateTextSize(int size) {
        if (size == 0) {
            //小
            textSize = 40;
        } else if (size == 1) {
            //中
            textSize = 50;
        } else {
            //大
            textSize = 60;
        }
    }

    private void updateTextColor(int color) {
        //不透明
        textColor = color | 0xFF000000;
    }

    private void updateTextFont(String font) {
        if (SystemInfoUtils.fontToName.containsKey(font)) {
            textFont = font;
        } else {
            Log.e(TAG, "Unsupport Text Font: " + font);
        }
    }

    private void updateTextSpeed(int speed) {
        if (speed == 0) {
            //慢
            scrollSpeed = 130;
        } else if (speed == 2) {
            //快
            scrollSpeed = 50;
        } else {
            //正常速度
            scrollSpeed = 90;
        }
    }

}







