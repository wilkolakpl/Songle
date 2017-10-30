package com.example.wilko.songle

import android.content.Context
import android.os.AsyncTask
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.Marker
import java.lang.ref.WeakReference

/**
 * Created by wilko on 10/30/2017.
 */

class SaveRemainingPlacemarks(val mapMarkersWithGeofences : HashMap<String, Pair<Marker, Geofence>>, val wContext : WeakReference<Context>) :
        AsyncTask<String, Void, Boolean>() {

    override fun doInBackground(vararg urls: String): Boolean{
        val context = wContext.get()
        if (context != null) {
            val dbPlacemarkHandler = MyPlacemarkDBHandler(context)
            dbPlacemarkHandler.deleteAll()
            val list = mutableListOf<Placemark>()
            for (marker in mapMarkersWithGeofences.values){
                val placemark = Placemark(marker.first.title, marker.first.snippet, "#" + marker.first.snippet, marker.first.position.latitude, marker.first.position.longitude)
                list.add(placemark)
            }
            dbPlacemarkHandler.addAll(list)
        }
        return true
    }
}