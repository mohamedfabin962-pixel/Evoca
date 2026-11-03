package com.fabin.evoca

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Patterns
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.fabin.evoca.data.Reminder
import com.fabin.evoca.permissions.EvocaPermissionManager
import com.fabin.evoca.ui.theme.EvocaBackground
import com.fabin.evoca.ui.theme.EvocaBorder
import com.fabin.evoca.ui.theme.EvocaDeepGreen
import com.fabin.evoca.ui.theme.EvocaEmber
import com.fabin.evoca.ui.theme.EvocaGreen
import com.fabin.evoca.ui.theme.EvocaInk
import com.fabin.evoca.ui.theme.EvocaMuted
import com.fabin.evoca.ui.theme.EvocaSans
import com.fabin.evoca.ui.theme.EvocaSerif
import com.fabin.evoca.ui.theme.EvocaSurface
import com.fabin.evoca.ui.theme.EvocaTransparent
import com.fabin.evoca.viewmodel.ReminderViewModel

import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
class MainActivity : ComponentActivity() {

    private lateinit var reminderViewModel: ReminderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reminderViewModel =
            ViewModelProvider(this)[ReminderViewModel::class.java]



        // Handle content shared from another app
        val sharedText =
            intent?.getStringExtra(Intent.EXTRA_TEXT)

        val sharedUrl =
            if (
                intent?.action == Intent.ACTION_SEND &&
                intent?.type == "text/plain" &&
                !sharedText.isNullOrBlank() &&
                Patterns.WEB_URL.matcher(sharedText.trim()).matches()
            ) {
                sharedText.trim()
            } else {
                null
            }

        setContent {
            EvocaApp(
                reminderViewModel = reminderViewModel,
                sharedUrl = sharedUrl,
                onShareComplete = {
                    finish()
                }
            )
        }
    }
}


/*
 * ---------------------------------------------------------
 * MAIN APP
 * ---------------------------------------------------------
 */

