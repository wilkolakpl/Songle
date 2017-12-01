package com.example.wilko.songle

import android.content.ContentValues
import android.content.Context
import org.apache.commons.io.IOUtils
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by wilko on 10/25/2017.
 */
class DownloadLyrics(caller : AsyncCompleteListener<Pair<DownloadType, List<Song>>>, val wContext : WeakReference<Context>) : DownloadTask<Pair<DownloadType, List<Song>>>(caller){

    override fun loadFromNetwork(urlString: String) : Pair<DownloadType, List<Song>> {
        val context = wContext.get()
        if (context != null) {
            val dbHandler = MySongDBHandler(context)
            val songs = mutableListOf<Song>()
            dbHandler.populateList(songs)

            val db = dbHandler.writableDatabase
            for (song in songs) {
                val stream = downloadUrl(urlString + String.format("%02d",song.number) + "/lyrics.txt")
                val lyricString = IOUtils.toString(stream, "UTF-8")
                val ctn = ContentValues()
                ctn.put("lyric", lyricString)
                val noOfWords = lyricString.split(" ") as MutableList<String>
                noOfWords.removeAll(Arrays.asList(""))
                ctn.put("noOfWords", noOfWords.size)
                db.update("songs", ctn, "number=" + song.number, null)
            }
            db.close()
        }
        return Pair(DownloadType.LYRIC, emptyList())
    }
}