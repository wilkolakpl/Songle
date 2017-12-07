package com.example.wilko.songle.downloaders

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.example.wilko.songle.App
import com.example.wilko.songle.utils.AsyncCompleteListener
import com.example.wilko.songle.dataClasses.Song
import com.example.wilko.songle.databaseHelpers.DBSongs
import java.io.IOException
import java.lang.ref.WeakReference


/**
 * Created by wilko on 10/22/2017.
 */

class DownloadKmls(caller : AsyncCompleteListener<DownloadType>) : DownloadTask<DownloadType>(caller){
    override fun loadFromNetwork() : DownloadType {
        val urlString = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"
        val dbHandler = DBSongs
        val songs = mutableListOf<Song>()
        dbHandler.populateList(songs)

        val db = dbHandler.writableDatabase
        for (song in songs) {
            val ctn = ContentValues()
            for (whichMap in 1..5){
                downloadKml(urlString, whichMap, song.number, App.instance)
                ctn.put("kmlLocation" + whichMap, App.instance.filesDir.toString() + "map" + whichMap + "song" + song.number + "cacheKml.kml")
            }
            db.update("songs", ctn, "number=" + song.number, null)
        }
        db.close()

        return DownloadType.KLMS
    }

    private fun downloadKml(urlString: String, whichMap: Int, whichSong: Int, context: Context){

        val kmlStream = downloadUrl(urlString + String.format("%02d",whichSong) + "/map" + whichMap + ".kml")
        val fileName = "map" + whichMap + "song" + whichSong + "cacheKml.kml"
        try {
            fromStreamtoCacheFile(context, fileName, kmlStream)
        } catch (e : IOException){
            Log.e("downloadKml", "couldn't save kmls to local storage") // @todo callback
        }
    }
}