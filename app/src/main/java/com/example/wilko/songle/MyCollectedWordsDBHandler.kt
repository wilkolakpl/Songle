package com.example.wilko.songle

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by wilko on 11/11/2017.
 */

class MyCollectedWordsDBHandler(context: Context) : SQLiteOpenHelper(context, "collectedWords.db", null, 1) {

    private val TABLE_COLLECTED = "collected"
    private val COLUMN_NAME = "name"

    override fun onCreate(db: SQLiteDatabase?) {
        val query = "CREATE TABLE IF NOT EXISTS " + TABLE_COLLECTED + "(" +
                COLUMN_NAME + " TEXT PRIMARY KEY );"
        if (db != null){
            db.execSQL(query)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        if (db != null){
            db.execSQL("DROP_TABLE_IF_EXISTS " + TABLE_COLLECTED)
            onCreate(db)
        }
    }

    fun add(collectedWord: CollectedWord){
        val db = writableDatabase
        val value = ContentValues()
        value.put(COLUMN_NAME, collectedWord.name)
        db.insert(TABLE_COLLECTED, null, value)
        db.close()
    }

    fun deleteAll(){
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLLECTED)
        onCreate(db)
        db.close()
    }

    fun howMany() : Int{
        val db = writableDatabase
        val num = db.compileStatement("SELECT Count(*) FROM " + TABLE_COLLECTED)
                .simpleQueryForLong()
                .toInt()
        db.close()
        return num
    }

    fun populateHashMap(map : HashMap<String, Boolean>){
        val db = writableDatabase
        val query = "SELECT * FROM " + TABLE_COLLECTED + " WHERE 1"
        val c = db.rawQuery(query, null)

        c.moveToFirst()
        while (!c.isAfterLast){
            map[c.getString(c.getColumnIndex(COLUMN_NAME))] = true
            c.moveToNext()
        }
        c.close()
        db.close()
        return
    }
}