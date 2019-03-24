package com.meticulous.creations.common.cal

import android.content.Context
import android.content.Intent
import java.util.*

/**
 * Created by c74241 on 10/19/16.
 */
class EventItem : Comparable<EventItem> {

    var header: String? = null
    var message: String? = null
    var time: String? = null
    var eventItemType: EventItemType? = null
    var eventStatus: ActivityDate.ActivityType = ActivityDate.ActivityType.UPCOMING
    var intent: Intent? = null
    var priority: Int = 0
    var eventDate: Calendar? = null
    var consultationId: Int = 0
    var dateCalendar: Calendar? = null
        get() = eventDate ?: Calendar.getInstance()

    enum class EventItemType {
        PROGRAM, CONSULTATION
    }

    //    public static ArrayList<EventItem> getEventDates(ArrayList<EventItem> eventItems){
    //        ArrayList<EventItem> dateList = new ArrayList<>();
    //        for(EventItem item : eventItems){
    //            dateList.addOrUpdate(LocalDateTime.fromCalendarFields(item.getEventDate()));
    //        }
    //        return dateList;
    //    }

    override fun compareTo(other: EventItem): Int {
        return if (eventItemType == EventItemType.CONSULTATION) {
            time?.compareTo(other.time.toString()) ?: 0
        } else {
            priority - other.priority
        }
    }



    fun getStatusColor(context: Context): Int {
        return eventStatus.getDotColor(context)
    }
}
