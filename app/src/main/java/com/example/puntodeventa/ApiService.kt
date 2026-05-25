package com.example.puntodeventa

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("productos")
    fun obtenerProductos(): Call<List<Producto>>

    @POST("productos")
    fun guardarProducto(@Body producto: Producto): Call<Producto>

    @PUT("productos/{id}")
    fun actualizarProducto(@Path("id") id: Int, @Body producto: Producto): Call<Producto>

    @DELETE("productos/{id}")
    fun eliminarProducto(@Path("id") id: Int): Call<Unit>
}
