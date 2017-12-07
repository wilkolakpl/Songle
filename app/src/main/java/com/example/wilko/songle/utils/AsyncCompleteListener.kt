package com.example.wilko.songle.utils

/**
 * Created by wilko on 10/13/2017.
 */

interface AsyncCompleteListener<E> {
    fun asyncComplete(result: E?)
}