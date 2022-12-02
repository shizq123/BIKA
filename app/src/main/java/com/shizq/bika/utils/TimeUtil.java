package com.shizq.bika.utils;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

@SuppressLint("SimpleDateFormat")
public class TimeUtil {
    //反编译源码 从里面复制的
    public static String B(String str) {
        if (str == null||str.equals("")) {
            return "";
        }
        Calendar instance = Calendar.getInstance();
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            instance.setTime(simpleDateFormat.parse(str));
            Calendar instance2 = Calendar.getInstance();
            instance2.add(13, -60);
            if (instance2.compareTo(instance) <= 0) {
                instance2.add(13, 60);
                return ((instance2.getTimeInMillis() - instance.getTimeInMillis()) / 1000) + "秒前";
            }
            Calendar instance3 = Calendar.getInstance();
            instance3.add(12, -60);
            if (instance3.compareTo(instance) <= 0) {
                instance3.add(12, 60);
                return ((instance3.getTimeInMillis() - instance.getTimeInMillis()) / 60000) + "分钟前";
            }
            Calendar instance4 = Calendar.getInstance();
            if (instance4.get(1) == instance.get(1) && instance4.get(6) == instance.get(6)) {
                SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm");
                return "今天 " + simpleDateFormat2.format(instance.getTime());
            }
            Calendar instance5 = Calendar.getInstance();
            instance5.add(6, -1);
            if (instance5.get(1) == instance.get(1) && instance5.get(6) == instance.get(6)) {
                SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat("HH:mm");
                return "昨天 " + simpleDateFormat3.format(instance.getTime());
            } else if (Calendar.getInstance().get(1) == instance.get(1)) {
                SimpleDateFormat simpleDateFormat4 = new SimpleDateFormat("M月d日 HH:mm");
                return "" + simpleDateFormat4.format(instance.getTime());
            } else {
                SimpleDateFormat simpleDateFormat5 = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                return "" + simpleDateFormat5.format(instance.getTime());
            }
        } catch ( ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

}

