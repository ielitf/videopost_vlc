package com.ceiv.videopost;

import android.app.Presentation;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
//import android.util.Log;
import com.ceiv.AutoRestartApplication;
import com.ceiv.log4j.Log;

import android.util.TypedValue;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.ceiv.BrtUtils.RouteInfo;
import com.ceiv.BrtUtils.StationItem;
import com.ceiv.View.AutoScrollTextView;
import com.ceiv.View.ScrollTextSurfaceView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.ceiv.communication.utils.DeviceInfo;
import com.ceiv.communication.utils.DeviceInfoUtils;
import com.ceiv.communication.utils.SystemInfoUtils;
import com.ceiv.videopost.StationInfo.StationInfoItem;
import com.ceiv.videopost.fragment.ButtomFragment;

import static com.ceiv.videopost.fragment.ButtomFragment.TipsContentFile;
import static com.ceiv.videopost.fragment.ButtomFragment.defaultTips;


/**
 * 下单异屏显示
 * */
public class ViceDisplay extends FragmentActivity {

	private final static String TAG = "ViceDisplay";

	private DisplayManager mDisplayManager;
	private ViceDisplayPresentation mPresentation;
	private Display[] displays;

	Handler handler;

   	private final static int MsgUpdateTemp = 0x01;
	private final static int MsgUpdateLeftBRT = 0x02;
	private final static int MsgUpdateRightBRT = 0x03;

	//底部的Tips
	ScrollTextSurfaceView scrollTextSurfaceView = null;
//	AutoScrollTextView autoScrollTextView = null;

    //视频播放View
    //private VideoView videoView = null;
	private RelativeLayout mRelativeLayout = null;
	private SurfaceView mVideoSurface = null;


    //视频播放控制器
    private VideoController videoController = null;
    //温度信息
    private TextView tempView = null;
    //温度默认值
    private String TempDefVal = "24~29°C";
    private String TempCurVal;

    //界面信息是否已经初始化完毕
    private boolean initialized = false;

	//车辆信息TextView正常大小
	private final int NormalSize = 86;
	//车辆信息TextView显示内容较多时的大小
	private final int SmallSize = 64;

    //副屏左边的到站信息
	private TextView fLto_station = null;
	//内容
	private String fLto_content;
	//颜色
	private int fLto_color;
	//大小
	private int fLto_size;

	private TextView fLnextto_station = null;
	//内容
	private String fLnextto_content;
	//颜色
	private int fLnextto_color;
	//大小
	private int fLnextto_size;

	//副屏右边的到站信息
	private TextView fRto_station = null;
	//内容
	private String fRto_content;
	//颜色
	private int fRto_color;
	//大小
	private int fRto_size;

	private TextView fRnextto_station = null;
	//内容
	private String fRnextto_content;
	//颜色
	private int fRnextto_color;
	//大小
	private int fRnextto_size;

	//到站信息默认值
	private final static String BRTInfoDefVal = "暂无车辆";

	//当为起始站点时的相关数据

    //当为终点站时的相关数据


    //当为中间站点时的相关数据


	private Context context = null;

	//当前站点所在的位置  1: 起始站  2: 终点站  3: 中间的站点
	private int posFlag = -1;
	//屏幕显示样式，含义下面有介绍
	private int themeStyle = -1;
	//当前屏幕是否是下行站点的屏幕（当显示样式为3/4时有意义）
	private boolean isDownline = false;

//	//路线内所有的站点数据（包括上/下行）
//	private StationInfo stationInfo = null;
	private RouteInfo routeInfo = null;

	//下行数据
	private ArrayList<StationItem> downline = null;
//	//下行数据
//	private ArrayList<StationInfoItem> downline = null;
	//下行站点数目
    private int downlineCount = -1;
    //当前站点在下行List中的标号
	private int curDownlineIndex = -1;

	//上行数据
	private ArrayList<StationItem> upline = null;
//	//上行数据
//	private ArrayList<StationInfoItem> upline = null;
	//上行站点数目
	private int uplineCount = -1;
	//当前站点在上行List中的标号
	private int curUplineIndex = -1;

