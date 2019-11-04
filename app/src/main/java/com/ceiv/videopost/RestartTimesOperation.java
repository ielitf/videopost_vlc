package com.ceiv.videopost;

import android.os.Environment;
//import android.util.Log;
import com.ceiv.log4j.Log;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class RestartTimesOperation {
    private final static String TAG = "RestartTimesOperation";
    private final static String RestartTimesInfoFile = "RestartTimes.xml";
    public static JSONObject readRestartTimesFile() throws IOException{
        File RestartTimesFile = new File(Environment.getExternalStorageDirectory() + "/" + RestartTimesInfoFile);
        // 创建SAXReader对象
        SAXReader sr = new SAXReader(); // 需要导入jar包:dom4j
        // 关联xml
        try {
            Document document = sr.read(RestartTimesFile);
            // 获取根元素
            Element root = document.getRootElement();
            Element times = root.element("times");
            Element flg = root.element("flg");
            Log.e(TAG, "Open RestartTimesInfoFile file successed!");
            JSONObject json = new JSONObject();
            try {
                json.put("times",times.getText());
                json.put("flg",flg.getText());
                Log.e(TAG, "Read RestartTimesInfoFile file successed!");
                Log.e(TAG, String.valueOf(json));
                return  json;
            } catch (JSONException e) {
                e.printStackTrace();
                return  null;
            }
        } catch (DocumentException e) {
            Log.e(TAG, "Open RestartTimesInfoFile file failed!");
            e.printStackTrace();
            return  null;
        }
    }
    public static boolean writeRestartTimesFile(JSONObject json) throws IOException, JSONException {
        File RestartTimesFile = new File(Environment.getExternalStorageDirectory() + "/" + RestartTimesInfoFile);
        // 创建SAXReader对象
        SAXReader sr = new SAXReader(); // 需要导入jar包:dom4j
        // 关联xml
        try {
            Document document = sr.read(RestartTimesFile);
            // 获取根元素
            Element root = document.getRootElement();
            Element times = root.element("times");
            Log.e(TAG, String.valueOf(json));
            Log.e(TAG, String.valueOf(String.valueOf(json.getString("times"))));
            Log.e(TAG, String.valueOf(String.valueOf(json.getString("flg"))));
            times.setText(String.valueOf(json.getString("times")));
            Element flg = root.element("flg");
            flg.setText(String.valueOf(json.getString("flg")));
            // 调用下面的静态方法完成xml的写出
            saveDocument(document,RestartTimesFile);
            return true;
        } catch (DocumentException e) {
            Log.e(TAG, "Read RestartTimesInfoFile file failed!");
            e.printStackTrace();
            return false;
        }
    }
    public static boolean checkRestartTimesFile() throws IOException{
        File RestartTimesFile = new File(Environment.getExternalStorageDirectory() + "/" + RestartTimesInfoFile);
        if (!RestartTimesFile.exists()) {
            try {
                // 生成xml的第一行 <?xml version="1.0" encoding="UTF-8"?>
                Document document = DocumentHelper.createDocument();
                // 添加一个元素,作为根元素students
                Element root = document.addElement("device");
//                // 在root标签里添加属性
////                root.addAttribute("times", "times001");
                Element times = root.addElement("times");
                times.setText("0");
                Element flg = root.addElement("flg");
                flg.setText("0");
                // 调用下面的静态方法完成xml的写出
                saveDocument(document,RestartTimesFile);
                return  true;
            } catch (IOException e) {
                Log.e(TAG, "Create RestartTimesInfoFile file failed!");
                e.printStackTrace();
                return  false;
            }
        }
        else {
            return  true;
        }
    }
    // 下面的为固定代码---------可以完成java对XML的写,改等操作
    public static void saveDocument(Document document, File outputXml) throws IOException{
        try {
            // 美化格式
            OutputFormat format = OutputFormat.createPrettyPrint();
           /*// 缩减格式
           OutputFormat format = OutputFormat.createCompactFormat();*/
           /*// 指定XML编码
            format.setEncoding("GBK");*/
            XMLWriter output = new XMLWriter(new FileWriter(outputXml), format);
            output.write(document);
            output.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
