package cl.seguel.mib

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Almacenamiento local SQLite. Migra las preferencias de la versión formativa. */
class AlmacenSqlite(contexto: Context, nombre: String) {
    private val db = object : SQLiteOpenHelper(contexto, "$nombre.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE valores (clave TEXT PRIMARY KEY, valor TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) = Unit
    }.writableDatabase
    init {
        val anterior = contexto.getSharedPreferences(nombre, Context.MODE_PRIVATE)
        if (anterior.all.isNotEmpty()) {
            db.beginTransaction()
            try {
                anterior.all.forEach { (k, v) ->
                    val fila = ContentValues().apply { put("clave", k); put("valor", v.toString()) }
                    db.insertWithOnConflict("valores", null, fila, SQLiteDatabase.CONFLICT_IGNORE)
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            check(anterior.edit().clear().commit())
        }
    }
    fun contains(clave: String): Boolean = getString(clave, null) != null
    fun getString(clave: String, defecto: String?): String? =
        db.query("valores", arrayOf("valor"), "clave = ?", arrayOf(clave), null, null, null).use {
            if (it.moveToFirst()) it.getString(0) else defecto
        }
    fun getLong(clave: String, defecto: Long) = getString(clave, null)?.toLongOrNull() ?: defecto
    fun getInt(clave: String, defecto: Int) = getString(clave, null)?.toIntOrNull() ?: defecto
    fun edit() = Editor()
    inner class Editor {
        private val cambios = linkedMapOf<String, String>()
        fun putString(clave: String, valor: String) = apply { cambios[clave] = valor }
        fun putLong(clave: String, valor: Long) = putString(clave, valor.toString())
        fun putInt(clave: String, valor: Int) = putString(clave, valor.toString())
        fun commit(): Boolean {
            db.beginTransaction()
            return try {
                cambios.forEach { (k, v) ->
                    val fila = ContentValues().apply { put("clave", k); put("valor", v) }
                    db.insertWithOnConflict("valores", null, fila, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
                true
            } finally { db.endTransaction() }
        }
        fun apply() { commit() }
    }
}
