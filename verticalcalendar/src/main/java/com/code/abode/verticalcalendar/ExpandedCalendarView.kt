package com.code.abode.verticalcalendar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import androidx.recyclerview.widget.RecyclerView
import com.meticulous.creations.common.cal.DayView
import com.meticulous.creations.common.cal.EventItem
import com.meticulous.creations.common.cal.util.ViewUtil
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.zakariya.stickyheaders.SectioningAdapter
import org.zakariya.stickyheaders.StickyHeaderLayoutManager
import timber.log.Timber
import java.util.*

/**
 * Created by Andrew Glose on 1/4/17
 *
 * This Calendar view is custom view used as a full screen multi calendar. Here the user can
 * see a specified amount of months in a vertically scrolling calendar. Month's are separated by
 * a "Sticky" Header that stays fixed at the top of the screen.
 *
 * Events can be added to this view in an ArrayList<EventItem> and be seen via activity 'dots'
 * located below the date number and colored according to the ActivityType
 *
</EventItem> */
@SuppressLint("TimberArgCount")
class ExpandedCalendarView : FrameLayout {
    private var mRecyclerView: RecyclerView? = null

    // the adapter that handles the 'sticky' headers and the gridviews
    private var mAdapter: MultiCalendarAdapter? = null

    // event list supplied by the caller
    private var mActiveDaysList: ArrayList<EventItem>? = null

    // on click listener to be implemented when a user clicks on a date
    private var mOnDateClickListener: OnDateClickListener? = null

    private var selectedDate: LocalDate? = null


    /*
    *  Public interface used to when user clicks on a specific date
    *
    *  @return dateSelected will be return to specify the date clicked
    * */
    interface OnDateClickListener {
        fun onDateClick(dateSelected: Calendar)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attributeSet: AttributeSet?) {
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val mRootView = inflater.inflate(R.layout.expanded_cal_layout, this, true) as FrameLayout
        mRecyclerView = mRootView.findViewById(R.id.calendar_recyclerview)

        mAdapter = MultiCalendarAdapter()
        mRecyclerView?.layoutManager = StickyHeaderLayoutManager()
        mRecyclerView?.adapter = mAdapter
    }

    /*
    * Set the date userDiscarded listener
    * */
    fun setOnDateListener(listener: OnDateClickListener) {
        mOnDateClickListener = listener
    }

    /*
    * Manually select a date to be highlighted in the Calendar. Also a set number of events can be
    * added
    * */
    fun setSelectedDate(date: Calendar, items: ArrayList<EventItem>?) {
        mAdapter?.setSelectedDayAndBuild(LocalDate.fromDateFields(date.time))
        if (items != null && items.size > 0) {
            setActiveDaysList(items)
        }
    }


    /*
    *  If the calendar has already been built we don't need to rebuild, just change the selected date
    * */
    fun changeToSelectedDate(date: LocalDate, items: ArrayList<EventItem>?) {
        mAdapter?.setSelectedDay(date)
        if (items != null && items.size > 0) {
            setActiveDaysList(items)
        }
    }

    /*
    * Set the list which will highlight certain days of the calendar to show there is activity
    * */
    fun setActiveDaysList(items: ArrayList<EventItem>) {
        mActiveDaysList = items
        var refresh = false
        if(!mActiveDaysList.isNullOrEmpty() && mAdapter?.sectionCalendarList?.isNotEmpty() == true){
            for (i in 0 until mAdapter?.sectionCalendarList?.size()!!) {
                val section = mAdapter?.sectionCalendarList?.get(i)
                for (j in mActiveDaysList?.indices!!) {
                    val eventItem = mActiveDaysList?.get(j)
                    val dateTime = LocalDate.fromCalendarFields(eventItem?.dateCalendar)

                    // we found the specific date, now set flags
                    if (section?.monthDate?.monthOfYear == dateTime.monthOfYear && section.monthDate?.year == dateTime.year) {
                        Timber.d(TAG, "setActiveDaysList section.calendarIndex: " + section.calendarIndex)
                        section.flagForEvent = true
                        refresh = true
                    }
                }
            }
        }

        if (refresh) {
            mAdapter?.notifyAllSectionsDataSetChanged()
        }
    }

