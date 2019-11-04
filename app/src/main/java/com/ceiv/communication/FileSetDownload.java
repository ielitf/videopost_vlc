package com.ceiv.communication;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.ceiv.communication.MD5Util.getFileMD5String;

/**
 * Created by zhangdawei on 2018/8/17.
 */


//该类对一系列文件合集进行下载，并根据下载文件个数进行反馈进度
public class FileSetDownload {

    private final static String TAG = "FileSetDownload";

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

    public static class FileDownloadItem {

        public FileDownloadItem(NetMgrDefine.MediaTypeEnum mediaTypeEnum, String name,String md5num) {
                this.fileType = mediaTypeEnum;
                this.fileName = name;
                this.md5num = md5num;
        }

        //文件的类型
        NetMgrDefine.MediaTypeEnum fileType;

        //文件的名字
        String fileName;

        //文件的MD5
        String md5num;

        //文件的总大小， 单位为：Byte
        long fileSize;

        //文件下载的状态：DOWNLOAD_RUNNING：正在进行， DOWNLOAD_FINISHED：已经结束
        int status;

        //文件下载的结果：SUCCESS：成功， INVALID_PARAM：参数不合法， FILE_NOT_EXISTS：文件不存在， FAILED：下载失败
        int result;
    }

    //回调函数，用来反馈下载结果
    public interface FileSetDownloadCallBack {
        public void download(int result);
    }



    private String basePath;
    private String videoPath;
    private String picturePath;
    private String textPath;

    private String serverVideoPath;
    private String serverPicturePath;
    private String serverTextPath;

    private int totalTaskCount;
    private int finishTaskCount;

    DownloadThreadPool downloadThreadPool;
    ArrayList<FileDownloadItem> fileList;
    FileSetDownloadCallBack callBack;

    public FileSetDownload(FileSetDownloadCallBack callBack) {
        this.callBack = callBack;
    }

