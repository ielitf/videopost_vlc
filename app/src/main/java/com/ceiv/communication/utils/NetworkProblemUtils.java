package com.ceiv.communication.utils;

import android.os.Environment;

import com.ceiv.log4j.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkProblemUtils {

    private static final String TAG = NetworkProblemUtils.class.getSimpleName();

    private static final String RebootRecordFileName = "RebootRecord.txt";

    /*
    *   系统/apk重启的原因大致有以下几点：
    *       1、系统重新上电
    *       2、apk升级
    *       3、apk崩溃后重启
    *       4、网络环境问题重启（这种情况下是我们主动reboot的）
    *
    *   所以要做一个重启类型记录，这里我们主要注意的是因为网络环境问题的重启
    *   网络问题导致的主动重启需要限制最大的次数，其他情况导致的重启则没有必要记录次数
    * */

    /*
    *   记录文件内容格式，times指重启次数（网络原因导致的）；
    *   flag为1时表示最近的一次重启是因为网络环境问题，其他值则表明不是
    *   当apk启动后，需要检查flag，如果flag为1，则不清零times，否则需要清零times
    *
    * */
    private static final String outputFormatString = "times:%d,flag:%d\n";

    private static final String inputPatterns = ".*times:(\\d),flag:(\\d).*";

    //由于网络问题需要重启的最大次数
    public static final int RebootMaxTimes = 3;
    public static final int RebootMaxTimes2 = -3;

//    public enum RebootReason {
//
//        //以太网口网线未连接
//        ReasonNetworkDisconnet,
//        //以太网IP未设置
//        ReasonIpNotConfigured,
//        //以太网默认路由未设置
//        ResaonDefRouteNotSet,
//        //连接不到服务器
//        ReasonCantConnectToServer
//
//    }


    //清零重启记录
    public static void resetRebootTimes() {
        Log.d(TAG, "resetRebootTimes");
        rebootRecordFileCheck(true);
    }

    //复位重启FLag
    public static void resetRebootFlag() {
        Log.d(TAG, "resetRebootFlag");
        File recordFile;
        BufferedReader bufferedReader = null;
        FileOutputStream fos = null;
        int rebootTimes = -1;
        String content;

        rebootRecordFileCheck(false);

        try {
            recordFile = new File(Environment.getExternalStorageDirectory() +
                    File.separator + RebootRecordFileName);

            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(recordFile)));
            content = bufferedReader.readLine();

            Pattern p1 = Pattern.compile(inputPatterns);
            Matcher matcher = p1.matcher(content);
            if (matcher.matches()) {
                rebootTimes = Integer.valueOf(matcher.group(1));
                Log.d(TAG, "rebootTimes: " + rebootTimes);

                fos = new FileOutputStream(recordFile);
                //清空重启标志
                String output = String.format(Locale.CHINA, outputFormatString, rebootTimes, 0);
                Log.d(TAG, "write to file: " + output);
                fos.write(output.getBytes());
                fos.flush();
            }

        } catch (Exception e) {
            Log.e(TAG, "reset RebootFlag error!", e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public static boolean isRebootFromNetworkProblem() {
        Log.d(TAG, "isRebootFromNetworkProblem");
        File recordFile;
        BufferedReader bufferedReader = null;
        FileOutputStream fos = null;
        int rebootFlag = -1;
        String content;

        rebootRecordFileCheck(false);

        try {
            recordFile = new File(Environment.getExternalStorageDirectory() +
                    File.separator + RebootRecordFileName);

            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(recordFile)));
            content = bufferedReader.readLine();

            Pattern p1 = Pattern.compile(inputPatterns);
            Matcher matcher = p1.matcher(content);
            if (matcher.matches()) {
                rebootFlag = Integer.valueOf(matcher.group(2));
                Log.d(TAG, "rebootFlag: " + rebootFlag);
            }

        } catch (Exception e) {
            Log.e(TAG, "Get RebootFlag error!", e);
        }

        return (rebootFlag == 1);
    }

    //取得因为网络问题而重启的次数
    public static int getRebootTimes() {
        Log.d(TAG, "getRebootTimes");
        File recordFile;
        BufferedReader bufferedReader = null;
        FileOutputStream fos = null;
        int rebootTimes = -1;
        String content;

        rebootRecordFileCheck(false);

        try {
            recordFile = new File(Environment.getExternalStorageDirectory() +
                    File.separator + RebootRecordFileName);

            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(recordFile)));
            content = bufferedReader.readLine();

            Pattern p1 = Pattern.compile(inputPatterns);
            Matcher matcher = p1.matcher(content);
            if (matcher.matches()) {
                rebootTimes = Integer.valueOf(matcher.group(1));
                Log.d(TAG, "getRebootTimes, rebootTimes: " + rebootTimes);
            }

        } catch (Exception e) {
            Log.e(TAG, "Get RebootTimes error!", e);
        }

        if (rebootTimes < 0) {
            Log.e(TAG, "Get Invalid RebootTimes: " + rebootTimes + ", gona set default value: 0");
            rebootTimes = 0;
        }
        return rebootTimes;
    }

    //重启之前记录一次
    public static void rebootRecord() {
        Log.d(TAG, "rebootRecord");
        File recordFile;
        BufferedReader bufferedReader = null;
        FileOutputStream fos = null;
        int rebootTimes = -1;
        int rebootFlag;
        String content;

        rebootRecordFileCheck(false);

        try {
            recordFile = new File(Environment.getExternalStorageDirectory() +
                    File.separator + RebootRecordFileName);

            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(recordFile)));
            content = bufferedReader.readLine();

            Pattern p1 = Pattern.compile(inputPatterns);
            Matcher matcher = p1.matcher(content);
            if (matcher.matches()) {
                rebootTimes = Integer.valueOf(matcher.group(1));
                Log.d(TAG, "Before rebootRecord, rebootTimes: " + rebootTimes);
                rebootFlag = Integer.valueOf(matcher.group(2));
                Log.d(TAG, "Before rebootRecord, rebootFlag: " + rebootFlag);
            }
            //记录加一
            rebootTimes++;
            //表明是因为网络原因重启的
            rebootFlag = 1;
            //重新写回文件
            fos = new FileOutputStream(recordFile);
            String output = String.format(Locale.CHINA, outputFormatString, rebootTimes, rebootFlag);
            Log.d(TAG, "write to file: " + output);
            fos.write(output.getBytes());
            fos.flush();

        } catch (Exception e) {

        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {

            }
        }
    }

    /*
    *   对记录文件进行检查，如果发现不存在则自动创建并初始化
    *   如果存在则检查内容的合法性，如果内容不合法则重新写为初始值
    *   参数：reset 是否不管原值是多少，都直接恢复初始值
    *
    * */
    private static void rebootRecordFileCheck(boolean reset) {
        Log.d(TAG, "rebootRecordFileCheck reset: " + (reset ? "yes" : "no"));
        File recordFile;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        int rebootTimes = -1;
        int rebootFlag = -1;
        String content;

        try {
            recordFile = new File(Environment.getExternalStorageDirectory() +
                    File.separator + RebootRecordFileName);

            if (!recordFile.exists()) {
                //如果不存在则创建新文件，并且写入默认值
                Log.d(TAG, "rebootRecord file doesn't exist, create new file");
                recordFile.createNewFile();
                fos = new FileOutputStream(recordFile);
                fos.write(String.format(Locale.CHINA, outputFormatString, 0, 0).getBytes());
                fos.flush();
                fos.close();

            } else {
                if (reset) {
                    //进行清零
                    FileOutputStream resetFos = new FileOutputStream(recordFile);
                    resetFos.write(String.format(Locale.CHINA, outputFormatString, 0, 0).getBytes());
                    resetFos.flush();
                    resetFos.close();
                } else {
                    fis = new FileInputStream(recordFile);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis));
                    content = bufferedReader.readLine();
                    Log.d(TAG, "Reboot Record File content: " + content);
                    try {

                        Pattern p1 = Pattern.compile(inputPatterns);
                        Matcher matcher = p1.matcher(content);
                        if (matcher.matches()) {
                            rebootTimes = Integer.valueOf(matcher.group(1));
                            //Log.d(TAG, "rebootTimes: " + rebootTimes);
                            rebootFlag = Integer.valueOf(matcher.group(2));
                            //Log.d(TAG, "rebootFlag: " + rebootFlag);
                        }
                        if (rebootTimes < 0 || rebootTimes > RebootMaxTimes) {
                            throw new Exception("Invalid Reboot Times: " + rebootTimes);
                        }
                        if (rebootFlag != 0 && rebootFlag != 1) {
                            throw new Exception("Invalid Reboot Flag: " + rebootFlag);
                        }
                    } catch (Exception e) {
                        //如果此处发生异常，表明文件的内容有问题，需要重新写入默认值
                        Log.e(TAG, "Invalid reboot record file, gona to set default value", e);
                        e.printStackTrace();
                        FileOutputStream tmpfos = new FileOutputStream(recordFile);
                        tmpfos.write(String.format(Locale.CHINA, outputFormatString, 0, 0).getBytes());
                        tmpfos.flush();
                        tmpfos.close();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {
                Log.e(TAG, "close error!", ioe);
                ioe.printStackTrace();
            }
        }
    }









}