    private inner class MultiCalendarAdapter : SectioningAdapter() {

        var sectionCalendarList: SparseArray<ExpandedCalendarView.Section>? = null

        private var startCalendarDate: LocalDate? = null
        private var endCalendarDate: LocalDate? = null

        private val mSelectedSectionIndex: Int = 0
        private var mSelectedItemPosition: Int = 0

        override fun onCreateItemViewHolder(parent: ViewGroup?, itemType: Int): SectioningAdapter.ItemViewHolder {
            val inflater = LayoutInflater.from(parent?.context)
            val v = inflater.inflate(R.layout.calendar_grid_layout, parent, false)
            return CalendarGridViewHolder(v)
        }

        override fun onCreateHeaderViewHolder(parent: ViewGroup?, headerType: Int): SectioningAdapter.HeaderViewHolder {
            val inflater = LayoutInflater.from(parent?.context)
            val v = inflater.inflate(R.layout.calendar_header_layout, parent, false)
            return StickyHeaderViewHolder(v)
        }

        override fun onBindItemViewHolder(viewHolder: SectioningAdapter.ItemViewHolder?, sectionIndex: Int, itemIndex: Int, itemType: Int) {
            val gridHolder = viewHolder as CalendarGridViewHolder?

            val section = sectionCalendarList?.get(sectionIndex) //sectionIndex is the same as the section.calendarIndex

            val mGridAdapter = object : MonthAdapter(section?.monthDate, section?.flagForEvent ?: false) {
                override fun onDate(localDate: LocalDate?) {
                    if (localDate != null) {
                        mOnDateClickListener?.onDateClick(Calendar.getInstance().apply { time = localDate.toDate() })
                    }
                }
            }
            gridHolder!!.calenderView.adapter = mGridAdapter

            // dynamically set the height of the calendar depending on how many rows necessary for the specific calendar
            val rowCount = Math.ceil(gridHolder.calenderView.count.toDouble() / 7.toDouble())
            Timber.d(TAG, "onBindItemViewHolder: " + Math.ceil(gridHolder.calenderView.count.toDouble() / gridHolder.calenderView.numColumns.toDouble()))
            if (rowCount == 5.0 || rowCount == 6.0) {
                gridHolder.calenderView.layoutParams.height = ViewUtil.dpToPx((DAY_VIEW_HEIGHT_DIPS * rowCount + 1.0 + DAY_VIEW_PADDING_DIPS.toDouble()).toInt())
            }
        }

        override fun onBindHeaderViewHolder(viewHolder: SectioningAdapter.HeaderViewHolder?, sectionIndex: Int, headerType: Int) {
            val s = sectionCalendarList!!.get(sectionIndex)
            val hvh = viewHolder as StickyHeaderViewHolder?
            hvh?.headerText?.text = s.header
        }

        override fun onCreateGhostHeaderViewHolder(parent: ViewGroup): SectioningAdapter.GhostHeaderViewHolder {
            val ghostView = View(parent.context)
            ghostView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            return SectioningAdapter.GhostHeaderViewHolder(ghostView)
        }

        override fun getNumberOfSections(): Int {
            return if (sectionCalendarList == null) 0 else sectionCalendarList!!.size()
        }

        /* Always make sure the header has a section */
        override fun doesSectionHaveHeader(sectionIndex: Int): Boolean {
            return true
        }

        /* Each section will contain one calendar */
        override fun getNumberOfItemsInSection(sectionIndex: Int): Int {
            return 1
        }

        /*
        *  Set the adapter to a specific day, build the necessary calendars, set the indices and notify
        *  the adapter about the changes
        * */
        fun setSelectedDayAndBuild(date: LocalDate) {
            selectedDate = date
            val start = Calendar.getInstance()
            start.set(Calendar.YEAR, 2017)
            start.set(Calendar.DAY_OF_YEAR, 1)
            startCalendarDate = LocalDate.fromCalendarFields(start) // Always start Jan 1, 2017
            endCalendarDate = date.plusYears(1) // End 2 years from today's date

            buildCalendarList()
            setIndexes()
            mSelectedItemPosition = findHeaderPositionInCalendar(selectedDate)
            notifyAllSectionsDataSetChanged()

            Timber.d(TAG, "setSelectedDayAndBuild: $mSelectedItemPosition")
            mRecyclerView?.scrollToPosition(mSelectedItemPosition)
        }

        internal fun setSelectedDay(date: LocalDate?) {
            Timber.d(TAG, "setSelectedDay: $date")
            selectedDate = date
            var position = findHeaderPositionInCalendar(selectedDate)
            if (position == 0) {
                position = mSelectedItemPosition
            }
            val adjustedPosition = position
            notifyAllSectionsDataSetChanged()
            Timber.d(TAG, "setSelectedDay position: $position")
            Thread {
                try {
                    Thread.sleep(800)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                (context as Activity).runOnUiThread { scrollChange(adjustedPosition) }
            }.run()

        }

        private fun scrollChange(position: Int) {
            mRecyclerView?.scrollToPosition(position)
        }

        /*
        *  With the selected date chosen, a padding of minus and plus 6 months is used to create a year
        *  window for the calendar view
        * */
        private fun buildCalendarList() {

            var monthCount = 0
            sectionCalendarList = SparseArray()
            Timber.d(TAG, "buildCalendarList startCalendarDate: " + startCalendarDate!!)
            Timber.d(TAG, "buildCalendarList endCalendarDate: " + endCalendarDate!!)
            while (startCalendarDate!!.toDate().before(endCalendarDate!!.toDate())) {
                val monthSection = Section()
                monthSection.calendarIndex = monthCount // calendarIndex is the same as sectionIndex passed by SectioningAdapter
                monthSection.monthDate = startCalendarDate
                monthSection.header = startCalendarDate!!.monthOfYear().getAsText(Locale.getDefault())
                monthSection.header = monthSection.header + " " + startCalendarDate!!.year().getAsText(Locale.getDefault())
                sectionCalendarList!!.put(monthSection.calendarIndex, monthSection)

                monthCount++
                startCalendarDate = startCalendarDate!!.plusMonths(1)
                Timber.d(TAG, "buildCalendarList monthDate: " + startCalendarDate!!.toString())
                Timber.d(TAG, "buildCalendarList header: " + startCalendarDate!!.monthOfYear().asShortText)
            }
        }

        /*
        *  This method assigns the section list items, their respecitve positions in the adapter. This
        *  has to be done after the calendar list is already build
        * */
        private fun setIndexes() {
            for (i in 0 until sectionCalendarList!!.size()) {
                val section = sectionCalendarList!!.get(i)
                section.headerPosition = getAdapterPositionForSectionHeader(i)
            }
        }

        /*
        *  Using a @param LocalDate this method returns the headerPosition in the adapter
        * */
        private fun findHeaderPositionInCalendar(date: LocalDate?): Int {
            for (i in 0 until sectionCalendarList!!.size()) {
                val section = sectionCalendarList!!.get(i)
                if (section.monthDate!!.year == date!!.year && section.monthDate!!.monthOfYear == date.monthOfYear) {
                    return section.headerPosition
                }
            }
            return 0
        }

        /*
        *  Basic DateViewHolder Pattern for the Item beneath each Header
        * */
        inner class CalendarGridViewHolder constructor(itemView: View) : SectioningAdapter.ItemViewHolder(itemView) {
            internal var calenderView: GridView = itemView.findViewById(R.id.gridview)
        }

        /*
        *  Basic DateViewHolder for the Sticky Headers
        * */
        inner class StickyHeaderViewHolder constructor(headerView: View) : SectioningAdapter.HeaderViewHolder(headerView) {
            internal var headerText: TextView = itemView.findViewById(R.id.header_month)
        }
    }

    /*
    *  Section object used to keep track of the month metadata
    * */
    private inner class Section {
        internal var headerPosition: Int = 0
        internal var calendarIndex: Int = 0
        internal var header: String? = null
        internal var monthDate: LocalDate? = null
        internal var flagForEvent: Boolean = false
    }

    /*
    * This adapter is used to populate the GridView which in turn is used to display each month's list
    * of numbers. Active days are displayed here as well
    * */
    abstract inner class MonthAdapter constructor(var date: LocalDate?, flagForEvent: Boolean) : BaseAdapter() {

        private val inflater: LayoutInflater

        /* The current calendar instance being implemented */
        private val mCalendarInstance: LocalDate

        /* Integers used to keep track of implemented calendar */
        private val mMonth: Int
        private val mYear: Int
        private var mDaysShown: Int = 0
        private var mDaysLastMonth: Int = 0

        /* Today's calendar */
        private val mToday: LocalDate

        /* List to hold all day item objects */
        private var mDayViewContainer = ArrayList<DayView?>()

        /* List of each month's total number of days */
        private val mDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        private var flagForEvent = false

        init {
            this.flagForEvent = flagForEvent
            if(date == null) date = LocalDate.now()
            mMonth = date?.monthOfYear!!
            mYear = date?.year!!
            mCalendarInstance = LocalDate(mYear, mMonth, 1)
            mToday = LocalDate.now()
            populateMonth()
            this.inflater = LayoutInflater.from(context)
        }

        protected abstract fun onDate(localDate: LocalDate?)

        private fun populateMonth() {
            mDayViewContainer = ArrayList()

            var dayView: DayView

            val firstDay = getDay(mCalendarInstance.dayOfWeek)

            Timber.d(TAG, "populateMonth firstDay: $firstDay")
            for (i in 0 until firstDay) {
                dayView = DayView()
                dayView.dateTime = null
                mDayViewContainer.add(dayView)
                mDaysLastMonth++
                mDaysShown++
            }

            val daysInMonth = daysInMonth(mMonth)
            for (day in 1..daysInMonth) {
                if (day >= 32) {
                    //some reason January 2020 has a 32 day number
                    Timber.d(TAG, "populateMonth: $mMonth")
                    Timber.d(TAG, "populateMonth: $mYear")

                } else {
                    dayView = DayView()
                    dayView.dateTime = mCalendarInstance.withDayOfMonth(day)
                    dayView.day = day.toString()
                    dayView.isToday = isToday(getDate(day))
                    if (flagForEvent) {
                        Timber.d(TAG, "populateMonth flagForEvent: $flagForEvent")
                        for (j in mActiveDaysList!!.indices) {
                            val eventItem = mActiveDaysList!![j]
                            val eventDate = LocalDate.fromCalendarFields(eventItem.eventDate)
                            if (day == eventDate.dayOfMonth
                                    && mCalendarInstance.monthOfYear == eventDate.monthOfYear
                                    && mCalendarInstance.year == eventDate.year) {
                                dayView.isActiveDay = true
                                dayView.activeType = eventItem.eventStatus
                            }
                        }
                    }

                    mDaysShown++
                    mDayViewContainer.add(dayView)
                }

            }
        }

        /*
        * Days in month returned. (ie. January = 1)
        * */
        private fun daysInMonth(month: Int): Int {
            var daysInMonth = mDaysInMonth[month - 1]
            if (month == 1 && mCalendarInstance.year().isLeap) {
                daysInMonth++
            }
            return daysInMonth
        }

        /*
        *  This method is used to organize the Jodatime constants to match the order we want this
        *  calendar to be laid out
        * */
        private fun getDay(day: Int): Int {
            return when (day) {
                DateTimeConstants.SUNDAY -> 0
                DateTimeConstants.MONDAY -> 1
                DateTimeConstants.TUESDAY -> 2
                DateTimeConstants.WEDNESDAY -> 3
                DateTimeConstants.THURSDAY -> 4
                DateTimeConstants.FRIDAY -> 5
                DateTimeConstants.SATURDAY -> 6
                else -> 0
            }
        }

        private fun isToday(date: IntArray): Boolean {
            return (mToday.year == date[YEAR_FIELD]
                    && mToday.monthOfYear == date[MONTH_FIELD]
                    && mToday.dayOfMonth == date[DAY_FIELD])
        }

        /*
        *  Return an int[] which has the day, month and year of the paramter passed
        * */
        private fun getDate(position: Int): IntArray {
            val date = IntArray(3)
            date[DAY_FIELD] = position
            Timber.d(TAG, "getDate: " + date[DAY_FIELD])
            date[MONTH_FIELD] = mMonth
            date[YEAR_FIELD] = mYear

            return date
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            val holder: DateViewHolder

            if (view == null) {
                view = inflater.inflate(R.layout.expanded_calendar_day_layout, null, false)

                holder = DateViewHolder()
                holder.dayOfMonth = view!!.findViewById(R.id.day_of_month_text)
                holder.clickableBackground = view.findViewById(R.id.clickable_background)
                holder.selectedBackground = view.findViewById(R.id.background_view)
                holder.activityDot = view.findViewById(R.id.activity_dot)

                if (mDayViewContainer[position] != null && mDayViewContainer[position]?.dateTime != null) {
                    val dayView = mDayViewContainer[position]
                    Timber.d(TAG, "getView dayView: $dayView")
                    Timber.d(TAG, "getView position: $position")
                    Timber.d(TAG, "getView mDaysLastMonth: $mDaysLastMonth")
                    holder.clickableBackground?.setOnClickListener {
                        for (dayView1 in mDayViewContainer) {
                            dayView1?.isSelected = false
                        }
                        dayView?.isSelected = true
                        mAdapter?.setSelectedDay(dayView?.dateTime) //this will reset the selected item
                        Timber.d(TAG, "onClick: " + dayView?.dateTime.toString())
                        Timber.d(TAG, "onClick: " + dayView?.day)
                        onDate(dayView?.dateTime)
                    }
                    holder.dayOfMonth!!.text = dayView?.day

                    //active
                    if (dayView?.isActiveDay == true) {
                        holder.activityDot!!.visibility = View.VISIBLE
                        Timber.d(TAG, "onBindViewHolder: " + dayView.dateTime)
                        Timber.d(TAG, "onBindViewHolder: " + dayView.activeType)
                        dayView.activeType?.getDotColor(context)?.let { holder.activityDot?.setTextColor(it) }
                    }

                    //selected
                    if (dayView?.dateTime?.isEqual(selectedDate) == true) {
                        Timber.d(TAG, "selected: " + dayView.day)
                        holder.dayOfMonth?.setTextColor(getColor(R.color.white))
                        holder.selectedBackground?.visibility = View.VISIBLE
                        holder.activityDot?.setTextColor(getColor(R.color.white))
                        if (dayView.isToday) {
                            //today
                            holder.selectedBackground?.setBackgroundResource(R.drawable.rounded_primary)
                        } else {
                            holder.selectedBackground?.setBackgroundResource(R.drawable.rounded_secondary)
                        }
                    } else {
                        if (dayView?.isToday == true) {
                            //today
                            holder.dayOfMonth?.setTextColor(getColor(R.color.primaryColor))
                        } else {
                            holder.dayOfMonth?.setTextColor(getColor(R.color.primaryDarkColor))
                        }

                        holder.selectedBackground?.visibility = View.INVISIBLE
                    }
                }

                view.tag = holder
            } else {
                holder = view.tag as DateViewHolder
            }

            return view
        }

        private fun getColor(colorResId: Int): Int {
            return ContextCompat.getColor(context, colorResId)
        }

        override fun getCount(): Int {
            return mDayViewContainer.size
        }

        override fun getItem(position: Int): Any? {
            return mDayViewContainer[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        private fun selectDay(position: Int) {
            val dayView = mDayViewContainer[position]
            dayView?.isSelected = true
        }

        /*
        *  ViewHolder pattern for the DayView objects in the calendar
        * */
        private inner class DateViewHolder {
            internal var dayOfMonth: TextView? = null
            internal var activityDot: TextView? = null
            internal var selectedBackground: View? = null
            internal var clickableBackground: FrameLayout? = null
        }
    }

    companion object {
        val TAG = "ExpandedCalView"

        // constants for the calendar day layout
        private val DAY_VIEW_HEIGHT_DIPS = 50
        private val DAY_VIEW_PADDING_DIPS = 30 // 10 + 20

        private val DAY_FIELD = 0
        private val MONTH_FIELD = 1
        private val YEAR_FIELD = 2
    }
}