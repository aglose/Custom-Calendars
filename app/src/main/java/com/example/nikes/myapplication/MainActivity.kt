package com.example.nikes.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        text_center.text = "yesser"
        text_center.setOnClickListener {
            val intent = Intent(this, ExpandedCalendarActivity::class.java)
            startActivity(intent)
        }
        calendar_view.buildCalendar(Calendar.getInstance())
    }
}
