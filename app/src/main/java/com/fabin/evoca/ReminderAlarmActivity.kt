

package com.fabin.evoca

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.fabin.evoca.data.AppDatabase
import com.fabin.evoca.data.Reminder
import com.fabin.evoca.ui.theme.EvocaBackground
import com.fabin.evoca.ui.theme.EvocaBorder
import com.fabin.evoca.ui.theme.EvocaEmber
import com.fabin.evoca.ui.theme.EvocaGreen
import com.fabin.evoca.ui.theme.EvocaInk
import com.fabin.evoca.ui.theme.EvocaMuted
import com.fabin.evoca.ui.theme.EvocaSans
import com.fabin.evoca.ui.theme.EvocaSerif
import com.fabin.evoca.ui.theme.EvocaSurface

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ReminderAlarmActivity : ComponentActivity() {

    private var reminder by mutableStateOf<Reminder?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // User opened the reminder,
        // so cancel the 5-minute escalation.
        cancelEscalation()

        val reminderId =
            intent.getIntExtra(
                "REMINDER_ID",
                -1
            )

        setContent {

            LaunchedEffect(reminderId) {

                if (reminderId != -1) {

                    reminder =
                        withContext(Dispatchers.IO) {

                            AppDatabase
                                .getDatabase(
                                    applicationContext
                                )
                                .reminderDao()
                                .getReminderById(
                                    reminderId
                                )
                        }
                }
            }

            ReminderAlarmScreen(
                reminder = reminder,

                onOpen = {

                    reminder?.url?.let { url ->

                        val openIntent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            )

                        startActivity(openIntent)
                    }
                },

                onReschedule = { newReminderTime ->

                    reminder?.let { currentReminder ->

                        rescheduleReminder(
                            currentReminder,
                            newReminderTime
                        )
                    }
                },

                onDone = {

                    reminder?.let { currentReminder ->

                        // Cancel escalation.
                        cancelEscalation()

                        // Cancel current notification.
                        val notificationManager =
                            getSystemService(
                                Context.NOTIFICATION_SERVICE
                            ) as android.app.NotificationManager

                        notificationManager.cancel(
                            currentReminder.id
                        )

                        lifecycleScope.launch {

                            withContext(Dispatchers.IO) {

                                AppDatabase
                                    .getDatabase(
                                        applicationContext
                                    )
                                    .reminderDao()
                                    .updateReminder(
                                        currentReminder.copy(
                                            status = "DONE"
                                        )
                                    )
                            }

                            finish()
                        }
                    }
                }
            )
        }
    }


    private fun rescheduleReminder(
        currentReminder: Reminder,
        newReminderTime: Long
    ) {

        if (
            newReminderTime <=
            System.currentTimeMillis()
        ) {

            Toast.makeText(
                this,
                "Please choose a future time.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val alarmManager =
            getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        /*
         * Check exact alarm permission
         * before changing the reminder.
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {

            Toast.makeText(
                this,
                "Allow exact alarms for Evoca.",
                Toast.LENGTH_LONG
            ).show()

            val settingsIntent =
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                ).apply {

                    data =
                        Uri.parse(
                            "package:$packageName"
                        )
                }

            startActivity(
                settingsIntent
            )

            return
        }

        // Cancel old alarm.
        cancelScheduledAlarm(
            currentReminder.id
        )

        // Cancel existing escalation.
        cancelEscalation()

        val newAlarmIntent =
            Intent(
                this,
                ReminderAlarmReceiver::class.java
            ).apply {

                putExtra(
                    "REMINDER_ID",
                    currentReminder.id
                )
            }

        val newPendingIntent =
            PendingIntent.getBroadcast(
                this,
                currentReminder.id,
                newAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val alarmInfo =
            AlarmManager.AlarmClockInfo(
                newReminderTime,
                newPendingIntent
            )

        alarmManager.setAlarmClock(
            alarmInfo,
            newPendingIntent
        )

        val newExpiresAt =
            newReminderTime +
                    (48 * 60 * 60 * 1000L)

        val updatedReminder =
            currentReminder.copy(
                reminderTime =
                    newReminderTime,
                expiresAt =
                    newExpiresAt,
                status =
                    "ACTIVE"
            )

        lifecycleScope.launch {

            withContext(Dispatchers.IO) {

                AppDatabase
                    .getDatabase(
                        applicationContext
                    )
                    .reminderDao()
                    .updateReminder(
                        updatedReminder
                    )
            }

            Toast.makeText(
                this@ReminderAlarmActivity,
                "Reminder rescheduled",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }


    private fun cancelScheduledAlarm(
        reminderId: Int
    ) {

        val alarmManager =
            getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val alarmIntent =
            Intent(
                this,
                ReminderAlarmReceiver::class.java
            )

        val pendingIntent =
            PendingIntent.getBroadcast(
                this,
                reminderId,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(
            pendingIntent
        )
    }


    private fun cancelEscalation() {

        val reminderId =
            intent.getIntExtra(
                "REMINDER_ID",
                -1
            )

        if (reminderId == -1) {
            return
        }

        ReminderAlarmReceiver.cancelEscalation(
            this,
            reminderId
        )
    }
}


/* =========================================================
   REMINDER ALARM SCREEN
   ========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderAlarmScreen(
    reminder: Reminder?,
    onOpen: () -> Unit,
    onReschedule: (Long) -> Unit,
    onDone: () -> Unit
) {

    if (reminder == null) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    EvocaBackground
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = "Loading reminder...",
                fontFamily = EvocaSans,
                fontSize = 14.sp,
                color = EvocaMuted
            )
        }

        return
    }


    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var showTimePicker by remember {
        mutableStateOf(false)
    }

    var selectedDateMillis by remember {
        mutableStateOf<Long?>(null)
    }

    var showTimeError by remember {
        mutableStateOf(false)
    }


    val currentTimeCalendar =
        Calendar.getInstance()


    val todayCalendar =
        Calendar.getInstance().apply {

            set(
                Calendar.HOUR_OF_DAY,
                0
            )

            set(
                Calendar.MINUTE,
                0
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )
        }


    val datePickerState =
        rememberDatePickerState(

            initialSelectedDateMillis =
                todayCalendar.timeInMillis,

            selectableDates =
                object : SelectableDates {

                    override fun isSelectableDate(
                        utcTimeMillis: Long
                    ): Boolean {

                        val selectedDate =
                            Calendar.getInstance().apply {

                                timeInMillis =
                                    utcTimeMillis
                            }

                        val today =
                            Calendar.getInstance()

                        return (
                                selectedDate.get(
                                    Calendar.YEAR
                                ) >
                                        today.get(
                                            Calendar.YEAR
                                        )
                                ) ||
                                (
                                        selectedDate.get(
                                            Calendar.YEAR
                                        ) ==
                                                today.get(
                                                    Calendar.YEAR
                                                ) &&
                                                selectedDate.get(
                                                    Calendar.DAY_OF_YEAR
                                                ) >=
                                                today.get(
                                                    Calendar.DAY_OF_YEAR
                                                )
                                        )
                    }
                }
        )


    val timePickerState =
        rememberTimePickerState(

            initialHour =
                currentTimeCalendar.get(
                    Calendar.HOUR_OF_DAY
                ),

            initialMinute =
                currentTimeCalendar.get(
                    Calendar.MINUTE
                )
        )


    val formattedTime =
        reminder.reminderTime?.let {

            SimpleDateFormat(
                "dd MMM · hh:mm a",
                Locale.getDefault()
            ).format(
                Date(it)
            )

        } ?: ""


    /*
     * MAIN SCREEN
     */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                EvocaBackground
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp,
                    vertical = 24.dp
                )
        ) {

            /*
             * HEADER
             */

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Evoca",
                    fontFamily = EvocaSerif,
                    fontSize = 24.sp,
                    fontWeight =
                        FontWeight.Normal,
                    color = EvocaInk
                )

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                Surface(
                    color = EvocaSurface,

                    border =
                        BorderStroke(
                            1.dp,
                            EvocaBorder
                        ),

                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                ) {

                    Text(
                        text = "REMINDER",

                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),

                        fontFamily =
                            EvocaSans,

                        fontSize = 9.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            EvocaGreen,

                        letterSpacing =
                            1.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(38.dp)
            )


            /*
             * REMINDER CARD
             */

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                color =
                    EvocaSurface,

                border =
                    BorderStroke(
                        1.dp,
                        EvocaBorder
                    ),

                shape =
                    RoundedCornerShape(
                        22.dp
                    ),

                shadowElevation = 2.dp
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 22.dp,
                                vertical = 24.dp
                            )
                ) {

                    Text(
                        text = "It's time.",

                        fontFamily =
                            EvocaSans,

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            EvocaGreen,

                        letterSpacing =
                            1.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )


                    Text(
                        text =
                            reminder.reminderMessage,

                        fontFamily =
                            EvocaSerif,

                        fontSize = 30.sp,

                        fontWeight =
                            FontWeight.Normal,

                        color =
                            EvocaInk,

                        lineHeight =
                            36.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(22.dp)
                    )


                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),

                        color =
                            EvocaBackground,

                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    horizontal = 15.dp,
                                    vertical = 13.dp
                                )
                        ) {

                            Text(
                                text =
                                    reminder.source
                                        ?: "Saved link",

                                fontFamily =
                                    EvocaSans,

                                fontSize = 12.sp,

                                fontWeight =
                                    FontWeight.Medium,

                                color =
                                    EvocaGreen
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )


                            Text(
                                text =
                                    formattedTime,

                                fontFamily =
                                    EvocaSans,

                                fontSize = 12.sp,

                                color =
                                    EvocaMuted
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )


            /*
             * OPEN
             */

            Button(
                onClick = onOpen,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            EvocaGreen,

                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text = "Open",

                    fontFamily =
                        EvocaSans,

                    fontSize = 14.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            /*
             * RESCHEDULE
             */

            OutlinedButton(
                onClick = {

                    showTimeError =
                        false

                    showDatePicker =
                        true
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),

                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                border =
                    BorderStroke(
                        1.dp,
                        EvocaBorder
                    ),

                colors =
                    ButtonDefaults
                        .outlinedButtonColors(
                            contentColor =
                                EvocaGreen
                        )
            ) {

                Text(
                    text = "Reschedule",

                    fontFamily =
                        EvocaSans,

                    fontSize = 13.sp,

                    fontWeight =
                        FontWeight.Medium
                )
            }


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            /*
             * DONE
             */

            TextButton(
                onClick = onDone,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Done",

                    fontFamily =
                        EvocaSans,

                    fontSize = 13.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        EvocaMuted
                )
            }


            Spacer(
                modifier =
                    Modifier.weight(1f)
            )


            /*
             * FOOTER
             */

            Text(
                text =
                    "Brought back by Evoca.",

                modifier =
                    Modifier.fillMaxWidth(),

                textAlign =
                    TextAlign.Center,

                fontFamily =
                    EvocaSans,

                fontSize = 11.sp,

                color =
                    EvocaMuted
            )
        }
    }


    /*
     * DATE PICKER
     */

    if (showDatePicker) {

        DatePickerDialog(

            onDismissRequest = {
                showDatePicker =
                    false
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        selectedDateMillis =
                            datePickerState
                                .selectedDateMillis

                        showDatePicker =
                            false

                        showTimeError =
                            false

                        showTimePicker =
                            true
                    }
                ) {

                    Text(
                        text = "Next",

                        fontFamily =
                            EvocaSans,

                        fontSize = 13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            EvocaGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDatePicker =
                            false
                    }
                ) {

                    Text(
                        text = "Cancel",

                        fontFamily =
                            EvocaSans,

                        fontSize = 13.sp,

                        color =
                            EvocaMuted
                    )
                }
            }

        ) {

            Column {

                Text(
                    text =
                        "Bring it back later.",

                    modifier =
                        Modifier.padding(
                            start = 24.dp,
                            top = 18.dp,
                            end = 24.dp
                        ),

                    fontFamily =
                        EvocaSerif,

                    fontSize = 22.sp,

                    color =
                        EvocaInk
                )


                Text(
                    text =
                        "Choose when Evoca should remind you.",

                    modifier =
                        Modifier.padding(
                            start = 24.dp,
                            top = 4.dp,
                            end = 24.dp,
                            bottom = 8.dp
                        ),

                    fontFamily =
                        EvocaSans,

                    fontSize = 12.sp,

                    color =
                        EvocaMuted
                )


                DatePicker(
                    state =
                        datePickerState
                )


                Text(
                    text =
                        "Past dates are unavailable.",

                    modifier =
                        Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 16.dp
                        ),

                    fontFamily =
                        EvocaSans,

                    fontSize = 11.sp,

                    color =
                        EvocaMuted
                )
            }
        }
    }


    /*
     * TIME PICKER
     */

    if (showTimePicker) {

        AlertDialog(

            onDismissRequest = {
                showTimePicker =
                    false
            },

            containerColor =
                EvocaSurface,

            title = {

                Column {

                    Text(
                        text =
                            "Choose a time.",

                        fontFamily =
                            EvocaSerif,

                        fontSize = 23.sp,

                        color =
                            EvocaInk
                    )


                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )


                    Text(
                        text =
                            "Evoca will bring it back then.",

                        fontFamily =
                            EvocaSans,

                        fontSize = 12.sp,

                        color =
                            EvocaMuted
                    )
                }
            },

            text = {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    TimePicker(
                        state =
                            timePickerState
                    )


                    if (showTimeError) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )


                        Text(
                            text =
                                "Please choose a future time.",

                            fontFamily =
                                EvocaSans,

                            fontSize = 12.sp,

                            color =
                                EvocaEmber
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        if (
                            selectedDateMillis == null
                        ) {

                            return@TextButton
                        }


                        val calendar =
                            Calendar.getInstance()


                        calendar.timeInMillis =
                            selectedDateMillis!!


                        calendar.set(
                            Calendar.HOUR_OF_DAY,
                            timePickerState.hour
                        )


                        calendar.set(
                            Calendar.MINUTE,
                            timePickerState.minute
                        )


                        calendar.set(
                            Calendar.SECOND,
                            0
                        )


                        calendar.set(
                            Calendar.MILLISECOND,
                            0
                        )


                        val newReminderTime =
                            calendar.timeInMillis


                        if (
                            newReminderTime <=
                            System.currentTimeMillis()
                        ) {

                            showTimeError =
                                true

                            return@TextButton
                        }


                        showTimeError =
                            false

                        showTimePicker =
                            false


                        onReschedule(
                            newReminderTime
                        )
                    }
                ) {

                    Text(
                        text = "Save",

                        fontFamily =
                            EvocaSans,

                        fontSize = 13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            EvocaGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showTimePicker =
                            false
                    }
                ) {

                    Text(
                        text = "Cancel",

                        fontFamily =
                            EvocaSans,

                        fontSize = 13.sp,

                        color =
                            EvocaMuted
                    )
                }
            }
        )
    }
}