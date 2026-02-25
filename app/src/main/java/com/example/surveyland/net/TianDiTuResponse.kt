package com.example.surveyland.net
data class TianDiTuResponse(
    val status: String,
    val result: ResultData?
)

data class ResultData(
    val formatted_address: String?,
    val addressComponent: AddressComponent?
)

data class AddressComponent(
    val province: String?,
    val city: String?,
    val county: String?,
    val town: String?,
    val village: String?
)