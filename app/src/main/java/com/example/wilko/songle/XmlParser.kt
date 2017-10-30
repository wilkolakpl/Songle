package com.example.wilko.songle

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

/**
 * Created by wilko on 10/13/2017.
 */


abstract class XmlParser {
    private val ns: String? = null

//    @Throws(XmlPullParserException::class, IOException::class)
//    fun readPropandAttribute(parser: XmlPullParser, name: String): Pair<String, String> {
//        parser.require(XmlPullParser.START_TAG, ns, name)
//        val att = parser.getAttributeValue(null, "lol")
//    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun readProp(parser: XmlPullParser, name: String): String {
        parser.require(XmlPullParser.START_TAG, ns, name)
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, name)
        return title
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun skip(parser: XmlPullParser) {
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