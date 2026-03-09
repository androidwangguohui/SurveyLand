package com.example.surveyland.entity

data class PoiResponse(
    val pois: List<PoiItem2>
)

data class PoiItem2(
    val name: String,
    val address: String,
    val location: String
)