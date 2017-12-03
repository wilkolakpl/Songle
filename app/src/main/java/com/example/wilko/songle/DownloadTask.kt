package com.example.wilko.songle

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import org.apache.commons.io.FileUtils
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by wilko on 10/13/2017.
 *
 * Parent class for all of the downloads, it will be inherited from by all the domain specific ones
 */

abstract class DownloadTask<E>(private val caller : AsyncCompleteListener<E>) :
        AsyncTask<String, Void, E?>() {

    val TAG = "DownloadTask"
    override fun doInBackground(vararg urls: String): E? {
        return try {
            loadFromNetwork(urls[0])
        } catch (e: IOException) {
            Log.e(TAG, "IOException")
            null
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "Error Parsing")
            null
        }
    }

    abstract fun loadFromNetwork(urlString: String): E

    @Throws(IOException::class)
    fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        // Also available: HttpsURLConnection
        conn.readTimeout = 10000 // milliseconds
        conn.connectTimeout = 15000 // milliseconds
        conn.requestMethod = "GET"
        conn.doInput = true
        // Starts the query
        conn.connect()
        return conn.inputStream
    }

    override fun onPostExecute(result: E?) {
        super.onPostExecute(result)
        caller.asyncComplete(result)
    }

    @Throws(IOException::class)
    fun fromStreamtoCacheFile(context: Context, cacheFileName: String, inputStream: InputStream) {
        val file = File(context.filesDir, cacheFileName)
        FileUtils.copyInputStreamToFile(inputStream, file)
    }
}