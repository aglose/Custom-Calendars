package com.code.abode.horizontalcalendar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.meticulous.creations.common.cal.ActivityDate
import com.meticulous.creations.common.cal.DayView
import com.meticulous.creations.common.cal.EventItem
import com.meticulous.creations.common.cal.util.ViewUtil
import org.joda.time.Days
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("TimberArgCount")
class HorizontalCalendarView : LinearLayout {
    //Listeners
    private var mOnDateClickListener: OnDateClickListener? = null
    private var mOnHeaderClickListener: OnHeaderClickListener? = null
    private var mHeaderUpdateListener: OnHeaderUpdateListener? = null

    //booleans
    private val mAlwaysDisplayMonth: Boolean = false
    private val mSelectTodayOnLoad: Boolean = false
    private var mHideHeaderView: Boolean = false
    private var mDisplayTimeInHeader: Boolean = false
    private var mHideCalendar: Boolean = false
    private var mShowCalendarIcon: Boolean = false
    private var mShowCalendarArrowIcon: Boolean = false
    private var preventFutureDateClick: Boolean = false

    //main components
    private var mRootView: LinearLayout? = null
    private var mRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mAdapter: HorizontalCalendarAdapter? = null
    private var mHeaderTitleView: TextView? = null
    private var mHeaderTimeView: TextView? = null
    private var mHeaderCalendarIcon: ImageView? = null
    private var mHeaderCalendarArrowIcon: ImageView? = null

    private var currentDate: LocalDateTime? = null //current date used temporarily during scrolls and other actions
    var ldfSelectedDate: LocalDateTime? = null
        private set //the current highlighted date
    private var calendarStartRange: LocalDateTime? = null //the start date of the calendar from the selected day
    private var calendarEndRange: LocalDateTime? = null //the end date of the calendar from the selected day
    private var totalNumberOfDaysInCalendar: Int = 0 //the full number of days the calendar will allow
    private val scrollLock = AtomicBoolean(false) //set lock to open for scrolling

    private var mNormalModeDateFormat: SimpleDateFormat? = null
    private var mEditModeDateFormat: SimpleDateFormat? = null
    private var mTimeFormat: SimpleDateFormat? = null
    private var defaultLocale: Locale? = null

    private var mActiveDaysMap: SparseArray<ArrayList<EventItem>>? = null
    private var mAvailableAppointments: SparseArray<LocalDateTime>? = null

    val selectedDate: Calendar
        get() {
            val cal = Calendar.getInstance()
            cal.time = ldfSelectedDate?.toDate()
            return cal
        }

    private var currentDayView: DayView? = null


    interface OnDateClickListener {
        fun onDateClick(dateSelected: LocalDateTime, position: Int)
    }

    interface OnHeaderClickListener {
        fun onClick()
    }

    interface OnHeaderUpdateListener {
        fun onUpdate(updatedHeaderDate: String?, updatedHeaderTime: String?)
    }

