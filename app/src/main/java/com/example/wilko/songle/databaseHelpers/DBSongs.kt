package com.example.wilko.songle.databaseHelpers

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.wilko.songle.App
import com.example.wilko.songle.dataClasses.Song

/**
 * Created by wilko on 10/22/2017.
 *
 * A SQLite database handler for saving a list of songs.
 *
 * credits to Bucky Roberts, whose YouTube tutorial guide was followed in the creation of this class
 * https://www.youtube.com/watch?v=Jcmp09LkU-I
 */

object DBSongs : SQLiteOpenHelper(App.instance, "songs.db", null, 1) {

    private val TABLE_SONGS = "songs"
    private val COLUMN_NUMBER = "number"
    private val COLUMN_ARTIST = "artist"
    private val COLUMN_TITLE ="tile"
    private val COLUMN_LINK ="link"
    private val COLUMN_LYRIC = "lyric"
    private val COLUMN_NOOFWORDS = "noOfWords"
    private val COLUMN_KMLLOCATION1 = "kmlLocation1"
    private val COLUMN_KMLLOCATION2 = "kmlLocation2"
    private val COLUMN_KMLLOCATION3 = "kmlLocation3"
    private val COLUMN_KMLLOCATION4 = "kmlLocation4"
    private val COLUMN_KMLLOCATION5 = "kmlLocation5"

    override fun onCreate(db: SQLiteDatabase?) {
        val query = "CREATE TABLE IF NOT EXISTS " + TABLE_SONGS + "(" +
                COLUMN_NUMBER + " INTEGER PRIMARY KEY, " +
                COLUMN_ARTIST + " TEXT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_LINK + " TEXT, " +
                COLUMN_LYRIC + " TEXT, " +
                COLUMN_NOOFWORDS + " TEXT, " +
                COLUMN_KMLLOCATION1 + " TEXT, " +
                COLUMN_KMLLOCATION2 + " TEXT, " +
                COLUMN_KMLLOCATION3 + " TEXT, " +
                COLUMN_KMLLOCATION4 + " TEXT, " +
                COLUMN_KMLLOCATION5 + " TEXT " +
                ");"
        if (db != null){
            db.execSQL(query)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        if (db != null){
            db.execSQL("DROP_TABLE_IF_EXISTS " + TABLE_SONGS)
            onCreate(db)
        }
    }

    fun addAll(list : List<Song>){
        val db = writableDatabase
        for (song in list){
            val values = ContentValues()
            values.put(COLUMN_NUMBER, song.number)
            values.put(COLUMN_ARTIST, song.artist)
            values.put(COLUMN_TITLE, song.title)
            values.put(COLUMN_LINK, song.link)
            values.put(COLUMN_LYRIC, song.lyric)
            values.put(COLUMN_NOOFWORDS, song.noOfWords)
            values.put(COLUMN_KMLLOCATION1, song.kmlLocation1)
            values.put(COLUMN_KMLLOCATION2, song.kmlLocation2)
            values.put(COLUMN_KMLLOCATION3, song.kmlLocation3)
            values.put(COLUMN_KMLLOCATION4, song.kmlLocation4)
            values.put(COLUMN_KMLLOCATION5, song.kmlLocation5)
            db.insert(TABLE_SONGS, null, values)
        }
        db.close()
    }

    fun deleteAll(){
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS)
        onCreate(db)
        db.close()
    }

    fun populateList(list : MutableList<Song>){
        val db = writableDatabase
        val query = "SELECT * FROM " + TABLE_SONGS + " WHERE 1"
        val c = db.rawQuery(query, null)
        c.moveToFirst()

        while (!c.isAfterLast){
            list.add(Song(c.getInt(c.getColumnIndex(COLUMN_NUMBER)),
                    c.getString(c.getColumnIndex(COLUMN_ARTIST)),
                    c.getString(c.getColumnIndex(COLUMN_TITLE)),
                    c.getString(c.getColumnIndex(COLUMN_LINK)),
                    c.getString(c.getColumnIndex(COLUMN_LYRIC)),
                    c.getInt(c.getColumnIndex(COLUMN_NOOFWORDS)),
                    c.getString(c.getColumnIndex(COLUMN_KMLLOCATION1)),
                    c.getString(c.getColumnIndex(COLUMN_KMLLOCATION2)),
                    c.getString(c.getColumnIndex(COLUMN_KMLLOCATION3)),
                    c.getString(c.getColumnIndex(COLUMN_KMLLOCATION4)),
                    c.getString(c.getColumnIndex(COLUMN_KMLLOCATION5))
            ))
            c.moveToNext()
        }
        c.close()
        db.close()
        return
    }

    fun howMany() : Int{
        val db = writableDatabase
        val num = db.compileStatement("SELECT Count(*) FROM " + TABLE_SONGS)
                .simpleQueryForLong()
                .toInt()
        db.close()
        return num
    }

    fun getProp(songNo: Int, prop : String): String{
        val db = writableDatabase
        val query = "SELECT * FROM " + TABLE_SONGS + " WHERE " + COLUMN_NUMBER + " = " + songNo
        val c = db.rawQuery(query, null)
        c.moveToFirst()
        val result = c.getString(c.getColumnIndex(prop))
        c.close()
        db.close()
        return result
    }
}