    public void downloadInit(final String mediaServerPrefix, ArrayList<FileDownloadItem> fileList) {

        Log.d(TAG, "init...");
        this.fileList = fileList;
        totalTaskCount = fileList.size();
        finishTaskCount = 0;

        //合成本地下载路径
        basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        videoPath = basePath + "/media/video";
        picturePath = basePath + "/media/picture";
        textPath = basePath + "/media/text";

        //合成http下载url前缀
        serverVideoPath = mediaServerPrefix + "/video";
        serverPicturePath = mediaServerPrefix + "/pic";
        serverTextPath = mediaServerPrefix + "/text";

        //和SingleThreadExecutor功能一样
        downloadThreadPool = new DownloadThreadPool(1,1,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), callBack);

    }

    public void downloadStart() {
        Log.d(TAG, "start... ");
        for (FileDownloadItem tmp : fileList) {
            downloadThreadPool.execute(new DownloadRunnable(tmp));
        }
    }

    private class DownloadRunnable implements Runnable {

        private FileDownloadItem item;

        public DownloadRunnable(FileDownloadItem item) {
            this.item = item;
        }

        @Override
        public void run() {

            Log.d(TAG, "download thread:" + item.fileName + " start ... ");
            String localPath = null;
            String strUrl = null;
            URL httpUrl = null;
            HttpURLConnection connection = null;
            FileOutputStream fileOutputStream = null;
            InputStream inputStream = null;

            switch (item.fileType) {
                case VIDEO:
                    strUrl = serverVideoPath + "/" + item.fileName;
                    localPath = videoPath;
                    break;
                case PIC:
                    strUrl = serverPicturePath + "/" + item.fileName;
                    localPath = picturePath;
                    break;
                case TEXT:
                    strUrl = serverTextPath + "/" + item.fileName;
                    localPath = textPath;
                    break;
                default:
                    break;
            }

            File pathFile = new File(localPath);
            if (!pathFile.exists()) {
                if (!pathFile.mkdirs()) {
                    item.status = DOWNLOAD_FINISHED;
                    item.result = FILE_WRITE_ERROR;
                    return;
                }
            }

            try {
                Log.d(TAG, "strUrl: " + strUrl);
                httpUrl = new URL(strUrl);
                connection = (HttpURLConnection) httpUrl.openConnection();

                connection.setRequestMethod("GET");    //请求方式
                connection.setReadTimeout(5000);
                connection.setConnectTimeout(5000);

                Log.d(TAG, "getResponseCode");
                int responseCode = connection.getResponseCode();
                if (HttpURLConnection.HTTP_OK != responseCode) {
                    Log.d(TAG, "http connection respone error, responeCode:" + responseCode);
                    item.status = DOWNLOAD_FINISHED;
                    item.result = HTTP_RESPONE_ERROR;
                } else {
                    //获取内容长度
                    int contentLength = connection.getContentLength();
                    item.fileSize = contentLength;
                    Log.d(TAG, "Remote file length:" + contentLength);

                    inputStream = connection.getInputStream();

                    File file = new File(localPath + "/" + item.fileName);
                    fileOutputStream = new FileOutputStream(file);

                    byte[] bytes = new byte[4 * 1024];
                    long totalReaded = 0;
                    int temp_len;

                    while ((temp_len = inputStream.read(bytes)) != -1) {
                        totalReaded += temp_len;
                        fileOutputStream.write(bytes, 0, temp_len);
                    }
                    fileOutputStream.flush();

                    //Log.d(TAG, "ret: " + connection.getResponseCode());
                    if (totalReaded != contentLength) {
                        Log.e(TAG, "Size of Download file wrong!");
                        Log.d(TAG, "Remote file size:" + contentLength + "Byte, local file size:" + totalReaded + "Byte");
                        item.status = DOWNLOAD_FINISHED;
                        item.result = FILE_WRITE_ERROR;
                    } else {
                        Log.d(TAG, "download file:" + item.fileName + ", size:" + contentLength + "Byte success");
                        String md5 = getFileMD5String(file, 8196, false).toUpperCase();
                        Log.d(TAG, "file md5:" + md5);
                        if (item.md5num.equals(md5)){
                            item.status = DOWNLOAD_FINISHED;
                            item.result = SUCCESS;
                        }else {
                            item.status = DOWNLOAD_FINISHED;
                            item.result = FILE_WRITE_ERROR;
                        }

                    }
                }
            } catch (IOException e) {
                Log.d(TAG, "download file failed!");
                e.printStackTrace();
                item.status = DOWNLOAD_RUNNING;
                item.result = RUNTIME_EXCEPTION;
            } finally {
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
                    item.status = DOWNLOAD_RUNNING;
                    item.result = RUNTIME_EXCEPTION;
                }
            }
        }
    }



    private class DownloadThreadPool extends ThreadPoolExecutor {
        public DownloadThreadPool(int corePoolSize, int maxinumPoolSize, long keepAliveTime,
                                  TimeUnit unit, BlockingQueue<Runnable> workQueue, FileSetDownloadCallBack callBack) {
            super(corePoolSize, maxinumPoolSize, keepAliveTime, unit, workQueue);
            this.callBack = callBack;
        }

        private FileSetDownloadCallBack callBack;

        //线程运行之前会执行
        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);

        }

        //线程运行之后会执行
        @Override
        public void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            DownloadRunnable downloadRunnable = (DownloadRunnable) r;

            FileDownloadItem fileItem = downloadRunnable.item;
            boolean downloadSuccess = true;
            if (DOWNLOAD_FINISHED == fileItem.status) {

                if (SUCCESS != fileItem.result) {
                    //只要有一个下载失败，就向上位机反馈失败
                    downloadSuccess = false;
                } else {
                    //若文件下载成功，向上位机反馈进度
                    finishTaskCount++;
                    downloadSuccess = true;
                }
            } else {
                //下载未完成，线程就结束的话反馈失败
                downloadSuccess = false;
            }

            if (downloadSuccess) {
                callBack.download((int) (100.0 * finishTaskCount / totalTaskCount));
            } else {
                deleteIncompleteFile(fileItem);
                //任何一个下载失败，则后续不在进行下载
                callBack.download(-1);
                this.shutdownNow();

            }


        }

        @Override
        protected void terminated() {
            super.terminated();

        }
    }

    //下载失败时检查本地残留的文件，如果有的话就删除
    private void deleteIncompleteFile(FileDownloadItem item) {

        String localPath = null;
        switch (item.fileType) {
            case VIDEO:
                localPath = videoPath + "/" + item.fileName;
                break;
            case PIC:
                localPath = picturePath + "/" + item.fileName;
                break;
            case TEXT:
                localPath = textPath + "/" + item.fileName;
                break;
            default:
                break;
        }

        Log.d(TAG, "download file: " + localPath + " failed, delete it");
        File file = new File(localPath);
        if (file.exists()) {
            file.delete();
        }
    }


}