    /*
     *  posFlag: 当前站点所在的位置  1: 起始站  2: 终点站  3: 中间的站点
     *  themeStyle: 当前屏幕的样式
     *      其不同的值代表含义如下：
     *          1: 站台在中间，上下行线路在两边。且面向主屏时左边是下行，右边是上行；对应的，面向副屏时左边是上行，右边是下行。
     *          2: 站台在中间，上下行线路在两边。且面向主屏时左边是上行，右边是下行；对应的，面向副屏时左边是下行，右边是上行。
     *          3: 中间是上/下行线路，两边是站台。且面向主屏时对应的线路在右侧，具体屏幕右侧是上行还是下行由双程号决定；对应的
     *              ，面向副屏时对应的线路在左侧
     *          4: 中间是上/下行线路，两边是站台。且面向主屏时对应的线路在左侧，具体屏幕右侧是上行还是下行由双程号决定；对应的
     *              ，面向副屏时对应的线路在右侧
     *  isDownline: 当屏幕样式是3/4时需要确定当前屏幕对应于下行还是上行
     *  stationInfo: 全部站点信息
     *  downlineIndex: 当前站点在下行线路ArrayList信息中的位置
     *  uplineIndex: 当前站点在上行线路ArrayList信息中的位置
     *
     * */
//	public void ViceDisplayInit(int posFlag, int themeStyle, boolean isDownline, StationInfo stationInfo, int downlineIndex, int uplineIndex) {
//        this.posFlag = posFlag;
//        this.themeStyle = themeStyle;
//        this.isDownline = isDownline;
//        this.stationInfo = stationInfo;
//        downline = stationInfo.downline;
//        downlineCount = downline.size();
//        upline = stationInfo.upline;
//        uplineCount = upline.size();
//        curDownlineIndex = downlineIndex;
//        curUplineIndex = uplineIndex;
//    }

	public void ViceDisplayInit(int posFlag, int themeStyle, boolean isDownline, RouteInfo routeInfo, int downlineIndex, int uplineIndex) {
		this.posFlag = posFlag;
		this.themeStyle = themeStyle;
		this.isDownline = isDownline;
		this.routeInfo = routeInfo;
		downline = routeInfo.getDownline().getStationList();
		downlineCount = downline.size();
		upline = routeInfo.getUpline().getStationList();
		uplineCount = upline.size();
		curDownlineIndex = downlineIndex;
		curUplineIndex = uplineIndex;
	}

	public ViceDisplay(Context context){
		try {
			mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
			displays = mDisplayManager.getDisplays();
			if (displays.length<2) {
				//单屏
				mPresentation = new ViceDisplayPresentation(context, displays[0]);
			}else {
				//双屏
				mPresentation = new ViceDisplayPresentation(context, displays[1]);
			}
			mPresentation.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			mPresentation.setOnDismissListener(mOnDismissListener);
			this.context = context;
		} catch (Exception e) {
			Log.e(TAG, "ViceDisplay Create error!", e);
			//e.printStackTrace();
		}
	}
	/**开启异显*/
	public void Show(){
		try {
			mPresentation.show();
		} catch (Exception ex) {
			mPresentation = null;
			Log.d(TAG, "Presentation show error!", ex);
			//ex.printStackTrace();
		}
	}
	
	public void Close() {
		try {
			mPresentation.dismiss();
		} catch (Exception ex) {
			mPresentation = null;
			Log.d(TAG, "Presentation dismiss error!", ex);
			//ex.printStackTrace();
		}
	}
	
	private final DialogInterface.OnDismissListener mOnDismissListener = new DialogInterface.OnDismissListener() {
		@Override
		public void onDismiss(DialogInterface dialog) {
			if (dialog == mPresentation) {
				mPresentation.dismiss();
				mPresentation = null;
			}
		}
	};

	private class ViceDisplayPresentation extends Presentation {
		public ViceDisplayPresentation(Context outerContext, Display display) {
			super(outerContext, display);
			// TODO Auto-generated constructor stub	
		}


