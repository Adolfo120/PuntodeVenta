package com.example.puntodeventa

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AgregarProductoActivity : AppCompatActivity() {

    private var productoId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_producto)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val etNombre = findViewById<TextInputEditText>(R.id.etNombre)
        val etPrecio = findViewById<TextInputEditText>(R.id.etPrecio)
        val etStock = findViewById<TextInputEditText>(R.id.etStock)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardar)

        productoId = intent.getIntExtra("producto_id", -1).takeIf { it != -1 }
        val editNombre = intent.getStringExtra("producto_nombre")
        val editPrecio = intent.getDoubleExtra("producto_precio", -1.0)
        val editStock = intent.getIntExtra("producto_stock", -1)

        if (productoId != null) {
            toolbar.title = getString(R.string.editar_producto)
            btnGuardar.text = getString(R.string.actualizar)
            etNombre.setText(editNombre)
            etPrecio.setText(if (editPrecio > 0) editPrecio.toString() else "")
            etStock.setText(if (editStock >= 0) editStock.toString() else "0")
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val precioString = etPrecio.text.toString().trim()
            val stockString = etStock.text.toString().trim()
            val precio = precioString.toDoubleOrNull() ?: 0.0
            val stock = stockString.toIntOrNull() ?: 0

            if (nombre.isNotEmpty() && precio > 0) {
                btnGuardar.isEnabled = false
                val producto = Producto(
                    id = if (productoId != null) productoId else null,
                    nombre = nombre,
                    precio = precio,
                    stock = stock
                )
                if (productoId != null) {
                    actualizarProducto(producto, btnGuardar)
                } else {
                    enviarProducto(producto, btnGuardar)
                }
            } else {
                Snackbar.make(btnGuardar, getString(R.string.campos_invalidos), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun enviarProducto(producto: Producto, btnGuardar: MaterialButton) {
        RetrofitClient.instance.guardarProducto(producto).enqueue(object : Callback<Producto> {
            override fun onResponse(call: Call<Producto>, response: Response<Producto>) {
                btnGuardar.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@AgregarProductoActivity, R.string.producto_guardado, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Snackbar.make(btnGuardar, getString(R.string.error_guardar), Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Producto>, t: Throwable) {
                btnGuardar.isEnabled = true
                Snackbar.make(btnGuardar, "${getString(R.string.error_conexion)}: ${t.message}", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun actualizarProducto(producto: Producto, btnGuardar: MaterialButton) {
        val id = producto.id ?: return
        RetrofitClient.instance.actualizarProducto(id, producto)
            .enqueue(object : Callback<Producto> {
                override fun onResponse(call: Call<Producto>, response: Response<Producto>) {
                    btnGuardar.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(this@AgregarProductoActivity, R.string.producto_actualizado, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Snackbar.make(btnGuardar, getString(R.string.error_guardar), Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Producto>, t: Throwable) {
                    btnGuardar.isEnabled = true
                    Snackbar.make(btnGuardar, "${getString(R.string.error_conexion)}: ${t.message}", Snackbar.LENGTH_LONG).show()
                }
            })
    }
}
