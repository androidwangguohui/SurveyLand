package com.example.surveyland.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.map_mapbox.tianditu.TiandituFragment
import com.example.surveyland.ui.fragment.AMapMeasureFragment
import com.example.surveyland.ui.fragment.LandListFragment

class FragmentViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AMapMeasureFragment.newInstance("首页内容")
            1 -> LandListFragment.newInstance("地块")
            2 -> TiandituFragment.newInstance("我的内容")
            else -> AMapMeasureFragment.newInstance("默认内容")
        }
    }
}