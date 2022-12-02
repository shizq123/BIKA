package com.shizq.bika.network;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by shizq on 2020/7/30.
 * 反编译源码
 */
public class HmacSHA256Util {
    protected static final char[] uq = "0123456789abcdef".toCharArray();
    String uo;

    public static String a(byte[] bArr) {
        char[] var4 = new char[bArr.length * 2];
        for(int i = 0; i < bArr.length; i++) {
            int var2 = bArr[i] & 255;
            var4[i * 2] = (char)uq[var2 >>> 4];
            var4[i * 2 + 1] = (char)uq[var2 & 15];
        }
        return new String(var4);
    }

    public synchronized String C(String str){
        String keyString = "~d}$Q7$eIni=V)9\\RK/P.RM4;9[7|@/CA}b~OW!3?EV`:<>M7pddUBL5n|0/*Cn";
        byte[] keybyte;
        keybyte = keyString.getBytes(StandardCharsets.UTF_8);
        String lowerCase = str.toLowerCase();
        uo = a(lowerCase, keybyte);
        return uo;
    }

    protected String a(String str, byte[] var2) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(var2, "HmacSHA256"));
            str = a(mac.doFinal(str.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
            str = null;
        }
        return str;
    }
}
