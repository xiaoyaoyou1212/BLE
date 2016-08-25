package com.vise.baseble.utils;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/**
 * @Description: Log工具，类似android.util.Log。
 * tag自动产生，格式: TAG:className.methodName(Line:lineNumber),
 * customTagPrefix为空时只输出：className.methodName(Line:lineNumber)。
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/5 20:43.
 */
public class BleLog {
    public static final String LINE_BREAK = "\r\n";
    private static final ThreadLocal<ReusableFormatter> thread_local_formatter = new ThreadLocal<ReusableFormatter>() {
        protected ReusableFormatter initialValue() {
            return new ReusableFormatter();
        }
    };
    public static boolean isAndroid = true;
    public static final String ROOT = Environment.getExternalStorageDirectory().getPath();// SD卡中的根目录
    public static final String PATH = "/vise/log";
    public static String TAG = "Bluetooth"; // 自定义Tag的前缀，可以是作者名

    public static boolean DEBUG = true;
    // 容许打印日志的类型，默认是true，设置为false则不打印
    public static boolean allowD = DEBUG && true;
    public static boolean allowE = DEBUG && true;
    public static boolean allowI = DEBUG && true;
    public static boolean allowV = DEBUG && true;
    public static boolean allowW = DEBUG && true;
    public static boolean allowWtf = DEBUG && true;
    private static String PATH_LOG_INFO;

    static {
        String os = System.getProperty("os.name");
        System.out.println("current os System is " + os);
        if (os.toLowerCase().contains("win") || os.toLowerCase().contains("mac")) {
            isAndroid = false;
        } else {
            PATH_LOG_INFO = ROOT + PATH;
            isAndroid = true;
        }
    }


    private static void loge(String tag, String content, Throwable tr) {
        if (isAndroid) {
            Log.e(tag, content, tr);
        } else {
            System.err.println(tag + "-" + content);
        }
    }

