package com.example.surveyland.ui.view

import android.content.Context


class PrivacyDialog(
    private val context: Context,
    private val callback: (Boolean) -> Unit
) {

    fun show() {

        CustomPromptDialog.Builder(context)
            .setTitle("隐私协议")
            .setMessage("我们会收集设备信息用于统计分析，请阅读《用户协议》和《隐私政策》。")
            .setCancel("同意"){
                callback(true)
            }
            .setConfirm("不同意") {
                callback(false)
            }
            .show()
    }
}