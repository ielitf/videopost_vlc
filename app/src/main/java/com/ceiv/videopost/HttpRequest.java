package com.ceiv.videopost;

import android.os.Environment;
import android.os.Handler;
//import android.util.Log;
import com.ceiv.log4j.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.ceiv.BrtUtils.BrtInfoUtils;
import com.ceiv.BrtUtils.RouteInfo;
import com.ceiv.videopost.StationInfo;
import com.ceiv.videopost.StationInfo.StationInfoItem;
/**
 * Created by zhangdawei on 2018/8/7.
 */

public class HttpRequest {
    private final static String TAG = "HttpRequest";

    //private final String TIME_SERVER = "http://124.227.197.82:1001/BusService/Query_ServerTime";
    private final String TIME_SERVER = "http://172.16.10.105:1001/BusService/Query_ServerTime";
    private final String WEATHER_SERVER = "http://wthrcdn.etouch.cn/WeatherApi?citykey=";

    private RequestCallBack callBack;
    List<String> listname=new ArrayList<>();
    public final static int TYPE_TIME = 0x01;
    public final static int TYPE_WEATHER = 0x02;
    public final static int TYPE_STATION = 0x03;

    public HttpRequest(RequestCallBack callBack) {
        this.callBack = callBack;
    }

    public Handler dateHandler;

    public class WeatherInfo {
        public String cityName;
        public String weather;
        public String windDir;
        public String windLevel;
        public int tempCur;
        public int tempHigh;
        public int tempLow;
        private int infoFlag;       //bit0:cityName, bit1:weather, bit2:windDir, bit3:windLevel, bit4:tmpCur, bit5: tmpHigh, bit6:tmpLow
        public WeatherInfo() {
            this.infoFlag = 0x0;
        }
    }

    public interface RequestCallBack {
        public void requestTime(String respone, boolean success);

        public void requestWeather(String respone, boolean success);

        public void requestRouteInfo(RouteInfo routeInfo, boolean success);
    }

    public void requestTimeInfo() {
        new Thread(new RequestTimeInfo(callBack, TIME_SERVER)).start();
    }

    public void requestWeatherInfo(String cityCode) {
        new Thread(new RequestWeatherInfo(callBack, WEATHER_SERVER, cityCode)).start();
    }

    public void requestRouteInfo(String server, String RouteID, String secretKey) {
        new Thread(new RequestRouteInfo(callBack, server, RouteID, secretKey)).start();
    }

    private class RequestTimeInfo implements Runnable {

        private RequestCallBack requestCallBack;
        private String timeServer;

        public RequestTimeInfo(RequestCallBack callBack, String timeServer) {
            this.timeServer = timeServer;
            requestCallBack = callBack;
        }

        @Override
        public void run() {

            try {
                URL url = new URL(timeServer);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(8000);
                urlConnection.setReadTimeout(8000);
                InputStream in = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuffer sb = new StringBuffer();
                String str;
                while ((str = reader.readLine()) != null) {
                    sb.append(str);
                    Log.d(TAG, "data from url: " + str);
                }
                String respone = sb.toString();

                Log.d(TAG, "respone: " + respone);
                urlConnection.disconnect();
                String time=parseJsonTime(respone);
                requestCallBack.requestTime(time, true);


            } catch (IOException ioException) {
                Log.e(TAG, " failed!", ioException);
                //ioException.printStackTrace();
                requestCallBack.requestTime(null, false);
            }
        }
    };

    private class RequestWeatherInfo implements Runnable {

        private RequestCallBack requestCallBack;
        private String weatherServer;
        private String cityCode;

        public RequestWeatherInfo(RequestCallBack callBack, String weatherServer, String cityCode) {
            this.weatherServer = weatherServer;
            requestCallBack = callBack;
            this.cityCode = cityCode;
        }

        @Override
        public void run() {

            try {
                URL url = new URL(weatherServer + cityCode);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(8000);
                urlConnection.setReadTimeout(8000);
                InputStream in = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuffer sb = new StringBuffer();
                String str;
                while ((str = reader.readLine()) != null) {
                    sb.append(str);
                    Log.d(TAG, "data from url: " + str);
                }
                String respone = sb.toString();
                Log.d(TAG, "respone: " + respone);
                urlConnection.disconnect();

                WeatherInfo weatherInfo = parseXML(respone);

                requestCallBack.requestWeather(  weatherInfo.tempLow + "~" + weatherInfo.tempHigh + "°C", true);

            } catch (IOException ioException) {
                Log.e(TAG, "get Weather Info failed!", ioException);
                //ioException.printStackTrace();
                requestCallBack.requestWeather(null, false);
            }
        }
    };


    private class RequestRouteInfo implements Runnable {

        private String reqRouteID;
        private String server;
        private String secretKey;
        private RequestCallBack requestCallBack;

