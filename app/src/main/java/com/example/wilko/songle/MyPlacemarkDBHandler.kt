package com.example.wilko.songle

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Created by wilko on 10/28/2017.
 */

class MyPlacemarkDBHandler(context: Context) : SQLiteOpenHelper(context, "placemarks.db", null, 1) {

    private val TABLE_PLACEMARKS = "placemarks"
    private val COLUMN_NAME = "name"
    private val COLUMN_DESCRIPTION = "description"
    private val COLUMN_STYLE ="styleUrl"
    private val COLUMN_LAT ="lat"
    private val COLUMN_LONG = "long"

    override fun onCreate(db: SQLiteDatabase?) {
        val query = "CREATE TABLE IF NOT EXISTS " + TABLE_PLACEMARKS + "(" +
                COLUMN_NAME + " TEXT PRIMARY KEY, " +
                COLUMN_DESCRIPTION + " TEXT, " +
                COLUMN_STYLE + " TEXT, " +
                COLUMN_LAT + " REAL, " +
                COLUMN_LONG + " REAL " +
                ");"
        if (db != null){
            db.execSQL(query)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        if (db != null){
            db.execSQL("DROP_TABLE_IF_EXISTS " + TABLE_PLACEMARKS)
            onCreate(db)
        }
    }

    fun addAll(list : List<Placemark>){
        val db = writableDatabase
        for (placemark in list){
            val values = ContentValues()
            values.put(COLUMN_NAME, placemark.name)
            values.put(COLUMN_DESCRIPTION, placemark.description)
            values.put(COLUMN_STYLE, placemark.styleUrl)
            values.put(COLUMN_LAT, placemark.lat)
            values.put(COLUMN_LONG, placemark.long)
            db.insert(TABLE_PLACEMARKS, null, values)
        }
        db.close()
    }

    fun deleteAll(){
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACEMARKS)
        onCreate(db)
        db.close()
    }

    fun delete(name : String){
        val db = writableDatabase
        db.delete(TABLE_PLACEMARKS, COLUMN_NAME + " = '" + name + "'", null)
        db.close()
    }

    fun populateHashMap(map : HashMap<String, MarkerOptions>){
        val db = writableDatabase
        val query = "SELECT * FROM " + TABLE_PLACEMARKS + " WHERE 1"
        val c = db.rawQuery(query, null)
        c.moveToFirst()

        while (!c.isAfterLast){

            val mrkr = MarkerOptions()
                    .title(c.getString(c.getColumnIndex(COLUMN_NAME)))
                    .snippet(c.getString(c.getColumnIndex(COLUMN_DESCRIPTION)))
                    .position(LatLng(c.getDouble(c.getColumnIndex(COLUMN_LAT)),
                            c.getDouble(c.getColumnIndex(COLUMN_LONG))))

            when(c.getString(c.getColumnIndex(COLUMN_STYLE))){
                "#unclassified" -> mrkr.icon(BitmapDescriptorFactory.fromFile("wht_blank.png"))
                "#boring" -> mrkr.icon(BitmapDescriptorFactory.fromFile("ylw_blank.png"))
                "#notboring" -> mrkr.icon(BitmapDescriptorFactory.fromFile("ylw_circle.png"))
                "#interesting" -> mrkr.icon(BitmapDescriptorFactory.fromFile("orange_diamond.png"))
                "#veryinteresting" -> mrkr.icon(BitmapDescriptorFactory.fromFile("red_stars.png"))
            }

            map[c.getString(c.getColumnIndex(COLUMN_NAME))] = mrkr

            c.moveToNext()
        }
        c.close()
        db.close()
        return
    }
}