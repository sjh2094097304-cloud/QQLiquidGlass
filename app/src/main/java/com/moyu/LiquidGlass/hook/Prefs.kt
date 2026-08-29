package com.moyu.LiquidGlass.hook

import android.content.Context

object Prefs {
    const val PREFS = "qq_liquid_glass"

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("enabled", true)

    fun cornerRadius(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("corner_radius", 28).toFloat()

    fun refraction(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("refraction", 48).toFloat()

    fun dispersion(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("dispersion", 10).toFloat()
}
