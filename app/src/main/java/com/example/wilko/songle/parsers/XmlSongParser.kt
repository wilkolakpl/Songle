package com.example.wilko.songle.parsers

import android.content.Context
import android.util.Xml
import com.example.wilko.songle.dataClasses.Song
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

/**
 * Created by wilko on 10/13/2017.
 */

class XmlSongParser : XmlParser() {
    private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(input : InputStream, context: Context): List<Song> {
        input.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            parser.nextTag()
            return readFeed(parser, context)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun readFeed(parser: XmlPullParser, context: Context): List<Song> {
        val entries = ArrayList<Song>()
        parser.require(XmlPullParser.START_TAG, ns, "Songs")

        // compares the timestamp of the stored xml with the one on the server
        val prevTimestamp = getTimestamp(context)
        val currTimestamp = parser.getAttributeValue(null, "timestamp")
        if (prevTimestamp == currTimestamp) {
            // if they are the same, the parsing ends here, and an empty list is returned
            return entries
        }
        setTimestamp(currTimestamp, context)

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the song tag
            if (parser.name == "Song") {
                entries.add(readSong(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    private fun getTimestamp(context: Context): String{
        val sharedPref = context.getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        return sharedPref.getString("timestamp", "")
    }

    private fun setTimestamp(value: String, context: Context){
        val sharedPref = context.getSharedPreferences("stateVars", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("timestamp", value)
        editor.apply()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readSong(parser: XmlPullParser): Song {
        parser.require(XmlPullParser.START_TAG, ns, "Song")
        var number = ""
        var artist = ""
        var title = ""
        var link = ""
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue
            when(parser.name){
                "Number" -> number = readProp(parser, parser.name)
                "Artist" -> artist = readProp(parser, parser.name)
                "Title" -> title = readProp(parser, parser.name)
                // formatting the link to be read by a YouTubePlayer
                "Link" -> link = readProp(parser, parser.name).split("/").last()
                else -> skip(parser)
            }
        }
        return Song(number.toInt(), artist, title, link, "", 0, "", "", "", "", "")
    }
}