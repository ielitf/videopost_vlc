package com.ceiv;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SimpleTimeWheel {

    //小时指针  一周共168小时  0 - 167
    private static int HourPointer;
    //挂载在时轮上的任务
    private static ArrayList<ArrayList<HourItem>> HourSlot;
    //分指针    1小时60分钟 0 - 59
    private static int MinutePointer;
    //挂载在分轮的任务
    private static ArrayList<ArrayList<MinuteItem>> MinuteSlot;
    //秒指针    1小时60秒钟 0 - 59
    private static int SecondPointer;
    //挂载在秒轮的任务
    private static ArrayList<ArrayList<TimeWheelItem>> SecondSlot;

    //临时变量
    private static ArrayList<TimeWheelItem> tmpTaskList;

    private static TimerTask internalTimerTask = new TimerTask() {
        @Override
        public void run() {
            synchronized (timeLock) {
                SecondPointer++;
                if (SecondPointer > 59) {
                    SecondPointer = 0;
                    MinutePointer++;
                    if (MinutePointer > 59) {
                        MinutePointer = 0;
                        HourPointer++;
                        if (HourPointer > 167) {
                            HourPointer = 0;
                        }
                        //查看时针上是否有任务需要移动到分针上
                        if (HourSlot.get(HourPointer).size() > 0) {
                            for (HourItem tmp : HourSlot.get(HourPointer)) {
                                MinuteSlot.get(tmp.getMinuteLeft()).add(tmp.getMinuteItem());
                            }
                            HourSlot.get(HourPointer).clear();
                        }
                    }
                    //查看是否分针上是否有任务需要移动到秒针上
                    if (MinuteSlot.get(MinutePointer).size() > 0) {
                        for (MinuteItem tmp : MinuteSlot.get(MinutePointer)) {
                            SecondSlot.get(tmp.getSecondLeft()).add(tmp.getTimeWheelItem());
                        }
                        MinuteSlot.get(MinutePointer).clear();
                    }
                }
                //查看是否有任务超时并执行之
                if (SecondSlot.get(SecondPointer).size() > 0) {
                    //不直接在这里调用回调函数是考虑到可能回调函数用的时间可能较长，而我们这里是加了锁的
                    tmpTaskList = new ArrayList<TimeWheelItem>();
                    tmpTaskList.addAll(SecondSlot.get(SecondPointer));
                    SecondSlot.get(SecondPointer).clear();
                }
            }
            if (null != tmpTaskList) {
                for (TimeWheelItem tmp : tmpTaskList) {
                    tmp.getTask().timeOut(tmp.getArgs());
                }
                tmpTaskList.clear();
                tmpTaskList = null;
            }

        }
    };

    //时间轮锁
    private final static Object timeLock;

    //在该类被调用时就开始计时
    static {
        tmpTaskList = null;
        //初始化指针数据
        SecondPointer = 0;
        MinutePointer = 0;
        HourPointer = 0;
        //考虑到java没有泛型数组，这里使用下面的数据结构
        //分配槽空间
        SecondSlot = new ArrayList<ArrayList<TimeWheelItem>>();
        MinuteSlot = new ArrayList<ArrayList<MinuteItem>>();
        HourSlot = new ArrayList<ArrayList<HourItem>>();
        for (int i = 0; i < 60; i++) {
            SecondSlot.add(new ArrayList<TimeWheelItem>());
        }
        for (int i = 0; i < 60; i++) {
            MinuteSlot.add(new ArrayList<MinuteItem>());
        }
        for (int i = 0; i < 168; i++) {
            HourSlot.add(new ArrayList<HourItem>());
        }
        timeLock = new Object();
        //一秒调用一次
        new Timer().scheduleAtFixedRate(internalTimerTask, 1000, 1000);
    }

    public interface TimeWheelTask {
        public void timeOut(Object args);
    }

    private static class TimeWheelItem {
        private Object args;
        private TimeWheelTask task;

        private TimeWheelItem(TimeWheelTask task, Object args) {
            this.task = task;
            this.args = args;
        }

        public Object getArgs() {
            return args;
        }

        public TimeWheelTask getTask() {
            return task;
        }
    }

    private static class MinuteItem {
        TimeWheelItem timeWheelItem;
        int secondLeft;
        private MinuteItem(TimeWheelItem timeWheelItem, int secondLeft) {
            this.timeWheelItem = timeWheelItem;
            this.secondLeft = secondLeft;
        }

        public TimeWheelItem getTimeWheelItem() {
            return timeWheelItem;
        }

        public int getSecondLeft() {
            return secondLeft;
        }
    }

    private static class HourItem {
        MinuteItem minuteItem;
        int minuteLeft;

        private HourItem(MinuteItem minuteItem, int minuteLeft) {
            this.minuteItem = minuteItem;
            this.minuteLeft = minuteLeft;
        }

        public MinuteItem getMinuteItem() {
            return minuteItem;
        }

        public int getMinuteLeft() {
            return minuteLeft;
        }
    }

    /*
    *   设置倒计时任务，最长时间一周左右，不能进行可能会阻塞的任务
    *   seconds     多少秒后执行任务
    *   task        执行的任务
    *   args        执行任务使用的参数
    * */
    public static void setTimeWheelItem(int seconds, TimeWheelTask task, Object args) throws Exception {

        if (seconds <= 0 || task == null) {
            throw new Exception("Invalid parameters!");
        }
        if (seconds > 604800) {
            throw new Exception("Time too long!");
        }

        int tmpSecondsIndex = 0;
        int tmpMinutesIndex = 0;
        int tmpHoursIndex = 0;
        int tmpSeconds = seconds % 60;
        int tmpMinutes = (seconds / 60) % 60;
        int tmpHours = seconds / 3600;

        synchronized (timeLock) {
            if (tmpHours == 0) {
                if (tmpMinutes == 0) {
                    tmpSecondsIndex = SecondPointer + tmpSeconds;
                    if (tmpSecondsIndex > 59) {
                        tmpSecondsIndex -= 60;
                    }
                    SecondSlot.get(tmpSecondsIndex).add(new TimeWheelItem(task, args));
                } else {
                    tmpMinutesIndex = MinutePointer + tmpMinutes;
                    if (tmpMinutesIndex > 59) {
                        tmpMinutesIndex -= 60;
                    }
                    MinuteSlot.get(tmpMinutesIndex).add(
                            new MinuteItem(new TimeWheelItem(task, args), tmpSeconds));
                }
            } else {
                tmpHoursIndex = HourPointer + tmpHours;
                if (tmpHoursIndex > 167) {
                    tmpHoursIndex -= 168;
                }
                HourSlot.get(tmpHoursIndex).add(
                        new HourItem(new MinuteItem(
                                new TimeWheelItem(task, args), tmpMinutes), tmpSeconds));
            }
        }
    }






}
