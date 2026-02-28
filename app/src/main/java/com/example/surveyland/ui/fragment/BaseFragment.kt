package com.example.surveyland.ui.fragment

import android.content.Intent
import com.example.surveyland.ui.view.LoadingDialog

open class BaseFragment : androidx.fragment.app.Fragment() {



    fun showLoading(msg: String){
        LoadingDialog.show(
            childFragmentManager,
            msg,
        )
    }

    fun updateLoading(msg: String){
        LoadingDialog.updateMessage(msg)
    }

    fun dismissLoading(){
        LoadingDialog.dismiss()
    }

    open fun startActivity(mAppCompatActivity:Class<*>){
        var intent = Intent(requireActivity(),mAppCompatActivity)
        startActivity(intent)
    }

    open fun startActivity(mAppCompatActivity:Class<*>,name:String,int:Int){
        var intent = Intent(requireActivity(),mAppCompatActivity)
        intent.putExtra(name,int)
        startActivity(intent)
    }

    open fun startActivity(mAppCompatActivity:Class<*>,name:String,int:String){
        var intent = Intent(requireActivity(),mAppCompatActivity)
        intent.putExtra(name,int)
        startActivity(intent)
    }
    open fun startActivity(
        mAppCompatActivity: Class<*>,

        int: Double?,
        int2: Double?,
        zoom: Double? = 15.0
    ){
        var intent = Intent(requireActivity(),mAppCompatActivity)
        intent.putExtra("latitude",int)
        intent.putExtra("longitude",int2)
        intent.putExtra("zoom",zoom)
        startActivity(intent)
    }
    open fun startActivity(
        mAppCompatActivity: Class<*>,
        name: String,
        name2: String,
        int: Double?,
        int2: Double?,
        type: Int? = 1,
        id: Long? = 0,
        zoom: Double? = 15.0
    ){
        var intent = Intent(requireActivity(),mAppCompatActivity)
        intent.putExtra(name,int)
        intent.putExtra(name2,int2)
        intent.putExtra("type",type)
        intent.putExtra("edit_id",id)
        intent.putExtra("zoom",zoom)
        startActivity(intent)
    }
}