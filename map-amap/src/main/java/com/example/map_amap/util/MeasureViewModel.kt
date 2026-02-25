package com.example.map_amap.util

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MeasureViewModel : ViewModel() {

    var longs = MutableLiveData<List<Double>>()

    var latitude = MutableLiveData<Double>()

}