package com.winfo.update.version_update.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class AppUtils {

    /**
     * 获取当前应用的版本号
     * @param mContext context
     * @return 版本号
     */
    public static int getVersionCode(Context mContext) {
        if (mContext != null) {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return 0;
    }

    /**
     * 获取当前应用的版本名称
     * @param mContext context
     * @return 版本名称
     */
    public static String getVersionName(Context mContext) {
        if (mContext != null) {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        return "";
    }
}
