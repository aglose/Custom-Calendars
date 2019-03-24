package com.meticulous.creations.common.cal

import android.content.Context

/**
 * Our event interface. This calendar deals in ExpandedCalendarViewEvent objects
 */
interface CalendarViewEvent<T> {


    val priority: Int

    val eventObject: T


    fun getStatusColor(context: Context): Int
}