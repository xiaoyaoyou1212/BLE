package com.vise.bledemo.common;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vise.bledemo.R;
import com.vise.utils.view.ViewUtil;

public class ToastUtil {
    private static final long DROP_DUPLICATE_TOAST_TS = 2 * 1000; // 2s

    private static String sLast = "";

    private static long sLastTs = 0;

    private static Toast mBasicToast = null;

    public enum CommonToastType {
        TOAST_TYPE_NORMAL, // 普通toast-笑脸
        TOAST_TYPE_SUC, // 成功toast-笑脸
        TOAST_TYPE_ACC, // 加速toast-笑脸
        TOAST_TYPE_SMILE, // 微笑toast-笑脸
        TOAST_TYPE_ALARM// 失败or警告toast-哭脸
    }

    /**
     * 上下文.
     */
    private static Context mContext = null;

    /**
     * 显示Toast.
     */
    public static final int SHOW_TOAST = 0;
    /**
     * 主要Handler类，在线程中可用
     * what：0.提示文本信息
     */
    private static Handler baseHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_TOAST:
                    showToast(mContext, msg.getData().getString("TEXT"));
                    break;
                default:
                    break;
            }
        }
    };


    public static void showToast(Context context, int strRes) {
        showToast(context, context.getResources().getString(strRes));
    }

    public static void showShortToast(Context context, String prompt) {
        showToast(context, prompt);

    }

    public static void showLongToast(Context context, String prompt) {
        showToast(context, prompt);
    }

    public static Toast makeText(Context context, CharSequence text, int duration) {
        mContext = context.getApplicationContext();
        Toast result = new Toast(context);

        LayoutInflater inflate = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.common_layout_toast, null);
        TextView tv = (TextView) v.findViewById(R.id.tvToast);
        tv.setText(text);

        result.setView(v);
        result.setGravity(Gravity.BOTTOM, 0, 163);
        result.setDuration(duration);

        return result;
    }

    public static synchronized void showToast(Context context, String str) {
        showToast(context, CommonToastType.TOAST_TYPE_NORMAL, str);
    }

    /**
     * toast（带动画）-显示时间2S
     *
     * @param context
     * @param type
     * @param str
     */
    public static synchronized void showToast(Context context, CommonToastType type, String str) {
        try {
            mContext = context.getApplicationContext();
            long newTs = System.currentTimeMillis();
            if (str != null
                    && (!str.equals(sLast) || newTs < sLastTs || (newTs - sLastTs) > DROP_DUPLICATE_TOAST_TS)) {
                sLast = str;
                sLastTs = newTs;
                if (mBasicToast == null) {
                    mBasicToast = new Toast(context);
                }
                View toastView = LayoutInflater.from(context).inflate(
                        R.layout.common_layout_toast, null);
                TextView txt = (TextView) toastView.findViewById(R.id.tvToast);
                txt.setText(str);

                mBasicToast.setView(toastView);
                int px = (int) ViewUtil.dip2px(context, 60);
                mBasicToast.setGravity(Gravity.BOTTOM, 0, px);
                mBasicToast.setDuration(Toast.LENGTH_SHORT);// 默认只显示2S
                mBasicToast.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 描述：在线程中提示文本信息.
     *
     * @param resId 要提示的字符串资源ID，消息what值为0,
     */
    public static void showToastInThread(Context context, int resId) {
        mContext = context.getApplicationContext();
        Message msg = baseHandler.obtainMessage(SHOW_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("TEXT", context.getResources().getString(resId));
        msg.setData(bundle);
        baseHandler.sendMessage(msg);
    }

    /**
     * 描述：在线程中提示文本信息.
     *
     * @param context
     * @param text
     */
    public static void showToastInThread(Context context, String text) {
        mContext = context.getApplicationContext();
        Message msg = baseHandler.obtainMessage(SHOW_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("TEXT", text);
        msg.setData(bundle);
        baseHandler.sendMessage(msg);
    }

    /**
     * 让Toast立马消失
     */
    public static void cancelToast() {
        if (mBasicToast != null) {
            mBasicToast.cancel();
        }
    }
}
