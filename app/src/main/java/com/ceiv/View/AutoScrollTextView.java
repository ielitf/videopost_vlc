package com.ceiv.View;

import android.content.Context;
import android.graphics.Color;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import android.os.Handler;

import java.util.Timer;
import java.util.TimerTask;

public class AutoScrollTextView extends ScrollView {

    private boolean initialized = false;


    //每次移动的step 单位pixels
    private final static int step = 2;
    //整个ScrollView的高度
    private int totalHeight;
    //内部单个TextView的实际高度
    private int singleViewHeight;

    //滚动时起点位置
    private int startPosY;
    //滚动时终点位置
    private int endPosY;

    TimerTask scrollTask = null;
    Timer scrollTimer = null;

    private boolean needScroll = false;
    private boolean isScroll = false;

    private String content;
    //更新周期单位ms
    private int cycleTime;
    private int textSize;
    private int textColor;
    private int lineSpacingExtra;
    private float lineSpacingMultiplier;

    private LinearLayout llData;
    private TextView tv1;
    private TextView tv2;

    private Handler handler = new Handler() {
        private long oldTime = 0;
        private long nowTime = 0;
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                nowTime = System.currentTimeMillis();
                Log.d("scroll" + getId(), "time:" + (nowTime - oldTime));
                oldTime = nowTime;
                if (getScrollY() < startPosY) {
                    scrollBy(0, step);
                } else {
                    scrollTo(0, endPosY);
                }
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendEmptyMessage(1);
                    }
                }, cycleTime);