@Composable
fun EvocaApp(
    reminderViewModel: ReminderViewModel,
    sharedUrl: String? = null,
    onShareComplete: () -> Unit
) {

    val context = LocalContext.current

    var showAddReminder by remember {
        mutableStateOf(false)
    }

    var editingReminder by remember {
        mutableStateOf<Reminder?>(null)
    }

    var showAddQuickReminder by remember {
        mutableStateOf(false)
    }

    var showNotificationPermissionDialog by remember {
        mutableStateOf(false)
    }

    var showExactAlarmPermissionDialog by remember {
        mutableStateOf(false)
    }

    /*
     * 0 = Scheduled
     * 1 = Quick
     */
    var homePage by remember {
        mutableStateOf(0)
    }

    var rescheduleReminder by remember {
        mutableStateOf<Reminder?>(null)
    }

    /*
     * Used when the user goes to Android Settings
     * to enable notifications.
     */
    var waitingForNotificationSettings by remember {
        mutableStateOf(false)
    }

    /*
     * Used when the user goes to Android Settings
     * to enable exact alarms.
     */
    var waitingForExactAlarmSettings by remember {
        mutableStateOf(false)
    }

    val scheduledReminders by
    reminderViewModel.scheduledReminders.collectAsState()

    val quickReminders by
    reminderViewModel.quickReminders.collectAsState()


    /*
     * -----------------------------------------------------
     * NOTIFICATION PERMISSION REQUESTED STATE
     * -----------------------------------------------------
     */

    val permissionPreferences =
        remember {
            context.getSharedPreferences(
                "evoca_permissions",
                Context.MODE_PRIVATE
            )
        }

    var notificationPermissionRequested by remember {
        mutableStateOf(
            permissionPreferences.getBoolean(
                "notification_permission_requested",
                false
            )
        )
    }


    /*
     * -----------------------------------------------------
     * OPEN NOTIFICATION SETTINGS
     * -----------------------------------------------------
     */

    fun openNotificationSettings() {

        val settingsIntent =
            Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).apply {

                putExtra(
                    Settings.EXTRA_APP_PACKAGE,
                    context.packageName
                )
            }

        waitingForNotificationSettings = true

        context.startActivity(
            settingsIntent
        )
    }


    /*
     * -----------------------------------------------------
     * OPEN EXACT ALARM SETTINGS
     * -----------------------------------------------------
     */

    fun openExactAlarmSettings() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            val settingsIntent =
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                ).apply {

                    data =
                        Uri.parse(
                            "package:${context.packageName}"
                        )
                }

            waitingForExactAlarmSettings = true

            context.startActivity(
                settingsIntent
            )

        } else {

            /*
             * Android versions below Android 12
             * don't require exact-alarm access.
             */
            homePage = 0
            showAddReminder = true
        }
    }


    /*
     * -----------------------------------------------------
     * OPEN SCHEDULED AFTER ALL PERMISSION CHECKS
     * -----------------------------------------------------
     */

    fun openScheduledReminders() {

        /*
         * STEP 1
         * Notification permission.
         */
        if (
            !EvocaPermissionManager
                .hasNotificationPermission(context)
        ) {

            showNotificationPermissionDialog = true
            return
        }


        /*
         * STEP 2
         * Exact alarm permission.
         */
        if (
            !EvocaPermissionManager
                .canScheduleExactAlarms(context)
        ) {

            showExactAlarmPermissionDialog = true
            return
        }


        /*
         * Everything required is available.
         */
        homePage = 0
        showAddReminder = true
    }


    /*
     * -----------------------------------------------------
     * NOTIFICATION PERMISSION REQUEST
     * -----------------------------------------------------
     */

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                /*
                 * Notification permission granted.
                 *
                 * Now check exact alarm permission too.
                 */
                openScheduledReminders()

            } else {

                /*
                 * Scheduled reminders cannot be used
                 * without notifications.
                 *
                 * Quick Reminders remain available.
                 */
                homePage = 1
                showAddReminder = false
                showNotificationPermissionDialog = false
                showExactAlarmPermissionDialog = false
            }
        }


    /*
     * -----------------------------------------------------
     * CHECK SETTINGS RESULT WHEN USER RETURNS
     * -----------------------------------------------------
     */

    val lifecycleOwner =
        LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (
                    event ==
                    Lifecycle.Event.ON_RESUME
                ) {

                    /*
                     * -------------------------------------
                     * Notification Settings
                     * -------------------------------------
                     */

                    if (
                        waitingForNotificationSettings
                    ) {

                        waitingForNotificationSettings =
                            false

                        if (
                            EvocaPermissionManager
                                .hasNotificationPermission(
                                    context
                                )
                        ) {

                            /*
                             * Notification is now enabled.
                             *
                             * Check exact alarm next.
                             */
                            openScheduledReminders()

                        } else {

                            /*
                             * User returned without enabling.
                             */
                            homePage = 1
                            showAddReminder = false
                        }
                    }


                    /*
                     * -------------------------------------
                     * Exact Alarm Settings
                     * -------------------------------------
                     */

                    if (
                        waitingForExactAlarmSettings
                    ) {

                        waitingForExactAlarmSettings =
                            false

                        if (
                            EvocaPermissionManager
                                .canScheduleExactAlarms(
                                    context
                                )
                        ) {

                            /*
                             * Exact alarm access is now available.
                             *
                             * Open Scheduled.
                             */
                            homePage = 0
                            showAddReminder = true

                        } else {

                            /*
                             * User returned without enabling.
                             *
                             * Quick remains available.
                             */
                            homePage = 1
                            showAddReminder = false
                        }
                    }
                }
            }

        lifecycleOwner.lifecycle.addObserver(
            observer
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(
                observer
            )
        }
    }


    /*
     * -----------------------------------------------------
     * SHARED URL
     * -----------------------------------------------------
     *
     * When the user shares something from YouTube,
     * Instagram, Chrome, etc., both required
     * Scheduled permissions are checked.
     */

    LaunchedEffect(sharedUrl) {

        if (!sharedUrl.isNullOrBlank()) {

            openScheduledReminders()
        }
    }


    /*
     * -----------------------------------------------------
     * NOTIFICATION PERMISSION DIALOG
     * -----------------------------------------------------
     */

    if (showNotificationPermissionDialog) {

        EvocaNotificationPermissionDialog(

            onEnableNotifications = {

                showNotificationPermissionDialog =
                    false

                /*
                 * Android 13+
                 */
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU
                ) {

                    if (
                        notificationPermissionRequested
                    ) {

                        /*
                         * Permission was already requested
                         * before.
                         *
                         * Open Android notification settings.
                         */
                        openNotificationSettings()

                    } else {

                        /*
                         * First permission request.
                         */

                        permissionPreferences
                            .edit()
                            .putBoolean(
                                "notification_permission_requested",
                                true
                            )
                            .apply()

                        notificationPermissionRequested =
                            true

                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }

                } else {

                    /*
                     * Older Android versions.
                     */
                    val notificationManager =
                        context.getSystemService(
                            Context.NOTIFICATION_SERVICE
                        ) as NotificationManager

                    if (
                        !notificationManager
                            .areNotificationsEnabled()
                    ) {

                        openNotificationSettings()

                    } else {

                        openScheduledReminders()
                    }
                }
            },

            onUseQuick = {

                showNotificationPermissionDialog =
                    false

                showAddReminder = false

                homePage = 1
            },

            onDismiss = {

                showNotificationPermissionDialog =
                    false

                showAddReminder = false

                homePage = 1
            }
        )
    }


    /*
     * -----------------------------------------------------
     * EXACT ALARM PERMISSION DIALOG
     * -----------------------------------------------------
     */

    if (showExactAlarmPermissionDialog) {

        EvocaExactAlarmPermissionDialog(

            onEnableExactAlarms = {

                showExactAlarmPermissionDialog =
                    false

                openExactAlarmSettings()
            },

            onUseQuick = {

                showExactAlarmPermissionDialog =
                    false

                showAddReminder = false

                homePage = 1
            },

            onDismiss = {

                showExactAlarmPermissionDialog =
                    false

                showAddReminder = false

                homePage = 1
            }
        )
    }


    /*
     * -----------------------------------------------------
     * EDIT REMINDER
     * -----------------------------------------------------
     */

    if (editingReminder != null) {

        EditReminderScreen(

            reminder = editingReminder!!,

            onBack = {
                editingReminder = null
            },

            onSave = { newMessage ->

                val updatedReminder =
                    editingReminder!!.copy(
                        reminderMessage = newMessage
                    )

                reminderViewModel.updateReminder(
                    updatedReminder
                )

                editingReminder = null

                Toast.makeText(
                    context,
                    "Reminder updated",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )


        /*
         * -----------------------------------------------------
         * SCHEDULED REMINDER CREATION
         * -----------------------------------------------------
         */

    } else if (showAddReminder) {

        AddReminderScreen(

            sharedUrl = sharedUrl,

            onBack = {
                showAddReminder = false
            },

            onCreateReminder = {
                    url,
                    note,
                    reminderTime ->

                /*
                 * Final safety check.
                 *
                 * Do not create a database reminder if
                 * exact alarm permission has disappeared.
                 */
                if (
                    !EvocaPermissionManager
                        .canScheduleExactAlarms(context)
                ) {

                    showAddReminder = false
                    showExactAlarmPermissionDialog = true
                    return@AddReminderScreen
                }

                val createdAt =
                    System.currentTimeMillis()

                val expiresAt =
                    reminderTime +
                            (48 * 60 * 60 * 1000L)

                val source =
                    detectSource(url)

                val reminder =
                    Reminder(

                        type = "SCHEDULED",

                        url = url,

                        reminderMessage = note,

                        source = source,

                        reminderTime =
                            reminderTime,

                        createdAt =
                            createdAt,

                        expiresAt =
                            expiresAt,

                        status = "ACTIVE"
                    )

                reminderViewModel.addReminder(
                    reminder
                ) { insertedId ->

                    val alarmManager =
                        context.getSystemService(
                            Context.ALARM_SERVICE
                        ) as AlarmManager

                    val alarmIntent =
                        Intent(
                            context,
                            ReminderAlarmReceiver::class.java
                        ).apply {

                            putExtra(
                                "REMINDER_ID",
                                insertedId
                            )
                        }

                    val pendingIntent =
                        PendingIntent.getBroadcast(
                            context,
                            insertedId,
                            alarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or
                                    PendingIntent.FLAG_IMMUTABLE
                        )

                    if (
                        Build.VERSION.SDK_INT <
                        Build.VERSION_CODES.S ||
                        alarmManager
                            .canScheduleExactAlarms()
                    ) {

                        val alarmInfo =
                            AlarmManager.AlarmClockInfo(
                                reminderTime,
                                pendingIntent
                            )

                        alarmManager.setAlarmClock(
                            alarmInfo,
                            pendingIntent
                        )


                        /*
                         * SUCCESS
                         */

                        if (
                            !sharedUrl.isNullOrBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Reminder saved ✓",
                                Toast.LENGTH_SHORT
                            ).show()

                            onShareComplete()

                        } else {

                            Toast.makeText(
                                context,
                                "Scheduled reminder added",
                                Toast.LENGTH_SHORT
                            ).show()

                            homePage = 0
                            showAddReminder = false
                        }

                    } else {

                        /*
                         * This should normally not happen because
                         * we already checked above.
                         *
                         * Keep the fallback for safety.
                         */
                        showAddReminder = false
                        showExactAlarmPermissionDialog = true
                    }
                }
            }
        )


        /*
         * -----------------------------------------------------
         * QUICK REMINDER CREATION
         * -----------------------------------------------------
         */

    } else if (showAddQuickReminder) {

        QuickReminderScreen(

            onBack = {
                showAddQuickReminder = false
            },

            onCreateReminder = { message ->

                val createdAt =
                    System.currentTimeMillis()

                val expiresAt =
                    createdAt +
                            (48 * 60 * 60 * 1000L)

                val reminder =
                    Reminder(

                        type = "QUICK",

                        url = null,

                        reminderMessage = message,

                        source = null,

                        reminderTime = null,

                        createdAt =
                            createdAt,

                        expiresAt =
                            expiresAt,

                        status = "ACTIVE"
                    )

                reminderViewModel.addReminder(
                    reminder
                ) { }

                Toast.makeText(
                    context,
                    "Quick reminder added",
                    Toast.LENGTH_SHORT
                ).show()

                homePage = 1

                showAddQuickReminder = false
            }
        )


        /*
         * -----------------------------------------------------
         * HOME
         * -----------------------------------------------------
         */

    } else {

        HomeScreen(

            initialPage = homePage,

            scheduledReminders =
                scheduledReminders,

            quickReminders =
                quickReminders,

            onReschedule = { reminder ->

                rescheduleReminder =
                    reminder
            },

            onAddReminder = {

                /*
                 * Scheduled Reminders go through
                 * notification + exact alarm checks.
                 */
                openScheduledReminders()
            },

            onAddQuickReminder = {

                /*
                 * Quick Reminders don't require
                 * notification or exact alarm permission.
                 */
                homePage = 1

                showAddQuickReminder = true
            },

            onDone = { reminder ->

                reminderViewModel.markAsDone(
                    reminder
                )
            },

            onEdit = { reminder ->

                editingReminder =
                    reminder
            },

            onDelete = { reminder ->

                reminderViewModel.deleteReminder(
                    reminder
                )
            }
        )


        /*
         * -------------------------------------------------
         * RESCHEDULE DIALOG
         * -------------------------------------------------
         */

        if (rescheduleReminder != null) {

            RescheduleReminderDialog(

                reminder =
                    rescheduleReminder!!,

                onDismiss = {

                    rescheduleReminder =
                        null
                },

                onReschedule = {
                        reminder,
                        newTime ->

                    val alarmManager =
                        context.getSystemService(
                            Context.ALARM_SERVICE
                        ) as AlarmManager


                    /*
                     * Check exact alarm permission
                     * before changing the alarm.
                     */
                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.S &&
                        !alarmManager
                            .canScheduleExactAlarms()
                    ) {

                        rescheduleReminder =
                            null

                        showExactAlarmPermissionDialog =
                            true

                        return@RescheduleReminderDialog
                    }


                    /*
                     * Cancel old alarm.
                     */

                    val oldAlarmIntent =
                        Intent(
                            context,
                            ReminderAlarmReceiver::class.java
                        )

                    val oldPendingIntent =
                        PendingIntent.getBroadcast(
                            context,
                            reminder.id,
                            oldAlarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or
                                    PendingIntent.FLAG_IMMUTABLE
                        )

                    alarmManager.cancel(
                        oldPendingIntent
                    )

                    oldPendingIntent.cancel()


                    /*
                     * Create new alarm.
                     */

                    val newAlarmIntent =
                        Intent(
                            context,
                            ReminderAlarmReceiver::class.java
                        ).apply {

                            putExtra(
                                "REMINDER_ID",
                                reminder.id
                            )
                        }

                    val newPendingIntent =
                        PendingIntent.getBroadcast(
                            context,
                            reminder.id,
                            newAlarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or
                                    PendingIntent.FLAG_IMMUTABLE
                        )


                    if (
                        Build.VERSION.SDK_INT <
                        Build.VERSION_CODES.S ||
                        alarmManager
                            .canScheduleExactAlarms()
                    ) {

                        val alarmInfo =
                            AlarmManager.AlarmClockInfo(
                                newTime,
                                newPendingIntent
                            )

                        alarmManager.setAlarmClock(
                            alarmInfo,
                            newPendingIntent
                        )


                        val updatedReminder =
                            reminder.copy(

                                reminderTime =
                                    newTime,

                                expiresAt =
                                    newTime +
                                            (48 * 60 * 60 * 1000L)
                            )

                        reminderViewModel.updateReminder(
                            updatedReminder
                        )

                        rescheduleReminder =
                            null

                        Toast.makeText(
                            context,
                            "Reminder rescheduled",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        rescheduleReminder =
                            null

                        showExactAlarmPermissionDialog =
                            true
                    }
                }
            )
        }
    }
}
@Composable
fun EvocaNotificationPermissionDialog(
    onEnableNotifications: () -> Unit,
    onUseQuick: () -> Unit,
    onDismiss: () -> Unit
) {

    Dialog(
        onDismissRequest = onDismiss
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = EvocaSurface,
            border = BorderStroke(
                1.dp,
                EvocaBorder
            ),
            shadowElevation = 10.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        top = 24.dp,
                        end = 24.dp,
                        bottom = 20.dp
                    )
            ) {

                /*
                 * Small Evoca mark
                 */
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            EvocaBackground
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "✦",
                        fontFamily = EvocaSerif,
                        fontSize = 18.sp,
                        color = EvocaEmber
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )


                Text(
                    text = "Bring things back\nwhen they matter.",
                    fontFamily = EvocaSerif,
                    fontSize = 29.sp,
                    lineHeight = 34.sp,
                    color = EvocaInk
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                Text(
                    text =
                        "Scheduled reminders use notifications to bring your saved things back when they're due.",
                    fontFamily = EvocaSans,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = EvocaMuted
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )


                /*
                 * Quick reminder information
                 */
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = EvocaBackground
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "•",
                            fontFamily = EvocaSans,
                            fontSize = 16.sp,
                            color = EvocaGreen
                        )

                        Spacer(
                            modifier = Modifier.width(9.dp)
                        )

                        Text(
                            text =
                                "Quick reminders don't need notifications.",
                            fontFamily = EvocaSans,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = EvocaMuted
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )


                /*
                 * Primary action
                 */
                Button(
                    onClick =
                        onEnableNotifications,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    shape =
                        RoundedCornerShape(14.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                EvocaGreen,
                            contentColor =
                                Color.White
                        )
                ) {

                    Text(
                        text = "Enable notifications",
                        fontFamily = EvocaSans,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )


                /*
                 * Secondary action
                 */
                TextButton(
                    onClick = onUseQuick,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "Use Quick instead",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Medium,
                        color = EvocaMuted
                    )
                }
            }
        }
    }
}

