package com.example.wilko.songle

import android.content.Context
import java.lang.ref.WeakReference

/**
 * Created by wilko on 10/13/2017.
 */

class DownloadSongXml(caller : AsyncCompleteListener<Pair<DownloadType, List<Song>>>, val wContext : WeakReference<Context>) : DownloadTask<Pair<DownloadType, List<Song>>>(caller){
    override fun loadFromNetwork(urlString: String) : Pair<DownloadType, List<Song>>{
        val stream = downloadUrl(urlString)
        val xmlSongParser = XmlSongParser()
        val context = wContext.get()
        var list = emptyList<Song>()
        if (context != null) {
            list = xmlSongParser.parse(stream, context)
            if (list.isEmpty()) {
                // this happens when the timestamp of the stored xml is the same as the one on the
                // server, the control then skips re-downloading the already up-to-date xml
                return Pair(DownloadType.NO_NEW_SONGS, list)
            }
        }
        return Pair(DownloadType.SONGS, list)
    }
}