        public RequestRouteInfo(RequestCallBack callBack, String server, String RouteID, String secretKey) {
            this.server = server;
            this.reqRouteID = RouteID;
            this.secretKey = secretKey;
            requestCallBack = callBack;
        }

        @Override
        public void run() {

            RouteInfo routeInfo = null;
            BufferedReader reader = null;
            InputStream inputStream = null;
            HttpURLConnection urlConnection = null;
            boolean readSuccess = false;
            StationInfo stationInfo = null;
            Log.d("request_debug", "thread run, RouteID:" + reqRouteID);
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
                String timeStamp = df.format(new Date());
                Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
//                int day = 14;
//                int week = 2;
                int delta = day - week;
                if(delta <= 10) {
                    delta += 7;
                }
//                String timeStamp = "20180904135900";

                String randomValue = "" + (int) (100 + Math.random() * (999 - 100 + 1));
                String signKey = SignUp(secretKey, timeStamp + randomValue);
                if (null == signKey) {
                    return;
                }
                char[] charRouteID = reqRouteID.toCharArray();
                for(int i = 0; i < charRouteID.length; i++) {
                    charRouteID[i] += delta;
                }
                String RouteID = String.copyValueOf(charRouteID);

                URL url = new URL(server + "/BusService/Query_RouteStatData/?RouteID=" + RouteID + "&timeStamp=" +
                        timeStamp + "&Random=" + randomValue + "&SignKey=" + signKey);
                Log.d(TAG, "StationInfo HttpRequest URL: " + server + "/BusService/Query_RouteStatData/?RouteID=" + RouteID + "&timeStamp=" +
                        timeStamp + "&Random=" + randomValue + "&SignKey=" + signKey);
                urlConnection = (HttpURLConnection) url.openConnection();
                /* optional request header */
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                /* optional request header */
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(8000);
                urlConnection.setReadTimeout(8000);
                int statusCode = urlConnection.getResponseCode();

                if(statusCode == HttpURLConnection.HTTP_OK){
                    inputStream = urlConnection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    routeInfo = BrtInfoUtils.parseJsonRouteInfo(reader.readLine());
                    if (routeInfo != null) {
                        readSuccess = true;
                    }
                }
            } catch (Exception e) {
                readSuccess = false;
                Log.e(TAG, "Request StationInfo error!", e);
                //e.printStackTrace();
            } finally {
                try {
                    //关闭流和连接
                    if (reader != null) {
                        reader.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                } catch (IOException e) {
                    readSuccess = false;
                    Log.e(TAG, "close resource error!", e);
                    //e.printStackTrace();
                }
            }
            if (readSuccess) {
                requestCallBack.requestRouteInfo(routeInfo, true);
            } else {
                requestCallBack.requestRouteInfo(null, false);
            }
        }
    }

