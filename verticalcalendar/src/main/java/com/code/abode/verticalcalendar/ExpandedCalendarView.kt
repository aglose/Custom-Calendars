package com.code.abode.verticalcalendar

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meticulous.creations.common.cal.DayView
import com.meticulous.creations.common.cal.EventItem
import com.meticulous.creations.common.cal.util.ViewUtil
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.zakariya.stickyheaders.SectioningAdapter
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
        mRecyclerView?.layoutManager = LinearLayoutManager(context)
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
        if(!mActiveDaysList.isNullOrEmpty() && mAdapter?.numberOfSections!! > 0){
            for (i in 0 until TOTAL_MONTHS) {
                mAdapter?.getSectionForIndex(i)?.let { section ->
                    for (j in mActiveDaysList?.indices!!) {
                        val eventItem = mActiveDaysList?.get(j)
                        val dateTime = LocalDate.fromCalendarFields(eventItem?.dateCalendar)

                        // we found the specific date, now set flags
                        if (section.monthDate?.monthOfYear == dateTime.monthOfYear && section.monthDate?.year == dateTime.year) {
                            Timber.d(TAG, "setActiveDaysList section.calendarIndex: %s", section.calendarIndex)
                            section.flagForEvent = true
                            refresh = true
                        }
                    }
                }
            }
        }

        if (refresh) {
            mAdapter?.notifyAllSectionsDataSetChanged()
        }
    }

    private inner class MultiCalendarAdapter : SectioningAdapter() {

        private var startCalendarDate: LocalDate? = null
        private var endCalendarDate: LocalDate? = null

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

            val section = getSectionForIndex(sectionIndex)

            val mGridAdapter = object : MonthAdapter(section.monthDate, section.flagForEvent) {
                override fun onDate(localDate: LocalDate?) {
                    if (localDate != null) {
                        mOnDateClickListener?.onDateClick(Calendar.getInstance().apply { time = localDate.toDate() })
                    }
                }
            }
            if(gridHolder != null){
                gridHolder.calenderView.adapter = mGridAdapter

                // dynamically set the height of the calendar depending on how many rows necessary for the specific calendar
                val rowCount = Math.ceil(gridHolder.calenderView.count.toDouble() / 7.toDouble())
                Timber.d(TAG, "onBindItemViewHolder: %s", Math.ceil(gridHolder.calenderView.count.toDouble() / gridHolder.calenderView.numColumns.toDouble()))
                if (rowCount == 5.0 || rowCount == 6.0) {
                    gridHolder.calenderView.layoutParams.height = ViewUtil.dpToPx((DAY_VIEW_HEIGHT_DIPS * rowCount + 1.0 + DAY_VIEW_PADDING_DIPS.toDouble()).toInt())
                }
            }
        }

        override fun onBindHeaderViewHolder(viewHolder: SectioningAdapter.HeaderViewHolder?, sectionIndex: Int, headerType: Int) {
            val s = getSectionForIndex(sectionIndex)
            val hvh = viewHolder as StickyHeaderViewHolder?
            hvh?.headerText?.text = s.header
        }

        override fun onCreateGhostHeaderViewHolder(parent: ViewGroup): SectioningAdapter.GhostHeaderViewHolder {
            val ghostView = View(parent.context)
            ghostView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            return SectioningAdapter.GhostHeaderViewHolder(ghostView)
        }

        override fun getNumberOfSections(): Int {
            return TOTAL_MONTHS
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
            startCalendarDate = date.minusYears(1)
            endCalendarDate = date.plusMonths(TOTAL_MONTHS)

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
            scrollChange(adjustedPosition)
        }

        private fun scrollChange(position: Int) {
            mRecyclerView?.scrollToPosition(position)
        }

        fun getSectionForIndex(sectionIndex: Int) : ExpandedCalendarView.Section {
            val currentYear = LocalDate.now().year()
            val monthSection = Section()
            val sectionMonth = startCalendarDate?.plusMonths(sectionIndex)
            monthSection.calendarIndex = sectionIndex // calendarIndex is the same as sectionIndex passed by SectioningAdapter
            monthSection.monthDate = sectionMonth
            monthSection.header = if(currentYear.compareTo(sectionMonth) == 0){
                sectionMonth?.monthOfYear()?.getAsText(Locale.getDefault())
            }else{
                sectionMonth?.monthOfYear()?.getAsText(Locale.getDefault()) + " " + sectionMonth?.year()?.getAsText(Locale.getDefault())
            }
            return monthSection
        }


        /*
        *  This method assigns the section list items, their respecitve positions in the adapter. This
        *  has to be done after the calendar list is already build
        * */
        private fun setIndexes() {
            for (i in 0 until TOTAL_MONTHS) {
                val section = getSectionForIndex(i)
                section.headerPosition = getAdapterPositionForSectionHeader(i)
            }
        }

        /*
        *  Using a @param LocalDate this method returns the headerPosition in the adapter
        * */
        private fun findHeaderPositionInCalendar(date: LocalDate?): Int {
            for (i in 0 until TOTAL_MONTHS) {
                val section = getSectionForIndex(i)
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
    inner class Section {
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
        /* The current calendar instance being implemented */
        private val mCalendarInstance: LocalDate

        /* Integers used to keep track of implemented calendar */
        private val mMonth: Int
        private val mYear: Int

        /* Today's calendar */
        private val mToday: LocalDate

        /* List of each month's total number of days */
        private val mDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        private var flagForEvent = false
        private var lastSelectedDay = -1

        init {
            this.flagForEvent = flagForEvent
            if(date == null) date = LocalDate.now()
            mMonth = date?.monthOfYear!!
            mYear = date?.year!!
            mCalendarInstance = LocalDate(mYear, mMonth, 1)
            mToday = LocalDate.now()
        }

        protected abstract fun onDate(localDate: LocalDate?)

        private fun getDayViewForPosition(position: Int): DayView? {
            val firstDay = getDay(mCalendarInstance.dayOfWeek)

            if(position < firstDay){
                return null
            }

            val dayAdjustedPosition = position - firstDay + 1

            return if(dayAdjustedPosition < 32){
                val dayView = DayView()
                dayView.dateTime = mCalendarInstance.withDayOfMonth(dayAdjustedPosition)
                dayView.day = dayAdjustedPosition.toString()
                dayView.isToday = isToday(getDate(dayAdjustedPosition))
                if (flagForEvent) {
                    Timber.d(TAG, "populateMonth flagForEvent: $flagForEvent")
                    for (j in mActiveDaysList!!.indices) {
                        val eventItem = mActiveDaysList!![j]
                        val eventDate = LocalDate.fromCalendarFields(eventItem.eventDate)
                        if (dayAdjustedPosition == eventDate.dayOfMonth
                                && mCalendarInstance.monthOfYear == eventDate.monthOfYear
                                && mCalendarInstance.year == eventDate.year) {
                            dayView.isActiveDay = true
                            dayView.activeType = eventItem.eventStatus
                        }
                    }
                }
                dayView
            }else { null }
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
            Timber.d(TAG, "getDate: %s", date[DAY_FIELD])
            date[MONTH_FIELD] = mMonth
            date[YEAR_FIELD] = mYear

            return date
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            val holder: DateViewHolder

            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.expanded_calendar_day_layout, null, false)

                holder = DateViewHolder()
                holder.dayOfMonth = view!!.findViewById(R.id.day_of_month_text)
                holder.clickableBackground = view.findViewById(R.id.clickable_background)
                holder.selectedBackground = view.findViewById(R.id.background_view)
                holder.activityDot = view.findViewById(R.id.activity_dot)

                val dayView = getDayViewForPosition(position)
                if (dayView != null) {
                    Timber.d(TAG, "getView dayView: $dayView")
                    Timber.d(TAG, "getView position: $position")
                    holder.clickableBackground?.setOnClickListener {
                        lastSelectedDay = position
                        dayView.isSelected = true
                        mAdapter?.setSelectedDay(dayView.dateTime) //this will reset the selected item
                        Timber.d(TAG, "onClick: %s", dayView.dateTime.toString())
                        Timber.d(TAG, "onClick: %s", dayView.day)
                        onDate(dayView.dateTime)
                    }
                    holder.dayOfMonth!!.text = dayView.day

                    //active
                    if (dayView.isActiveDay) {
                        holder.activityDot!!.visibility = View.VISIBLE
                        Timber.d(TAG, "onBindViewHolder: %s", dayView.dateTime)
                        Timber.d(TAG, "onBindViewHolder: %s", dayView.activeType)
                        dayView.activeType?.getDotColor(context)?.let { holder.activityDot?.setTextColor(it) }
                    }

                    //selected
                    if (dayView.dateTime?.isEqual(selectedDate) == true) {
                        Timber.d(TAG, "selected: %s", dayView.day)
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
                        if (dayView.isToday) {
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
            return daysInMonth(mMonth) + getDay(mCalendarInstance.dayOfWeek)
        }

        override fun getItem(position: Int): Any? {
            return getDayViewForPosition(position)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
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

        const val TOTAL_MONTHS = 120
    }
}