package com.example.wilko.songle.databaseHelpers

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.wilko.songle.App
import com.example.wilko.songle.dataClasses.CollectedWord

/**
 * Created by wilko on 11/11/2017.
 *
 * A SQLite database handler for saving a list of collected words.
 *
 * credits to Bucky Roberts, whose YouTube tutorial guide was followed in the creation of this class
 * https://www.youtube.com/watch?v=Jcmp09LkU-I
 */

object DBCollectedWords : SQLiteOpenHelper(App.instance, "collectedWords.db", null, 1) {

    private val TABLE_COLLECTED = "collected"
    private val COLUMN_NAME = "name" // name is the line and word number in this format %:%

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

        c.moveToFirst() // point to first result
        while (!c.isAfterLast){
            map[c.getString(c.getColumnIndex(COLUMN_NAME))] = true
            // the boolean value doesn't matter. The hashmap is only used for O(1) key lookup.
            c.moveToNext() // point to the next result
        }
        c.close()
        db.close()
        return
    }
}