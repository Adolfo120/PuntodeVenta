package com.example.puntodeventa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private val lista: List<Producto>,
    private val onItemClick: (Producto) -> Unit = {}
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.txtNombre)
        val precio: TextView = view.findViewById(R.id.txtPrecio)
        val stock: TextView = view.findViewById(R.id.txtStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val producto = lista[position]

        holder.nombre.text = producto.nombre
        holder.precio.text = "$${String.format("%.2f", producto.precio)}"
        holder.stock.text = "Stock: ${producto.stock}"

        val stockColor = when {
            producto.stock <= 0 -> android.graphics.Color.parseColor("#FFD32F2F")
            producto.stock < 5 -> android.graphics.Color.parseColor("#FFE65100")
            else -> android.graphics.Color.parseColor("#FF2E7D32")
        }
        holder.stock.setTextColor(stockColor)

        holder.itemView.setOnClickListener { onItemClick(producto) }
    }
}
