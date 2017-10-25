package com.example.wilko.songle

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

/**
 * Created by wilko on 10/13/2017.
 */


class XmlSongParser {
    private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(input : InputStream): List<Song> {
        input.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            parser.nextTag()
            return readFeed(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): List<Song> {
        val entries = ArrayList<Song>()
        parser.require(XmlPullParser.START_TAG, ns, "Songs")
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
                "Link" -> link = readProp(parser, parser.name)
                else -> skip(parser)
            }
        }
        return Song(number.toInt(), artist, title, link, "", "", "", "", "", "")
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readProp(parser: XmlPullParser, name: String): String {
        parser.require(XmlPullParser.START_TAG, ns, name)
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, name)
        return title
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}