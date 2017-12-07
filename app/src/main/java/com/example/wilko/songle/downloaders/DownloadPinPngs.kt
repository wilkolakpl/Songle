package com.example.wilko.songle.downloaders

import android.content.Context
import android.util.Log
import com.example.wilko.songle.App
import com.example.wilko.songle.utils.AsyncCompleteListener
import com.example.wilko.songle.dataClasses.Song
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Created by wilko on 10/24/2017.
 */

class DownloadPinPngs(caller : AsyncCompleteListener<DownloadType>) : DownloadTask<DownloadType>(caller){
    override fun loadFromNetwork() : DownloadType {
        val TAG = "DownloadPinPngs"

        val urlString = "http://maps.google.com/mapfiles/kml/paddle/"
        val pinUnclassifiedStream = downloadUrl(urlString + "wht-blank.png")
        val pinBoringStream = downloadUrl(urlString +"ylw-blank.png")
        val pinNotBoringStream = downloadUrl(urlString +"ylw-circle.png")
        val pinInterestingStream = downloadUrl(urlString +"orange-diamond.png")
        val pinVeryInterestingStream = downloadUrl(urlString +"red-stars.png")

        try {
            fromStreamtoCacheFile(App.instance, "wht_blank.png", pinUnclassifiedStream)
            fromStreamtoCacheFile(App.instance, "ylw_blank.png", pinBoringStream)
            fromStreamtoCacheFile(App.instance, "ylw_circle.png", pinNotBoringStream)
            fromStreamtoCacheFile(App.instance, "orange_diamond.png", pinInterestingStream)
            fromStreamtoCacheFile(App.instance, "red_stars.png", pinVeryInterestingStream)
        } catch (e : IOException) {
            Log.e(TAG, "couldn't save pngs to local storage") //@todo callback
        }

        return DownloadType.IMG
    }
}