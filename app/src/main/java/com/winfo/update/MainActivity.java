package com.winfo.update;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.winfo.update.download.httpdownload.DownInfo;
import com.winfo.update.download.httpdownload.DownState;
import com.winfo.update.download.httpdownload.HttpDownManager;
import com.winfo.update.download.listener.HttpProgressOnNextListener;
import com.winfo.update.version_update.utils.UpdateChecker;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressBar qqProgressBar, alipayProgressBar, weixinProgressbar;
    private TextView tvQQMsg, tvAlipayMsg, tvWeixinMsg;
    private TextView tvQQProgress, tvAlipayProgress, tvWeixinProgress;
    private Button btnQQStart, btnQQPause, btnAlipayStart, btnAlipayPause, btnWeixinStart, btnWeixinPause;

    private DownInfo qqDownInfo, alipayDownInfo, weixinDownInfo;

    private ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.android_auto_update_dialog_checking));

        qqProgressBar = findViewById(R.id.progressBar_qq);
        alipayProgressBar = findViewById(R.id.progressBar_alipay);
        weixinProgressbar = findViewById(R.id.progressBar_weixin);

        tvQQProgress = findViewById(R.id.tv_text_qq);
        tvAlipayProgress = findViewById(R.id.tv_text_alipay);
        tvWeixinProgress = findViewById(R.id.tv_text_weixin);

        tvQQMsg = findViewById(R.id.tv_msg_qq);
        tvAlipayMsg = findViewById(R.id.tv_msg_alipay);
        tvWeixinMsg = findViewById(R.id.tv_msg_weixin);

        btnQQStart = findViewById(R.id.btn_startDown_qq);
        btnQQPause = findViewById(R.id.btn_pauseDown_qq);

        btnAlipayStart = findViewById(R.id.btn_startDown_alipay);
        btnAlipayPause = findViewById(R.id.btn_pauseDown_alipay);

        btnWeixinStart = findViewById(R.id.btn_startDown_weixin);
        btnWeixinPause = findViewById(R.id.btn_pauseDown_weixin);

        btnQQStart.setOnClickListener(this);
        btnQQPause.setOnClickListener(this);
        btnAlipayStart.setOnClickListener(this);
        btnAlipayPause.setOnClickListener(this);
        btnWeixinStart.setOnClickListener(this);
        btnWeixinPause.setOnClickListener(this);

        String weixinDwonloadUrl = "http://dldir1.qq.com/weixin/android/weixin667android1320.apk";
        weixinDownInfo = new DownInfo(weixinDwonloadUrl);
        String weixinApkName = weixinDwonloadUrl.substring(weixinDwonloadUrl.lastIndexOf("/") + 1, weixinDwonloadUrl.length());
        File weixinApkFile = new File(getExternalCacheDir(), weixinApkName);
        weixinDownInfo.setSavePath(weixinApkFile.getAbsolutePath());
        weixinDownInfo.setState(DownState.START);


        String qqDwonloadUrl = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";
        qqDownInfo = new DownInfo(qqDwonloadUrl);
        String qqApkName = qqDwonloadUrl.substring(qqDwonloadUrl.lastIndexOf("/") + 1, qqDwonloadUrl.length());
        File qqApkFile = new File(getExternalCacheDir(), qqApkName);
        qqDownInfo.setSavePath(qqApkFile.getAbsolutePath());
        qqDownInfo.setState(DownState.START);

        String alipayDwonloadUrl = "http://gdown.baidu.com/data/wisegame/87b1af6e50012cb5/zhifubao_128.apk";
        alipayDownInfo = new DownInfo(alipayDwonloadUrl);
        String alipayApkName = alipayDwonloadUrl.substring(alipayDwonloadUrl.lastIndexOf("/") + 1, alipayDwonloadUrl.length());
        File alipayApkFile = new File(getExternalCacheDir(), alipayApkName);
        alipayDownInfo.setSavePath(alipayApkFile.getAbsolutePath());
        alipayDownInfo.setState(DownState.START);

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_startDown_qq:
                HttpDownManager.getInstance().startDown(qqDownInfo, new HttpProgressOnNextListener<DownInfo>() {

                    @Override
                    public void onNext(DownInfo downInfo) {
                        tvQQMsg.setText("QQ下载完成" + downInfo.getSavePath());
                    }

                    @Override
                    public void updateProgress(long readLength, long countLength) {
                        qqProgressBar.setMax((int) countLength);
                        qqProgressBar.setProgress((int) readLength);
                        tvQQProgress.setText(readLength * 100 / countLength + "%");
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvQQMsg.setText(e.getMessage());
                    }
                });
                break;
            case R.id.btn_pauseDown_qq:
                String tag = (String) btnQQPause.getTag();
                if (tag.equals("true")) {
                    btnQQPause.setText("继续下载");
                    btnQQPause.setTag("false");
                    HttpDownManager.getInstance().pause(qqDownInfo);
                } else {
                    btnQQPause.setText("暂停下载");
                    btnQQPause.setTag("true");
                    HttpDownManager.getInstance().continueDownload(qqDownInfo);
                }

                break;
            case R.id.btn_startDown_alipay:
                HttpDownManager.getInstance().startDown(alipayDownInfo, new HttpProgressOnNextListener<DownInfo>() {

                    @Override
                    public void onNext(DownInfo downInfo) {
                        tvAlipayMsg.setText("下载完成" + downInfo.getSavePath());
                    }

                    @Override
                    public void updateProgress(long readLength, long countLength) {
                        alipayProgressBar.setMax((int) countLength);
                        alipayProgressBar.setProgress((int) readLength);
                        tvAlipayProgress.setText(readLength * 100 / countLength + "%");
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvAlipayMsg.setText(e.getMessage());
                    }
                });
                break;
            case R.id.btn_pauseDown_alipay:
                String tag1 = (String) btnAlipayPause.getTag();
                if (tag1.equals("true")) {
                    btnAlipayPause.setText("继续下载");
                    btnAlipayPause.setTag("false");
                    HttpDownManager.getInstance().pause(alipayDownInfo);
                } else {
                    btnAlipayPause.setText("暂停下载");
                    btnAlipayPause.setTag("true");
                    HttpDownManager.getInstance().continueDownload(alipayDownInfo);
                }
                break;
            case R.id.btn_startDown_weixin:
                HttpDownManager.getInstance().startDown(weixinDownInfo, new HttpProgressOnNextListener<DownInfo>() {

                    @Override
                    public void onNext(DownInfo downInfo) {
                        tvWeixinMsg.setText("下载完成" + downInfo.getSavePath());
                    }

                    @Override
                    public void updateProgress(long readLength, long countLength) {
                        weixinProgressbar.setMax((int) countLength);
                        weixinProgressbar.setProgress((int) readLength);
                        tvWeixinProgress.setText(readLength * 100 / countLength + "%");
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvWeixinMsg.setText(e.getMessage());
                    }
                });
                break;
            case R.id.btn_pauseDown_weixin:
                String tag2 = (String) btnWeixinPause.getTag();
                if (tag2.equals("true")) {
                    btnWeixinPause.setText("继续下载");
                    btnWeixinPause.setTag("false");
                    HttpDownManager.getInstance().pause(weixinDownInfo);
                } else {
                    btnWeixinPause.setText("暂停下载");
                    btnWeixinPause.setTag("true");
                    HttpDownManager.getInstance().continueDownload(weixinDownInfo);
                }
                break;
        }
    }


    public void updateDialog(View view) {
        UpdateChecker.checkForDialog(this, dialog);
    }

    public void updateNotification(View view) {
        UpdateChecker.checkForNotification(this, dialog);
    }
}