    /**
     * Constructors
     */
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    /**
     * Initialization
     */
    private fun init(context: Context, attributeSet: AttributeSet?) {
        //Get custom attributes
        if (attributeSet != null) {
            val a = context.theme.obtainStyledAttributes(attributeSet, R.styleable.HorizontalCalendarView, 0, 0)

            try {
                //Colors
                mDayTextColor = getColor(R.color.primaryTextColor)
                mSelectedDayTextColor = getColor(R.color.secondaryTextColor)
                mTodayTextColor = getColor(R.color.primaryDarkColor)

                mDaysContainerBackgroundColor = a.getColor(R.styleable.HorizontalCalendarView_daysContainerBackgroundColor, getColor(R.color.secondaryColor))
                mSelectedDayBackgroundColor = getColor(R.color.primaryColor)
                //                int mTodayBackgroundColor = getColor(R.color.cigna_orange);

                //Labels
                mHideHeaderView = a.getBoolean(R.styleable.HorizontalCalendarView_hideHeader, false)
                mDisplayTimeInHeader = a.getBoolean(R.styleable.HorizontalCalendarView_showTimeInHeader, false)
                mHideCalendar = a.getBoolean(R.styleable.HorizontalCalendarView_hideCalendar, false)
                mShowCalendarIcon = a.getBoolean(R.styleable.HorizontalCalendarView_showCalendarIcon, false)
                mShowCalendarArrowIcon = a.getBoolean(R.styleable.HorizontalCalendarView_showCalendarArrowIcon, false)
            } finally {
                a.recycle()
            }
        }

        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mRootView = inflater.inflate(R.layout.horizontal_calendar_view, this, true) as LinearLayout

        /* RecyclerView setup Begin */
        mRecyclerView = mRootView?.findViewById(R.id.calendar)
        if (mHideCalendar) {
            mRecyclerView?.visibility = View.GONE
        }
        mLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mRecyclerView?.layoutManager = mLayoutManager
        mAdapter = HorizontalCalendarAdapter()

        currentDate = LocalDateTime.now()
        ldfSelectedDate = currentDate
        mRecyclerView?.adapter = mAdapter
        mAdapter?.selectedPosition?.let { mLayoutManager?.scrollToPosition(it) }
        /* RecyclerView setup End */

        /* Header View setup Begin */
        val mHeaderRootView = mRootView?.findViewById<RelativeLayout>(R.id.calendar_header_layout)
        mHeaderCalendarIcon = mRootView?.findViewById(R.id.calendar_icon)
        mHeaderCalendarArrowIcon = mRootView?.findViewById(R.id.calendar_arrow_icon)
        if (mShowCalendarIcon) {
            mHeaderCalendarIcon?.visibility = View.VISIBLE
        }
        if (mShowCalendarArrowIcon) {
            mHeaderCalendarArrowIcon?.visibility = View.VISIBLE
        }
        mHeaderTitleView = mRootView?.findViewById(R.id.month_header)
        mHeaderTimeView = mRootView?.findViewById(R.id.time_header)
        if (mHideHeaderView) {
            mHeaderRootView?.visibility = View.GONE
        }
        if (mDisplayTimeInHeader) {
            mHeaderTimeView?.visibility = View.VISIBLE
        } else {
            mHeaderTimeView?.visibility = View.GONE
        }
        mNormalModeDateFormat = SimpleDateFormat(context.getString(R.string.calendar_header_normal_date_format), Locale.getDefault())
        mEditModeDateFormat = SimpleDateFormat(context.getString(R.string.calendar_header_edit_date_format), Locale.getDefault())
        mTimeFormat = SimpleDateFormat(context.getString(R.string.calendar_header_time_format), Locale.getDefault())

        defaultLocale = Locale.getDefault()
        /* Header View setup End */
    }

    /*
    * Set the date userDiscarded listener
    *
    * */
    fun setOnDateClickListener(listener: OnDateClickListener) {
        mOnDateClickListener = listener
    }

    /*
    * Set the header userDiscarded listener
    *
    * */
    fun setOnHeaderClickListener(listener: OnHeaderClickListener) {
        mOnHeaderClickListener = listener
        mHeaderTitleView?.setOnClickListener {  mOnHeaderClickListener?.onClick() }
    }

    /*
   * If the user hides the header they can set this listener to be notified of a new header when
   * the user changes or selects a date
   *
   * */
    fun setOnHeaderUpdateListener(listener: OnHeaderUpdateListener) {
        mHeaderUpdateListener = listener
    }

    /*
    * Manually select a date use a LocalDateTime object
    *
    * */
    fun setSelectedDate(date: LocalDate) {
        mAdapter?.setSelectedDay(date.monthOfYear, date.dayOfMonth, date.year)
    }

    /*
    * Manually select a date use a Calendar object
    *
    * */
    fun setSelectedDate(date: Calendar) {
        val dateTime = LocalDateTime.fromCalendarFields(date)
        mAdapter?.setSelectedDay(dateTime.monthOfYear, dateTime.dayOfMonth, dateTime.year)
        ldfSelectedDate = dateTime
    }

    /*
    * Set a different type of format for the header
    *
    * */
    fun setHeaderDateFormat(sdf: SimpleDateFormat) {
        mNormalModeDateFormat = sdf
    }

