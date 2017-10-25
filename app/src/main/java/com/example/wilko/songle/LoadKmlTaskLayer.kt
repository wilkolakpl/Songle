package com.example.wilko.songle

import android.content.Context
import android.os.AsyncTask
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.kml.KmlLayer
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Created by wilko on 10/24/2017.
 */

class LoadKmlTaskLayer(private val caller : DownloadCompleteListener<Boolean>, var wContext : WeakReference<Context>, val mMap : GoogleMap) :
        AsyncTask<String, Void, Boolean?>() {

    override fun doInBackground(vararg urls: String): Boolean? {
        return try {
            val context = wContext.get()
            if (context != null) {
                mMap.clear()
                val file = File(context.cacheDir, "cacheKml.kml")
                val input = FileInputStream(file)
                KmlLayer(mMap, input, context).addLayerToMap()
            }
            return true
        } catch (e: IOException) {
            //"Unable to load content. Check your network connection"
            null
        } catch (e: XmlPullParserException) {
            //"Error parsing XML"
            null
        }
    }
}
