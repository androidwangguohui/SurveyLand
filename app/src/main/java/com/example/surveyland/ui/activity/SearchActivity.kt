package com.example.surveyland.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.example.buddha.activity.BaseActivity
import com.example.surveyland.databinding.ActivitySearchBinding
import com.example.surveyland.entity.PoiItem2
import com.example.surveyland.entity.PositionEvent
import com.example.surveyland.net.model.GeoViewModel
import com.example.surveyland.net.model.PoiViewModel
import com.example.surveyland.ui.adapter.PoiAdapter
import com.example.surveyland.ui.view.AppToast

import org.greenrobot.eventbus.EventBus


class SearchActivity : BaseActivity(), PoiSearch.OnPoiSearchListener {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: PoiAdapter
    private val poiList = mutableListOf<PoiItem>()
    private val dataList = mutableListOf<PoiItem2>()
    private val viewModel: PoiViewModel by viewModels()
//    private val viewModel: GeoViewModel by viewModels()

    private var keyword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            finish()
        }

        initData()



        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                keyword = s.toString()
                if (keyword.isNotEmpty()){
                    viewModel.search(keyword)
                } else {
                    poiList.clear()
                    dataList.clear()
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun initData() {
        adapter = PoiAdapter(poiList,dataList,{ poiItem ->

            EventBus.getDefault().post(PositionEvent(poiItem.latLonPoint.longitude, poiItem.latLonPoint.latitude))

            finish()

        },{
                poiItem ->
            val lat = poiItem.location.split(",")[1].toDouble()
            val lng = poiItem.location.split(",")[0].toDouble()
            EventBus.getDefault().post(PositionEvent(lng,lat))

            finish()
        })
        viewModel.poiList.observe(this){
            if(null != it && it.isNotEmpty()){
                dataList.clear()
                poiList.clear()
                dataList.addAll(it)
                adapter.notifyDataSetChanged()

            }else{
                    searchPoi(keyword)

            }
        }
    }

    private fun searchPoi(keyword: String) {
        val query = PoiSearch.Query(keyword, "", "") // 全国搜索
        query.pageSize = 20
        query.pageNum = 0
        val poiSearch = PoiSearch(this, query)
        poiSearch.setOnPoiSearchListener(this)
        poiSearch.searchPOIAsyn()
    }

    override fun onPoiSearched(result: PoiResult?, rCode: Int) {
        if (rCode == 1000) {
            poiList.clear()
            dataList.clear()
            result?.pois?.let { poiList.addAll(it) }
            adapter.notifyDataSetChanged()
        } else {
            AppToast.show(this, "搜索失败，请稍后重试")
        }
    }

    override fun onPoiItemSearched(p0: com.amap.api.services.core.PoiItem?, p1: Int) {

    }

}


