package com.example.surveyland.ui.fragment

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.map_mapbox.databinding.FragmentMapBinding
import com.example.map_mapbox.ui.MapBoxFragment
import com.example.surveyland.dao.AppDatabase
import com.example.surveyland.databinding.FragmentLandListBinding
import com.example.surveyland.ui.activity.MeasureActivity
import com.example.surveyland.ui.activity.WalkAroundActivity
import com.example.surveyland.ui.adapter.LandAdapter
import com.example.surveyland.ui.view.AppToast
import com.example.surveyland.ui.view.CustomPromptDialog
import kotlinx.coroutines.launch

class LandListFragment: BaseFragment() {

    private lateinit var mFragmentLandListBinding: FragmentLandListBinding

    private lateinit var adapter: LandAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        mFragmentLandListBinding = FragmentLandListBinding.inflate(inflater,container,false)

        return mFragmentLandListBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mFragmentLandListBinding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = LandAdapter (requireActivity(),mutableListOf()){
            if(it.type == "走一圈"){
                AppToast.show(requireContext(),"走一圈不可编辑")
            }else{

                AlertDialog.Builder(requireActivity())
                    .setTitle("地块操作")
                    .setItems(arrayOf("编辑", "删除")) { _, which ->

                        when (which) {
                            0 -> startActivity(MeasureActivity::class.java, "latitude", "longitude", it.lat, it.lng,2,it.id)
                            1 -> deleteLand(it.id)
                        }
                    }
                    .show()
                // 点击跳转编辑

            }
        }

        mFragmentLandListBinding.recyclerView.adapter = adapter

    }

    private fun deleteLand(landId: Long) {
        CustomPromptDialog.Builder(requireActivity())
            .setTitle("提示")
            .setMessage("是否删除该地块？")
            .setCancel("取消")
            .setConfirm("确定") {
                // 1️⃣ 删除所有地块信息
                lifecycleScope.launch {
                    val dao = AppDatabase.getDatabase(requireContext()).landDao()
                    val landList = dao.getAll()
                    val targetLand = landList.firstOrNull { it.id == landId }
                    targetLand?.let { dao.delete(it) }
                    loadData()
                }
            }
            .show()

    }

    private fun loadData() {

        lifecycleScope.launch {
            val list = AppDatabase.getDatabase(requireActivity())
                .landDao()
                .getAll()

            // 更新 Adapter 内部数据
            adapter.updateList(list)

        }
    }


    override fun onResume() {
        super.onResume()
        // 每次 Fragment 可见并获取焦点时都会调用
        loadData()
    }

    companion object {
        private const val ARG_PARAM1 = "param1"

        @JvmStatic
        fun newInstance(param1: String) =
            LandListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}