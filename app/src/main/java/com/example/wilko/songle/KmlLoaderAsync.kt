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
 * This async task prepares the kml maps- parses kml and populates database.
 */

class KmlLoaderAsync(private val caller : AsyncCompleteListener<Int>) :
        AsyncTask<Int, Void, Int>() {

    private val dbPlacemarkHandler = DBPlacemarks
    private val TAG = "KmlLoaderAsync"

    override fun doInBackground(vararg mapNoArg: Int?): Int{
        // retrieve map number
        val mapNo = mapNoArg[0]

        if (mapNo != null) {
            val kmlFile : InputStream?
            try {
                kmlFile = FileInputStream(App.instance.filesDir.toString() + "/map" + mapNo + "song" + getCurrentSong() + "cacheKml.kml")
            } catch (e : IOException) {
                Log.e(TAG, "couldn't open the kml from local storage")
                return -1 // invalid mapNo
            }
            // parsing kml and populating database
            val kmlParser = KmlParser()
            val placemarks = kmlParser.parse(kmlFile)
            dbPlacemarkHandler.deleteAll()
            dbPlacemarkHandler.addAll(placemarks)
            return mapNo // indicates successful completion
        } else {
            Log.e(TAG, "mapNo not provided")
            return -1 // invalid mapNo
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