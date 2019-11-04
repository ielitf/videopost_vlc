package com.ceiv.videopost.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
//import android.util.Log;
import com.ceiv.log4j.Log;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ceiv.videopost.R;

/**
 * Created by chu on 2018/8/16.
 */

public class QLeftFragment extends Fragment {

    private final static String TAG = "QLeftFragment";
    private TextView nextStaName,nextStaEName,dstStationNameTV,dstStationENameTV;
//    private TextView nStaName = null;
//    private TextView nStaEName = null;
//
//    private final static int MsgSetNextStaName = 0x01;
//    private final static int MsgSetNextStaEName = 0x02;
//
//    private Handler handler = handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case MsgSetNextStaName:
//                    nStaName.setText((String) msg.obj);
//                    break;
//                case MsgSetNextStaEName:
//                    nStaEName.setText((String) msg.obj);
//                    break;
//                default:
//                    break;
//            }
//        }
//    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.qleft_fragment,container,false);
        Log.e(TAG, "起点QLeftFragment");
//        nStaName = view.findViewById(R.id.leftNextStaName);
//        nStaEName = view.findViewById(R.id.leftNextStaEName);

        dstStationNameTV = view.findViewById(R.id.LdstStationName);
        dstStationENameTV = view.findViewById(R.id.LdstStationEName);

        //正常情况下，该View显示中文名称
        nextStaName = view.findViewById(R.id.leftNextStaName);
        //正常情况下，该View显示英文名称，当中文名称太长的话，该View显示后半部的中文
        nextStaEName = view.findViewById(R.id.leftNextStaEName);
        //当中文名称太长需要分行显示的是否，该View显示英文名称
        TextView nextStaEName2 = view.findViewById(R.id.leftNextStaEName2);

        Bundle args = getArguments();
        if (args == null) {
            Log.e(TAG, "Can't get Init Parameters!");
            return view;
        }
        String tmpName = args.getString("name");
        String tmpEName = args.getString("ename");
        String directionStation = args.getString("directionStation");
        String directionStationEn = args.getString("directionStationEn");

        dstStationNameTV.setText("开往"+directionStation+"方向");
        dstStationENameTV.setText("to "+directionStationEn);

        int bracketLeftIndex = -1;
        int bracketRightIndex = -1;
        //每行中文最多显示8个字符
        if (tmpName.length() > 8) {
            String name1 = null;
            String name2 = null;
            //查看是否有中文括号的内容
            bracketLeftIndex = tmpName.indexOf("（");
            bracketRightIndex = tmpName.indexOf("）");
            if (bracketLeftIndex < 0 || bracketRightIndex < 0 || bracketLeftIndex > bracketRightIndex) {
                //没有括号或者括号有问题则直接分行显示
                name1 = tmpName.substring(0, (int) (tmpName.length() / 2));
                name2 = tmpName.substring((int) (tmpName.length() / 2), tmpName.length());
            } else {
                //有括号的话，将括号中的内容放到第二行显示
                StringBuilder sb = new StringBuilder();
                //括号两边的凑在一起
                sb.append(tmpName.split("（")[0]).append(tmpName.split("）")[1]);
                name1 = sb.toString();
                //括号内的单独一行
                name2 = "（" + tmpName.split("（")[1].split("）")[0] + "）";
            }
            nextStaName.setText(name1);
            nextStaEName.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50);
            nextStaEName.setText(name2);
            nextStaEName2.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20);
            nextStaEName2.setText(tmpEName);
        } else {
            nextStaName.setText(tmpName);
            nextStaEName.setText(tmpEName);
            nextStaEName2.setVisibility(View.INVISIBLE);
        }
        return view;
    }

    public static QLeftFragment newInstance(String name, String ename,String directionStation,String directionStationEn) {
        if (name == null || "".equals(name) || ename == null || "".equals(ename)) {
            Log.e(TAG, "Invalid Station Name!");
            return null;
        }
        QLeftFragment qLeftFragment = new QLeftFragment();
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("ename", ename);
        bundle.putString("directionStation", directionStation);
        bundle.putString("directionStationEn", directionStationEn);
        qLeftFragment.setArguments(bundle);
        return qLeftFragment;
    }

    /**
     * 新添加的方法 by litf
     * 设置开往什么方向，以及下一站是什么车
     */
    public void setDirectionAndNestStation(String directionStation,String directionStationEn,String  nextStation,String  nextStationEn){
        Log.i(TAG, "direStation：" +directionStation+ "/direStationEn:"+directionStationEn+"/nextStation:"+nextStation+"/nextStationEn:"+nextStationEn);
        if(dstStationNameTV !=null && dstStationENameTV !=null && nextStaName !=null && nextStaEName !=null ){
            nextStaName.setText(nextStation);
            nextStaEName.setText(nextStationEn);
            dstStationNameTV.setText("开往" + directionStation + "方向");
            dstStationENameTV.setText("to " + directionStationEn);
        }
        else{
            Log.i(TAG, "textView：没有初始化");
        }
    }

//    public void setStaName(String name) {
//        if (name == null || "".equals(name)) {
//            return;
//        }
//        Message msg = Message.obtain();
//        msg.what = MsgSetNextStaName;
//        msg.obj = name;
//        handler.sendMessage(msg);
//    }
//
//    public void setStaEName(String ename) {
//        if (ename == null || "".equals(ename)) {
//            return;
//        }
//        Message msg = Message.obtain();
//        msg.what = MsgSetNextStaEName;
//        msg.obj = ename;
//        handler.sendMessage(msg);
//    }
}
