package com.example.wilko.songle.downloaders

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.example.wilko.songle.utils.AsyncCompleteListener
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
    override fun doInBackground(vararg vararg: String): E? {
        return try {
            loadFromNetwork()
        } catch (e: IOException) {
            // in this case, either there is no internet connection or unable to store locally
            // a message will be propagated to the UI: "No internet or not enough storage"
            Log.e(TAG, "IOException")
            null
        } catch (e: XmlPullParserException) {
            // this exception is not fixable by the user, so it will not be propagated to UI
            Log.e(TAG, "Error Parsing")
            null
        }
    }

    abstract fun loadFromNetwork(): E

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
    fun fromStreamToFile(context: Context, cacheFileName: String, inputStream: InputStream) {
        val file = File(context.filesDir, cacheFileName)
        FileUtils.copyInputStreamToFile(inputStream, file)
    }
}