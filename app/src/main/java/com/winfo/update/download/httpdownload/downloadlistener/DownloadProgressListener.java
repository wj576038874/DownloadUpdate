package com.winfo.update.download.httpdownload.downloadlistener;

/**
 * 成功回调处理
 * Created by pc12 on 2017/2/5.
 */

public interface DownloadProgressListener {
    /**
     * 下载进度
     * @param read 进度
     * @param count 总长度
     */
    void update(long read, long count, boolean done);
}
