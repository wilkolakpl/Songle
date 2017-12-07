package com.example.wilko.songle.utils

import kotlin.properties.ReadWriteProperty

/**
 * Created by wilko on 12/7/2017.
 *
 * This class is Antonio Leiva's implementation of the notNullSingleValue delegate.
 * https://github.com/CesarValiente/KotlinForecastApp/blob/master/app/src/main/java/com/cesarvaliente/kotlinforecastapp/ui/utils/DelegatesExt.kt
 */

object DelegatesExt {
    fun <T> notNullSingleValue(): ReadWriteProperty<Any?, T> = NotNullSingleValueVar()
}