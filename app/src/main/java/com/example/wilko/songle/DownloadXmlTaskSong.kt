package com.example.wilko.songle

/**
 * Created by wilko on 10/13/2017.
 */

class DownloadXmlTaskSong(caller : DownloadCompleteListener<Pair<DownloadType, List<Song>>>) : DownloadTask<Pair<DownloadType, List<Song>>>(caller){
    //val result = StringBuilder()
    override fun loadFromNetwork(urlString: String) : Pair<DownloadType, List<Song>>{
        val stream = downloadUrl(urlString)
        val xmlSongParser = XmlSongParser()
        return Pair(DownloadType.SONGS, xmlSongParser.parse(stream))
    }
}