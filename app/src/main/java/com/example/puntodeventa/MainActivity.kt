package com.example.puntodeventa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recycler: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var txtTotalCount: TextView
    private lateinit var txtValorTotal: TextView
    private lateinit var txtStockBajo: TextView
    private lateinit var navView: NavigationView

    private var productos: List<Producto> = emptyList()
    private var productosFiltrados: List<Producto> = emptyList()
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        recycler = findViewById(R.id.recyclerProductos)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        progressBar = findViewById(R.id.progressBar)
        txtTotalCount = findViewById(R.id.txtTotalCount)
        txtValorTotal = findViewById(R.id.txtValorTotal)
        txtStockBajo = findViewById(R.id.txtStockBajo)
        navView = findViewById(R.id.navView)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        recycler.layoutManager = LinearLayoutManager(this)

        val swipeHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos in productosFiltrados.indices) {
                    val producto = productosFiltrados[pos]
                    producto.id?.let { id ->
                        RetrofitClient.instance.eliminarProducto(id).enqueue(object : Callback<Unit> {
                            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                                if (response.isSuccessful) {
                                    cargarProductos()
                                    Snackbar.make(recycler, R.string.producto_eliminado, Snackbar.LENGTH_SHORT).show()
                                } else {
                                    cargarProductos()
                                    mostrarError("Error al eliminar")
                                }
                            }
                            override fun onFailure(call: Call<Unit>, t: Throwable) {
                                cargarProductos()
                                mostrarError(getString(R.string.error_conexion))
                            }
                        })
                    } ?: run { cargarProductos() }
                }
            }
        }
        ItemTouchHelper(swipeHelper).attachToRecyclerView(recycler)

        findViewById<FloatingActionButton>(R.id.fabAgregar).setOnClickListener {
            startActivity(Intent(this, AgregarProductoActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener { cargarProductos() }

        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_agregar -> {
                    startActivity(Intent(this, AgregarProductoActivity::class.java))
                    true
                }
                R.id.nav_cerrar -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        cargarProductos()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.buscar)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filtrar(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrar(newText ?: "")
                return true
            }
        })
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                searchQuery = ""
                filtrar("")
                return true
            }
        })
        return true
    }

    private fun filtrar(query: String) {
        searchQuery = query
        productosFiltrados = if (query.isBlank()) {
            productos
        } else {
            productos.filter {
                it.nombre.contains(query, ignoreCase = true) ||
                        it.precio.toString().contains(query)
            }
        }
        actualizarLista()
    }

    override fun onResume() {
        super.onResume()
        cargarProductos()
    }

    private fun cargarProductos() {
        progressBar.visibility = ProgressBar.VISIBLE
        layoutEmpty.visibility = LinearLayout.GONE

        RetrofitClient.instance.obtenerProductos()
            .enqueue(object : Callback<List<Producto>> {
                override fun onResponse(call: Call<List<Producto>>, response: Response<List<Producto>>) {
                    progressBar.visibility = ProgressBar.GONE
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful) {
                        productos = response.body() ?: emptyList()
                        filtrar(searchQuery)
                        actualizarStats()
                    } else {
                        mostrarError("Error del servidor: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<Producto>>, t: Throwable) {
                    progressBar.visibility = ProgressBar.GONE
                    swipeRefresh.isRefreshing = false
                    mostrarError(getString(R.string.error_conexion))
                }
            })
    }

    private fun actualizarStats() {
        val total = productos.size
        val valorTotal = productos.sumOf { it.precio * it.stock }
        val stockBajo = productos.count { it.stock <= 5 }

        txtTotalCount.text = total.toString()
        txtValorTotal.text = "$${String.format("%,.0f", valorTotal)}"
        txtStockBajo.text = stockBajo.toString()
    }

    private fun actualizarLista() {
        if (productosFiltrados.isEmpty() && searchQuery.isBlank()) {
            layoutEmpty.visibility = LinearLayout.VISIBLE
        } else if (productosFiltrados.isEmpty()) {
            layoutEmpty.visibility = LinearLayout.VISIBLE
            findViewById<TextView>(R.id.txtEmpty).text = "Sin resultados para \"$searchQuery\""
        } else {
            layoutEmpty.visibility = LinearLayout.GONE
        }

        val adapter = ProductoAdapter(productosFiltrados) { producto ->
            val intent = Intent(this, AgregarProductoActivity::class.java).apply {
                putExtra("producto_id", producto.id)
                putExtra("producto_nombre", producto.nombre)
                putExtra("producto_precio", producto.precio)
                putExtra("producto_stock", producto.stock)
            }
            startActivity(intent)
        }

        val prevAdapter = recycler.adapter
        recycler.adapter = adapter

        if (prevAdapter == null) {
            val anim = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
            recycler.layoutAnimation = anim
        }
    }

    fun eliminarProducto(producto: Producto, position: Int) {
        val id = producto.id ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirmar_eliminar)
            .setMessage(getString(R.string.confirmar_eliminar_msg, producto.nombre))
            .setPositiveButton(R.string.eliminar) { _, _ ->
                RetrofitClient.instance.eliminarProducto(id)
                    .enqueue(object : Callback<Unit> {
                        override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                            if (response.isSuccessful) {
                                cargarProductos()
                                Snackbar.make(recycler, R.string.producto_eliminado, Snackbar.LENGTH_SHORT).show()
                            } else {
                                mostrarError("Error al eliminar")
                            }
                        }

                        override fun onFailure(call: Call<Unit>, t: Throwable) {
                            mostrarError(getString(R.string.error_conexion))
                        }
                    })
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun logout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de cerrar la sesión?")
            .setPositiveButton("Cerrar") { _, _ ->
                getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_logged_in", false)
                    .apply()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarError(mensaje: String) {
        Snackbar.make(recycler, mensaje, Snackbar.LENGTH_LONG)
            .setAction(R.string.reintentar) { cargarProductos() }
            .show()
    }
}