@Composable
fun EvocaExactAlarmPermissionDialog(
    onEnableExactAlarms: () -> Unit,
    onUseQuick: () -> Unit,
    onDismiss: () -> Unit
) {

    Dialog(
        onDismissRequest = onDismiss
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = EvocaSurface,
            border = BorderStroke(
                1.dp,
                EvocaBorder
            ),
            shadowElevation = 10.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        top = 24.dp,
                        end = 24.dp,
                        bottom = 20.dp
                    )
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            EvocaBackground
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "✦",
                        fontFamily = EvocaSerif,
                        fontSize = 18.sp,
                        color = EvocaEmber
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "Bring it back\nat the right moment.",
                    fontFamily = EvocaSerif,
                    fontSize = 29.sp,
                    lineHeight = 34.sp,
                    color = EvocaInk
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Evoca needs permission to schedule your reminder precisely when you choose.",
                    fontFamily = EvocaSans,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = EvocaMuted
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = EvocaBackground
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "•",
                            fontFamily = EvocaSans,
                            fontSize = 16.sp,
                            color = EvocaGreen
                        )

                        Spacer(
                            modifier = Modifier.width(9.dp)
                        )

                        Text(
                            text =
                                "Quick reminders don't need this permission.",
                            fontFamily = EvocaSans,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = EvocaMuted
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                Button(
                    onClick =
                        onEnableExactAlarms,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    shape =
                        RoundedCornerShape(14.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                EvocaGreen,
                            contentColor =
                                Color.White
                        )
                ) {

                    Text(
                        text = "Enable precise reminders",
                        fontFamily = EvocaSans,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                TextButton(
                    onClick = onUseQuick,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "Use Quick instead",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Medium,
                        color = EvocaMuted
                    )
                }
            }
        }
    }
}
/*
 * ---------------------------------------------------------
 * SOURCE DETECTION
 * ---------------------------------------------------------
 */

private fun detectSource(
    url: String
): String {

    return when {

        url.contains(
            "instagram.com",
            ignoreCase = true
        ) -> "Instagram"

        url.contains(
            "youtube.com",
            ignoreCase = true
        ) ||
                url.contains(
                    "youtu.be",
                    ignoreCase = true
                ) -> "YouTube"

        else -> "Website"
    }
}