		@Override
		protected void onCreate(Bundle savedInstanceState) {
			Log.d(TAG, "Presentation onCreate");
			super.onCreate(savedInstanceState);
			//界面初始化
			PresentationInit();
			handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
						case MsgUpdateTemp:
							Log.d("request_debug", "viceDisplay updateTemp");
							if (tempView != null) {
								Log.d("request_debug", "viceDisplay updateTemp, value: " + TempCurVal);
								tempView.setText(TempCurVal);
							}
							break;
						case MsgUpdateLeftBRT:
							fLto_station.setText(fLto_content);
							fLto_station.setTextColor(fLto_color);
							fLto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, fLto_size);
//							fLnextto_station.setText(fLnextto_content);
//							fLnextto_station.setTextColor(fLnextto_color);
//							fLnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, fLnextto_size);
							break;
						case MsgUpdateRightBRT:
							fRto_station.setText(fRto_content);
							fRto_station.setTextColor(fRto_color);
							fRto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, fRto_size);
//							fRnextto_station.setText(fRnextto_content);
//							fRnextto_station.setTextColor(fRnextto_color);
//							fRnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, fRnextto_size);
							break;
						default:
							break;
					}
				}
			};
		}

		@Override
		public void onDisplayChanged() {
			Log.d(TAG, "onDisplayChanged");
			super.onDisplayChanged();
		}

		private void PresentationInit() {

			//当前站点View
			TextView curStaName = null;
			TextView curStaEName = null;
			//左边的下一站View
			TextView lNextStaName = null;
			TextView lNextStaEName = null;
			TextView lNextStaEName2 = null;
			//右边的下一站View
			TextView rNextStaName = null;
			TextView rNextStaEName = null;
			TextView rNextStaEName2 = null;

			if (posFlag == 3) {
				//大部分情况都是中间站点
				setContentView(R.layout.visdisplay);

				scrollTextSurfaceView = findViewById(R.id.ViceScrollView);
//				autoScrollTextView = findViewById(R.id.ViceScrollView);
//				autoScrollTextView.show("请乘客朋友们注意安全，遵守交通规则，有序乘车，营造良好乘车环境。祝您出行愉快。",
//						160, 40, 0xFFFFFFFF, 6, 1.0f);

				//videoView = findViewById(R.id.ViceVideoView);
				mRelativeLayout = findViewById(R.id.re_mvideo);
				mVideoSurface = new SurfaceView(context);
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(950, 710);
				layoutParams.setMargins(0,0,0,0);
				mVideoSurface.setLayoutParams(layoutParams);
				mVideoSurface.layout(0, 0, 950, 710);
				mRelativeLayout.addView(mVideoSurface);

				tempView = findViewById(R.id.TempView);

				//当前站点信息
				curStaName = findViewById(R.id.fStaName);
				curStaEName = findViewById(R.id.fStaEName);
				//对于中间站点来说，上下行线路中的名称都是一样的，这里选择下行/上行都行
//				curStaName.setText(downline.get(curDownlineIndex).name);
//				curStaEName.setText(downline.get(curDownlineIndex).ename);
				curStaName.setText(downline.get(curDownlineIndex).getStationName());
				curStaEName.setText(downline.get(curDownlineIndex).getStationEName());

				//目的地站点信息
				//左边
				TextView lDstStaName = findViewById(R.id.fLdstStaName);
				TextView lDstStaEName = findViewById(R.id.fLdstStaEName);
				ImageView lDirImg = findViewById(R.id.fLDirImg);
				//右边
				TextView rDstStaName = findViewById(R.id.fRdstStaName);
				TextView rDstStaEName = findViewById(R.id.fRdstStaEName);
				ImageView rDirImg = findViewById(R.id.fRDirImg);

				//下一站信息
				//左边
				lNextStaName = findViewById(R.id.fLnextStaName);
				lNextStaEName = findViewById(R.id.fLnextStaEName);
				lNextStaEName2 = findViewById(R.id.fLnextStaEName2);
				//右边
				rNextStaName = findViewById(R.id.fRnextStaName);
				rNextStaEName = findViewById(R.id.fRnextStaEName);
				rNextStaEName2 = findViewById(R.id.fRnextStaEName2);

				//到站信息
				//左边
				fLto_station = findViewById(R.id.fLto_station);
//				fLnextto_station = findViewById(R.id.fLnextto_station);
				//右边
				fRto_station = findViewById(R.id.fRto_station);
//				fRnextto_station = findViewById(R.id.fRnextto_station);

//				ArrayList<StationInfoItem> tmpList = null;
				ArrayList<StationItem> tmpList = null;
				if (1 == themeStyle) {
					//设置目的站点
					//左边（上行）
//					lDstStaName.setText("开往" + upline.get(uplineCount - 1).name.replace("站", "") + "方向");
//					lDstStaEName.setText("Bound to " + upline.get(uplineCount - 1).ename);
					lDstStaName.setText("开往" + upline.get(uplineCount - 1).getStationName().replace("站", "") + "方向");
					lDstStaEName.setText("Bound to " + upline.get(uplineCount - 1).getStationEName());
					lDirImg.setImageResource(R.drawable.left);
					//右边（下行）
//					rDstStaName.setText("开往" + downline.get(downlineCount - 1).name.replace("站", "") + "方向");
//					rDstStaEName.setText("Bound to " + downline.get(downlineCount - 1).ename);
					rDstStaName.setText("开往" + downline.get(downlineCount - 1).getStationName().replace("站", "") + "方向");
					rDstStaEName.setText("Bound to " + downline.get(downlineCount - 1).getStationEName());
					rDirImg.setImageResource(R.drawable.right);
					//设置下一站
					//左边
//					setNextStation(upline.get(curUplineIndex + 1).name, upline.get(curUplineIndex + 1).ename,
//							lNextStaName, lNextStaEName, lNextStaEName2);
					setNextStation(upline.get(curUplineIndex + 1).getStationName(), upline.get(curUplineIndex + 1).getStationEName(),
							lNextStaName, lNextStaEName, lNextStaEName2);
//					lNextStaName.setText(upline.get(curUplineIndex + 1).name);
//					lNextStaEName.setText(upline.get(curUplineIndex + 1).ename);
					//右边
//					setNextStation(downline.get(curDownlineIndex + 1).name, downline.get(curDownlineIndex + 1).ename,
//							rNextStaName, rNextStaEName, rNextStaEName2);
					setNextStation(downline.get(curDownlineIndex + 1).getStationName(), downline.get(curDownlineIndex + 1).getStationEName(),
							rNextStaName, rNextStaEName, rNextStaEName2);
//					rNextStaName.setText(downline.get(curDownlineIndex + 1).name);
//					rNextStaEName.setText(downline.get(curDownlineIndex + 1).ename);
				} else if (2 == themeStyle) {
					//设置目的站点
					//左边（下行）
//					lDstStaName.setText("开往" + downline.get(downlineCount - 1).name.replace("站", "") + "方向");
//					lDstStaEName.setText("Bound to " + downline.get(downlineCount - 1).ename);
					lDstStaName.setText("开往" + downline.get(downlineCount - 1).getStationName().replace("站", "") + "方向");
					lDstStaEName.setText("Bound to " + downline.get(downlineCount - 1).getStationEName());
					lDirImg.setImageResource(R.drawable.left);
					//右边（上行）
//					rDstStaName.setText("开往" + upline.get(uplineCount - 1).name.replace("站", "") + "方向");
//					rDstStaEName.setText("Bound to " + upline.get(uplineCount - 1).ename);
					rDstStaName.setText("开往" + upline.get(uplineCount - 1).getStationName().replace("站", "") + "方向");
					rDstStaEName.setText("Bound to " + upline.get(uplineCount - 1).getStationEName());
					rDirImg.setImageResource(R.drawable.right);
					//设置下一站
					//左边
//					setNextStation(downline.get(curDownlineIndex + 1).name, downline.get(curDownlineIndex + 1).ename,
//							lNextStaName, lNextStaEName, lNextStaEName2);
					setNextStation(downline.get(curDownlineIndex + 1).getStationName(), downline.get(curDownlineIndex + 1).getStationEName(),
							lNextStaName, lNextStaEName, lNextStaEName2);
//					lNextStaName.setText(downline.get(curDownlineIndex + 1).name);
//					lNextStaEName.setText(downline.get(curDownlineIndex + 1).ename);
					//右边
//					setNextStation(upline.get(curUplineIndex + 1).name, upline.get(curUplineIndex + 1).ename,
//							rNextStaName, rNextStaEName, rNextStaEName2);
					setNextStation(upline.get(curUplineIndex + 1).getStationName(), upline.get(curUplineIndex + 1).getStationEName(),
							rNextStaName, rNextStaEName, rNextStaEName2);
//					rNextStaName.setText(upline.get(curUplineIndex + 1).name);
//					rNextStaEName.setText(upline.get(curUplineIndex + 1).ename);
				} else if (3 == themeStyle || 4 == themeStyle) {
					int tmpIndex = -1;
					if (isDownline) {
						tmpList = downline;
						tmpIndex = curDownlineIndex;
					} else {
						tmpList = upline;
						tmpIndex = curUplineIndex;
					}
					if (3 == themeStyle) {
						//此时，面对副屏时，路线在左侧
						lDirImg.setImageResource(R.drawable.left);
						rDirImg.setImageResource(R.drawable.left);
					} else {
						//此时，面对副屏时，路线在右侧
						lDirImg.setImageResource(R.drawable.right);
						rDirImg.setImageResource(R.drawable.right);
					}
					//设置目的站点
					//左边
//					lDstStaName.setText("开往" + tmpList.get(tmpList.size() - 1).name.replace("站", "") + "方向");
//					lDstStaEName.setText("Bound to " + tmpList.get(tmpList.size() - 1).ename);
					lDstStaName.setText("开往" + tmpList.get(tmpList.size() - 1).getStationName().replace("站", "") + "方向");
					lDstStaEName.setText("Bound to " + tmpList.get(tmpList.size() - 1).getStationEName());
					//右边
//					rDstStaName.setText("开往" + tmpList.get(tmpList.size() - 1).name.replace("站", "") + "方向");
//					rDstStaEName.setText("Bound to " + tmpList.get(tmpList.size() - 1).ename);
					rDstStaName.setText("开往" + tmpList.get(tmpList.size() - 1).getStationName().replace("站", "") + "方向");
					rDstStaEName.setText("Bound to " + tmpList.get(tmpList.size() - 1).getStationEName());

					//设置下一站
					//左边
//					setNextStation(tmpList.get(tmpIndex + 1).name, tmpList.get(tmpIndex + 1).ename,
//							lNextStaName, lNextStaEName, lNextStaEName2);
					setNextStation(tmpList.get(tmpIndex + 1).getStationName(), tmpList.get(tmpIndex + 1).getStationEName(),
							lNextStaName, lNextStaEName, lNextStaEName2);
//					lNextStaName.setText(tmpList.get(tmpIndex + 1).name);
//					lNextStaEName.setText(tmpList.get(tmpIndex + 1).ename);
					//右边
//					setNextStation(tmpList.get(tmpIndex + 1).name, tmpList.get(tmpIndex + 1).ename,
//							rNextStaName, rNextStaEName, rNextStaEName2);
					setNextStation(tmpList.get(tmpIndex + 1).getStationName(), tmpList.get(tmpIndex + 1).getStationEName(),
							rNextStaName, rNextStaEName, rNextStaEName2);
//					rNextStaName.setText(tmpList.get(tmpIndex + 1).name);
//					rNextStaEName.setText(tmpList.get(tmpIndex + 1).ename);
				} else {
					Log.e(TAG, "Invalid themeStyle!");
				}
				//初始化BRT到站信息
				//左边
				fLto_station.setText(BRTInfoDefVal);
				fLto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);
//				fLnextto_station.setText(BRTInfoDefVal);
//				fLnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);
				//右边
				fRto_station.setText(BRTInfoDefVal);
				fRto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);
