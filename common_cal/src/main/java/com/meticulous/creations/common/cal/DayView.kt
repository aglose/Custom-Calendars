package com.meticulous.creations.common.cal

/**
 * Created by c74241 on 10/27/16.
 */

import org.joda.time.LocalDate

/**
 * DateView class
 */
class DayView {
    var dateTime: LocalDate? = null
    var day: String? = null
    var month: Int? = 0
    var year: Int? = 0
    var textColor: Int? = 0
    var activeType: ActivityDate.ActivityType? = null

    var dayOfWeek: String? = null
    var dayNumber: String? = null
    var monthShortName: String? = null
    var dateHolder: String? = null
    var isActiveDay: Boolean = false
    var isSelected: Boolean = false
    var isHideDayOfWeek: Boolean = false
    private var showMonth: Boolean = false
    var isToday: Boolean = false

    fun showMonthShortName(): Boolean {
        return showMonth
    }

    fun setShowMonthShortName(showMonth: Boolean) {
        this.showMonth = showMonth
    }
}