    /*
    * Set a different type of format for the header
    *
    * */
    fun setHeaderTimeFormat(sdf: SimpleDateFormat) {
        mTimeFormat = sdf
    }


    /*
    * Set the list which will highlight certain days of the calendar to show there is activity
    * */
    fun setActiveDaysList(list: ArrayList<EventItem>) {
        mActiveDaysMap = SparseArray()
        for (i in list.indices) {
            val eventItem = list[i]
            val calendar = eventItem.eventDate
            //adjust month by 1 due to Calendar and JodaTime differences
            val index = findPositionById(calendar?.get(Calendar.DAY_OF_MONTH), calendar?.get(Calendar.MONTH)?.plus(1), calendar?.get(Calendar.YEAR))
            val previousItems = if (mActiveDaysMap?.get(index) == null) ArrayList() else mActiveDaysMap?.get(index)
            previousItems?.add(eventItem)
            mActiveDaysMap?.put(index, previousItems)
        }
        mAdapter?.notifyDataSetChanged()
    }

    /*
    * Highlight the available dates in the adapter list
    * */
    fun setAvailableAppointmentDays(eventDates: ArrayList<LocalDateTime>) {
        mAvailableAppointments = SparseArray()
        for (i in eventDates.indices) {
            val date = eventDates[i]
            val index = findPositionById(date.dayOfMonth, date.monthOfYear, date.year)
            Timber.d(TAG, "findPositionById date: $date")
            mAvailableAppointments?.put(index, date)
        }
        mAdapter?.notifyDataSetChanged()
    }

    /*
    * Convert EventItem list to an ActivityDate list
    * */
    private fun convertEvents(list: ArrayList<EventItem>): ArrayList<ActivityDate> {
        val activityDates = ArrayList<ActivityDate>()
        var date: ActivityDate
        for (i in list.indices) {
            val eventItem = list[i]
            date = ActivityDate()
            date.dateTime = LocalDateTime.fromCalendarFields(eventItem.dateCalendar)
            activityDates.add(date)
        }
        return activityDates
    }

    /*
    * Toggle the view of the calendar icon
    * */
    fun showCalendarIcon(value: Boolean) {
        if (value) {
            mHeaderCalendarIcon?.visibility = View.VISIBLE
        } else {
            mHeaderCalendarIcon?.visibility = View.GONE
        }
    }

    /*
    * Toggle the view of the calendar arrow icon
    * */
    fun showCalendarArrowIcon(value: Boolean) {
        if (value) {
            mHeaderCalendarArrowIcon?.visibility = View.VISIBLE
        } else {
            mHeaderCalendarArrowIcon?.visibility = View.GONE
        }
    }

