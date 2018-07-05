package com.winfo.update.version_update.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.winfo.update.version_update.utils.NotificationHelper;
import com.winfo.update.version_update.utils.Constants;
import com.winfo.update.version_update.utils.NotificationBarUtil;
import com.winfo.update.download.httpdownload.DownInfo;
import com.winfo.update.download.httpdownload.HttpDownManager;
import com.winfo.update.download.listener.HttpProgressOnNextListener;
import com.winfo.update.version_update.utils.ApkUtils;
import com.winfo.update.version_update.utils.StorageUtils;

import java.io.File;

/**
 * 下载服务
 */
public class DownloadService extends Service {

    private DownInfo downInfo;
    private int oldProgress = 0;
    private NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        downInfo = new DownInfo();
        notificationHelper = new NotificationHelper(this);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        assert intent != null;
        String urlStr = intent.getStringExtra(Constants.APK_DOWNLOAD_URL);
        downInfo.setUrl(urlStr);
        File dir = StorageUtils.getExternalCacheCustomDirectory(this);
//        File dir = StorageUtils.getExternalCacheDirectory(this);
//        File dir = StorageUtils.getCacheDirectory(this);
        String apkName = urlStr.substring(urlStr.lastIndexOf("/") + 1, urlStr.length());
        File apkFile = new File(dir, apkName);
        downInfo.setSavePath(apkFile.getAbsolutePath());
        downLoadFile();
        return super.onStartCommand(intent, flags, startId);
    }


    private void downLoadFile() {
        HttpDownManager.getInstance().startDown(downInfo, new HttpProgressOnNextListener<DownInfo>() {
            @Override
            public void onNext(DownInfo downInfo) {
                //收起通知栏
                NotificationBarUtil.setNotificationBarVisibility(DownloadService.this, false);
                //安装
                ApkUtils.installAPk(DownloadService.this, new File(downInfo.getSavePath()));
            }

            @Override
            public void onComplete() {
                notificationHelper.cancel();
                stopSelf();
            }

            @Override
            public void updateProgress(long readLength, long countLength) {
                int progress = (int) (readLength * 100 / countLength);
                // 如果进度与之前进度相等，则不更新，如果更新太频繁，否则会造成界面卡顿
                if (progress != oldProgress) {
                    notificationHelper.updateProgress(progress);
                }
                oldProgress = progress;
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(DownloadService.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
