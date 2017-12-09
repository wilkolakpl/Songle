package com.example.wilko.songle.downloaders

import android.content.ContentValues
import com.example.wilko.songle.utils.AsyncCompleteListener
import com.example.wilko.songle.dataClasses.Song
import com.example.wilko.songle.databaseHelpers.DBSongs
import org.apache.commons.io.IOUtils
import java.util.*

/**
 * Created by wilko on 10/25/2017.
 */
class DownloadLyrics(caller : AsyncCompleteListener<DownloadType>) : DownloadTask<DownloadType>(caller){

    override fun loadFromNetwork() : DownloadType {
        val urlString = "http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"
        val dbHandler = DBSongs
        val songs = mutableListOf<Song>()
        dbHandler.populateList(songs)

        val db = dbHandler.writableDatabase
        for (song in songs) {
            val stream = downloadUrl(urlString + String.format("%02d",song.number) + "/lyrics.txt")
            val lyricString = IOUtils.toString(stream, "UTF-8")
            val ctn = ContentValues()
            ctn.put("lyric", lyricString)
            // calculating the number of words in a song, needed for the scoring system
            val noOfWords = lyricString.split(" ") as MutableList<String>
            noOfWords.removeAll(Arrays.asList(""))
            ctn.put("noOfWords", noOfWords.size)

            db.update("songs", ctn, "number=" + song.number, null)
        }
        db.close()

        return DownloadType.LYRICS
    }
}