//				fRnextto_station.setText(BRTInfoDefVal);
//				fRnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);

			} else if (posFlag == 2) {
				//终点站
//				ArrayList<StationInfoItem> tmpList = null;
				ArrayList<StationItem> tmpList = null;
				if (isDownline) {
					tmpList = downline;
				} else {
					tmpList = upline;
				}
				setContentView(R.layout.zdisplay);

				scrollTextSurfaceView = findViewById(R.id.ZViceScrollView);
//				autoScrollTextView = findViewById(R.id.ZViceScrollView);
//				autoScrollTextView.show("请乘客朋友们注意安全，遵守交通规则，有序乘车，营造良好乘车环境。祝您出行愉快。",
//						160, 40, 0xFFFFFFFF, 6, 1.0f);

				//videoView = findViewById(R.id.zViceVideoView);

				mRelativeLayout = findViewById(R.id.re_zdi);
				mVideoSurface = new SurfaceView(context);
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(950, 710);
				layoutParams.setMargins(0,0,0,0);
				mVideoSurface.setLayoutParams(layoutParams);
				mVideoSurface.layout(0, 0, 950, 710);
				mRelativeLayout.addView(mVideoSurface);

				tempView = findViewById(R.id.zTempView);
				//初始化当前站点（终点站）
				curStaName = findViewById(R.id.fzStaName);
				curStaEName = findViewById(R.id.fzStaEName);
//				curStaName.setText(tmpList.get(tmpList.size() - 1).name);
//				curStaEName.setText(tmpList.get(tmpList.size() - 1).ename);
				curStaName.setText(tmpList.get(tmpList.size() - 1).getStationName());
				curStaEName.setText(tmpList.get(tmpList.size() - 1).getStationEName());

				//到站信息
				//左边
				fLto_station = findViewById(R.id.fZLto_station);
//				fLnextto_station = findViewById(R.id.fZLnextto_station);
				//右边
				fRto_station = findViewById(R.id.fZRto_station);
//				fRnextto_station = findViewById(R.id.fZRnextto_station);
				//初始化BRT到站信息
				//左边
				fLto_station.setText(BRTInfoDefVal);
				fLto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);
