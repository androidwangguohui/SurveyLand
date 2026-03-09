package com.example.surveyland.entity

data class GeoResponse(
    val geocodes: List<GeoItem>
)

data class GeoItem(
    val formatted_address: String,
    val location: String
)