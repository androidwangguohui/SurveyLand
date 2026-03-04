package com.example.surveyland.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.icu.text.DecimalFormat
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.services.core.PoiItem
import com.example.surveyland.dao.AppDatabase
import com.example.surveyland.databinding.ItemLandBinding
import com.example.surveyland.entity.LandEntity
import com.example.surveyland.ui.view.AppToast
import com.example.surveyland.ui.view.CustomPromptDialog
import kotlinx.coroutines.launch

class LandAdapter(
    private val context: Context,
    private val list: MutableList<LandEntity>,
    private val onClick: (LandEntity) -> Unit,
    private val onNameChanged: (LandEntity) -> Unit,

) : ListAdapter<LandEntity, LandAdapter.LandViewHolder>(DiffCallback()) {

    inner class LandViewHolder(val binding: ItemLandBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LandViewHolder {
        val binding = ItemLandBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LandViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LandViewHolder, position: Int) {

        val item = list [position]
        val df = DecimalFormat("#.##") // 保留两位小数
        val formatted = df.format(item.distance) // "123.46"
        holder.binding.tvName.setText(item.villageName)
        holder.binding.tvType.text = item.type
//        holder.binding.tvArea.text = "${item.area} ㎡"
        holder.binding.tvArea.text = "面积："+"%.2f亩".format(item.area / 666.67)
        holder.binding.tvDistance.text = "周长："+formatted+"米"
        holder.binding.ivThumbnail.setImageBitmap(BitmapFactory.decodeFile(item.thumbnailPath))

        if (item.thumbnailPath != null) {
            holder.binding.ivThumbnail.setImageBitmap(
                BitmapFactory.decodeFile(item.thumbnailPath)
            )
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
        holder.binding.ivEidt.setOnClickListener {
            holder.binding.tvName.requestFocus() // 请求焦点
            // 将光标移动到文本末尾
            holder.binding.tvName.setSelection(holder.binding.tvName.text.length)
            // 弹出软键盘
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(holder.binding.tvName, InputMethodManager.SHOW_IMPLICIT)
        }
        holder.binding.tvNavigation.setOnClickListener {
            navigateToLand(item.lat,item.lng)
        }
        holder.binding.tvName.doAfterTextChanged {
            item.villageName = it.toString()
            onNameChanged(item)

        }
    }
    override fun getItemCount(): Int = list.size

    fun navigateToLand(lat: Double, lng: Double) {

        CustomPromptDialog.Builder(context)
            .setTitle("提示")
            .setMessage("是否跳转高德进行导航？")
            .setCancel("取消")
            .setConfirm("确定") {
                // 构建高德导航 URI
                val uriString = "amapuri://route/plan/?dlat=$lat&dlon=$lng&dname=目的地&dev=0&t=0"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                intent.setPackage("com.autonavi.minimap") // 指定高德 App
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    AppToast.show(context,"请先安装高德地图")
                }
            }
            .show()

    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<LandEntity>) {
        list.clear()
        list.addAll(newItems)
        notifyDataSetChanged() // 刷新列表
    }

    class DiffCallback : DiffUtil.ItemCallback<LandEntity>() {
        override fun areItemsTheSame(oldItem: LandEntity, newItem: LandEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LandEntity, newItem: LandEntity) =
            oldItem == newItem
    }
}