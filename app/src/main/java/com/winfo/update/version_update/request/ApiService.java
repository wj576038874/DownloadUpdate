package com.winfo.update.version_update.request;

import com.winfo.update.version_update.entity.VersionInfo;
import com.winfo.update.version_update.utils.Constants;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.GET;

public interface ApiService {

    /**
     * 版本检测
     */
    @GET(Constants.UPDATE_URL)
    Observable<Response<VersionInfo>> checkVersion();

}
