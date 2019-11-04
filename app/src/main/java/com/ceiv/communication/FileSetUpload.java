package com.ceiv.communication;

import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhangdawei on 2018/8/17.
 */

//该类对一系列文件合集进行上传，并根据上传文件个数进行反馈进度
public class FileSetUpload {

    private final static String TAG = "FileSetUpload";

    //上传线程的状态
    public final static int UPLOAD_RUNNING = 0xfe;
    public final static int UPLOAD_FINISHED = 0xff;
    //上传的结果
    public final static int SUCCESS = 0x00;
    public final static int INVALID_PARAM = 0x01;
    public final static int FILE_NOT_EXISTS = 0x02;
    public final static int FILE_WRITE_ERROR = 0x03;
    public final static int HTTP_RESPONE_ERROR = 0x04;
    public final static int RUNTIME_EXCEPTION = 0x05;
    public final static int FAILED = 0xff;

    public static class FileUploadItem {

        public FileUploadItem(NetMgrDefine.MediaTypeEnum mediaTypeEnum, String name) {
            this.fileType = mediaTypeEnum;
            this.fileName = name;
        }

        //文件的类型
        NetMgrDefine.MediaTypeEnum fileType;

        //文件的名字
        String fileName;

        //文件的总大小， 单位为：Byte
        long fileSize;

        //文件上传的状态：UPLOAD_RUNNING：正在进行， UPLOAD_FINISHED：已经结束
        int status;

        //文件上传的结果：SUCCESS：成功， INVALID_PARAM：参数不合法， FILE_NOT_EXISTS：文件不存在， FAILED：下载失败
        //RUNTIME_EXCEPTION：运行时异常， HTTP_RESPONE_ERROR：http服务器反馈异常
        int result;
    }

    //回调函数，用来反馈上传结果
    public interface FileSetUploadCallBack {
        public void upload(int result);
    }



    private String basePath;
    private String videoPath;
    private String picturePath;
    private String textPath;

    private String serverVideoUrl;
    private String serverPictureUrl;
    private String serverTextUrl;

    private int totalTaskCount;
    private int finishTaskCount;

    FileSetUpload.UploadThreadPool uploadThreadPool;
    ArrayList<FileSetUpload.FileUploadItem> fileList;
    FileSetUpload.FileSetUploadCallBack callBack;

    public FileSetUpload(FileSetUpload.FileSetUploadCallBack callBack) {
        this.callBack = callBack;
    }

