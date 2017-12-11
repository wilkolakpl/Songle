package com.example.wilko.songle

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.example.wilko.songle.databaseHelpers.DBPlacemarks
import com.example.wilko.songle.parsers.KmlParser
import com.example.wilko.songle.utils.AsyncCompleteListener
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by wilko on 12/11/2017.
 *
 * This async task prepares the kml maps.
 */

class KmlLoaderAsync(private val caller : AsyncCompleteListener<Int>) :
        AsyncTask<Int, Void, Int>() {

    private val dbPlacemarkHandler = DBPlacemarks
    private val TAG = "KmlLoaderAsynch"

    override fun doInBackground(vararg mapNoArg: Int?): Int{

        val mapNo = mapNoArg[0]
        if (mapNo != null) {
            val kmlFile : InputStream?
            try {
                kmlFile = FileInputStream(App.instance.filesDir.toString() + "/map" + mapNo + "song" + getCurrentSong() + "cacheKml.kml")
            } catch (e : IOException) {
                Log.e(TAG, "couldn't open the kml from local storage")
                return -1
            }
            val kmlParser = KmlParser()
            val placemarks = kmlParser.parse(kmlFile)
            dbPlacemarkHandler.deleteAll()
            dbPlacemarkHandler.addAll(placemarks)
            return mapNo
        } else {
            Log.e(TAG, "mapNo not provided")
            return -1
        }
    }

    fun getCurrentSong(): Int{
        val sharedPref = App.instance.getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        return sharedPref.getInt("currentSong", 0)
    }

    override fun onPostExecute(mapNo: Int) {
        super.onPostExecute(mapNo)
        caller.asyncComplete(mapNo)
    }
}