    private String SignUp(String secretKey, String plain)
    {
        byte[] keyBytes = secretKey.getBytes();
        byte[] plainBytes = plain.getBytes();

        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");

            SecretKeySpec secret_key = new SecretKeySpec(keyBytes, "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hashs = sha256_HMAC.doFinal(plainBytes);
            StringBuilder sb = new StringBuilder();
            for (byte x : hashs) {
                String b = Integer.toHexString(x & 0xff);
                if (b.length() == 1) {
                    b = '0' + b;
                }
                sb.append(b);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException nogorException) {
            Log.e(TAG, "can't find request algorithm!", nogorException);
            //nogorException.printStackTrace();
        } catch (InvalidKeyException invalidKeyException) {
            Log.e(TAG, "invalid key exception!", invalidKeyException);
            //invalidKeyException.printStackTrace();
        }
        return null;
    }

    private WeatherInfo parseXML(String xmlData) {

        WeatherInfo weatherInfo = new WeatherInfo();

        String regEX = "[^0-9]";
        Pattern p = Pattern.compile(regEX);

        boolean findForecast = false;
        boolean findTodayInfo = false;

        String dayOrNight;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 18) {
            dayOrNight = "day";
        } else {
            dayOrNight = "night";
        }
        Log.d(TAG, "当前时间是：" + dayOrNight);

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(new StringReader(xmlData));

            int eventType = xmlPullParser.getEventType();
            Log.d(TAG, "start parse xml");

            while (eventType != xmlPullParser.END_DOCUMENT && weatherInfo.infoFlag != 0x7f) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("city")) {
                            weatherInfo.cityName = xmlPullParser.nextText();
                            if ("".equals(weatherInfo.cityName)) {
                                Log.d(TAG, "获取的城市名称出错");
                                return null;
                            } else {
                                Log.d(TAG, "查询的城市为：" + weatherInfo.cityName);
                                weatherInfo.infoFlag |= 0x01;
                            }
                        } else if (xmlPullParser.getName().equals("wendu")) {
                            weatherInfo.tempCur = Integer.valueOf(xmlPullParser.nextText());
                            Log.d(TAG, "当前温度为：" + weatherInfo.tempCur);
                            weatherInfo.infoFlag |= 0x10;
                            if (weatherInfo.tempCur > 50.0 || weatherInfo.tempCur < -20) {
                                Log.d(TAG, "获取的温度值异常：" + weatherInfo.tempCur);
                                return null;
                            }
                        } else if (xmlPullParser.getName().equals("fengli")) {
                            weatherInfo.windLevel = xmlPullParser.nextText();
                            weatherInfo.infoFlag |= 0x08;
                            Log.d(TAG, "风力：" + weatherInfo.windLevel);
                        } else if (xmlPullParser.getName().equals("fengxiang")) {
                            weatherInfo.windDir = xmlPullParser.nextText();
                            weatherInfo.infoFlag |= 0x04;
                            Log.d(TAG, "风向：" + weatherInfo.windDir);
                        } else if (xmlPullParser.getName().equals("forecast")) {
                            Log.d(TAG, "get forecast info");
                            findForecast = true;
                        } else if (xmlPullParser.getName().equals("weather")) {
                            //在“forecast”里面第一个“weather”信息就是今天的信息
                            if (findForecast) {
                                findTodayInfo = true;
                            }
                        } else if (xmlPullParser.getName().equals("low")) {
                            if (findTodayInfo) {
                                String tmp = xmlPullParser.nextText();
                                Log.d(TAG, "<low>" + tmp + "</low>");
                                Matcher m = p.matcher(tmp);
                                weatherInfo.tempLow = Integer.valueOf(m.replaceAll("").trim());
                                weatherInfo.infoFlag |= 0x40;
                                if (weatherInfo.tempLow < -50 || weatherInfo.tempLow > 70) {
                                    Log.e(TAG, "获取的最低温度值异常：" + weatherInfo.tempLow);
                                    return null;
                                }
                            }
                        } else if (xmlPullParser.getName().equals("high")) {
                            if (findTodayInfo) {
                                String tmp = xmlPullParser.nextText();
                                Log.d(TAG, "<high>" + tmp + "</high>");
                                Matcher m = p.matcher(tmp);
                                weatherInfo.tempHigh = Integer.valueOf(m.replaceAll("").trim());
                                weatherInfo.infoFlag |= 0x20;
                                if (weatherInfo.tempHigh < -50 || weatherInfo.tempHigh > 70) {
                                    Log.e(TAG, "获取的最高温度值异常：" + weatherInfo.tempHigh);
                                    return null;
                                }
                            }
                        } else if (xmlPullParser.getName().equals("day") || xmlPullParser.getName().equals("night")){
                            if (findTodayInfo) {
                                if (dayOrNight.equals(xmlPullParser.getName())) {
                                    eventType = xmlPullParser.next();
                                    if (xmlPullParser.getName().equals("type")) {
                                        weatherInfo.weather = xmlPullParser.nextText();
                                        Log.d(TAG, "天气：" + weatherInfo.weather);
                                        weatherInfo.infoFlag |= 0x02;
                                    } else {
                                        Log.d(TAG, "<day> or <night>标签后面没有<type>标签！！！");
                                        return null;
                                    }
                                }
                            }
                        } else {

                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    default:
                        //其他的标志直接跳过
                        break;
                }
                eventType = xmlPullParser.next();
            }
            if (weatherInfo.infoFlag != 0x7f) {
                Log.e(TAG, "获取的天气信息不完整！");
                return null;
            } else if (weatherInfo.tempLow > weatherInfo.tempHigh) {
                Log.e(TAG, "最低气温大于最高气温！");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "parseXML 异常！", e);
            //e.printStackTrace();
        }
        Log.d(TAG, "get weather info finished: city: " + weatherInfo.cityName +
                " type: " + weatherInfo.weather + " wind dir: " + weatherInfo.windDir +
                " wind level: " + weatherInfo.windLevel + " temp cur: " + weatherInfo.tempCur +
                " temp low: " + weatherInfo.tempLow + " temp high: " + weatherInfo.tempHigh);
        return weatherInfo;
    }
    private void parseJSONWithJSONObject(String jsonData) {
        try {
            JSONArray array = new JSONArray(jsonData);
            for(int i = 0;i<array.length();i++){
                JSONObject object = array.getJSONObject(i);
                String PicAddress = object.getString("PicAddress");
                Log.d("main",PicAddress);
                listname.add(PicAddress);
            }


        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private String parseJsonTime(String jsonData) {
        try {
            JSONObject jsonObject=new JSONObject(jsonData);
            String time=jsonObject.get("ErrorInfo").toString();
            System.out.println(time);
            return time;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