    private static void logd(String tag, String content) {
        if (isAndroid) {
            Log.d(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logw(String tag, String content) {
        if (isAndroid) {
            Log.w(tag, "" + content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logi(String tag, String content) {
        if (isAndroid) {
            Log.i(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logv(String tag, String content) {
        if (isAndroid) {
            Log.v(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logwtf(String tag, Throwable tr) {
        if (isAndroid) {
            Log.wtf(tag, tr);
        } else {
            if (tr != null) {
                System.out.println(tag + "-" + tr.getMessage());
            }
        }
    }

    private static void logwtf(String tag, String content) {
        if (isAndroid) {
            Log.wtf(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logwtf(String tag, String content, Throwable tr) {
        if (isAndroid) {
            Log.wtf(tag, content, tr);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    public static void v(String content) {
        if (!allowV)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logv(tag, content);
    }

    public static void v(String content, boolean isSaveLog) {
        if (!allowV)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logv(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void v(String uTag, String content) {
        if (!allowV) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logv(tag, content);
    }

    public static void v(String uTag, String content, boolean isSaveLog) {
        if (!allowV) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logv(tag, content);

        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void d(String content) {
        if (!allowD)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logd(tag, content);
    }

    public static void d(String content, boolean isSaveLog) {
        if (!allowD)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logd(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void d(String uTag, String content) {
        if (!allowD) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logd(tag, content);
    }

    public static void d(String uTag, String content, boolean isSaveLog) {
        if (!allowD) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logd(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void i(String content) {
        if (!allowI)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logi(tag, content);
    }

    public static void i(String content, boolean isSaveLog) {
        if (!allowI)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logi(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void i(String uTag, String content) {
        if (!allowI) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logi(tag, content);
    }

    public static void i(String uTag, String content, boolean isSaveLog) {
        if (!allowI) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logi(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void w(String content) {
        if (!allowW)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logw(tag, content);
    }

    public static void w(String content, boolean isSaveLog) {
        if (!allowW)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logw(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void w(String uTag, String content) {
        if (!allowW) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logw(tag, content);
    }

    public static void w(String uTag, String content, boolean isSaveLog) {
        if (!allowW) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logw(tag, content);

        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void e(String content) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        loge(tag, content, null);
    }

    public static void e(String content, boolean isSaveLog) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        loge(tag, content, null);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void e(String uTag, String content) {
        if (!allowE) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        loge(tag, content, null);
    }

    public static void e(String uTag, String content, boolean isSaveLog) {
        if (!allowE) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        loge(tag, content, null);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void e(String content, Throwable tr) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(tag, content, tr);
    }

    public static void e(String uTag, String content, Throwable tr) {
        if (!allowE)
            return;
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(tag, content, tr);
    }

    public static void e(String content, Throwable tr, boolean isSaveLog) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(tag, content, tr);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, getThrowable(tr, content));
        }
    }

    public static void e(Throwable tr, boolean isSaveLog) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        String content = getThrowable(tr, null);
        loge(tag, content, null);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    public static void e(Throwable tr) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        String content = getThrowable(tr, null);
        loge(tag, content, null);
    }

    public static void e(String uTag, String content, Throwable tr, boolean isSaveLog) {
        if (!allowE)
            return;
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        loge(tag, content, tr);

        String msg = getThrowable(tr, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, msg);
        }
    }

    public static void wtf(String content) {
        if (!allowWtf)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        logwtf(tag, content);
    }

    public static void wtf(String content, Throwable tr) {
        if (!allowWtf)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        logwtf(tag, content, tr);
    }

    public static void wtf(Throwable tr) {
        if (!allowWtf)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        logwtf(tag, tr);
    }

    private static StackTraceElement getCallerStackTraceElement() {
        return Thread.currentThread().getStackTrace()[4];
    }

    public static void point(String path, String tag, String msg) {
        if (isSDAva()) {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("",
                    Locale.SIMPLIFIED_CHINESE);
            dateFormat.applyPattern("yyyy");
            path = path + dateFormat.format(date) + "/";
            dateFormat.applyPattern("MM");
            path += dateFormat.format(date) + "/";
            dateFormat.applyPattern("dd");
            path += dateFormat.format(date) + ".log";
            dateFormat.applyPattern("[yyyy-MM-dd HH:mm:ss]");
            String time = dateFormat.format(date);
            File file = new File(path);
            if (!file.exists())
                createDipPath(path);
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file, true)));
                out.write(time + " " + tag + " " + msg + "\r\n");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 根据文件路径 递归创建文件
     *
     * @param file
     */
    public static void createDipPath(String file) {
        String parentFile = file.substring(0, file.lastIndexOf("/"));
        File file1 = new File(file);
        File parent = new File(parentFile);
        if (!file1.exists()) {
            parent.mkdirs();
            try {
                file1.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String format(String msg, Object... args) {
        ReusableFormatter formatter = thread_local_formatter.get();
        return formatter.format(msg, args);
    }

    public static boolean isSDAva() {
        if (isAndroid && Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)
                || Environment.getExternalStorageDirectory().exists()) {
            return true;
        } else {
            return false;
        }
    }

    private static String generateTag(StackTraceElement caller) {
        String tag = "(%s:%d).%s"; // 占位符
        String callerClazzName = caller.getFileName();
        tag = String.format(tag, callerClazzName, caller.getLineNumber(), caller.getMethodName()); // 替换
        tag = (TAG == null || "".equalsIgnoreCase(TAG)) ? tag : TAG + ":"
                + tag;
        return tag;
    }

    private static String getThrowable(Throwable throwable, String mag) {
        /* 打印异常 */
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(mag)) {
            sb.append(mag);
        }
        if (throwable != null) {
            sb.append(LINE_BREAK);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            sb.append(stringWriter.toString());
        }
        return sb.toString();
    }

    /**
     * A little trick to reuse a formatter in the same thread
     */
    private static class ReusableFormatter {

        private Formatter formatter;

        private StringBuilder builder;

        public ReusableFormatter() {
            builder = new StringBuilder();
            formatter = new Formatter(builder);
        }

        public String format(String msg, Object... args) {
            formatter.format(msg, args);
            String s = builder.toString();
            builder.setLength(0);
            return s;
        }

    }
}
