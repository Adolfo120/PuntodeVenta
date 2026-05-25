package com.example.puntodeventa

data class Producto(
    val id: Int? = null,
    val nombre: String,
    val precio: Double,
    val stock: Int = 0
)
