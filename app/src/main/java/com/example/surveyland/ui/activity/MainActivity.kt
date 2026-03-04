package com.example.surveyland.ui.activity

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import com.example.buddha.activity.BaseActivity
import com.example.map_amap.util.LocationPermissionViewModel
import com.example.surveyland.databinding.ActivityMainBinding
import com.example.surveyland.ui.FragmentViewPagerAdapter
import com.example.surveyland.ui.view.AppToast
import com.example.surveyland.ui.view.CustomPromptDialog
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity: BaseActivity() ,DialogInterface.OnClickListener{

    private val permissionVM: LocationPermissionViewModel by viewModels()
    private lateinit var mActivityMainBinding: ActivityMainBinding

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLoading("加载中...")
        // ⚠️ 必须在任何高德 SDK 使用前调用
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        // ✅ 必须先展示隐私政策
        MapsInitializer.updatePrivacyShow(this, true, true)
        // ✅ 必须同意隐私政策
        MapsInitializer.updatePrivacyAgree(this, true)

        mActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mActivityMainBinding.root)
//        requestLocationPermission()
        mActivityMainBinding.viewPager.isUserInputEnabled = false
        // 设置ViewPager适配器
        val adapter = FragmentViewPagerAdapter(this)
        mActivityMainBinding.viewPager.adapter = adapter
        // 设置TabLayout与ViewPager联动
        TabLayoutMediator(mActivityMainBinding.tabLayout, mActivityMainBinding.viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> "首页"
                1 -> "地块"
                2 -> "我的"
                else -> ""
            }
        }.attach()
//        mActivityMainBinding.viewPager.offscreenPageLimit = 3
    }


    private var isRequestingPermission = false
    // 多权限申请
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            when {
                fineGranted -> {
                    // 精准定位
                    startLocation(true)
                }

                coarseGranted -> {
                    // 只有模糊定位
                    startLocation(false)
                }

                else -> {
                    // 全拒绝
                    handlePermissionDenied()
                }
            }
        }


    // ===============================
    // 权限检查入口
    // ===============================
    private fun checkLocationPermission() {

        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            startLocation(fine)
        } else {
            // ⚠️ 如果正在请求，就不要重复弹
            if (!isRequestingPermission) {
                isRequestingPermission = true
                requestLocationPermission()
            }
        }
    }

    // ===============================
    // 发起权限申请
    // ===============================
    private fun requestLocationPermission() {

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // ===============================
    // 权限被拒绝处理
    // ===============================
    private fun handlePermissionDenied() {

        val showRationaleFine =
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

        val showRationaleCoarse =
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (!showRationaleFine && !showRationaleCoarse) {
            dismissLoading()
            // 用户点了“不再询问”
            showGoSettingDialog()

        } else {
            dismissLoading()
            // 普通拒绝，下次进来会再弹
            AppToast.show(this,"定位权限被拒绝")
        }
    }

    // ===============================
    // 引导去设置
    // ===============================
    private fun showGoSettingDialog() {

        CustomPromptDialog.Builder(this)
            .setTitle("需要定位权限")
            .setMessage("请前往设置页面开启定位权限，否则无法使用地图定位功能")
            .setCancel("取消")
            .setConfirm("去设置") {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .show()

//        AlertDialog.Builder(this)
//            .setTitle("需要定位权限")
//            .setMessage("请前往设置页面开启定位权限，否则无法使用地图定位功能")
//            .setPositiveButton("去设置") { _, _ ->
//                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                intent.data = Uri.fromParts("package", packageName, null)
//                startActivity(intent)
//            }
//            .setNegativeButton("取消", this)
//            .show()
    }

    // ===============================
    // 开始定位
    // ===============================
    var isNotify = false
    private fun startLocation(isPrecise: Boolean) {

//        if (isPrecise) {
//            Toast.makeText(this, "已开启精准定位", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "仅开启模糊定位", Toast.LENGTH_SHORT).show()
//        }

        // 在这里初始化地图定位层
        // Mapbox 或 高德定位代码写这里
        if(isPrecise && !isNotify){
            isNotify = true
            permissionVM.notifyGranted()
        }
        dismissLoading()
    }



    override fun onResume() {
        super.onResume()
        checkLocationPermission()
    }

    override fun onClick(p0: DialogInterface?, p1: Int) {
        finish()
    }

}