/*
 * ---------------------------------------------------------
 * HOME SCREEN
 * ---------------------------------------------------------
 */

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(
    initialPage: Int,
    scheduledReminders: List<Reminder>,
    quickReminders: List<Reminder>,
    onAddReminder: () -> Unit,
    onAddQuickReminder: () -> Unit,
    onDone: (Reminder) -> Unit,
    onReschedule: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    val pagerState =
        rememberPagerState(
            initialPage = initialPage,
            pageCount = { 2 }
        )

    val scope =
        rememberCoroutineScope()

    LaunchedEffect(initialPage) {
        if (pagerState.currentPage != initialPage) {
            pagerState.animateScrollToPage(initialPage)
        }
    }

    Scaffold(
        containerColor = EvocaBackground,
        topBar = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EvocaBackground)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = 8.dp
                    )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Evoca",
                        fontFamily = EvocaSerif,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Normal,
                        color = EvocaInk
                    )

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {

                                scope.launch {

                                    val nextPage =
                                        if (
                                            pagerState.currentPage == 0
                                        ) {
                                            1
                                        } else {
                                            0
                                        }

                                    pagerState.animateScrollToPage(
                                        nextPage
                                    )
                                }
                            },

                        color = EvocaSurface,

                        border = BorderStroke(
                            1.dp,
                            EvocaBorder
                        ),

                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Row(
                            modifier = Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 7.dp
                            ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    if (
                                        pagerState.currentPage == 0
                                    ) {
                                        "Quick"
                                    } else {
                                        "Scheduled"
                                    },

                                fontFamily = EvocaSans,

                                fontSize = 11.sp,

                                fontWeight =
                                    FontWeight.SemiBold,

                                color = EvocaGreen
                            )

                            Spacer(
                                modifier = Modifier.width(6.dp)
                            )

                            Text(
                                text = "↗",

                                fontFamily = EvocaSans,

                                fontSize = 12.sp,

                                color = EvocaMuted
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->

                when (page) {

                    0 -> {

                        ScheduledRemindersPage(
                            reminders =
                                scheduledReminders,

                            onAddReminder =
                                onAddReminder,

                            onDone =
                                onDone,

                            onReschedule =
                                onReschedule,

                            onEdit = { reminder ->
                                onEdit(reminder)
                            },

                            onDelete = { reminder ->
                                onDelete(reminder)
                            }
                        )
                    }

                    1 -> {

                        QuickRemindersPage(
                            reminders =
                                quickReminders,

                            onAddQuickReminder =
                                onAddQuickReminder,

                            onDone =
                                onDone
                        )
                    }
                }
            }

            // ─────────────────────────────
            // Pager indicator
            // ─────────────────────────────

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 4.dp,
                        bottom = 14.dp
                    ),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(
                            if (
                                pagerState.currentPage == 0
                            ) {
                                7.dp
                            } else {
                                5.dp
                            }
                        )
                        .clip(CircleShape)
                        .background(
                            if (
                                pagerState.currentPage == 0
                            ) {
                                EvocaGreen
                            } else {
                                EvocaBorder
                            }
                        )
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Box(
                    modifier = Modifier
                        .size(
                            if (
                                pagerState.currentPage == 1
                            ) {
                                7.dp
                            } else {
                                5.dp
                            }
                        )
                        .clip(CircleShape)
                        .background(
                            if (
                                pagerState.currentPage == 1
                            ) {
                                EvocaGreen
                            } else {
                                EvocaBorder
                            }
                        )
                )
            }
        }
    }
}


/*
 * ---------------------------------------------------------
 * SCHEDULED REMINDERS PAGE
 * ---------------------------------------------------------
 */

@Composable
fun ScheduledRemindersPage(
    reminders: List<Reminder>,
    onAddReminder: () -> Unit,
    onDone: (Reminder) -> Unit,
    onReschedule: (Reminder) -> Unit,
    onEdit: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit
) {
    Scaffold(
        containerColor = EvocaBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminder,
                containerColor = EvocaGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 5.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "+",
                    fontFamily = EvocaSans,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp
                )
        ) {

            Text(
                text = "Scheduled",
                fontFamily = EvocaSerif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaInk
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Bring it back when it matters.",
                fontFamily = EvocaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaMuted
            )

            if (reminders.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    ScheduledEmptyIllustration()

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Nothing waiting to come back.",
                        fontFamily = EvocaSerif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = EvocaInk
                    )

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Text(
                        text =
                            "Share something worth remembering.",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = EvocaMuted
                    )

                    Text(
                        text =
                            "Evoca will bring it back when it matters.",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = EvocaMuted
                    )
                }

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    reminders.forEach { reminder ->

                        ReminderCard(
                            reminder = reminder,

                            onDone = {
                                onDone(reminder)
                            },

                            onReschedule = {
                                onReschedule(reminder)
                            },

                            onEdit = {
                                onEdit(reminder)
                            },

                            onDelete = {
                                onDelete(reminder)
                            }
                        )
                    }
                }
            }
        }
    }
}

/*
 * ---------------------------------------------------------
 * QUICK REMINDERS PAGE
 * ---------------------------------------------------------
 */

