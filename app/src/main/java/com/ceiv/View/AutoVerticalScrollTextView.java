package com.ceiv.View;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


public class AutoVerticalScrollTextView extends ScrollView {

    private final static String TAG = "VerScrollView";

    private final static int GetShowAreaSize = 0x01;
    private final static int GetTextAreaSize = 0x02;
    private final static int MsgScroll = 0xAA55AA55;

    private final static int step = 1;

    private AutoVerticalScrollTextView self;
    private LinearLayout textWrapper;
    private TextView textView1;
    private TextView textView2;

    private String textContent;
    private int textSize;
    private int textColor;
    private float lineSpace;
    private float multi;
    private String textFont;
    private int scrollTime;
    private boolean needScroll;

    private boolean isParaSet;

    private int showAreaWidth;
    private int showAreaHeight;

    private int dualTextHeight;
    private int scrollMaxHeight;
    private int textRealHeight;


    private int curPostionY;


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
//                case GetShowAreaSize:
//                    showAreaWidth = self.getMeasuredWidth();
//                    showAreaHeight = self.getMeasuredHeight();
//                    Log.i(TAG, "showAreaWidth: " + showAreaWidth);
//                    Log.i(TAG, "showAreaHeight: " + showAreaHeight);
//                    innerTextView = new TextView(self.getContext());
//                    innerTextView.setText(textContent);
//                    innerTextView.setTextSize(textSize);
//                    innerTextView.setWidth(showAreaWidth);
//                    innerTextView.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
//                    self.addView(innerTextView);
//                    handler.sendEmptyMessage(GetTextAreaSize);
//                    break;
//                case GetTextAreaSize:
//                    textRealWidth = innerTextView.getMeasuredWidth();
//                    textRealHeight = innerTextView.getMeasuredHeight();
//                    Log.i(TAG, "textRealWidth" + textRealWidth);
//                    Log.i(TAG, "textRealHeight" + textRealHeight);
//                    if (textRealHeight > showAreaHeight) {
//                        //需要滚动显示
//                        Log.i(TAG, "needScroll true");
//                        needScroll = true;
//                        String tmp = textContent + "\n" + textContent;
//                        innerTextView.setText(tmp);
//                    } else {
//                        //不需要滚动显示，则居中显示
//                        Log.i(TAG, "needScroll false");
//                        needScroll = false;
//                        ScrollView.LayoutParams layoutParams = (ScrollView.LayoutParams) innerTextView.getLayoutParams();
//                        layoutParams.gravity = Gravity.CENTER;
//                        innerTextView.setLayoutParams(layoutParams);
//                    }
//                    break;
                case MsgScroll:
                    //Log.i(TAG, "Thread: " + Thread.currentThread().getId() + " curTime: " + System.currentTimeMillis());
                    if (getScrollY() < scrollMaxHeight) {
//                            Log.i(TAG, "" + getScrollY());
                        scrollBy(0, step);
                    } else {
//                            Log.i(TAG, ">=");
                        scrollTo(0, step);
                    }
                    //handler.removeCallbacksAndMessages(null);
                    long now = SystemClock.uptimeMillis();
                    Log.i(TAG, "now Clock time: " + now);
                    long next = now + scrollTime - (now % scrollTime);
                    Log.i(TAG, "next Clock time: " + next);
                    //handler.removeMessages(MsgScroll);
                    //handler.sendEmptyMessageDelayed(MsgScroll, scrollTime);
                    handler.sendEmptyMessageAtTime(MsgScroll, next);

                    break;
                default:
            }
        }
    };

    public AutoVerticalScrollTextView(Context context) {
        this(context, null);
    }

    public AutoVerticalScrollTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoVerticalScrollTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AutoVerticalScrollTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        isParaSet = false;
        self = this;

        //handler.sendEmptyMessage(GetShowAreaSize);
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        Log.i(TAG, "onMeasure");
//    }

//    //属于ScrollView
//    @Override
//    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
//        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
//        Log.i(TAG, "onOverScrolled");
//    }
//
//    //属于View
//    @Override
//    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
//        super.onScrollChanged(l, t, oldl, oldt);
//        Log.i(TAG, "onScrollChanged");
//    }

    public void InitView() {

    }

    public void setTextParameters(String content, int size, int color, float lineSpace, float multi, String font) {
        Log.i(TAG, String.format("size: %d color: %x lineSpace: %f multi: %f", size, color, lineSpace, multi));
        textContent = content;
        textSize = size;
        textColor = color;
        textFont = font;
        this.lineSpace = lineSpace;
        this.multi = multi;
    }

    public void setScrollParameters(int speed) {
        Log.i(TAG, "scrollTime: " + speed);
        scrollTime = speed;
    }

    public void show() {

        //测量显示区域大小
        post(new Runnable() {
            @Override
            public void run() {
                showAreaWidth = self.getMeasuredWidth();
                showAreaHeight = self.getMeasuredHeight();
                Log.i(TAG, "showAreaWidth: " + showAreaWidth);
                Log.i(TAG, "showAreaHeight: " + showAreaHeight);

                textWrapper = new LinearLayout(self.getContext());
                ScrollView.LayoutParams wrapperLayoutPara = new ScrollView.LayoutParams(showAreaWidth, ScrollView.LayoutParams.WRAP_CONTENT);
                textWrapper.setOrientation(LinearLayout.VERTICAL);
                textWrapper.setLayoutParams(wrapperLayoutPara);

                textView1 = new TextView(textWrapper.getContext());
                textView1.setText(textContent);
                textView1.setTextSize(textSize);
                textView1.setTextColor(textColor);
                textView1.setLineSpacing(lineSpace, multi);
                //LinearLayout.LayoutParams textLayoutPara = new LinearLayout.LayoutParams(showAreaWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
                final LinearLayout.LayoutParams textLayoutPara = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                textView1.setLayoutParams(textLayoutPara);

                textView2 = new TextView(textWrapper.getContext());
                textView2.setText(textContent);
                textView2.setTextSize(textSize);
                textView2.setTextColor(textColor);
                textView2.setLineSpacing(lineSpace, multi);
                textView2.setLayoutParams(textLayoutPara);

                textWrapper.addView(textView1);
                textWrapper.addView(textView2);
                self.addView(textWrapper);

                textWrapper.post(new Runnable() {
                    @Override
                    public void run() {
                        dualTextHeight = textWrapper.getMeasuredHeight();
                        Log.i(TAG, "dualTextHeight: " + dualTextHeight);
                        if (dualTextHeight > showAreaHeight * 2) {
                            //需要滚动显示
                            Log.i(TAG, "needScroll true");
                            needScroll = true;
                            scrollMaxHeight = dualTextHeight / 2;
                            Log.i(TAG, "scrollMaxHeight: " + scrollMaxHeight);
                            handler.sendEmptyMessage(MsgScroll);
                        } else {
                            //不需要滚动显示，则居中显示
                            Log.i(TAG, "needScroll false");
                            needScroll = false;
                            textWrapper.removeViewAt(1);
                            ScrollView.LayoutParams wrapperLayoutPara = new ScrollView.LayoutParams(showAreaWidth, ScrollView.LayoutParams.WRAP_CONTENT);
                            wrapperLayoutPara.gravity = Gravity.CENTER;
                            textWrapper.setOrientation(LinearLayout.VERTICAL);
                            textWrapper.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
                            textWrapper.setLayoutParams(wrapperLayoutPara);
                        }
                    }
                });
            }
        });
    }

    public void stopScroll() {

    }

}

