package com.winfo.update.download.httpdownload;


import android.util.Log;

import com.winfo.update.download.exception.HttpTimeException;
import com.winfo.update.download.exception.RetryWhenNetworkException;
import com.winfo.update.download.http.HttpService;
import com.winfo.update.download.httpdownload.downloadlistener.DownloadInterceptor;
import com.winfo.update.download.listener.HttpProgressOnNextListener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * http下载处理类
 * Created by pc12 on 2017/2/5.
 */

public class HttpDownManager {
    //记录下载数据
    private Set<DownInfo> downInfos;
    //回调sub队列
    private HashMap<String, ProgressDownSubscriber> subMap;
    //进度监听队列
    private HashMap<String, HttpProgressOnNextListener<DownInfo>> mProgressListenerHashMap;

    //单利对象
    private volatile static HttpDownManager INSTANCE;

    private HttpDownManager() {
        downInfos = new HashSet<>();
        subMap = new HashMap<>();
        mProgressListenerHashMap = new HashMap<>();
    }

    /**
     * 获取单例
     */
    public static HttpDownManager getInstance() {
        if (INSTANCE == null) {
            synchronized (HttpDownManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HttpDownManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 继续下载
     */
    public void continueDownload(DownInfo downInfo) {
        //根据下载的url获取到进度监听队列中的监听器
        HttpProgressOnNextListener<DownInfo> httpProgressOnNextListener = mProgressListenerHashMap.get(downInfo.getUrl());
        if (httpProgressOnNextListener != null) {
            startDown(downInfo, httpProgressOnNextListener);
        }
    }


    /**
     * 开始下载
     */
    public void startDown(final DownInfo info, HttpProgressOnNextListener<DownInfo> httpProgressOnNextListener) {
//        正在下载返回异常信息
        if (info.getState() == DownState.DOWN) {
            httpProgressOnNextListener.onError(new Exception("正在下载中"));
            return;
        }
        //添加回调处理类
        ProgressDownSubscriber<DownInfo> subscriber = new ProgressDownSubscriber<>(info, httpProgressOnNextListener);
        //将监听进度的器添加到队列中，以便于继续下载时获取
        mProgressListenerHashMap.put(info.getUrl(), httpProgressOnNextListener);
        //记录回调sub
        subMap.put(info.getUrl(), subscriber);
        //获取service，多次请求公用一个service
        HttpService httpService;
        //设置文件的状态为下载
        info.setState(DownState.START);
        if (downInfos.contains(info)) {
            httpService = info.getService();
        } else {
            DownloadInterceptor interceptor = new DownloadInterceptor(subscriber);
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            //手动创建一个OkHttpClient并设置超时时间
            builder.connectTimeout(info.getConnectionTime(), TimeUnit.SECONDS);
            builder.addInterceptor(interceptor);

            Retrofit retrofit = new Retrofit.Builder()
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .baseUrl(info.getBaseUrl())
                    .build();
            httpService = retrofit.create(HttpService.class);
            info.setService(httpService);
            downInfos.add(info);
        }
        //得到rx对象-上一次下载的位置开始下载
        httpService.download("bytes=" + info.getReadLength() + "-", info.getUrl())
                //指定线程
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                //失败后的retry配置
                .retryWhen(new RetryWhenNetworkException())
                .map(new Function<ResponseBody, DownInfo>() {
                    @Override
                    public DownInfo apply(ResponseBody responseBody) {
                        try {
                            writeCache(responseBody, new File(info.getSavePath()), info);
                        } catch (IOException e) {
                            //失败抛出异常
                            throw new HttpTimeException(e.getMessage());
                        }
                        return info;
                    }
                })//回调线程
                .observeOn(AndroidSchedulers.mainThread())
                //数据回调
                .subscribe(subscriber);
    }


    /**
     * 停止下载
     */
    public void stopDown(DownInfo info) {
        if (info == null) return;
        info.setState(DownState.STOP);
        if (subMap.containsKey(info.getUrl())) {
            ProgressDownSubscriber subscriber = subMap.get(info.getUrl());
            subscriber.unSubscribe();
            subMap.remove(info.getUrl());
        }
    }


    /**
     * 删除
     */
    @SuppressWarnings("unused")
    public void deleteDown(DownInfo info) {
        stopDown(info);
    }


    /**
     * 暂停下载
     */
    public void pause(DownInfo info) {
        if (info == null) return;
        info.setState(DownState.PAUSE);
        if (subMap.containsKey(info.getUrl())) {
            ProgressDownSubscriber subscriber = subMap.get(info.getUrl());
            subscriber.unSubscribe();
            subMap.remove(info.getUrl());
        }
    }

    /**
     * 停止全部下载
     */
    @SuppressWarnings("unused")
    public void stopAllDown() {
        for (DownInfo downInfo : downInfos) {
            stopDown(downInfo);
        }
        subMap.clear();
        downInfos.clear();
    }

    /**
     * 暂停全部下载
     */
    @SuppressWarnings("unused")
    public void pauseAll() {
        for (DownInfo downInfo : downInfos) {
            pause(downInfo);
        }
        subMap.clear();
        downInfos.clear();
    }


    /**
     * 返回全部正在下载的数据
     */
    @SuppressWarnings("unused")
    public Set<DownInfo> getDownInfos() {
        return downInfos;
    }


    /**
     * 写入文件
     */
    private void writeCache(ResponseBody responseBody, File file, DownInfo info) throws IOException {
        if (!file.getParentFile().exists()) {
            boolean bol = file.getParentFile().mkdirs();
            if (!bol) {
                Log.e("TAG", "文件创建失败");
            }
        }
        long allLength;
        if (info.getCountLength() == 0) {
            allLength = responseBody.contentLength();
        } else {
            allLength = info.getCountLength();
        }
        FileChannel channelOut;
        RandomAccessFile randomAccessFile;
        randomAccessFile = new RandomAccessFile(file, "rwd");
        channelOut = randomAccessFile.getChannel();
        MappedByteBuffer mappedBuffer = channelOut.map(FileChannel.MapMode.READ_WRITE,
                info.getReadLength(), allLength - info.getReadLength());
        byte[] buffer = new byte[1024 * 8];
        int len;
        while ((len = responseBody.byteStream().read(buffer)) != -1) {
            mappedBuffer.put(buffer, 0, len);
        }
        responseBody.byteStream().close();
        channelOut.close();
        randomAccessFile.close();
    }

}
