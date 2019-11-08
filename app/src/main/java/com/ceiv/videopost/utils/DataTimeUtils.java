package com.ceiv.videopost.utils;

import java.text.ParseException;

import java.text.SimpleDateFormat;

import java.util.Calendar;

import java.util.Date;

import java.util.Locale;

public class DataTimeUtils {

    private static DataTimeUtils instance;

    private DataTimeUtils() {
    }
    public static DataTimeUtils getInstance() {
        if (instance == null) {
            instance = new DataTimeUtils();
        }
        return instance;
    }

    //将日期转化为毫秒数
    public long transDataToTime(String data) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        long time  = 0;
        try {
            time = formatter.parse(data).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    //将时间毫秒数转换为：yyyy/MM/dd HH:mm:ss格式的
    public String transTimeToData(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date(time);
        String curDate = formatter.format(date);
        return curDate;
    }
    //将时间毫秒数转换为：时分
    public String transTimeToHHMM(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        Date date = new Date(time);
        String curDate = formatter.format(date);
        return curDate;
    }

    //将时间毫秒数转换为：月日
    public String transTimeToMMDD(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");
        Date date = new Date(time);
        String curDate = formatter.format(date);
        return curDate;
    }

    //将时间毫秒数转换为：年月日

    public String transTimeToYYMMDD(long time) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        Date date = new Date(time);

        String curDate = formatter.format(date);

        return curDate;

    }



    //获取系统当前日期时间

    public String getCurrentDateTime() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");

        Date date = new Date(System.currentTimeMillis());

        String curDate = formatter.format(date);

        return curDate;

    }





    //获取系统当前日期(月日)

    public String getCurrentDateMMDD() {

        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");

        Date date = new Date(System.currentTimeMillis());

        String curDate = formatter.format(date);

        return curDate;

    }

    //获取系统当前日期(年月日)

    public String getCurrentDateYYMMDD() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

        Date date = new Date(System.currentTimeMillis());

        String curDate = formatter.format(date);

        return curDate;

    }





    //获取系统当前日期(英文格式)

    public String getCurrentDateEnglish() {

        SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);

        Date date = new Date(System.currentTimeMillis());

        String curDate = formatter.format(date);

        return curDate;

    }





    //获取系统当前时间

    public String getCurrentTime() {

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

        Date date = new Date(System.currentTimeMillis());

        String curDate = formatter.format(date);

        return curDate;

    }





    //获取系统当前时间

    public String getCurrentTimeHHMM() {

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");

        Date date = new Date(System.currentTimeMillis());

        String curDate = formatter.format(date);

        return curDate;

    }





    //获取系统当前是星期几

    public String getCurrentWeekDay(int type) {

        String week = "";

        Calendar c1 = Calendar.getInstance();

        int day = c1.get(Calendar.DAY_OF_WEEK);

        if (type == 2) {

            switch (day) {

                case 1:

                    week = "Sunday";

                    break;

                case 2:

                    week = "Monday";

                    break;

                case 3:

                    week = "Tuesdays";

                    break;

                case 4:

                    week = "Wednesday";

                    break;

                case 5:

                    week = "Thursday";

                    break;

                case 6:

                    week = "Fridays";

                    break;

                case 7:

                    week = "Saturday";

                    break;





            }

        } else {

            switch (day) {

                case 1:

                    week = "星期日";

                    break;

                case 2:

                    week = "星期一";

                    break;

                case 3:

                    week = "星期二";

                    break;

                case 4:

                    week = "星期三";

                    break;

                case 5:

                    week = "星期四";

                    break;

                case 6:

                    week = "星期五";

                    break;

                case 7:

                    week = "星期六";

                    break;



            }

        }





        return week;

    }

}
