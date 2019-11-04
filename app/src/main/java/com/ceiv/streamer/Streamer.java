package com.ceiv.streamer;


import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;

import com.ceiv.videopost.IPlay;



public class Streamer {
    static {
        System.loadLibrary("streamer");
    }

    /**
     * 获取视频/音频的时长
     * @param filePath 文件路劲
     * @return 时长
     */
    private int getLongTime(String filePath){
        int duration=0;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath); //在获取前，设置文件路径（应该只能是本地路径）
        String durationStr =retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        retriever.release(); //释放
        if(!TextUtils.isEmpty(durationStr)){
            duration = Integer.valueOf(durationStr);
        }
        return duration;
    }

    public int creat(final String mediaPath, String ip, int port) {
        final String sout = "#transcode{scodec=none}:rtp{dst=" + ip + "," + String.valueOf(port) + ",mux=ts,sap,name=ceiv}";
        new Thread(new Runnable() {
            @Override
            public void run() {
                creatStream(mediaPath, sout,10);
            }
        }).start();

        return 0;
    };

    public int creat(final String mediaPath, String ip, int port, final IPlay iPlay) {
        final String sout = "#transcode{scodec=none}:rtp{dst=" + ip + "," + String.valueOf(port) + ",mux=ts,sap,name=ceiv}";
        new Thread(new Runnable() {
            @Override
            public void run() {
                int duration = getLongTime(mediaPath)/1000;
                Log.d("streamer", "run:开始播放： "+mediaPath+" 时间："+duration);
                creatStream(mediaPath, sout,duration);
                Log.d("streamer", "run:播放完成： "+mediaPath);
                iPlay.playEnd();
            }
        }).start();

        return 0;
    };

    public int stop() {
        return stopStream();
    };

    public int pause() {
        return pauseStream();
    };

    public int start() {
        return startStream();
    };

    public int add(String mediaPath) {
        return addInput(mediaPath);
    };

    public int del(String mediaPath) {
        return delMedia(mediaPath);
    };

    public int change(final String mediaPath, String ip, int port) {
        final String sout = "#transcode{scodec=none}:rtp{dst=" + ip + "," + String.valueOf(port) + ",mux=ts,sap,name=ceiv}";
        changeMedia(mediaPath, sout);
        return 0;
    };

    // jni
    private native int creatStream(String mediaPath, String sout,int time);
    private native int stopStream();
    private native int startStream();
    private native int pauseStream();
    private native int addInput(String mediaPath);
    private native int delMedia(String mediaPath);
    private native int changeMedia(String mediaPath, String sout);
}