    /*
    * Toggle the view of the calendar
    * */
    fun showCalendarView(value: Boolean) {
        if (value) {
            mRootView?.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.horizontal_full_height))
            mRecyclerView?.visibility = View.VISIBLE
            mHeaderTitleView?.text = mNormalModeDateFormat?.format(ldfSelectedDate?.toDate())
        } else {
            mRootView?.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.horizontal_header_height))
            mRecyclerView?.visibility = View.GONE
            mHeaderTitleView?.text = mEditModeDateFormat?.format(ldfSelectedDate?.toDate())
        }
    }

    /*
    * Toggle the time view in the header
    * */
    fun showHeaderTime(value: Boolean) {
        mDisplayTimeInHeader = value
        if (value) {
            mHeaderTimeView?.visibility = View.VISIBLE
        } else {
            mHeaderTimeView?.visibility = View.GONE
        }
    }

    private fun getColor(colorResId: Int): Int {
        return ContextCompat.getColor(context, colorResId)
    }

    private fun findPositionById(day: Int?, month: Int?, year: Int?): Int {
        if(day == null || month == null || year == null) return -1
        val localDateTime = LocalDateTime.now().withDate(year, month, day)
        return Days.daysBetween(calendarStartRange, localDateTime).days
    }

    //update the calendar header with the currentDate
    private fun updateHeader() {
        Timber.d(TAG, "selectDay day: ${mNormalModeDateFormat?.format(currentDate?.toDate())}" )
        if (!mHideHeaderView) {
            mHeaderTitleView?.text = mNormalModeDateFormat?.format(currentDate?.toDate())
            mHeaderTimeView?.text = mTimeFormat?.format(ldfSelectedDate?.toDate())
        } else if (mHeaderUpdateListener != null) {
            mHeaderUpdateListener?.onUpdate(mNormalModeDateFormat?.format(currentDate?.toDate()), mTimeFormat?.format(currentDate?.toDate()))
        }
    }


    fun buildCalendar(selectedDay: Calendar) {
        completeCalendarBuild(true, LocalDateTime.fromCalendarFields(selectedDay))
    }

    private fun completeCalendarBuild(previousDates: Boolean, selectedDay: LocalDateTime) {
        findViewById<View>(R.id.progress_bar_cal).visibility = View.VISIBLE
        currentDate = selectedDay
        ldfSelectedDate = currentDate

        buildCalendarRange(previousDates, selectedDay)
        val position = findPositionById(ldfSelectedDate?.dayOfMonth, ldfSelectedDate?.monthOfYear, ldfSelectedDate?.year)
        mAdapter?.selectedPosition = position

        findViewById<View>(R.id.progress_bar_cal).visibility = View.GONE
        if (previousDates) {
            mLayoutManager?.scrollToPosition(position - 3)
        } else {
            mLayoutManager?.scrollToPosition(position)
        }
        updateHeader()
        mAdapter?.notifyDataSetChanged()
    }

    private fun buildCalendarRange(previousDates: Boolean, selectedDay: LocalDateTime) {
        calendarStartRange = LocalDateTime.fromDateFields(selectedDay.toDate())
        if (previousDates) {
            calendarStartRange = calendarStartRange?.minusMonths(12)
        }

        calendarEndRange = LocalDateTime.fromDateFields(selectedDay.toDate()).plusMonths(24)

        totalNumberOfDaysInCalendar = Days.daysBetween(calendarStartRange, calendarEndRange).days
    }

    private fun getDayViewForPosition(position: Int): DayView {
        val localDate = calendarStartRange?.plusDays(position)
        currentDayView = DayView()
        //Set the prefix of the months that are not currently selected
        val prefixDate = localDate?.dayOfWeek()?.getAsShortText(defaultLocale)
        if (prefixDate?.length!! > 3) {
            currentDayView?.dayOfWeek = prefixDate.substring(0, 3)
        } else {
            currentDayView?.dayOfWeek = prefixDate.substring(0, prefixDate.length)
        }

        //set all the necessary day values.xml
        var day = localDate.dayOfMonth.toString()
        if (localDate.dayOfMonth < 10) {
            day = 0.toString() + localDate.dayOfMonth
        }
        currentDayView?.day = day
        currentDayView?.month = localDate.monthOfYear
        currentDayView?.year = localDate.year
        currentDayView?.monthShortName = localDate.monthOfYear().getAsShortText(defaultLocale).substring(0, 3)

        //Hide month if range in same month
        if (localDate.monthOfYear == ldfSelectedDate?.monthOfYear) {
            currentDayView?.setShowMonthShortName(false)
        } else {
            currentDayView?.setShowMonthShortName(true)
        }

        //change colors if selected
        if (ldfSelectedDate?.toLocalDate()?.isEqual(localDate.toLocalDate()) == true) {
            currentDayView?.textColor = HorizontalCalendarView.mSelectedDayTextColor
            currentDayView?.isSelected = true
        } else {
            currentDayView?.textColor = HorizontalCalendarView.mDayTextColor
            currentDayView?.isSelected = false
        }

        //if active days exist, turn them on and set their values.xml
        if (mActiveDaysMap != null) {
            val eventItems = mActiveDaysMap?.get(position)
            if (eventItems != null) {
                currentDayView?.isActiveDay = true
                currentDayView?.activeType = if (eventItems.size > 1) calculatePriorityColorForMultipleEvents(eventItems) else eventItems[0].eventStatus
            } else {
                currentDayView?.isActiveDay = false
            }
        } else {
            currentDayView?.isActiveDay = false
        }

        //if this is an appointment calendar then highlight the appointments available
        if (mAvailableAppointments != null) {
            val date = mAvailableAppointments?.get(position)
            if (date != null) {
                currentDayView?.isActiveDay = true
                currentDayView?.activeType = ActivityDate.ActivityType.SCHEDULE
            } else {
                currentDayView?.isActiveDay = false
            }
        }

        //finally, check if this date is today
        if (localDate.toLocalDate() == LocalDate.now()) {
            Timber.d(TAG, "buildDateList: today")
            currentDayView?.isToday = true
        }
        return currentDayView!!
    }

    private fun calculatePriorityColorForMultipleEvents(eventItems: ArrayList<EventItem>): ActivityDate.ActivityType {
        var itemToChoose = eventItems[0]
        for (i in 1 until eventItems.size) {
            val currentItem = eventItems[i]
            if (currentItem.eventStatus.priority > itemToChoose.eventStatus.priority) {
                itemToChoose = currentItem
            }
        }
        return itemToChoose.eventStatus
    }

    fun enableDateClick() {
        preventFutureDateClick = false
    }

    fun disableFutureDateClick() {
        preventFutureDateClick = true
    }

    inner class HorizontalCalendarAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val TAG = "HorizontalCalendarAdapter"
        internal var selectedPosition: Int = 0
        internal var mDisplayDayOfWeek: Boolean = false

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemView = inflater.inflate(R.layout.horizontal_calendar_date_layout, parent, false)
            return DateCalendarViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            try {
                val dayView = getDayViewForPosition(position)
                val mHolder = holder as DateCalendarViewHolder

                //text
                mHolder.mDayNumber.text = dayView.day
                mHolder.mMonthShortName.text = dayView.monthShortName
                mHolder.mDayOfWeek.text = dayView.dayOfWeek

                //active
                if (dayView.isActiveDay) {
                    mHolder.activityDot.visibility = View.VISIBLE
                    Timber.d(TAG, "onBindViewHolder: ${dayView.day}")
                    Timber.d(TAG, "onBindViewHolder: ${dayView.activeType}")
                    dayView.activeType?.getDotColor(context)?.let { mHolder.activityDot.setTextColor(it) }
                } else {
                    mHolder.activityDot.visibility = View.INVISIBLE
                }

                //selected
                if (dayView.isSelected) {
                    mHolder.mMonthShortName.setTextColor(getColor(R.color.white))
                    mHolder.mDayNumber.setTextColor(getColor(R.color.white))
                    mHolder.selectedView.visibility = View.VISIBLE
                    mHolder.activityDot.setTextColor(getColor(R.color.white))
                    if (dayView.isToday) {
                        //today
                        mHolder.selectedView.background = ContextCompat.getDrawable(context, R.drawable.rounded_primary)
                    } else {
                        mHolder.selectedView.background = ContextCompat.getDrawable(context, R.drawable.rounded_secondary)
                    }
                } else {
                    if (dayView.isToday) {
                        //today
                        mHolder.mMonthShortName.setTextColor(getColor(R.color.primaryColor))
                        mHolder.mDayNumber.setTextColor(getColor(R.color.primaryColor))
                    } else {
                        mHolder.mMonthShortName.setTextColor(getColor(R.color.primaryLightColor))
                        mHolder.mDayNumber.setTextColor(getColor(R.color.primaryLightColor))
                    }

                    mHolder.selectedView.visibility = View.INVISIBLE
                }

                //hide month
                if (dayView.showMonthShortName()) {
                    mHolder.mMonthShortName.visibility = View.VISIBLE
                } else {
                    mHolder.mMonthShortName.visibility = View.GONE
                }
            } catch (e: IndexOutOfBoundsException) {
                Timber.e(TAG, e.toString())
                Snackbar.make(this@HorizontalCalendarView, "Inconsistency detected", Snackbar.LENGTH_LONG).show()
            }

        }

        override fun getItemCount(): Int {
            return totalNumberOfDaysInCalendar
        }

        private inner class DateCalendarViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            val selectedView: View = itemView.findViewById(R.id.background_view)
            val fullLayout: LinearLayout = itemView.findViewById(R.id.date_layout)
            val mDayOfWeek: TextView = itemView.findViewById(R.id.day_of_week)
            val mDayNumber: TextView = itemView.findViewById(R.id.day_number)
            val mMonthShortName: TextView = itemView.findViewById(R.id.month_short_name)
            val activityDot: TextView = itemView.findViewById(R.id.activity_dot)

            init {
                fullLayout.setOnClickListener(this)
            }


            override fun onClick(v: View) {
                val dayView = getDayViewForPosition(adapterPosition)

                val selectedTemp = LocalDateTime.now().withMonthOfYear(dayView.month!!)
                        .withYear(dayView.year!!)
                        .withDayOfMonth(dayView.day?.toInt()!!)
                Timber.d(TAG, "onClick: $preventFutureDateClick")
                Timber.d(TAG, "onClick selected date: %s", selectedTemp.dayOfYear)
                Timber.d(TAG, "onClick now date: %s", LocalDateTime.now().dayOfYear)
                if (!preventFutureDateClick || preventFutureDateClick && !selectedTemp.isAfter(LocalDateTime.now())) {
                    setSelectedDay(dayView.month, dayView.day?.toInt(), dayView.year)

                    scrollLock.set(true) //ensure the userDiscarded will affect updates

                    currentDate = selectedTemp
                    ldfSelectedDate = currentDate
                    updateHeader()
                    mAdapter?.updatePrefixMonth()

                    /* If the user clicks on an item that is partially obscured offscreen then scroll */
                    val viewRightPos = (v.x + v.width).toDouble()
                    val viewLeftPos = v.x.toDouble()

                    val newViewEndPosition = viewRightPos + ViewUtil.dpToPx(5)
                    val newViewStartPosition = viewLeftPos - ViewUtil.dpToPx(5)
                    val screenEndPosition = Resources.getSystem().displayMetrics.widthPixels.toDouble()
                    val screenStartPosition = 0.0

                    if (newViewEndPosition > screenEndPosition) {
                        mRecyclerView?.smoothScrollBy((newViewEndPosition - screenEndPosition).toInt(), 0)
                    } else if (newViewStartPosition < screenStartPosition) {
                        mRecyclerView?.smoothScrollBy(newViewStartPosition.toInt(), 0)
                    }

                    scrollLock.set(false)

                    if (mOnDateClickListener != null && ldfSelectedDate != null) {
                        mOnDateClickListener?.onDateClick(ldfSelectedDate!!, adapterPosition)
                    }
                }
            }
        }


        fun setSelectedDay(month: Int?, day: Int?, year: Int?): Int {
            Timber.d(TAG, "setSelectedDay: month $month")
            Timber.d(TAG, "setSelectedDay: day $day")
            val position = findPositionById(day, month, year)
            Timber.d(TAG, "setSelectedDay headerPosition: $position")
            //Deselect day selected
            if (selectedPosition >= 0) {
                unSelectDay(selectedPosition)
            }

            //Set selected day
            selectedPosition = position
            selectDay(selectedPosition)

            //notify adapter
            return selectedPosition
        }

        fun unSelectDay(position: Int) {
            val dayView = getDayViewForPosition(position)
            dayView.textColor = R.color.primaryTextColor
            dayView.isSelected = false
            notifyItemChanged(position)
        }

        fun selectDay(position: Int) {
            val dayView = getDayViewForPosition(position)
            dayView.isSelected = true
            dayView.textColor = R.color.white
            notifyItemChanged(position)
        }

        fun setTodaySelected(): Int {
            val today = LocalDateTime.now()
            return setSelectedDay(today.monthOfYear, today.dayOfMonth, today.year)
        }

        fun updatePrefixMonth() {
            notifyDataSetChanged()
        }

    }

    companion object {
        val TAG = "HorizontalCalendarView"

        //Colors
        var mDayTextColor: Int = 0
        var mSelectedDayTextColor: Int = 0
        var mTodayTextColor: Int = 0
        var mDaysContainerBackgroundColor: Int = 0
        var mSelectedDayBackgroundColor: Int = 0
    }
}

