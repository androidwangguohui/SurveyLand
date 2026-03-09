package com.example.surveyland.net.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surveyland.entity.PoiItem2
import com.example.surveyland.net.repository.PoiRepository
import kotlinx.coroutines.launch

class PoiViewModel : ViewModel() {

    private val repository = PoiRepository()

    val poiList = MutableLiveData<List<PoiItem2>>()

    fun search(keyword: String, city: String = "") {

        viewModelScope.launch {

            try {

                val result = repository.search(keyword, city)

                poiList.postValue(result)

            } catch (e: Exception) {

                e.printStackTrace()

            }

        }

    }

}