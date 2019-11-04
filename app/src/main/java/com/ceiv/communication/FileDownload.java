package com.ceiv.communication;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by zhangdawei on 2018/8/10.
 */

public class FileDownload {

    private final static String TAG = "FileDownload";

    //下载的状态
    public final static int DOWNLOAD_RUNNING = 0xfe;
    public final static int DOWNLOAD_FINISHED = 0xff;
    //下载的结果
    public final static int SUCCESS = 0x00;
    public final static int INVALID_PARAM = 0x01;
    public final static int FILE_NOT_EXISTS = 0x02;
    public final static int FILE_WRITE_ERROR = 0x03;
    public final static int HTTP_RESPONE_ERROR = 0x04;
    public final static int RUNTIME_EXCEPTION = 0x05;
    public final static int FAILED = 0xff;

    private FileDownload.FileDownloadCallBack callback;

    public FileDownload(FileDownload.FileDownloadCallBack callback) {
        this.callback = callback;
    }


    public interface FileDownloadCallBack {

        public void download(int status, int result);

    }

//    public void download(final String srcUrl, final String remoteFile, final String localPath, final String localFile) {
    public void download(final String srcUrl, final String localPath, final String localFile) {
        Log.d(TAG, "Download file:" + localPath + "/" + localFile + " from " + srcUrl);
//
//        if (TextUtils.isEmpty(srcUrl) || TextUtils.isEmpty(filePath)) {
//            Log.e(TAG, "invalid parameters!");
//            callback.download(DOWNLOAD_FINISHED, INVALID_PARAM);
//            return;
//        }

//        final File file = new File(filePath);
//        if (!file.exists() || !file.isFile()) {
//            Log.e(TAG, "file does't exists!");
//            callback.download(DOWNLOAD_FINISHED, FILE_NOT_EXISTS);
//            return;
//        }

        final String saveFileAllName = localPath + "/" + localFile;

        new Thread(new Runnable() {
            @Override
            public void run() {

                URL httpUrl = null;
                HttpURLConnection connection = null;
                FileOutputStream fileOutputStream = null;
                InputStream inputStream = null;

                boolean downloadSuccess = false;
                File downloadFile = null;
                int process = 0;

                try {
//                    httpUrl = new URL(srcUrl + "/" + remoteFile);
                    httpUrl = new URL(srcUrl);
                    connection = (HttpURLConnection) httpUrl.openConnection();

                    connection.setRequestMethod("GET");    //请求方式
                    connection.setReadTimeout(5000);
                    connection.setConnectTimeout(5000);
//                    connection.setRequestProperty("Connection", "Keep-Alive");
//                    connection.setRequestProperty("Charset", "UTF-8");  //设置编码
//                    connection.setDoInput(true);        //允许输入流
//                    connection.setDoOutput(true);       //允许输出流
//                    connection.setUseCaches(false);     //不允许使用缓存
                    //打开连接
                    //connection.connect();

                    Log.d(TAG, "getResponseCode");
                    int responseCode = connection.getResponseCode();
                    if (HttpURLConnection.HTTP_OK != responseCode) {
                        Log.d(TAG, "http connection respone error, responeCode:" + responseCode);
                        callback.download(DOWNLOAD_FINISHED, HTTP_RESPONE_ERROR);
                    } else {
                        //获取内容长度
                        int contentLength = connection.getContentLength();
                        Log.d(TAG, "Remote file length:" + contentLength);
                        File filePath = new File(localPath);

                        if (!filePath.exists()) {
                            filePath.mkdirs();
                        }

                        inputStream = connection.getInputStream();

                        downloadFile = new File(saveFileAllName);
                        fileOutputStream = new FileOutputStream(downloadFile);

                        byte[] bytes = new byte[4 * 1024];
                        long totalReaded = 0;
                        int temp_len;
                        int temp_process = 0;

                        while ((temp_len = inputStream.read(bytes)) != -1) {
                            totalReaded += temp_len;
                            fileOutputStream.write(bytes, 0, temp_len);
                            temp_process = (int) (100 * totalReaded / contentLength);
                            if (temp_process > process + 20) {
                                process = temp_process;
                                callback.download(DOWNLOAD_RUNNING, process);
                            }
                        }
                        fileOutputStream.flush();

                        if (totalReaded != contentLength) {
                            Log.e(TAG, "Size of Download file wrong!");
                            Log.d(TAG, "Remote file size:" + contentLength + "Byte, local file size:" + totalReaded + "Byte");
                            callback.download(DOWNLOAD_FINISHED, FILE_WRITE_ERROR);
                            downloadSuccess = false;
                        } else {
                            Log.d(TAG, "download file:" + saveFileAllName + ", size:" + contentLength + "Byte success");
                            callback.download(DOWNLOAD_FINISHED, SUCCESS);
                            downloadSuccess = true;
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG, "download file failed!");
                    e.printStackTrace();
                    callback.download(DOWNLOAD_FINISHED, RUNTIME_EXCEPTION);
                    downloadSuccess = false;
                } finally {
                    if (!downloadSuccess) {
                        //下载失败，删除残留文件
                        Log.d(TAG, "download file:" + saveFileAllName + " failed, delete it!");
                        if (null != downloadFile) {
                            downloadFile.delete();
                        }
                    }
                    try {
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "close input output stream error!");
                        e.printStackTrace();
                        callback.download(DOWNLOAD_FINISHED, RUNTIME_EXCEPTION);
                    }
                }
            }
        }).start();
    }
}