//                nowTime = System.currentTimeMillis();
//                Log.d("scroll" + getId(), "time:" + (nowTime - oldTime));
//                oldTime = nowTime;
//                if (getScrollY() < startPosY) {
//                    scrollBy(0, step);
//                } else {
//                    scrollTo(0, endPosY);
//                }
            } else if (msg.what == 2) {
                if (llData != null) {
                    llData.removeViewInLayout(tv2);
                }
            }
        }
    };

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    public AutoScrollTextView(Context context) {
        this(context, null);
    }

    public AutoScrollTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //InitAutoScrollTextView();
    }

    public AutoScrollTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AutoScrollTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /*
     *  content: 要显示的内容
     *  cycleTime: 更新的周期 ms  越大滚动的越慢，越小滚动的越快
     *  textSize: 字体大小
     *  textColor: 字体颜色 格式ARGB8888
     *  lineSpacingExtra: 行间距
     *  lineSpacingMultiplier: 行间距倍率
     */
    public void InitAutoScrollTextView(String content, final int cycleTime, int textSize, int textColor, int lineSpacingExtra, float lineSpacingMultiplier) {
        initialized = false;

        this.content = content;
        this.cycleTime = cycleTime;
        this.textSize = textSize;
        this.textColor = textColor;
        this.lineSpacingExtra = lineSpacingExtra;
        this.lineSpacingMultiplier = lineSpacingMultiplier;

        llData = new LinearLayout(getContext());
        llData.setOrientation(LinearLayout.VERTICAL);
        llData.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(llData);

        tv1 = new TextView(getContext());
        tv1.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tv1.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tv1.setMaxLines(10);
        tv1.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
        tv1.setText(content);
        tv1.setTextColor(textColor);
        tv1.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        tv1.setVisibility(VISIBLE);
        llData.addView(tv1);
        tv2 = new TextView(getContext());
        tv2.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tv2.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tv2.setMaxLines(10);
        tv2.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
        tv2.setText(content);
        tv2.setTextColor(textColor);
        tv2.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        tv2.setVisibility(VISIBLE);
        llData.addView(tv2);

        initialized = true;
    }

    public void setText(String text) {
        if (!initialized) {
            return;
        }
        if (scrollTimer != null) {
            scrollTimer.cancel();
            scrollTimer = null;
        }

        if (tv1 != null) {
            llData.removeViewInLayout(tv1);
            tv1 = null;
        }
        if (tv2 != null) {
            llData.removeViewInLayout(tv2);
            tv2 = null;
        }

        tv1 = new TextView(getContext());
        tv1.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tv1.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tv1.setMaxLines(10);
        tv1.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
        tv1.setText(text);
        tv1.setTextColor(textColor);
        tv1.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        tv1.setVisibility(VISIBLE);
        llData.addView(tv1);
        tv2 = new TextView(getContext());
        tv2.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tv2.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tv2.setMaxLines(10);
        tv2.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
        tv2.setText(text);
        tv2.setTextColor(textColor);
        tv2.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        tv2.setVisibility(VISIBLE);
        llData.addView(tv2);

        show();
    }

    public void setTextSize(int size) {
        if (!initialized) {
            return;
        }
        if (scrollTimer != null) {
            scrollTimer.cancel();
            scrollTimer = null;
        }
        if (tv1 != null) {
            llData.removeViewInLayout(tv1);
            tv1 = null;
        }
        if (tv2 != null) {
            llData.removeViewInLayout(tv2);
            tv2 = null;
        }

        tv1 = new TextView(getContext());
        tv1.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tv1.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tv1.setMaxLines(10);
        tv1.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
        tv1.setText(content);
        tv1.setTextColor(textColor);
        tv1.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tv1.setVisibility(VISIBLE);
        llData.addView(tv1);
        tv2 = new TextView(getContext());
        tv2.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tv2.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tv2.setMaxLines(10);
        tv2.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
        tv2.setText(content);
        tv2.setTextColor(textColor);
        tv2.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tv2.setVisibility(VISIBLE);
        llData.addView(tv2);

        show();
    }

    public void setScrollSpeed(int speed) {
        if (!initialized) {
            return;
        }

    }

    public void show() {

        post(new Runnable() {
                 @Override
                 public void run() {
                     initialized = false;
                     needScroll = false;

                     totalHeight = getMeasuredHeight();
                     Log.d("debug_test", "totalHeight: " + totalHeight);
                     Log.d("debug_test", "totalWidth: " + getMeasuredWidth());
                     singleViewHeight = tv1.getMeasuredHeight();
                     Log.d("debug_test", "singleViewHeight: " + singleViewHeight);
                     Log.d("debug_test", "singleViewWidth: " + tv1.getMeasuredWidth());

                     if (totalHeight > 0 && singleViewHeight > 0) {
                         initialized = true;
                         startPosY = 2 * singleViewHeight - totalHeight - step;
                         endPosY = singleViewHeight - totalHeight;
                         //当界面不能完全显示内容时需要滚动显示
                         if (singleViewHeight > totalHeight) {
                             Log.d("debug_test", "need scroll");
                             needScroll = true;
                         }
                     }
                     if (needScroll) {
//                         Message msg = Message.obtain();
//                         msg.what = 1;
//                         handler.sendMessage(msg);

                         scrollTask = new TimerTask() {
                             private long oldTime;
                             private long nowTime;
                             @Override
                             public void run() {
                                 nowTime = System.currentTimeMillis();
                                 //Log.d("scroll" + getId(), "time:" + (nowTime - oldTime));
                                 oldTime = nowTime;
                                 if (getScrollY() < startPosY) {
                                     smoothScrollBy(0, step);
                                 } else {
                                     scrollTo(0, endPosY);
                                 }
                             }
                         };
                         scrollTimer = new Timer();
                         scrollTimer.schedule(scrollTask, cycleTime, cycleTime);

//                         new Timer().schedule(new TimerTask() {
//                             private long oldTime;
//                             private long nowTime;
//                             @Override
//                             public void run() {
//                                 nowTime = System.currentTimeMillis();
//                                 //Log.d("scroll" + getId(), "time:" + (nowTime - oldTime));
//                                 oldTime = nowTime;
//                                 if (getScrollY() < startPosY) {
//                                     smoothScrollBy(0, step);
//                                 } else {
//                                     scrollTo(0, endPosY);
//                                 }
//                             }
//                         }, cycleTime, cycleTime);

//                         scrollTask = new TimerTask() {
//                             private long oldTime = 0;
//                             private long newTime = 0;
//                             @Override
//                             public void run() {
//                                 Message msg = Message.obtain();
//                                 msg.what = 1;
//                                 newTime = System.currentTimeMillis();
//                                 Log.d("scrollmsg" + getId(), "time:" + (newTime - oldTime));
//                                 oldTime = newTime;
//                                 handler.sendMessage(msg);
//                             }
//                         };
//
//                         scrollTimer = new Timer();
//                         Log.d("debug_test", "Timer schedule");
//                         scrollTimer.schedule(scrollTask, cycleTime, cycleTime);
                     } else {
                         Message msg = Message.obtain();
                         msg.what = 2;
                         handler.sendMessage(msg);
                     }

                 }
             }
        );
    }

}
