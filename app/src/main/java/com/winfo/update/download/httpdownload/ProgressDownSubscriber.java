package com.winfo.update.download.httpdownload;


import android.annotation.SuppressLint;

import com.winfo.update.download.httpdownload.downloadlistener.DownloadProgressListener;
import com.winfo.update.download.listener.HttpProgressOnNextListener;

import java.lang.ref.WeakReference;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


/**
 * 用于在Http请求开始时，自动显示一个ProgressDialog
 * 在Http请求结束是，关闭ProgressDialog
 * 调用者自己对请求数据进行处理
 */
public class ProgressDownSubscriber<T> implements Observer<T>, DownloadProgressListener {
    //弱引用结果回调
    private WeakReference<HttpProgressOnNextListener<T>> mHttpProgressOnNextListener;

    /*下载数据*/
    private DownInfo downInfo;

    private Disposable mDisposable;

    public ProgressDownSubscriber(DownInfo downInfo, HttpProgressOnNextListener<T> httpProgressOnNextListener) {
        this.mHttpProgressOnNextListener = new WeakReference<>(httpProgressOnNextListener);
        this.downInfo = downInfo;
    }

    public void unSubscribe() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }
    }


    @Override
    public void onSubscribe(Disposable disposable) {
        mDisposable = disposable;
    }

    @Override
    public void onNext(T t) {
        if (mHttpProgressOnNextListener.get() != null) {
            mHttpProgressOnNextListener.get().onNext(t);
        }
    }

    /**
     * 对错误进行统一处理
     * 隐藏ProgressDialog
     *
     * @param e 异常
     */
    @Override
    public void onError(Throwable e) {
        mDisposable.dispose();
        /*停止下载*/
        HttpDownManager.getInstance().stopDown(downInfo);
        if (mHttpProgressOnNextListener.get() != null) {
            mHttpProgressOnNextListener.get().onError(e);
        }
        downInfo.setState(DownState.ERROR);
    }

    /**
     * 完成，隐藏ProgressDialog
     */
    @Override
    public void onComplete() {
        mDisposable.dispose();
        if (mHttpProgressOnNextListener.get() != null) {
            mHttpProgressOnNextListener.get().onComplete();
        }
        downInfo.setState(DownState.FINISH);
    }

    @SuppressLint("CheckResult")
    @Override
    public void update(long read, long count, boolean done) {
        if (downInfo.getCountLength() > count) {
            read = downInfo.getCountLength() - count + read;
        } else {
            downInfo.setCountLength(count);
        }
        downInfo.setReadLength(read);
        if (mHttpProgressOnNextListener.get() != null) {
            /*接受进度消息，造成UI阻塞，如果不需要显示进度可去掉实现逻辑，减少压力*/
            Observable.just(read).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) {
                            /*如果暂停或者停止状态延迟，不需要继续发送回调，影响显示*/
                            if (downInfo.getState() == DownState.PAUSE || downInfo.getState() == DownState.STOP)
                                return;
                            downInfo.setState(DownState.DOWN);
                            mHttpProgressOnNextListener.get().updateProgress(aLong, downInfo.getCountLength());
                        }
                    });
        }
    }
}