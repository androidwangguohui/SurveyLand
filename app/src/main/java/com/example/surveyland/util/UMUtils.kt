package com.example.surveyland.util

import android.content.Context
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure

object UMUtils {



    fun initUmeng(context: Context) {
        // 1️⃣ 提交隐私授权结果
        UMConfigure.submitPolicyGrantResult(context, true)
        UMConfigure.init(
            context,
            "69a93bd76f259537c76de03c",
            "Umeng",
            UMConfigure.DEVICE_TYPE_PHONE,
            null
        )

    }


    /**
     * 普通事件
     */
    open fun event(context: Context, eventId: String) {
        MobclickAgent.onEvent(context, eventId)
    }

    /**
     * 带参数事件
     */
    open fun event(context: Context, eventId: String, map: Map<String, String>) {
        MobclickAgent.onEvent(context, eventId, map)
    }

    /**
     * 页面开始
     */
    open fun pageStart(pageName: String) {
        MobclickAgent.onPageStart(pageName)
    }

    /**
     * 页面结束
     */
   open fun pageEnd(pageName: String) {
        MobclickAgent.onPageEnd(pageName)
    }
}