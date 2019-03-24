package com.example.nikes.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.code.abode.verticalcalendar.ExpandedCalendarView
import java.util.*

class ExpandedCalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: ExpandedCalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expanded_calendar)

        calendarView = findViewById(R.id.calendar_view)
        calendarView.setOnDateListener(object : ExpandedCalendarView.OnDateClickListener{
            override fun onDateClick(dateSelected: Calendar) {

            }
        })

        calendarView.setSelectedDate(Calendar.getInstance(), null)
    }
}


