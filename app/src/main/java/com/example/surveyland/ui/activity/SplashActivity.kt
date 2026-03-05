package com.example.surveyland.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.surveyland.ui.view.AppToast
import com.example.surveyland.ui.view.PrivacyDialog
import com.example.surveyland.util.SPUtils
import com.example.surveyland.util.UMUtils
import com.umeng.commonsdk.UMConfigure

class SplashActivity: AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //隐藏状态栏
        enableEdgeToEdge()
        val agreed = SPUtils.getBoolean("privacy_agree", false)

        if (agreed) {
            initUmeng()
            startMain()
        } else {
            showPrivacy()
        }
    }

    private fun showPrivacy() {
        PrivacyDialog(this) {

            if (it) {

                SPUtils.put("privacy_agree", true)

                initUmeng()

                startMain()

            } else {
                UMConfigure.submitPolicyGrantResult(this, false)
                AppToast.show(this, "必须同意隐私协议才能使用App",)
                finish()

            }

        }.show()
    }

    private fun initUmeng() {
        UMUtils.initUmeng(this)
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}