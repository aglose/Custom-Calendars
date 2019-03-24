package com.meticulous.creations.common.cal

import android.content.Context

import androidx.core.content.ContextCompat

import org.joda.time.LocalDateTime

/**
 * Created by c74241 on 11/29/16.
 */
class ActivityDate {
    var dateTime: LocalDateTime? = null
    var activityType: ActivityType? = null

    enum class ActivityType constructor(private val dotColor: Int) {
        COMPLETED(R.color.primaryLightColor),
        UPCOMING(R.color.secondaryColor),
        VIDEO_CONSULT(R.color.secondaryLightColor),
        SKIPPED(R.color.primaryColor),
        PAST_DUE(R.color.secondaryTextColor),
        SCHEDULE(R.color.primaryLightColor);

        val priority: Int
            get() {
                return when (this) {
                    COMPLETED -> 0
                    UPCOMING -> 2
                    SKIPPED -> 3
                    PAST_DUE -> 1
                    VIDEO_CONSULT -> 4
                    else -> 0
                }
            }

        fun getDotColor(context: Context): Int {
            return ContextCompat.getColor(context, dotColor)
        }
    }
}
