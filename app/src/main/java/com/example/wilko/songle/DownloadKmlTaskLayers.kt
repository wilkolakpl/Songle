package com.example.wilko.songle

import android.content.ContentValues
import android.content.Context
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.lang.ref.WeakReference
import java.io.File



/**
 * Created by wilko on 10/22/2017.
 */

class DownloadKmlTaskLayers(caller : DownloadCompleteListener<Pair<DownloadType, List<Song>>>, val wContext : WeakReference<Context>) : DownloadTask<Pair<DownloadType, List<Song>>>(caller){
    override fun loadFromNetwork(urlString: String) : Pair<DownloadType, List<Song>> {

        val context = wContext.get()
        if (context != null) {
            val dbHandler = MySongDBHandler(context)
            val songs = mutableListOf<Song>()
            dbHandler.populateList(songs)

            val db = dbHandler.writableDatabase
            for (song in songs) {
                val ctn = ContentValues()
                for (whichMap in 1..5){
                    downloadKml(urlString, whichMap, song.number, context)
                    ctn.put("kmlLocation" + whichMap, context.filesDir.toString() + "map" + whichMap + "song" + song.number + "cacheKml.kml")
                }
                db.update("songs", ctn, "number=" + song.number, null)
            }
            db.close()
        }
        return Pair(DownloadType.KLMS, emptyList())
    }

    private fun downloadKml(urlString: String, whichMap: Int, whichSong: Int, context: Context){
        val kmlStream = downloadUrl(urlString + String.format("%02d",whichSong) + "/map" + whichMap + ".kml")
        var kmlString = IOUtils.toString(kmlStream, "UTF-8")

        kmlString = kmlString.replace("http://maps.google.com/mapfiles/kml/paddle/wht-blank.png", context.filesDir.toString()+"/wht_blank.png")
        kmlString = kmlString.replace("http://maps.google.com/mapfiles/kml/paddle/ylw-blank.png", context.filesDir.toString()+"/ylw_blank.png")
        kmlString = kmlString.replace("http://maps.google.com/mapfiles/kml/paddle/ylw-circle.png", context.filesDir.toString()+"/ylw_circle.png")
        kmlString = kmlString.replace("http://maps.google.com/mapfiles/kml/paddle/orange-diamond.png", context.filesDir.toString()+"/orange_diamond.png")
        kmlString = kmlString.replace("http://maps.google.com/mapfiles/kml/paddle/red-stars.png", context.filesDir.toString()+"/red_stars.png")

        val fileName = "map" + whichMap + "song" + whichSong + "cacheKml.kml"
        val file = File(context.filesDir, fileName)
        FileUtils.writeStringToFile(file, kmlString)
    }
}