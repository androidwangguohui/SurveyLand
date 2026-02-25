package com.example.buddha.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.surveyland.ui.view.LoadingDialog


open class BaseActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //隐藏状态栏
//        hideStatusBar()
        enableEdgeToEdge()
    }
    // 隐藏状态栏（保留底部导航栏）
    private fun hideStatusBar() {
        // 获取当前窗口的装饰视图
        val decorView = window.decorView

        // 设置系统UI可见性标志
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN // 核心：隐藏状态栏

    }


    fun showLoading(msg: String){
        LoadingDialog.show(
            supportFragmentManager,
            msg,
            false
        )
    }

    fun updateLoading(msg: String){
        LoadingDialog.updateMessage(msg)
    }

    fun dismissLoading(){
        LoadingDialog.dismiss()
    }

    // 恢复状态栏显示
    private fun showStatusBar() {
        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE // 恢复默认显示
    }
    open fun startActivity(mAppCompatActivity:Class<*>){
        var intent = Intent(this,mAppCompatActivity)
        startActivity(intent)
    }

    open fun startActivity(mAppCompatActivity: Class<*>, name:String, int:Int){
        var intent = Intent(this,mAppCompatActivity)
        intent.putExtra(name,int)
        startActivity(intent)
    }

    open fun startActivity(mAppCompatActivity:Class<*>,name:String,int:String){
        var intent = Intent(this,mAppCompatActivity)
        intent.putExtra(name,int)
        startActivity(intent)
    }
    open fun startActivity(mAppCompatActivity:Class<*>,name:String,name2:String,int: Double,int2: Double){
        var intent = Intent(this,mAppCompatActivity)
        intent.putExtra(name,int)
        intent.putExtra(name2,int2)
        intent.putExtra("type",1)
        startActivity(intent)
    }

}