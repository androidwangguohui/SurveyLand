package com.example.surveyland.entity

import com.mapbox.geojson.Point

data class Parcel(val id: String,
                  val points: MutableList<Point>,
                  var locationName: String = "",)
