package com.example.wilko.songle

import android.content.Context
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.lang.ref.WeakReference

/**
 * Created by wilko on 10/24/2017.
 */

class DownloadPinPngs(caller : DownloadCompleteListener<Pair<DownloadType, List<Song>>>, val wContext : WeakReference<Context>) : DownloadTask<Pair<DownloadType, List<Song>>>(caller){
    override fun loadFromNetwork(urlString: String) : Pair<DownloadType, List<Song>> {
        val pinUnclassifiedStream = downloadUrl(urlString + "wht-blank.png")
        val pinBoringStream = downloadUrl(urlString +"ylw-blank.png")
        val pinNotBoringStream = downloadUrl(urlString +"ylw-circle.png")
        val pinInterestingStream = downloadUrl(urlString +"orange-diamond.png")
        val pinVeryInterestingStream = downloadUrl(urlString +"red-stars.png")

        val context = wContext.get()
        if (context != null) {
            fromStreamtoCacheFile(context, "wht_blank.png", pinUnclassifiedStream)
            fromStreamtoCacheFile(context, "ylw_blank.png", pinBoringStream)
            fromStreamtoCacheFile(context, "ylw_circle.png", pinNotBoringStream)
            fromStreamtoCacheFile(context, "orange_diamond.png", pinInterestingStream)
            fromStreamtoCacheFile(context, "red_stars.png", pinVeryInterestingStream)
        }
        return Pair(DownloadType.IMG, emptyList())
    }
}