package com.example.surveyland.net.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surveyland.entity.GeoItem
import com.example.surveyland.net.repository.GeoRepository
import kotlinx.coroutines.launch

class GeoViewModel : ViewModel() {

    private val repository = GeoRepository()

    val result = MutableLiveData<GeoItem?>()

    fun search(address: String) {

        viewModelScope.launch {

            val data = repository.search(address)

            result.postValue(data)

        }

    }

}