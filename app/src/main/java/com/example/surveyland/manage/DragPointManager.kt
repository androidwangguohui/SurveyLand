package com.example.surveyland.manage

import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures

class DragPointManager(
    private val mapView: MapView,
    private val drawManager: DrawManager
) {
    private var draggingIndex = -1


}