@Composable
fun QuickRemindersPage(
    reminders: List<Reminder>,
    onAddQuickReminder: () -> Unit,
    onDone: (Reminder) -> Unit
) {
    Scaffold(
        containerColor = EvocaBackground,

        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddQuickReminder,
                containerColor = EvocaGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 5.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "+",
                    fontFamily = EvocaSans,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp
                )
        ) {

            Text(
                text = "Quick",
                fontFamily = EvocaSerif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaInk
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Things you don't want to forget.",
                fontFamily = EvocaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaMuted,
                lineHeight = 20.sp
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            if (reminders.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    QuickEmptyIllustration()

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Keep it here. Don’t lose it.",
                        fontFamily = EvocaSerif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = EvocaInk
                    )

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Text(
                        text =
                            "A thought, a task, a thing to remember —",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        color = EvocaMuted
                    )

                    Text(
                        text = "keep it with Evoca for now.",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        color = EvocaMuted
                    )
                }

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    reminders.forEach { reminder ->

                        QuickReminderCard(
                            reminder = reminder,

                            onDone = {
                                onDone(reminder)
                            }
                        )
                    }
                }
            }
        }
    }
}
/*
 * ---------------------------------------------------------
 * SCHEDULED REMINDER CARD
 * ---------------------------------------------------------
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderCard(
    reminder: Reminder,
    onDone: (Reminder) -> Unit,
    onReschedule: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    var showMenu by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 5.dp
            )
            .combinedClickable(
                onClick = {
                    reminder.url?.let { url ->
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )

                        context.startActivity(intent)
                    }
                },

                onLongClick = {
                    showMenu = true
                }
            ),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = EvocaSurface
        ),

        border = BorderStroke(
            1.dp,
            EvocaBorder
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 18.dp,
                        top = 17.dp,
                        end = 18.dp,
                        bottom = 13.dp
                    )
            ) {

                // ─────────────────────────────
                // Reminder message
                // ─────────────────────────────

                Text(
                    text = reminder.reminderMessage,

                    fontFamily = EvocaSans,

                    fontSize = 16.sp,

                    fontWeight = FontWeight.SemiBold,

                    color = EvocaInk,

                    lineHeight = 22.sp
                )

                Spacer(
                    modifier = Modifier.height(11.dp)
                )

                // ─────────────────────────────
                // Source + reminder time
                // ─────────────────────────────

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            reminder.source ?: "Link",

                        fontFamily = EvocaSans,

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color = EvocaGreen
                    )

                    Text(
                        text =
                            reminder.reminderTime?.let {

                                SimpleDateFormat(
                                    "dd MMM · hh:mm a",
                                    Locale.getDefault()
                                ).format(
                                    Date(it)
                                )

                            } ?: "",

                        fontFamily = EvocaSans,

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.Normal,

                        color = EvocaMuted
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                // ─────────────────────────────
                // Actions
                // ─────────────────────────────

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.End,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    TextButton(
                        onClick = onReschedule
                    ) {

                        Text(
                            text = "Reschedule",

                            fontFamily = EvocaSans,

                            fontSize = 12.sp,

                            fontWeight =
                                FontWeight.Medium,

                            color = EvocaMuted
                        )
                    }

                    TextButton(
                        onClick = {
                            onDone(reminder)
                        }
                    ) {

                        Text(
                            text = "Done",

                            fontFamily = EvocaSans,

                            fontSize = 12.sp,

                            fontWeight =
                                FontWeight.SemiBold,

                            color = EvocaGreen
                        )
                    }
                }
            }

            // ─────────────────────────────
            // Evoca long-press menu
            // ─────────────────────────────

            if (showMenu) {

                Dialog(
                    onDismissRequest = {
                        showMenu = false
                    }
                ) {

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.72f),

                        color = EvocaSurface,

                        shape = RoundedCornerShape(18.dp),

                        border = BorderStroke(
                            1.dp,
                            EvocaBorder
                        ),

                        shadowElevation = 10.dp
                    ) {

                        Column(
                            modifier = Modifier.padding(
                                vertical = 6.dp
                            )
                        ) {

                            TextButton(
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },

                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 6.dp
                                    )
                            ) {

                                Text(
                                    text = "Edit",

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    fontFamily = EvocaSans,

                                    fontSize = 14.sp,

                                    fontWeight =
                                        FontWeight.Medium,

                                    color = EvocaInk
                                )
                            }

                            TextButton(
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },

                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 6.dp
                                    )
                            ) {

                                Text(
                                    text = "Delete",

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    fontFamily = EvocaSans,

                                    fontSize = 14.sp,

                                    fontWeight =
                                        FontWeight.Medium,

                                    color = EvocaEmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
/*
 * ---------------------------------------------------------
 * QUICK REMINDER CARD
 * ---------------------------------------------------------
 */


@Composable
fun QuickReminderCard(
    reminder: Reminder,
    onDone: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 5.dp
            ),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = EvocaSurface
        ),

        border = BorderStroke(
            1.dp,
            EvocaBorder
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    top = 17.dp,
                    end = 18.dp,
                    bottom = 13.dp
                )
        ) {

            Text(
                text = reminder.reminderMessage,

                fontFamily = EvocaSans,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EvocaInk,
                lineHeight = 22.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "We’ll remember this with you.",

                    fontFamily = EvocaSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = EvocaMuted
                )

                TextButton(
                    onClick = onDone
                ) {
                    Text(
                        text = "Done",

                        fontFamily = EvocaSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EvocaGreen
                    )
                }
            }
        }
    }
}



@Composable
fun ScheduledEmptyIllustration() {
    Canvas(
        modifier = Modifier.size(210.dp)
    ) {
        val w = size.width
        val h = size.height

        // Soft landscape
        val ground = Path().apply {
            moveTo(w * 0.08f, h * 0.68f)
            cubicTo(
                w * 0.28f, h * 0.56f,
                w * 0.40f, h * 0.72f,
                w * 0.58f, h * 0.58f
            )
            cubicTo(
                w * 0.72f, h * 0.48f,
                w * 0.86f, h * 0.60f,
                w * 0.94f, h * 0.56f
            )
            lineTo(w * 0.94f, h * 0.78f)
            lineTo(w * 0.08f, h * 0.78f)
            close()
        }

        drawPath(
            path = ground,
            color = Color(0xFFD8DCC8)
        )

        // Inner hill
        val hill = Path().apply {
            moveTo(w * 0.18f, h * 0.72f)
            cubicTo(
                w * 0.35f, h * 0.60f,
                w * 0.48f, h * 0.70f,
                w * 0.63f, h * 0.57f
            )
            cubicTo(
                w * 0.72f, h * 0.52f,
                w * 0.79f, h * 0.60f,
                w * 0.86f, h * 0.64f
            )
            lineTo(w * 0.86f, h * 0.76f)
            lineTo(w * 0.18f, h * 0.76f)
            close()
        }

        drawPath(
            path = hill,
            color = EvocaGreen.copy(alpha = 0.55f)
        )

        // Small trees
        drawCircle(
            color = EvocaDeepGreen,
            radius = 8f,
            center = Offset(
                w * 0.48f,
                h * 0.49f
            )
        )

        drawLine(
            color = EvocaDeepGreen,
            start = Offset(w * 0.48f, h * 0.52f),
            end = Offset(w * 0.48f, h * 0.68f),
            strokeWidth = 4f
        )

        drawCircle(
            color = EvocaGreen,
            radius = 6f,
            center = Offset(
                w * 0.60f,
                h * 0.53f
            )
        )

        drawLine(
            color = EvocaGreen,
            start = Offset(w * 0.60f, h * 0.56f),
            end = Offset(w * 0.60f, h * 0.68f),
            strokeWidth = 3f
        )

        // Warm accent sun
        drawCircle(
            color = EvocaEmber.copy(alpha = 0.35f),
            radius = 20f,
            center = Offset(
                w * 0.70f,
                h * 0.28f
            )
        )
    }
}

@Composable
fun QuickEmptyIllustration() {
    Canvas(
        modifier = Modifier.size(190.dp)
    ) {
        val w = size.width
        val h = size.height

        // Soft background shape
        val cloud = Path().apply {
            moveTo(w * 0.20f, h * 0.68f)
            cubicTo(
                w * 0.10f, h * 0.55f,
                w * 0.22f, h * 0.39f,
                w * 0.38f, h * 0.42f
            )
            cubicTo(
                w * 0.40f, h * 0.25f,
                w * 0.62f, h * 0.22f,
                w * 0.69f, h * 0.39f
            )
            cubicTo(
                w * 0.88f, h * 0.36f,
                w * 0.94f, h * 0.56f,
                w * 0.78f, h * 0.68f
            )
            close()
        }

        drawPath(
            path = cloud,
            color = Color(0xFFE2DED0)
        )

        // Stem
        drawLine(
            color = EvocaDeepGreen,
            start = Offset(w * 0.51f, h * 0.76f),
            end = Offset(w * 0.55f, h * 0.34f),
            strokeWidth = 5f
        )

        // Branches
        drawLine(
            color = EvocaDeepGreen,
            start = Offset(w * 0.54f, h * 0.52f),
            end = Offset(w * 0.38f, h * 0.39f),
            strokeWidth = 4f
        )

        drawLine(
            color = EvocaDeepGreen,
            start = Offset(w * 0.53f, h * 0.61f),
            end = Offset(w * 0.69f, h * 0.46f),
            strokeWidth = 4f
        )

        drawLine(
            color = EvocaDeepGreen,
            start = Offset(w * 0.55f, h * 0.42f),
            end = Offset(w * 0.67f, h * 0.31f),
            strokeWidth = 3f
        )

        // Leaves
        val leaves = listOf(
            Offset(w * 0.35f, h * 0.36f),
            Offset(w * 0.42f, h * 0.46f),
            Offset(w * 0.70f, h * 0.44f),
            Offset(w * 0.67f, h * 0.29f),
            Offset(w * 0.48f, h * 0.34f)
        )

        leaves.forEachIndexed { index, point ->
            drawOval(
                color = if (index % 2 == 0)
                    EvocaGreen
                else
                    EvocaDeepGreen,
                topLeft = Offset(
                    point.x - 7f,
                    point.y - 12f
                ),
                size = androidx.compose.ui.geometry.Size(
                    14f,
                    24f
                )
            )
        }
    }
}

