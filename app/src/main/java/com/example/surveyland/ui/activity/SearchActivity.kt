package com.example.surveyland.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.example.buddha.activity.BaseActivity
import com.example.surveyland.databinding.ActivitySearchBinding
import com.example.surveyland.entity.PositionEvent
import com.example.surveyland.ui.adapter.PoiAdapter
import com.example.surveyland.ui.view.AppToast

import org.greenrobot.eventbus.EventBus


class SearchActivity : BaseActivity(), PoiSearch.OnPoiSearchListener {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: PoiAdapter
    private val poiList = mutableListOf<PoiItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            finish()
        }
        adapter = PoiAdapter(poiList) { poiItem ->

            EventBus.getDefault().post(PositionEvent(poiItem.latLonPoint.longitude, poiItem.latLonPoint.latitude))

            finish()

        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString()
                if (keyword.isNotEmpty()) searchPoi(keyword)
                else {
                    poiList.clear()
                    adapter.notifyDataSetChanged()
                }
            }
        })
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
            result?.pois?.let { poiList.addAll(it) }
            adapter.notifyDataSetChanged()
        } else {
            AppToast.show(this, "搜索失败，请稍后重试$rCode")
//            Toast.makeText(this, "搜索失败, code: $rCode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPoiItemSearched(p0: com.amap.api.services.core.PoiItem?, p1: Int) {

    }

}