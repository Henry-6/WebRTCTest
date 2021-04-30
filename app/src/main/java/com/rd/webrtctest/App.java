package com.rd.webrtctest;

import android.app.Application;

import rxhttp.RxHttp;

/**
 * @author haimian on 2021/4/22 0022
 */
public class App extends Application {


    @Override
    public void onCreate() {
        super.onCreate();


        RxHttp.setDebug(true);
    }
}
