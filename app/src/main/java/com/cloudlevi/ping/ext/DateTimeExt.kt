package com.cloudlevi.ping.ext

import android.content.Context
import com.cloudlevi.ping.R
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.*

const val TIME = "hh:mm"
const val DATE = "dd.MM.yyyy"
const val DATE_NO_YEAR = "MMMM dd"
const val DATE_SHORT = "dd.MM"
const val DATE_CHECKIN_CHEKOUT = "dd MMM yyyy"
const val DATE_TIME = "dd MMM yyyy hh:mm"

fun Long.showTime(): String{
    val sdf = SimpleDateFormat(TIME, Locale.CANADA)
    return sdf.format(this)
}

fun Long.showDateFull(): String{
    val sdf = SimpleDateFormat(DATE, Locale.ENGLISH)
    return sdf.format(this)
}

fun Long.showDateNoYear(): String{
    val sdf = SimpleDateFormat(DATE_NO_YEAR, Locale.ENGLISH)
    return sdf.format(this)
}

fun Long.showDateShort(): String{
    val sdf = SimpleDateFormat(DATE_SHORT, Locale.ENGLISH)
    return sdf.format(this)
}

fun Long.showDateTime(): String{
    val sdf = SimpleDateFormat(DATE_TIME, Locale.ENGLISH)
    return sdf.format(this)
}

fun Long.showDateCheckinCheckout(): String{
    val sdf = SimpleDateFormat(DATE_CHECKIN_CHEKOUT, Locale.ENGLISH)
    return sdf.format(this)
}

fun Long.toJodaTime(): DateTime{
    return DateTime(this)
}

fun DateTime.getTime(): Long {
    return this.toDate().time
}

fun Long.isDateToday(): Boolean{
    val dateTime = DateTime(this).withMillisOfDay(0)
    val now = DateTime.now().withMillisOfDay(0)
    return dateTime == now
}

fun Long.isSameYear() =
    DateTime(this).year == DateTime.now().year

fun hoursMinutesToMillis(hours: Int, mins: Int) =
    (hours.toLong() * 3600000) + (mins.toLong() * 60000)

fun howLongAgo(context: Context, timeStamp: Long): String{
    val diff = System.currentTimeMillis() - timeStamp

    val hours = diff.toDouble() / hoursInMilli
    val days = diff.toDouble() / daysInMilli
    val months = days / 31
    val years = months / 12

    var formatArgs: Int? = null

    val resID: Int = when {
        years > 1.0 -> {
            formatArgs = years.toInt()
            R.string.years_ago
        }
        years == 1.0 -> R.string.one_year_ago
        months > 1.0 -> {
            formatArgs = months.toInt()
            R.string.months_ago
        }
        months == 1.0 -> R.string.one_month_ago
        days > 1.0 -> {
            formatArgs = days.toInt()
            R.string.days_ago
        }
        days == 1.0 -> R.string.one_day_ago
        hours > 1.0 -> {
            formatArgs = hours.toInt()
            R.string.hours_ago
        }
        hours == 1.0 -> R.string.one_hour_ago
        else -> R.string.just_now
    }

    return context.getString(resID, formatArgs)
}

var secondsInMilli: Long = 1000
var minutesInMilli = secondsInMilli * 60
var hoursInMilli = minutesInMilli * 60
var daysInMilli = hoursInMilli * 24