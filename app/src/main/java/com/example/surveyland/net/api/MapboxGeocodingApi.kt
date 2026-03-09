package com.example.surveyland.net.api
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MapboxGeocodingApi {
    @GET("geocoding/v5/mapbox.places/{lng},{lat}.json")
    suspend fun reverseGeocode(
        @Path("lng") lng: Double,
        @Path("lat") lat: Double,
        @Query("access_token") token: String
    ): GeocodeResponse
}

data class GeocodeResponse(
    val features: List<Feature>
)

data class Feature(
    val id: String,
    val text: String,
    val place_type: List<String>
)