//				fLnextto_station.setText(BRTInfoDefVal);
//				fLnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);
				//右边
				fRto_station.setText(BRTInfoDefVal);
				fRto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);
//				fRnextto_station.setText(BRTInfoDefVal);
//				fRnextto_station.setTextSize(TypedValue.COMPLEX_UNIT_PX, NormalSize);

			} else {
				//两者都不是的话，认为是起始站点
				ArrayList<StationItem> tmpList = null;
//				ArrayList<StationInfoItem> tmpList = null;
				if (isDownline) {
					tmpList = downline;
				} else {
					tmpList = upline;
				}

				setContentView(R.layout.qvisdisplay);

				scrollTextSurfaceView = findViewById(R.id.QViceScrollView);
//				autoScrollTextView = findViewById(R.id.QViceScrollView);
//				autoScrollTextView.show("请乘客朋友们注意安全，遵守交通规则，有序乘车，营造良好乘车环境。祝您出行愉快。",
//						160, 40, 0xFFFFFFFF, 6, 1.0f);

				//videoView = findViewById(R.id.qViceVideoView);
				mRelativeLayout = findViewById(R.id.video_wrapper);
				mVideoSurface = new SurfaceView(context);
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(950, 710);
				layoutParams.setMargins(0,0,0,0);
				mVideoSurface.setLayoutParams(layoutParams);
				mVideoSurface.layout(0, 0, 950, 710);
				mRelativeLayout.addView(mVideoSurface);

				tempView = findViewById(R.id.qTempView);
				//初始化当前站点（起始站点）
				curStaName = findViewById(R.id.fqStaName);
				curStaEName = findViewById(R.id.fqStaEName);
//				curStaName.setText(tmpList.get(0).name);
//				curStaEName.setText(tmpList.get(0).ename);
				curStaName.setText(tmpList.get(0).getStationName());
				curStaEName.setText(tmpList.get(0).getStationEName());

				//初始化下一站信息
				//左边
				lNextStaName = findViewById(R.id.qFLnextStaName);
				lNextStaEName = findViewById(R.id.qFLnextStaEName);
				lNextStaEName2 = findViewById(R.id.qFLnextStaEName2);
//				setNextStation(tmpList.get(1).name, tmpList.get(1).ename,
//						lNextStaName, lNextStaEName, lNextStaEName2);
				setNextStation(tmpList.get(1).getStationName(), tmpList.get(1).getStationEName(),
						lNextStaName, lNextStaEName, lNextStaEName2);
//				lNextStaName.setText(tmpList.get(1).name);
//				lNextStaEName.setText(tmpList.get(1).ename);
				//右边
				rNextStaName = findViewById(R.id.qFRnextStaName);
				rNextStaEName = findViewById(R.id.qFRnextStaEName);
				rNextStaEName2 = findViewById(R.id.qFRnextStaEName2);
//				setNextStation(tmpList.get(1).name, tmpList.get(1).ename,
//						rNextStaName, rNextStaEName, rNextStaEName2);
				setNextStation(tmpList.get(1).getStationName(), tmpList.get(1).getStationEName(),
						rNextStaName, rNextStaEName, rNextStaEName2);
//				rNextStaName.setText(tmpList.get(1).name);
//				rNextStaEName.setText(tmpList.get(1).ename);
			}

			//测试surfaceview版滚动字幕
			tipsInit();

			//测试是否解决界面卡顿
            getWindow().setBackgroundDrawable(null);

			//开始播放视频
			try {
				videoController = new VideoController(context,mVideoSurface,
						"android.resource://" + context.getPackageName() + "/" + R.raw.nanning,
						Environment.getExternalStorageDirectory() + "/media/video");
				videoController.startVideo();
			} catch (Exception e) {
				Log.d(TAG, "play video error!", e);
				//e.printStackTrace();
			}

			//初始化温度信息
			if (null != TempCurVal) {
				tempView.setText(TempCurVal);
			} else {
				tempView.setText(TempDefVal);
			}

			initialized = true;
		}
	}

	public void tipsInit() {

		String textContent = defaultTips;
		int textSize = 50;
		int textColor = 0xFFFFFFFF;
		Typeface textFont = SystemInfoUtils.getTypeface(getParent(), SystemInfoUtils.fontToName.get("heiti"));
		int textSpeed = 90;
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

		if (DeviceInfoUtils.DeviceInfoUtilsInit(getParent())) {
			DeviceInfo deviceInfo = DeviceInfoUtils.getDeviceInfoFromFile();
			if (deviceInfo != null) {
				if (deviceInfo.getTextSize() == 0) {
					textSize = 40;
				} else if (deviceInfo.getTextSize() == 1) {
					textSize = 50;
				} else {
					textSize = 60;
				}
				textColor = deviceInfo.getTextColor();
				textFont = SystemInfoUtils.getTypeface(getParent(), SystemInfoUtils.fontToName.get(deviceInfo.getTextFont()));
				if (deviceInfo.getTextSpeed() == 0) {
					textSpeed = 130;
				} else if (deviceInfo.getTextSpeed() == 1) {
					textSpeed = 90;
				} else {
					textSpeed = 50;
				}

			}
		}
		try {
			scrollTextSurfaceView.displayParametersInit(textContent.trim(), textSize, textColor, textFont, textSpeed);
			scrollTextSurfaceView.show();
		} catch (Exception e) {
			Log.e(TAG, "vice display surfaceview error!", e);
			//e.printStackTrace();
		}
	}

	//更新界面下方Tips
	public void updateTips() {
		try {
			scrollTextSurfaceView.displayParametersInit(ButtomFragment.textContent, ButtomFragment.textSize, ButtomFragment.textColor,
					SystemInfoUtils.getTypeface(getParent(), SystemInfoUtils.fontToName.get(ButtomFragment.textFont)), ButtomFragment.scrollSpeed);
			scrollTextSurfaceView.show();
		} catch (Exception e) {
			Log.e(TAG, "update tips error!", e);
			//e.printStackTrace();
		}
	}

	//刷新视频播放列表
	public void refreshVideoList() {
		if (videoController != null) {
		    Log.d(TAG, "refresh ViceDisplay videolist");
			videoController.refreshVideoList();
		}
	}

	//停止播放现在的视频
	public void stopVideo(boolean playDefVi) {
		if (videoController != null) {
		    Log.d(TAG, "stop ViceDisplay videolist");
			videoController.stopVideo(playDefVi);
		}
	}

	//设置界面的下一站信息
	/*
	 *	name: 要设置的站点中文名
	 *	ename: 要设置的站点英文名
	 *	nameView: 正常情况下，该View显示中文名称
	 *	enameView: 正常情况下，该View显示英文名称，当中文名称太长的话，该View显示后半部的中文
	 *	enameView2: 当中文名称太长需要分行显示的时候，该View显示英文名称
	 */
	private void setNextStation(String name, String ename, TextView nameView, TextView enameView, TextView enameView2) {

		int bracketLeftIndex = -1;
		int bracketRightIndex = -1;
		//每行中文最多显示8个字符
		if (name.length() > 8) {
			String name1 = null;
			String name2 = null;
			//查看是否有中文括号的内容
			bracketLeftIndex = name.indexOf("（");
			bracketRightIndex = name.indexOf("）");
			if (bracketLeftIndex < 0 || bracketRightIndex < 0 || bracketLeftIndex > bracketRightIndex) {
				//没有括号或者括号有问题则直接分行显示
				name1 = name.substring(0, (int) (name.length() / 2));
				name2 = name.substring((int) (name.length() / 2), name.length());
			} else {
				//有括号的话，将括号中的内容放到第二行显示
				StringBuilder sb = new StringBuilder();
				//括号两边的凑在一起
				sb.append(name.split("（")[0]).append(name.split("）")[1]);
				name1 = sb.toString();
				//括号内的单独一行
				name2 = "（" + name.split("（")[1].split("）")[0] + "）";
			}
			nameView.setText(name1);
			//Log.d("debug_test", "name size:" + nameView.getTextSize());
			//这里需要注意的是setTextSize的默认单位是“sp”， getTextSize的默认单位是“px”
			enameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50);
			enameView.setText(name2);
			//Log.d("debug_test", "ename size:" + enameView.getTextSize());
			enameView2.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20);
			enameView2.setText(ename);
			//Log.d("debug_test", "ename2 size:" + enameView2.getTextSize());
			enameView2.setVisibility(View.VISIBLE);
		} else {
			nameView.setText(name);
			enameView.setText(ename);
			enameView2.setVisibility(View.INVISIBLE);
		}
	}

