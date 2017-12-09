package com.example.wilko.songle.downloaders

import com.example.wilko.songle.App
import com.example.wilko.songle.utils.AsyncCompleteListener

/**
 * Created by wilko on 10/24/2017.
 */

class DownloadPinPngs(caller : AsyncCompleteListener<DownloadType>) : DownloadTask<DownloadType>(caller){
    override fun loadFromNetwork() : DownloadType {

        val urlString = "http://maps.google.com/mapfiles/kml/paddle/"
        val pinUnclassifiedStream = downloadUrl(urlString + "wht-blank.png")
        val pinBoringStream = downloadUrl(urlString +"ylw-blank.png")
        val pinNotBoringStream = downloadUrl(urlString +"ylw-circle.png")
        val pinInterestingStream = downloadUrl(urlString +"orange-diamond.png")
        val pinVeryInterestingStream = downloadUrl(urlString +"red-stars.png")

        fromStreamToFile(App.instance, "wht_blank.png", pinUnclassifiedStream)
        fromStreamToFile(App.instance, "ylw_blank.png", pinBoringStream)
        fromStreamToFile(App.instance, "ylw_circle.png", pinNotBoringStream)
        fromStreamToFile(App.instance, "orange_diamond.png", pinInterestingStream)
        fromStreamToFile(App.instance, "red_stars.png", pinVeryInterestingStream)
        // PS. if saving fails, an IOException will propagate
        return DownloadType.IMGS
    }
}