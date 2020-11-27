package com.zx.encryptstack.utils;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Zhenxi on 2020-11-27
 */
public class GzipUtils {
    /**
     * Gzip 压缩数据
     *
     * @param unGzipStr
     * @return
     */
//    public static String compressForGzip(String unGzipStr) {
//
//        if (TextUtils.isEmpty(unGzipStr)) {
//            return null;
//        }
//        try {
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            GZIPOutputStream gzip = new GZIPOutputStream(baos);
//            gzip.write(unGzipStr.getBytes());
//            gzip.close();
//            byte[] encode = baos.toByteArray();
//            baos.flush();
//            baos.close();
//            return Base64Encoder.encode(encode);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//
//    /**
//     * Gzip解压数据
//     *
//     * @param gzipStr
//     * @return
//     */
//    public static String decompressForGzip(String gzipStr) {
//        if (TextUtils.isEmpty(gzipStr)) {
//            return null;
//        }
//        byte[] t = Base64Decoder.decodeToBytes(gzipStr);
//        try {
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            ByteArrayInputStream in = new ByteArrayInputStream(t);
//            GZIPInputStream gzip = new GZIPInputStream(in);
//            byte[] buffer = new byte[BUFFERSIZE];
//            int n = 0;
//            while ((n = gzip.read(buffer, 0, buffer.length)) > 0) {
//                out.write(buffer, 0, n);
//            }
//            gzip.close();
//            in.close();
//            out.close();
//            return out.toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}