//	//更新BRT信息
//	/*
//	 *  ProductID: 车辆ID
//	 *  dualSerial: 双程号
//	 *  IsArrLeft: 到离站信息 1: 表示到站  2: 表示离站
//	 * */
//	public void updateBRTInfo(String ProductID, int dualSerial, int IsArrLeft) {
//
//		if (posFlag == 2) {
//			//中间站点
//
//		} else if (posFlag == 3) {
//			//终点
//
//		} else {
//			//起点站，不需要BRT信息，这里什么都不做
//
//		}
//	}

	/*
	 *	由于主屏、副屏信息是对称的，所以副屏这里不再处理Mqtt收到
	 *	的BRT信息了，直接由主屏通知副屏修改BRT信息及其内容
	 */
	//更新副屏界面左边的BRT信息
	public void updateLeftBrtInfo(String content1, int colorValue1, int size1, String content2, int colorValue2, int size2) {
		if (!initialized) {
			return;
		}

		if (posFlag == 2 || posFlag == 3) {
			//中间站点或者终点
			fLto_content = content1;
			fLto_color = colorValue1;
			fLto_size = size1;
			fLnextto_content = content2;
			fLnextto_color = colorValue2;
			fLnextto_size = size2;

			Message msg = Message.obtain();
			msg.what = MsgUpdateLeftBRT;
			handler.sendMessage(msg);
		}
	}

	//更新副屏界面右边的BRT信息
	public void updateRightBrtInfo(String content1, int colorValue1, int size1, String content2, int colorValue2, int size2) {
		if (!initialized) {
			return;
		}

		if (posFlag == 2 || posFlag == 3) {
			//中间站点或者终点
			fRto_content = content1;
			fRto_color = colorValue1;
			fRto_size = size1;
//			Log.d("size", String.valueOf(fRto_size));
			fRnextto_content = content2;
			fRnextto_color = colorValue2;
			fRnextto_size = size2;
			Message msg = Message.obtain();
			msg.what = MsgUpdateRightBRT;
			handler.sendMessage(msg);
		}
	}

	//更新天气（温度）信息
	public void updateTemp(String temp) {
		Log.d("request_debug", "vicedisplay updateTemp");
		TempCurVal = temp;
		if (!initialized) {
			return;
		}
		if (temp != null && !"".equals(temp)) {
			Log.d("request_debug", "gona to vicedisplay updateTemp");
			Message msg = Message.obtain();
			msg.what = MsgUpdateTemp;
			handler.sendMessage(msg);
		}
	}

}