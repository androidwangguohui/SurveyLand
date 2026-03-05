package com.example.surveyland

import android.app.Application
import com.example.surveyland.util.SPUtils
import com.umeng.commonsdk.UMConfigure

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        UMConfigure.preInit(this, "69a93bd76f259537c76de03c", "Umeng")
        // 调试日志
        UMConfigure.setLogEnabled(true)
        SPUtils.init(this)
    }

}
