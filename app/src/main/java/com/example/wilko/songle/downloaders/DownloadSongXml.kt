package com.example.wilko.songle.downloaders

import android.content.Context
import com.example.wilko.songle.App
import com.example.wilko.songle.utils.AsyncCompleteListener
import com.example.wilko.songle.parsers.XmlSongParser
import com.example.wilko.songle.dataClasses.Song
import com.example.wilko.songle.databaseHelpers.DBSongs
import java.lang.ref.WeakReference

/**
 * Created by wilko on 10/13/2017.
 */

class DownloadSongXml(caller : AsyncCompleteListener<DownloadType>) : DownloadTask<DownloadType>(caller){
    override fun loadFromNetwork() : DownloadType{
        val urlString = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml"
        val stream = downloadUrl(urlString)
        val xmlSongParser = XmlSongParser()
        val list = xmlSongParser.parse(stream, App.instance)
        if (list.isEmpty()) {
            // this happens when the timestamp of the stored xml is the same as the one on the
            // server, the control then skips re-downloading the already up-to-date xml
            return DownloadType.NO_NEW_SONGS
        }
        val dbSongHandler = DBSongs
        dbSongHandler.deleteAll()
        dbSongHandler.addAll(list)

        return DownloadType.SONGS
    }
}