package com.ceiv.videopost;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
//import android.util.Log;
import com.ceiv.AutoRestartApplication;
import com.ceiv.communication.utils.ProperTies;
import com.ceiv.log4j.Log;
import com.ceiv.streamer.Streamer;

import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;

/**
 * Created by zhangdawei on 2018/9/12.
 */

public class VideoController implements IVLCVout.OnNewVideoLayoutListener,IPlay{

    private final static String TAG = "VideoController";
    private static final String SAMPLE_URL = "rtp://@238.0.0.111:5004";

    private SurfaceView surfaceView;
    private String defVideoUri = null;
    private String videoPath = null;

    private RelativeLayout mRelativeLayout = null;
    //private SurfaceView mVideoSurface = null;
    private Media media;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;

    private Streamer streamer = null;
    private Context context;
    private String sendRtsp;

    /*
     *  视频播放的flag， 0: 代表还未调整过layout参数
     *  1: 代表当前layout参数适合较宽的视频播放  2: 代表当前layout参数适合较窄的视频播放
     */
    private int scaleFlag = 0;
    //videoView长宽参数
    private int viewWidth;
    private int viewHeight;
    //videoView长宽参数是否已经测得
    private boolean whMeasured = false;

    //正在播放的视频的长宽参数
    private int curWidth;
    private int curHeight;

    //视频文件列表
    private ArrayList<File> videoList;
    //视屏列表总数目
    private int videoCount;
    //当前正在播放的视屏的标号
    private int curVideoIndex;
    //播放标志 true:正在播放视屏，false:还未开始播放
    private boolean isPlaying;

    private int maxVideoTime = 0;
    private int curVideoTime = 0;

    //调整videoView布局参数，以便使视频在长宽比不变的情况下，最大化播放区域
    private void adjustLayoutParams() {

        if (isPlaying && whMeasured) {
            /*
             *  如果视频已经正在播放，需要查看是否需要调整videoVIew的参数，
             *  以便在视频长宽比不变的情况下，最大化播放区域
             */
            if ((viewWidth * 1.0f / viewHeight) > (curWidth * 1.0f / curHeight)) {
                //播放的视频比较窄
                if (scaleFlag != 2) {
//                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) videoView.getLayoutParams();
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
//                layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
//                layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    //videoView.setLayoutParams(layoutParams);
                    Log.d(TAG, "adjust VideoView layout params to TOP BOTTOM");
                    scaleFlag = 2;
                }

            } else {
                //播放的视频比较宽
                if (scaleFlag != 1) {
//                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) videoView.getLayoutParams();
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
//                layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
//                layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                    surfaceView.setLayoutParams(layoutParams);
                    Log.d(TAG, "adjust VideoView layout params to START END");
                    scaleFlag = 1;
                }
            }
        }
    }

    public VideoController(Context context,SurfaceView surfaceView, String defVideoUri, String videoPath) throws Exception{
        this.context = context;
        sendRtsp = ProperTies.getProperties(context).getProperty("sendrtsp");
        //检查参数是否合法
        if (surfaceView == null || defVideoUri == null || "".equals(defVideoUri)
                || videoPath == null || "".equals(videoPath)) {
            throw new Exception("Invalid arguments!");
        }
        streamer = new Streamer();

        final ArrayList<String> args = new ArrayList<>();
//        args.add("-vvv");
        mLibVLC = new LibVLC(context, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);


        this.surfaceView = surfaceView;
        this.defVideoUri = defVideoUri;
        this.videoPath = videoPath;

        viewWidth = -1;
        viewHeight = -1;
        whMeasured = false;
        isPlaying = false;
        scaleFlag = 0;
//        //videoView绘制完毕后，获取view的长宽参数
//        this.surfaceView.post(new Runnable() {
//            @Override
//            public void run() {
//                viewWidth = videoView.getMeasuredWidth();
//                viewHeight = videoView.getMeasuredHeight();
//                whMeasured = true;
//                Log.d("video_debug", "video getWidth: " + videoView.getWidth());
//                Log.d("video_debug", "video getHeight: " + videoView.getHeight());
//                Log.d("video_debug", "video getMeasuredWidth: " + videoView.getMeasuredWidth());
//                Log.d("video_debug", "video getMeasuredHeight: " + videoView.getMeasuredHeight());
//                adjustLayoutParams();
//            }
//        });

    }
    /* 视频操作相关 */

