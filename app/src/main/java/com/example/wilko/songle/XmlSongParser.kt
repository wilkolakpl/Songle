package com.example.wilko.songle

import android.content.Context
import android.util.Xml
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

        val prevTimestamp = getInfo("timestamp", context)
        val currTimestamp = parser.getAttributeValue(null, "timestamp")
        if (prevTimestamp == currTimestamp) {
            return entries
        }
        saveInfo("timestamp", currTimestamp, context)

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

    private fun getInfo(key: String, context: Context): String{
        val sharedPref = context.getSharedPreferences("permStrs", Context.MODE_PRIVATE)
        return sharedPref.getString(key, "")
    }

    private fun saveInfo(key: String, value: String, context: Context){
        val sharedPref = context.getSharedPreferences("permStrs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(key, value)
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
                "Link" -> link = readProp(parser, parser.name).split("/").last()
                else -> skip(parser)
            }
        }
        return Song(number.toInt(), artist, title, link, "", 0, "", "", "", "", "")
    }
}