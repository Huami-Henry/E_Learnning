package com.inesanet.dmedia.base;

import android.app.Application;
import android.content.Context;

import com.inesanet.dmedia.util.SPCache;

/**
 * Created by Administrator on 2017/5/3.
 */

public class TApplication extends Application {
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        initCache();
    }
    private void initCache() {
        SPCache.init(this);
    }
    public static Context getContext() {
        return context;
    }
}
