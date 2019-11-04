package com.ceiv.videopost.fragment;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
//import android.util.Log;
import com.ceiv.log4j.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;

import com.ceiv.videopost.R;

/**
 * Created by chu on 2018/8/14.
 */


public class TopFragment extends Fragment {

    private final static String TAG = "TopFragment";

    private TextView curStationName = null;
    private TextView curStationEName = null;
    private TextView tempView = null;
    private TextView line = null;

    private String TempDefVal = "24~29°C";

    private final static int MsgSetStationName = 0x01;
    private final static int MsgSetStationEName = 0x02;
    private final static int MsgUpdateWeatherInfo = 0x03;
    private final static int MsgSetStationLine = 0x04;

    Handler handler;


    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.top_fragment,container,false);

        curStationName = view.findViewById(R.id.curStationName);
        curStationEName = view.findViewById(R.id.curStationEName);
        tempView = view.findViewById(R.id.tempText);
        line = view.findViewById(R.id.line);

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MsgSetStationLine:
                        line.setText((String) msg.obj+"路公交");
                        break;
                    case MsgSetStationName:
                        curStationName.setText((String) msg.obj);
                        break;
                    case MsgSetStationEName:
                        curStationEName.setText((String) msg.obj);
                        break;
                    case MsgUpdateWeatherInfo:
                        String wh = msg.getData().getString("wh");
                        tempView.setText((wh == null || "".equals(wh)) ? TempDefVal : wh);
                        break;
                    default:
                        break;
                }
            }
        };

        return view;
    }

    /*
     *  这里使用Message发送消息而不直接通过tempView修改内容的原因是，updateTemp可能
     *  会在子线程中被调用，而子线程是不能够直接修改界面的。
     */
    public void updateTemp(String temp) {
        Log.d("request_debug", "topfragment updateTemp");
        if (tempView != null && temp != null && !"".equals(temp)) {
            Log.d("request_debug", "gona to set:" + temp);
            Message msg = Message.obtain();
            msg.what = MsgUpdateWeatherInfo;
            Bundle bundle = new Bundle();
            bundle.putString("wh", temp);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    public void setCurStationName(String name) {
        if (curStationName != null && name != null && !("".equals(name))) {
            Log.d(TAG, "setCurStaName:" + name);
            Message msg = Message.obtain();
            msg.what = MsgSetStationName;
            msg.obj = name;
            handler.sendMessage(msg);
        }
    }

    public void setCurStationEName(String ename) {
        if (curStationEName != null && ename != null && !("".equals(ename))) {
            Log.d(TAG, "setCurStaEName:" + ename);
            Message msg = Message.obtain();
            msg.what = MsgSetStationEName;
            msg.obj = ename;
            handler.sendMessage(msg);
        }
    }

    public void setCurLine(String ename) {
        if (line != null && ename != null && !("".equals(ename))) {
            Log.d(TAG, "setCurLine:" + ename);
            Message msg = Message.obtain();
            msg.what = MsgSetStationLine;
            msg.obj = ename;
            handler.sendMessage(msg);
        }
    }

}
