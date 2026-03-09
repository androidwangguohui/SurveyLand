package com.example.surveyland.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.services.core.PoiItem
import com.example.surveyland.entity.PoiItem2
import com.example.surveyland.databinding.ItemPoiBinding


class PoiAdapter(
    private val list: List<PoiItem>,
    private val list2: List<PoiItem2>,
    private val itemClick: (PoiItem) -> Unit,
    private val itemClick2: (PoiItem2) -> Unit,
) : RecyclerView.Adapter<PoiAdapter.PoiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoiViewHolder {
        val binding = ItemPoiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PoiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PoiViewHolder, position: Int) {
        if(list.isNotEmpty()){
            val poi = list[position]
            holder.binding.poiTitle.text = poi.title // 名称
            holder.binding.poiAddress.text = poi.snippet ?: poi.typeDes // 地址或类型
            holder.binding.root.setOnClickListener { itemClick(poi) }
        }else{
            val poi = list2[position]
            holder.binding.poiTitle.text = poi.name // 名称
            holder.binding.poiAddress.text = poi.address // 地址或类型
            holder.binding.root.setOnClickListener { itemClick2(poi) }
        }
    }

    override fun getItemCount(): Int = if(list.isNotEmpty())list.size else list2.size

    class PoiViewHolder(val binding: ItemPoiBinding) : RecyclerView.ViewHolder(binding.root)
}