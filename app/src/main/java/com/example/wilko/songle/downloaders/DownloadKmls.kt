package com.example.wilko.songle.downloaders

import android.content.ContentValues
import android.content.Context
import com.example.wilko.songle.App
import com.example.wilko.songle.utils.AsyncCompleteListener
import com.example.wilko.songle.dataClasses.Song
import com.example.wilko.songle.databaseHelpers.DBSongs


/**
 * Created by wilko on 10/22/2017.
 *
 * Class for downloading and storing all Kmls.
 */

class DownloadKmls(caller : AsyncCompleteListener<DownloadType>) : DownloadTask<DownloadType>(caller){
    override fun loadFromNetwork() : DownloadType {
        val urlString = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"
        val dbHandler = DBSongs
        val songs = mutableListOf<Song>()
        dbHandler.populateList(songs)

        // iterating through all songs and difficulties
        val db = dbHandler.writableDatabase
        for (song in songs) {
            val ctn = ContentValues()
            for (whichMap in 1..5){
                downloadKml(urlString, whichMap, song.number, App.instance)

                // saving the location of where it was stored in the DBSongs database
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
        fromStreamToFile(context, fileName, kmlStream) // if saving fails, an IOException will propagate
    }
}