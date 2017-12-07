package com.example.wilko.songle.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by wilko on 12/7/2017.
 *
 * This class is Antonio Leiva's implementation of the notNullSingleValueVar delegate.
 * https://github.com/CesarValiente/KotlinForecastApp/blob/master/app/src/main/java/com/cesarvaliente/kotlinforecastapp/ui/utils/DelegatesExt.kt
 */

class NotNullSingleValueVar<T> : ReadWriteProperty<Any?, T> {

    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException("${property.name} " + "not initialized")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = if (this.value == null) value
        else throw IllegalStateException("${property.name} already initialized")
    }
}