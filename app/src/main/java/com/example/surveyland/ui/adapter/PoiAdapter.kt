package com.example.surveyland.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.services.core.PoiItem
import com.example.surveyland.databinding.ItemPoiBinding


class PoiAdapter(
    private val list: List<PoiItem>,
    private val itemClick: (PoiItem) -> Unit
) : RecyclerView.Adapter<PoiAdapter.PoiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoiViewHolder {
        val binding = ItemPoiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PoiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PoiViewHolder, position: Int) {
        val poi = list[position]
        holder.binding.poiTitle.text = poi.title // 名称
        holder.binding.poiAddress.text = poi.snippet ?: poi.typeDes // 地址或类型
        holder.binding.root.setOnClickListener { itemClick(poi) }
    }

    override fun getItemCount(): Int = list.size

    class PoiViewHolder(val binding: ItemPoiBinding) : RecyclerView.ViewHolder(binding.root)
}