    //停止播放视频，playDefRes代表停止播放后是否播放应用自带的视频文件
    public void stopVideo(boolean playDefRes)
    {
        if (surfaceView != null && mMediaPlayer != null) {
            mMediaPlayer.stop();
            videoList.clear();
            if (playDefRes) {
                playDefaultVideo();
            }
        }
    }

    public void refreshVideoList() {
        stopVideo(false);
        startVideo();
    }

    //开始播放视频
    public void startVideo()
    {
        if (null == mMediaPlayer) {
            Log.d(TAG, "Get mMediaPlayer failed!");
            return;
        }
        if (null == surfaceView) {
            Log.d(TAG, "Get surfaceView failed!");
            return;
        }

        IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(surfaceView);
        vlcVout.attachViews(this);
        vlcVout.setWindowSize(950, 710);
        mMediaPlayer.setScale(0);
        mMediaPlayer.setAspectRatio("" + 950 + ":" + 710);

        //首先搜索sd卡的/media/video目录，重新建立播放列表
        File pathFile = new File(videoPath);
        Log.d(TAG, videoPath + " 存在？"+pathFile.exists());
        if(!pathFile.exists()){
            pathFile.mkdirs();
        }

        if (!pathFile.isDirectory()) {
            Log.d(TAG, "VideoPath: " + videoPath + " is not a directory!");
            playDefaultVideo();
            return;
        }
        //生成播放列表
        videoList = new ArrayList<File>();
        for(File tmp : pathFile.listFiles()) {
            if (tmp.isFile()) {
                videoList.add(tmp);
            }
        }
        //初始化播放视频标志
        videoCount = videoList.size();
        curVideoIndex = 0;

        //如果本地没有视屏文件，则循环播放自带的视频文件
        if (videoCount == 0) {
            Log.d(TAG, "本地没有视屏文件：开始播放默认视频");
            playDefaultVideo();
            return;
        }

        //videoView.setVideoURI(Uri.parse(videoList.get(curVideoIndex).getAbsolutePath()));
        //media = new Media(mLibVLC, Uri.parse("file://"+videoList.get(curVideoIndex).getAbsolutePath()));
        media = new Media(mLibVLC, Uri.parse(SAMPLE_URL.toString().trim()));
        mMediaPlayer.setMedia(media);
        media.release();
        isPlaying = true;
        maxVideoTime = (int)mMediaPlayer.getLength();
        curVideoTime = 0;
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        retr.setDataSource(videoList.get(curVideoIndex).getAbsolutePath());
        curWidth = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        curHeight = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        Log.d(TAG, "video: " + videoList.get(curVideoIndex).getName() +
                " width: " + curWidth + " height: " + curHeight+" URI:"+Uri.parse("file://"+videoList.get(curVideoIndex).getAbsolutePath()));
        adjustLayoutParams();

        //设置播放结束监听器
        mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                try {
                    if (event.getTimeChanged() == 0 || maxVideoTime == 0 || event.getTimeChanged() > maxVideoTime) {
                        return;
                    }
                    //可以用来刷新播放进度条
                    //播放结束
                    if (mMediaPlayer.getPlayerState() == Media.State.Ended) {
                        if (curVideoIndex < videoCount - 1) {
                            curVideoIndex++;
                        } else {
                            curVideoIndex = 0;
                        }
                        media = new Media(mLibVLC, Uri.parse(SAMPLE_URL.toString().trim()));
                        maxVideoTime = (int)mMediaPlayer.getLength();
                        curVideoTime = 0;
                        Log.d(TAG, "gona to play video:" + videoList.get(curVideoIndex).getName());
                        mMediaPlayer.setMedia(media);
                        media.release();
                        mMediaPlayer.play();
                        if ("yes;".equals(sendRtsp)){
                            streamer.creat(videoList.get(curVideoIndex).getAbsolutePath(), "238.0.0.111", 5004,VideoController.this);
                        }
                    }
                } catch (Exception e) {
                    Log.d("vlc-event", e.toString());
                }
            }
        });
        Log.d(TAG, "gona to play video:" + videoList.get(curVideoIndex).getName());
        media.release();
        mMediaPlayer.play();
        Log.d(TAG, "###:" + sendRtsp);
        if ("yes;".equals(sendRtsp)){
            streamer.creat(videoList.get(curVideoIndex).getAbsolutePath(), "238.0.0.111", 5004,this);
        }
    }

    private void playDefaultVideo()
    {
        Log.d(TAG, "gona to play default video!");
        if (null == mMediaPlayer) {
            Log.d(TAG, "Get mMediaPlayer failed!");
            return;
        }
        if (null == surfaceView) {
            Log.d(TAG, "Get surfaceView failed!");
            return;
        }

        Log.d(TAG, "URI:"+defVideoUri);
        //media = new Media(mLibVLC, Uri.parse("file:///sdcard/Movies/nanning.mp4"));
        media = new Media(mLibVLC, Uri.parse(SAMPLE_URL.toString().trim()));

        isPlaying = true;
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
//        retr.setDataSource(videoView.getContext(), Uri.parse(defVideoUri));
//        curWidth = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
//        curHeight = Integer.valueOf(retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
//        Log.d(TAG, "default video: width: " + curWidth + " height: " + curHeight);
//        adjustLayoutParams();

        mMediaPlayer.setMedia(media);
        maxVideoTime = (int)mMediaPlayer.getLength();
        curVideoTime = 0;
        mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                try {
                    if (event.getTimeChanged() == 0 || maxVideoTime == 0 || event.getTimeChanged() > maxVideoTime) {
                        return;
                    }
                    //可以用来刷新播放进度条
                    //播放结束
                    if (mMediaPlayer.getPlayerState() == Media.State.Ended) {
                        media = new Media(mLibVLC, Uri.parse(SAMPLE_URL.toString().trim()));
                        maxVideoTime = (int)mMediaPlayer.getLength();
                        curVideoTime = 0;
                        Log.d(TAG, "gona to play video:" + videoList.get(curVideoIndex).getName());
                        mMediaPlayer.setMedia(media);
                        media.release();
                        mMediaPlayer.play();
                        if ("yes;".equals(sendRtsp)){
                            streamer.creat("/sdcard/Movies/nanning.mp4", "238.0.0.111", 5004,VideoController.this);
                        }
                    }
                } catch (Exception e) {
                    Log.d("vlc-event", e.toString());
                }
            }
        });

        media.release();
        mMediaPlayer.play();
        if ("yes;".equals(sendRtsp))
        {
            streamer.creat("/sdcard/Movies/nanning.mp4", "238.0.0.111", 5004,this);
        }

    }


    @Override
    public void onNewVideoLayout(IVLCVout ivlcVout, int i, int i1, int i2, int i3, int i4, int i5) {

    }

    @Override
    public void playEnd() {
        Log.d(TAG, "播放完场媒体："+curVideoIndex+" 总共有："+videoCount );
        if (videoCount == 0) {
            media = new Media(mLibVLC, Uri.parse(SAMPLE_URL.toString().trim()));
            maxVideoTime = (int)mMediaPlayer.getLength();
            curVideoTime = 0;
//            Log.d(TAG, "gona to play video:" + videoList.get(curVideoIndex).getName());
            mMediaPlayer.setMedia(media);
            media.release();
            mMediaPlayer.play();
            if ("yes;".equals(sendRtsp)){
                streamer.creat("/sdcard/Movies/nanning.mp4", "238.0.0.111", 5004,this);
            }
        }else {
            if (curVideoIndex < videoCount - 1) {
                curVideoIndex++;
            } else {
                curVideoIndex = 0;
            }
            media = new Media(mLibVLC, Uri.parse(SAMPLE_URL.toString().trim()));
            maxVideoTime = (int)mMediaPlayer.getLength();
            curVideoTime = 0;
            Log.d(TAG, "gona to play video:" + videoList.get(curVideoIndex).getName());
            mMediaPlayer.setMedia(media);
            media.release();
            mMediaPlayer.play();
            if ("yes;".equals(sendRtsp)){
                streamer.creat(videoList.get(curVideoIndex).getAbsolutePath(), "238.0.0.111", 5004,this);
            }
        }
    }
}