    public void uploadInit(final String mediaServerPrefix, ArrayList<FileSetUpload.FileUploadItem> fileList) {

        Log.d(TAG, "init...");
        this.fileList = fileList;
        totalTaskCount = fileList.size();
        finishTaskCount = 0;

        //上传文件本地路径
        basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        videoPath = basePath + "/media/video";
        picturePath = basePath + "/media/picture";
        textPath = basePath + "/media/text";

        //合成http上传url
        serverVideoUrl = mediaServerPrefix + "/video_upload";
        serverPictureUrl = mediaServerPrefix + "/pic_upload";
        serverTextUrl = mediaServerPrefix + "/text_upload";

        //和SingleThreadExecutor功能一样
        uploadThreadPool = new FileSetUpload.UploadThreadPool(1,1,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), callBack);

    }

    public void uploadStart() {
        Log.d(TAG, "start... ");
        for (FileSetUpload.FileUploadItem tmp : fileList) {
            uploadThreadPool.execute(new FileSetUpload.UploadRunnable(tmp));
        }
    }

    private class UploadRunnable implements Runnable {

        private FileSetUpload.FileUploadItem item;

        public UploadRunnable(FileSetUpload.FileUploadItem item) {
            this.item = item;
        }

        @Override
        public void run() {

            Log.d(TAG, "upload thread:" + item.fileName + " start ... ");

            String localPath = null;
            String strUrl = null;
            URL httpUrl = null;
            HttpURLConnection connection = null;
            FileInputStream fileInputStream = null;
            DataOutputStream dataOutputStream = null;

            switch (item.fileType) {
                case VIDEO:
                    strUrl = serverVideoUrl;
                    localPath = videoPath;
                    break;
                case PIC:
                    strUrl = serverPictureUrl;
                    localPath = picturePath;
                    break;
                case TEXT:
                    strUrl = serverTextUrl;
                    localPath = textPath;
                    break;
                default:
                    break;
            }

            File file = new File(localPath + "/" + item.fileName);
            if (!file.exists()) {
                Log.d(TAG, "Request upload file:" + localPath + "/" + item.fileName + "do not exist!");
                item.status = UPLOAD_FINISHED;
                item.result = FILE_NOT_EXISTS;
                return;
            }
            long fileSize = file.length();
            long transferredSize = 0;

            //边界标识，随机生成
            String BOUNDARY = UUID.randomUUID().toString();
            String PREFIX = "--";
            String LINE_END = "\r\n";
            String CONTENT_TYPE = "multipart/form-data";    //内容类型

            try {
                httpUrl = new URL(strUrl);
                connection = (HttpURLConnection) httpUrl.openConnection();

                //选择流式输出，否则会出现内存溢出的错误：Throwing OutOfMemoryError
                connection.setChunkedStreamingMode(4 * 1024);

                connection.setReadTimeout(5000);
                connection.setConnectTimeout(5000);
                connection.setDoInput(true);        //允许输入流
                connection.setDoOutput(true);       //允许输出流
                connection.setUseCaches(false);     //不允许使用缓存
                connection.setRequestMethod("POST");    //请求方式
                connection.setRequestProperty("Charset", "UTF-8");  //设置编码
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);

                dataOutputStream = new DataOutputStream(connection.getOutputStream());
                StringBuffer sb = new StringBuffer();
                sb.append(PREFIX);
                sb.append(BOUNDARY);
                sb.append(LINE_END);
                sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + item.fileName + "\"" + LINE_END);
                sb.append("Content-Type: application/octet-stream; charset=" + "UTF-8" + LINE_END);
                sb.append(LINE_END);
                dataOutputStream.write(sb.toString().getBytes());

                fileInputStream = new FileInputStream(file);
                byte[] bytes = new byte[4 * 1024];
                int len = 0;
                while ((len = fileInputStream.read(bytes)) != -1) {
                    dataOutputStream.write(bytes, 0, len);
                    transferredSize += len;
                }

                dataOutputStream.write(LINE_END.getBytes());
                byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes();
                dataOutputStream.write(end_data);
                dataOutputStream.flush();

                if (fileSize != transferredSize) {
                    Log.e(TAG, "Size of Upload file wrong!");
                    Log.d(TAG, "Remote file size:" + transferredSize + "Byte, local file size:" + fileSize + "Byte");
                    item.status = UPLOAD_FINISHED;
                    item.result = FILE_WRITE_ERROR;
                } else {
                    int res = connection.getResponseCode();
                    if (HttpURLConnection.HTTP_OK == res) {
                        Log.d(TAG, "upload file:" + localPath + "/" + item.fileName + ", size:" + fileSize + "Byte success");
                        item.status = UPLOAD_FINISHED;
                        item.result = SUCCESS;
                    } else {
                        Log.d(TAG, "upload file failed, http server respone:" + res);
                        item.status = UPLOAD_FINISHED;
                        item.result = HTTP_RESPONE_ERROR;
                    }
                }
            } catch (IOException e) {
                Log.d(TAG, "upload file failed!");
                e.printStackTrace();
                item.status = UPLOAD_RUNNING;
                item.result = RUNTIME_EXCEPTION;
            } finally {
                try {
                    if (dataOutputStream != null) {
                        dataOutputStream.close();
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close input output stream error!");
                    e.printStackTrace();
                    item.status = UPLOAD_RUNNING;
                    item.result = RUNTIME_EXCEPTION;
                }
            }
        }
    }



    private class UploadThreadPool extends ThreadPoolExecutor {
        public UploadThreadPool(int corePoolSize, int maxinumPoolSize, long keepAliveTime,
                                  TimeUnit unit, BlockingQueue<Runnable> workQueue, FileSetUpload.FileSetUploadCallBack callBack) {
            super(corePoolSize, maxinumPoolSize, keepAliveTime, unit, workQueue);
            this.callBack = callBack;
        }

        private FileSetUpload.FileSetUploadCallBack callBack;

        //线程运行之前会执行
        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);

        }

        //线程运行之后会执行
        @Override
        public void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            FileSetUpload.UploadRunnable uploadRunnable = (FileSetUpload.UploadRunnable) r;

            FileSetUpload.FileUploadItem fileItem = uploadRunnable.item;
            boolean uploadSuccess = true;
            if (UPLOAD_FINISHED == fileItem.status) {

                if (SUCCESS != fileItem.result) {
                    //只要有一个上传失败，就向上位机反馈失败
                    uploadSuccess = false;
                } else {
                    //若文件上传成功，向上位机反馈进度
                    finishTaskCount++;
                    uploadSuccess = true;
                }
            } else {
                //上传未完成，线程就结束的话反馈失败
                uploadSuccess = false;
            }

            if (uploadSuccess) {
                callBack.upload((int) (100.0 * finishTaskCount / totalTaskCount));
            } else {
                //任何一个文件上传失败，则后续不在进行上传
                callBack.upload(-1);
                this.shutdownNow();
            }


        }

        @Override
        protected void terminated() {
            super.terminated();

        }
    }


}
