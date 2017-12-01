package com.example.wilko.songle

import android.content.Context
import java.lang.ref.WeakReference

/**
 * Created by wilko on 10/13/2017.
 */

class DownloadXmlTaskSong(caller : AsyncCompleteListener<Pair<DownloadType, List<Song>>>, val wContext : WeakReference<Context>) : DownloadTask<Pair<DownloadType, List<Song>>>(caller){
    //val result = StringBuilder()
    override fun loadFromNetwork(urlString: String) : Pair<DownloadType, List<Song>>{
        val stream = downloadUrl(urlString)
        val xmlSongParser = XmlSongParser()
        val context = wContext.get()
        var list = emptyList<Song>()
        if (context != null) {
            list = xmlSongParser.parse(stream, context)
            if (list.isEmpty()) {
                return Pair(DownloadType.NO_NEW_SONGS, list)
            }
        }
        return Pair(DownloadType.SONGS, list)
    }
}