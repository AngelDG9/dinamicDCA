package com.dam.dinamicdca

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "CryptoPlans.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_PLANS = "plans"

        // Columnas
        private const val COL_ID = "id"
        private const val COL_NOMBRE = "nombre"
        private const val COL_MONEDA = "moneda"
        private const val COL_ATHV = "athv"
        private const val COL_FECHA_ATHV = "fecha_athv"
        private const val COL_BUYPLAN = "buyplan"
        private const val COL_SELLPLAN = "sellplan"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_PLANS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT UNIQUE NOT NULL,
                $COL_MONEDA TEXT NOT NULL,
                $COL_ATHV REAL NOT NULL,
                $COL_FECHA_ATHV TEXT NOT NULL,
                $COL_BUYPLAN TEXT,
                $COL_SELLPLAN TEXT
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLANS")
        onCreate(db)
    }

    fun insertPlan(plan: Plan): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE, plan.nombre)
            put(COL_MONEDA, plan.moneda)
            put(COL_ATHV, plan.athv)
            put(COL_FECHA_ATHV, plan.fechaAthv)
            put(COL_BUYPLAN, plan.buyplan)
            put(COL_SELLPLAN, plan.sellplan)
        }

        val id = db.insert(TABLE_PLANS, null, values)
        db.close()
        return id
    }

    fun getAllPlans(): List<Plan> {
        val plans = mutableListOf<Plan>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PLANS", null)

        if (cursor.moveToFirst()) {
            do {
                val plan = Plan(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
                    moneda = cursor.getString(cursor.getColumnIndexOrThrow(COL_MONEDA)),
                    athv = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ATHV)),
                    fechaAthv = cursor.getString(cursor.getColumnIndexOrThrow(COL_FECHA_ATHV)),
                    buyplan = cursor.getString(cursor.getColumnIndexOrThrow(COL_BUYPLAN)) ?: "",
                    sellplan = cursor.getString(cursor.getColumnIndexOrThrow(COL_SELLPLAN)) ?: ""
                )
                plans.add(plan)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return plans
    }

    fun updatePlan(plan: Plan): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE, plan.nombre)
            put(COL_MONEDA, plan.moneda)
            put(COL_ATHV, plan.athv)
            put(COL_FECHA_ATHV, plan.fechaAthv)
            put(COL_BUYPLAN, plan.buyplan)
            put(COL_SELLPLAN, plan.sellplan)
        }

        val result = db.update(TABLE_PLANS, values, "$COL_ID = ?", arrayOf(plan.id.toString()))
        db.close()
        return result
    }

    fun deletePlan(planId: Long): Int {
        val db = writableDatabase
        val result = db.delete(TABLE_PLANS, "$COL_ID = ?", arrayOf(planId.toString()))
        db.close()
        return result
    }
}