package com.example.wilko.songle

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

/**
 * Created by wilko on 10/28/2017.
 */


class KmlParser : XmlParser() {
    private val ns: String? = null
    private val collectedWords = HashMap<String, Boolean>()

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(input : InputStream, context: Context): List<Placemark> {
        input.use {
            val dbCollectedWordsHandler = DBCollectedWords(context)
            dbCollectedWordsHandler.populateHashMap(collectedWords)

            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            parser.nextTag()
            return readFeed(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun readFeed(parser: XmlPullParser): List<Placemark> {
        parser.require(XmlPullParser.START_TAG, ns, "kml")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the Document tag
            if (parser.name == "Document") {
                return readDocument(parser)
            } else {
                skip(parser)
            }
        }
        return emptyList()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun readDocument(parser: XmlPullParser): List<Placemark> {
        val entries = ArrayList<Placemark>()
        parser.require(XmlPullParser.START_TAG, ns, "Document")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.name == "Placemark") {
                val placemark = readPlacemark(parser)
                // add placemark only if not collected yet (in case of difficulty change)
                if (placemark.name !in collectedWords.keys){
                    entries.add(placemark)
                }
            } else {
                skip(parser)
            }
        }
        return entries
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readPlacemark(parser: XmlPullParser): Placemark {
        parser.require(XmlPullParser.START_TAG, ns, "Placemark")
        var name = ""
        var description = ""
        var styleUrl = ""
        var coordinates = Pair(0.0,0.0)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue
            when(parser.name){
                "name" -> name = readProp(parser, parser.name)
                "description" -> description = readProp(parser, parser.name)
                "styleUrl" -> styleUrl = readProp(parser, parser.name)
                "Point" -> coordinates = readCoordinates(parser)
                else -> skip(parser)
            }
        }
        return Placemark(name, description, styleUrl, coordinates.second, coordinates.first)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun readCoordinates(parser: XmlPullParser): Pair<Double, Double> {
        parser.require(XmlPullParser.START_TAG, ns, "Point")
        var result = Pair(0.0,0.0)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.name == "coordinates") {
                parser.require(XmlPullParser.START_TAG, ns, "coordinates")
                val coor = readText(parser)
                parser.require(XmlPullParser.END_TAG, ns, "coordinates")
                val coorS = coor.split(",")
                result = Pair(coorS[0].toDouble(), coorS[1].toDouble())
            } else {
                skip(parser)
            }
        }
        return result
    }
}