/*
 * ---------------------------------------------------------
 * QUICK REMINDER CREATION SCREEN
 * ---------------------------------------------------------
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReminderScreen(
    onBack: () -> Unit,
    onCreateReminder: (String) -> Unit
) {

    var message by remember {
        mutableStateOf("")
    }

    Scaffold(
        containerColor = EvocaBackground,

        topBar = {
            Surface(
                color = EvocaBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 14.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    TextButton(
                        onClick = onBack
                    ) {
                        Text(
                            text = "Back",
                            fontFamily = EvocaSans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = EvocaMuted
                        )
                    }

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "Quick",
                        fontFamily = EvocaSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EvocaGreen
                    )
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            Text(
                text = "Hold this thought.",

                fontFamily = EvocaSerif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaInk,
                lineHeight = 36.sp
            )

            Text(
                text =
                    "Keep something here for now.\n" +
                            "You can clear it when you're done.",

                fontFamily = EvocaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaMuted,
                lineHeight = 21.sp
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            OutlinedTextField(
                value = message,

                onValueChange = {
                    message = it
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 120.dp
                    ),

                placeholder = {
                    Text(
                        text =
                            "Example: Don't forget my keys",

                        fontFamily = EvocaSans,
                        fontSize = 14.sp,
                        color = EvocaMuted
                    )
                },

                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = EvocaSans,
                    fontSize = 15.sp,
                    color = EvocaInk,
                    lineHeight = 22.sp
                ),

                shape = RoundedCornerShape(16.dp),

                colors =
                    androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor =
                            EvocaSurface,

                        unfocusedContainerColor =
                            EvocaSurface,

                        focusedBorderColor =
                            EvocaGreen,

                        unfocusedBorderColor =
                            EvocaBorder,

                        cursorColor =
                            EvocaGreen,

                        focusedTextColor =
                            EvocaInk,

                        unfocusedTextColor =
                            EvocaInk
                    ),

                maxLines = 5
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    onCreateReminder(
                        message.trim()
                    )
                },

                enabled = message.isNotBlank(),

                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),

                shape = RoundedCornerShape(14.dp),

                colors =
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor =
                            EvocaGreen,

                        contentColor =
                            Color.White,

                        disabledContainerColor =
                            EvocaBorder,

                        disabledContentColor =
                            EvocaMuted
                    )
            ) {
                Text(
                    text = "Keep it here",

                    fontFamily = EvocaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )
        }
    }
}

/*
 * ---------------------------------------------------------
 * ADD SCHEDULED REMINDER SCREEN
 * ---------------------------------------------------------
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    sharedUrl: String? = null,
    onBack: () -> Unit,
    onCreateReminder: (String, String, Long) -> Unit
) {
    val context = LocalContext.current

    var url by remember {
        mutableStateOf(sharedUrl ?: "")
    }

    var note by remember {
        mutableStateOf("")
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

    var selectedHour by remember {
        mutableStateOf<Int?>(null)
    }

    var selectedMinute by remember {
        mutableStateOf<Int?>(null)
    }

    // ---------------------------------------------------------
    // DATE PICKER
    // ---------------------------------------------------------

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                System.currentTimeMillis(),
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
                                selectedDate.get(Calendar.YEAR) >
                                        today.get(Calendar.YEAR)
                                ) || (
                                selectedDate.get(Calendar.YEAR) ==
                                        today.get(Calendar.YEAR) &&
                                        selectedDate.get(Calendar.DAY_OF_YEAR) >=
                                        today.get(Calendar.DAY_OF_YEAR)
                                )
                    }
                }
        )

    // ---------------------------------------------------------
    // TIME PICKER
    // ---------------------------------------------------------

    val currentTime =
        Calendar.getInstance()

    val timePickerState =
        rememberTimePickerState(
            initialHour =
                currentTime.get(Calendar.HOUR_OF_DAY),
            initialMinute =
                currentTime.get(Calendar.MINUTE)
        )

    // ---------------------------------------------------------
    // FINAL REMINDER TIME
    // ---------------------------------------------------------

    val selectedReminderTime: Long? =
        if (
            selectedDateMillis != null &&
            selectedHour != null &&
            selectedMinute != null
        ) {

            val calendar =
                Calendar.getInstance()

            calendar.timeInMillis =
                selectedDateMillis!!

            calendar.set(
                Calendar.HOUR_OF_DAY,
                selectedHour!!
            )

            calendar.set(
                Calendar.MINUTE,
                selectedMinute!!
            )

            calendar.set(
                Calendar.SECOND,
                0
            )

            calendar.set(
                Calendar.MILLISECOND,
                0
            )

            calendar.timeInMillis

        } else {
            null
        }

    // ---------------------------------------------------------
    // FUTURE VALIDATION
    // ---------------------------------------------------------

    val isValidReminderTime =
        selectedReminderTime != null &&
                selectedReminderTime >
                System.currentTimeMillis()

    // ---------------------------------------------------------
    // SCREEN
    // ---------------------------------------------------------

    Scaffold(
        containerColor = EvocaBackground
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    horizontal = 20.dp
                )
        ) {

            // -------------------------------------------------
            // HEADER
            // -------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp,
                        bottom = 18.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onBack
                ) {
                    Text(
                        text = "Back",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EvocaMuted
                    )
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "SCHEDULED",
                    fontFamily = EvocaSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EvocaGreen,
                    letterSpacing = 1.sp
                )
            }

            // -------------------------------------------------
            // TITLE
            // -------------------------------------------------

            Text(
                text = "Bring this back\nwhen it matters.",
                fontFamily = EvocaSerif,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaInk,
                lineHeight = 37.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text =
                    "Save the link, add a reason,\nand choose when Evoca should return it.",
                fontFamily = EvocaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaMuted,
                lineHeight = 21.sp
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // -------------------------------------------------
            // LINK
            // -------------------------------------------------

            Text(
                text = "LINK",
                fontFamily = EvocaSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = EvocaMuted,
                letterSpacing = 1.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            OutlinedTextField(
                value = url,
                onValueChange = {
                    if (sharedUrl.isNullOrBlank()) {
                        url = it
                    }
                },
                readOnly = !sharedUrl.isNullOrBlank(),
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = {
                    Text(
                        text =
                            "Paste Instagram, YouTube or website link",
                        fontFamily = EvocaSans,
                        fontSize = 14.sp,
                        color = EvocaMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor =
                            EvocaSurface,
                        unfocusedContainerColor =
                            EvocaSurface,
                        focusedBorderColor =
                            EvocaGreen,
                        unfocusedBorderColor =
                            EvocaBorder,
                        cursorColor =
                            EvocaGreen,
                        focusedTextColor =
                            EvocaInk,
                        unfocusedTextColor =
                            EvocaInk
                    ),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = EvocaSans,
                        fontSize = 14.sp,
                        color = EvocaInk
                    )
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // -------------------------------------------------
            // NOTE
            // -------------------------------------------------

            Text(
                text = "WHY",
                fontFamily = EvocaSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = EvocaMuted,
                letterSpacing = 1.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 105.dp
                    ),
                placeholder = {
                    Text(
                        text = "Example: Apply for this",
                        fontFamily = EvocaSans,
                        fontSize = 14.sp,
                        color = EvocaMuted
                    )
                },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = EvocaSans,
                        fontSize = 15.sp,
                        color = EvocaInk,
                        lineHeight = 22.sp
                    ),
                shape = RoundedCornerShape(15.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor =
                            EvocaSurface,
                        unfocusedContainerColor =
                            EvocaSurface,
                        focusedBorderColor =
                            EvocaGreen,
                        unfocusedBorderColor =
                            EvocaBorder,
                        cursorColor =
                            EvocaGreen,
                        focusedTextColor =
                            EvocaInk,
                        unfocusedTextColor =
                            EvocaInk
                    ),
                maxLines = 4
            )

            Spacer(
                modifier = Modifier.height(17.dp)
            )

            // -------------------------------------------------
            // WHEN
            // -------------------------------------------------

            Text(
                text = "WHEN",
                fontFamily = EvocaSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = EvocaMuted,
                letterSpacing = 1.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(15.dp)
                    )
                    .clickable {
                        showDatePicker = true
                    },
                color = EvocaSurface,
                border = BorderStroke(
                    1.dp,
                    if (isValidReminderTime) {
                        EvocaGreen
                    } else {
                        EvocaBorder
                    }
                ),
                shape = RoundedCornerShape(15.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                if (
                                    selectedDateMillis != null &&
                                    selectedHour != null &&
                                    selectedMinute != null
                                ) {
                                    SimpleDateFormat(
                                        "dd MMM · hh:mm a",
                                        Locale.getDefault()
                                    ).format(
                                        Date(
                                            selectedReminderTime!!
                                        )
                                    )
                                } else {
                                    "Choose date & time"
                                },
                            fontFamily = EvocaSans,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Medium,
                            color =
                                if (
                                    selectedReminderTime != null
                                ) {
                                    EvocaInk
                                } else {
                                    EvocaMuted
                                }
                        )

                        if (
                            selectedReminderTime == null
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(2.dp)
                            )

                            Text(
                                text =
                                    "Evoca will bring it back then.",
                                fontFamily = EvocaSans,
                                fontSize = 11.sp,
                                color = EvocaMuted
                            )
                        }
                    }

                    Text(
                        text = "→",
                        fontFamily = EvocaSans,
                        fontSize = 18.sp,
                        color = EvocaGreen
                    )
                }
            }

            if (
                selectedReminderTime != null &&
                !isValidReminderTime
            ) {

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Please choose a future time.",
                    fontFamily = EvocaSans,
                    fontSize = 12.sp,
                    color = EvocaEmber
                )
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // -------------------------------------------------
            // CREATE
            // -------------------------------------------------

            Button(
                onClick = {

                    val cleanUrl =
                        url.trim()

                    val isValidUrl =
                        try {

                            val uri =
                                Uri.parse(cleanUrl)

                            (
                                    uri.scheme == "http" ||
                                            uri.scheme == "https"
                                    ) &&
                                    !uri.host.isNullOrBlank()

                        } catch (e: Exception) {
                            false
                        }

                    if (!isValidUrl) {

                        Toast.makeText(
                            context,
                            "Please enter a valid URL.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    if (
                        note.isNotBlank() &&
                        selectedReminderTime != null &&
                        isValidReminderTime
                    ) {

                        onCreateReminder(
                            cleanUrl,
                            note.trim(),
                            selectedReminderTime
                        )
                    }
                },
                enabled =
                    url.isNotBlank() &&
                            note.isNotBlank() &&
                            isValidReminderTime,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            EvocaGreen,
                        contentColor =
                            Color.White,
                        disabledContainerColor =
                            EvocaBorder,
                        disabledContentColor =
                            EvocaMuted
                    )
            ) {

                Text(
                    text = "Set reminder",
                    fontFamily = EvocaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
    }

    // ---------------------------------------------------------
    // DATE PICKER DIALOG
    // ---------------------------------------------------------

    if (showDatePicker) {

        DatePickerDialog(

            onDismissRequest = {
                showDatePicker = false
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val selectedDate =
                            datePickerState
                                .selectedDateMillis

                        if (selectedDate != null) {

                            selectedDateMillis =
                                selectedDate

                            showDatePicker =
                                false

                            showTimePicker =
                                true
                        }
                    }
                ) {

                    Text(
                        text = "Next",
                        fontFamily = EvocaSans,
                        fontWeight = FontWeight.SemiBold,
                        color = EvocaGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {

                    Text(
                        text = "Cancel",
                        fontFamily = EvocaSans,
                        color = EvocaMuted
                    )
                }
            }

        ) {

            Column {

                DatePicker(
                    state = datePickerState
                )

                Text(
                    text =
                        "Past dates are unavailable. Choose today or a future date.",
                    modifier =
                        Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 16.dp
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }

    // ---------------------------------------------------------
    // TIME PICKER DIALOG
    // ---------------------------------------------------------

    if (showTimePicker) {

        AlertDialog(

            onDismissRequest = {
                showTimePicker = false
            },

            title = {
                Text(
                    text = "Choose time",
                    fontFamily = EvocaSerif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = EvocaInk
                )
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

                    if (
                        selectedDateMillis != null
                    ) {

                        val previewCalendar =
                            Calendar.getInstance()

                        previewCalendar.timeInMillis =
                            selectedDateMillis!!

                        previewCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            timePickerState.hour
                        )

                        previewCalendar.set(
                            Calendar.MINUTE,
                            timePickerState.minute
                        )

                        previewCalendar.set(
                            Calendar.SECOND,
                            0
                        )

                        previewCalendar.set(
                            Calendar.MILLISECOND,
                            0
                        )

                        if (
                            previewCalendar.timeInMillis <=
                            System.currentTimeMillis()
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    "Please choose a future time.",
                                fontFamily = EvocaSans,
                                fontSize = 12.sp,
                                color = EvocaEmber
                            )
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        selectedHour =
                            timePickerState.hour

                        selectedMinute =
                            timePickerState.minute

                        showTimePicker =
                            false
                    }
                ) {

                    Text(
                        text = "Done",
                        fontFamily = EvocaSans,
                        fontWeight = FontWeight.SemiBold,
                        color = EvocaGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showTimePicker = false
                    }
                ) {

                    Text(
                        text = "Cancel",
                        fontFamily = EvocaSans,
                        color = EvocaMuted
                    )
                }
            }
        )
    }
}
@Composable
fun EditReminderScreen(
    reminder: Reminder,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember {
        mutableStateOf(reminder.reminderMessage)
    }

    Scaffold(
        containerColor = EvocaBackground
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {

            // -------------------------------------------------
            // HEADER
            // -------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp,
                        bottom = 18.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onBack
                ) {
                    Text(
                        text = "Back",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = EvocaMuted
                    )
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "EDIT",
                    fontFamily = EvocaSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EvocaGreen,
                    letterSpacing = 1.sp
                )
            }

            // -------------------------------------------------
            // TITLE
            // -------------------------------------------------

            Text(
                text = "Change what\nthis means to you.",
                fontFamily = EvocaSerif,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaInk,
                lineHeight = 37.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text =
                    "Update the reason you saved this.\nThe link and reminder time stay unchanged.",
                fontFamily = EvocaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = EvocaMuted,
                lineHeight = 21.sp
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // -------------------------------------------------
            // WHY
            // -------------------------------------------------

            Text(
                text = "WHY",
                fontFamily = EvocaSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = EvocaMuted,
                letterSpacing = 1.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 120.dp
                    ),
                placeholder = {
                    Text(
                        text = "What do you want to remember?",
                        fontFamily = EvocaSans,
                        fontSize = 14.sp,
                        color = EvocaMuted
                    )
                },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = EvocaSans,
                        fontSize = 15.sp,
                        color = EvocaInk,
                        lineHeight = 22.sp
                    ),
                shape = RoundedCornerShape(15.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor =
                            EvocaSurface,
                        unfocusedContainerColor =
                            EvocaSurface,
                        focusedBorderColor =
                            EvocaGreen,
                        unfocusedBorderColor =
                            EvocaBorder,
                        cursorColor =
                            EvocaGreen,
                        focusedTextColor =
                            EvocaInk,
                        unfocusedTextColor =
                            EvocaInk
                    ),
                maxLines = 5
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // -------------------------------------------------
            // SAVE
            // -------------------------------------------------

            Button(
                onClick = {
                    if (note.isNotBlank()) {
                        onSave(note.trim())
                    }
                },
                enabled = note.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            EvocaGreen,
                        contentColor =
                            Color.White,
                        disabledContainerColor =
                            EvocaBorder,
                        disabledContentColor =
                            EvocaMuted
                    )
            ) {
                Text(
                    text = "Save changes",
                    fontFamily = EvocaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
    }
}
/*
 * ---------------------------------------------------------
 * TIME PICKER DIALOG
 * ---------------------------------------------------------
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    timePickerState: TimePickerState
) {

    AlertDialog(

        onDismissRequest =
            onDismiss,

        confirmButton = {

            TextButton(
                onClick = onConfirm
            ) {

                Text("OK")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Cancel")
            }
        },

        text = {

            TimePicker(
                state = timePickerState
            )
        }
    )
}


/*
 * ---------------------------------------------------------
 * RESCHEDULE REMINDER DIALOG
 * ---------------------------------------------------------
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleReminderDialog(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onReschedule: (Reminder, Long) -> Unit
) {
    var showDatePicker by remember {
        mutableStateOf(true)
    }

    var showTimePicker by remember {
        mutableStateOf(false)
    }

    var selectedDateMillis by remember {
        mutableStateOf<Long?>(null)
    }

    var selectedHour by remember {
        mutableStateOf<Int?>(null)
    }

    var selectedMinute by remember {
        mutableStateOf<Int?>(null)
    }

    var showError by remember {
        mutableStateOf(false)
    }

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                System.currentTimeMillis(),

            selectableDates =
                object : SelectableDates {

                    override fun isSelectableDate(
                        utcTimeMillis: Long
                    ): Boolean {

                        val selectedDate =
                            Calendar.getInstance(
                                java.util.TimeZone.getTimeZone(
                                    "UTC"
                                )
                            ).apply {
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
                                ) || (
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

    val currentTime =
        Calendar.getInstance()

    val timePickerState =
        rememberTimePickerState(
            initialHour =
                currentTime.get(
                    Calendar.HOUR_OF_DAY
                ),
            initialMinute =
                currentTime.get(
                    Calendar.MINUTE
                )
        )

    val selectedReminderTime: Long? =
        if (
            selectedDateMillis != null &&
            selectedHour != null &&
            selectedMinute != null
        ) {

            val calendar =
                Calendar.getInstance()

            calendar.timeInMillis =
                selectedDateMillis!!

            calendar.set(
                Calendar.HOUR_OF_DAY,
                selectedHour!!
            )

            calendar.set(
                Calendar.MINUTE,
                selectedMinute!!
            )

            calendar.set(
                Calendar.SECOND,
                0
            )

            calendar.set(
                Calendar.MILLISECOND,
                0
            )

            calendar.timeInMillis

        } else {
            null
        }

    val isFuture =
        selectedReminderTime != null &&
                selectedReminderTime >
                System.currentTimeMillis()

    // ---------------------------------------------------------
    // DATE PICKER
    // ---------------------------------------------------------

    if (showDatePicker) {

        DatePickerDialog(

            onDismissRequest = {
                onDismiss()
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val selectedDate =
                            datePickerState
                                .selectedDateMillis

                        if (selectedDate != null) {

                            selectedDateMillis =
                                selectedDate

                            showDatePicker =
                                false

                            showTimePicker =
                                true
                        }
                    }
                ) {

                    Text(
                        text = "Next",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color = EvocaGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) {

                    Text(
                        text = "Cancel",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        color = EvocaMuted
                    )
                }
            }

        ) {

            Column {

                Text(
                    text = "Bring it back later.",
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 18.dp,
                        end = 24.dp
                    ),
                    fontFamily = EvocaSerif,
                    fontSize = 22.sp,
                    fontWeight =
                        FontWeight.Normal,
                    color = EvocaInk
                )

                Text(
                    text =
                        "Choose when Evoca should remind you.",
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 4.dp,
                        end = 24.dp,
                        bottom = 8.dp
                    ),
                    fontFamily = EvocaSans,
                    fontSize = 12.sp,
                    color = EvocaMuted
                )

                DatePicker(
                    state = datePickerState
                )

                Text(
                    text =
                        "Past dates are unavailable.",
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 16.dp
                    ),
                    fontFamily = EvocaSans,
                    fontSize = 11.sp,
                    color = EvocaMuted
                )
            }
        }
    }

    // ---------------------------------------------------------
    // TIME PICKER
    // ---------------------------------------------------------

    if (showTimePicker) {

        AlertDialog(

            onDismissRequest = {
                onDismiss()
            },

            containerColor = EvocaSurface,

            title = {

                Column {

                    Text(
                        text = "Choose a time.",
                        fontFamily = EvocaSerif,
                        fontSize = 23.sp,
                        fontWeight =
                            FontWeight.Normal,
                        color = EvocaInk
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            "Evoca will bring it back then.",
                        fontFamily = EvocaSans,
                        fontSize = 12.sp,
                        color = EvocaMuted
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

                    if (
                        showError &&
                        !isFuture
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Please choose a future time.",
                            fontFamily = EvocaSans,
                            fontSize = 12.sp,
                            color = EvocaEmber
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        selectedHour =
                            timePickerState.hour

                        selectedMinute =
                            timePickerState.minute

                        val calendar =
                            Calendar.getInstance()

                        calendar.timeInMillis =
                            selectedDateMillis
                                ?: System.currentTimeMillis()

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

                        val finalTime =
                            calendar.timeInMillis

                        if (
                            finalTime >
                            System.currentTimeMillis()
                        ) {

                            onReschedule(
                                reminder,
                                finalTime
                            )

                        } else {

                            showError = true
                        }
                    }
                ) {

                    Text(
                        text = "Save",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color = EvocaGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) {

                    Text(
                        text = "Cancel",
                        fontFamily = EvocaSans,
                        fontSize = 13.sp,
                        color = EvocaMuted
                    )
                }
            }
        )
    }
}