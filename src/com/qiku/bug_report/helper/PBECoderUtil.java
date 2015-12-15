package com.qiku.bug_report.helper;

import java.math.BigInteger;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.util.Log;

public class PBECoderUtil {
    static String tag = "BugReportPBECoderUtil";
    // public static final String DEFAULTSALT = "sn201209";
    public static final String DEFAULTSALT = "tr201503";
    public static final int ITERATIONSNUM = 50;
    public static final String ALGORITHM = "PBEWITHMD5andDES";

    public  static byte[] SART = DEFAULTSALT.getBytes();
    private static final int FIFTY = 50;

    private static Key toKey(String password) throws Exception {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);

        SecretKey secretKey = keyFactory.generateSecret(keySpec);

        return secretKey;
    }

    public static byte[] encrypt(byte[] data, String password, byte[] salt) throws Exception {

        Key key = toKey(password);

        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, FIFTY);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

        return cipher.doFinal(data);

    }

    public static String encrypty(String key, String content) throws Exception {
        byte[] data = null;
        byte[] input = content.trim().getBytes();
        data = encrypt(input, key.trim(), SART);
        return byte2hex(data);

    }

    public static String decrypty(String key, String content) throws Exception {
        byte[] input = PBECoderUtil.hex2byte(content);
        byte[] output = PBECoderUtil.decrypt(input, key, SART);
        String outputStr = new String(output);
        return outputStr;
    }

    public static byte[] decrypt(byte[] data, String password, byte[] salt) throws Exception {

        Key key = toKey(password);

        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, FIFTY);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

        return cipher.doFinal(data);

    }

    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";

        for (int n = 0; n < b.length; n++) {
            stmp = java.lang.Integer.toHexString(b[n] & 0XFF);

            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
                // hs = hs.concat("0").concat(stmp);
            } else {
                // hs = hs + stmp;
                hs = hs.concat(stmp);
            }
        }
        return hs.toUpperCase();
    }

    public static String getHexString(byte[] b) throws Exception {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public static byte[] getByteArray(String hexString) {
        return new BigInteger(hexString, 16).toByteArray();
    }

    public static byte[] hex2byte(String str) {
        if (str == null) {
            return null;
        }

        str = str.trim();
        int len = str.length();
        if (Math.abs(len) == 0 || Math.abs(len) % 2 == 1) {
            return null;
        }

        byte[] b = new byte[len / 2];

        try {
            for (int i = 0; i < str.length(); i += 2) {
                b[i / 2] = (byte) Integer.decode("0x" + str.substring(i, i + 2)).intValue();
            }

            return b;
        } catch (Exception e) {
            Log.e(tag, "hex2byte() error", e);
            return null;
        }
    }

}

