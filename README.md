# Evoca

### A temporary reminder inbox for things you find online.

**Share → Remind → Notify → Act → Delete**

Evoca is an Android reminder app designed for things you discover online and want to act on later.

Instead of permanently saving links and forgetting why they mattered, Evoca brings them back at the right moment — then lets you act on them and move on.

---

## ✦ The Problem

You find something useful on Instagram, YouTube, Chrome, or another app.

You save the link.

Later, you have dozens of saved items and no idea why you saved them.

Evoca is designed around a different idea:

> Save it temporarily. Remember why it mattered. Get it back when it matters. Then let it go.

---

## ✦ Core Flow

```text
Discover something
       ↓
Share to Evoca
       ↓
Add a reason
       ↓
Choose when to be reminded
       ↓
Evoca notifies you
       ↓
Open / Act
       ↓
Done
       ↓
The reminder disappears

✦ Reminder Types
Scheduled Reminders
For things that need to come back at a specific time.

Examples:
- A YouTube tutorial to watch later
- An Instagram Reel to try later
- An article to read
- A product to check
- Something you want to act on tomorrow
Each scheduled reminder contains:
- URL
- Reason / note
- Reminder date
- Reminder time
Quick Reminders
For simple things that don't need a link or scheduled notification.

Examples:
- Take documents
- Bring keys
- Buy something
- Remember a task
Quick reminders are intentionally independent from scheduled reminders.
✦ Features
- Scheduled reminders
- Quick reminders
- Android Share Intent
- Share URLs directly from other apps
- URL validation
- Local reminder storage
- Exact-time alarm scheduling
- Android notifications
- Custom Evoca notification sound
- Reminder escalation
- Open saved links
- Reschedule reminders
- Edit reminder notes
- Delete reminders
- Mark reminders as done
- 48-hour temporary reminder lifecycle
- Notification permission handling
- Exact alarm capability handling
- Graceful fallback to Quick Reminders
- Works with the app closed
- Physical-device tested
✦ Share to Evoca
Evoca integrates with Android's sharing system.

For example:
YouTube / Instagram / Chrome
            ↓
          Share
            ↓
          Evoca
            ↓
       Add reminder
            ↓
        Set a time

The shared URL is automatically inserted into the reminder form.

This is one of the core parts of Evoca's workflow.
✦ Reminder Lifecycle
Scheduled reminders follow a temporary lifecycle:
ACTIVE
  ↓
Notification
  ↓
Open / Reschedule / Done
  ↓
Completed

Reminders also have a temporary expiry window rather than becoming a permanent archive.

The current implementation uses a 48-hour expiry window.
✦ Notification System
Evoca uses Android's notification and alarm APIs.

When a scheduled reminder becomes due:
1. AlarmManager triggers the reminder.
2. Evoca creates a high-priority notification.
3. A custom Evoca notification sound is played.
4. The user can open the reminder.
5. If necessary, Evoca schedules an escalation after 5 minutes.
6. The reminder can be opened, rescheduled, or completed.
The application also handles cancellation when reminders are deleted or completed through the reminder flow.
✦ Permissions
Evoca intentionally requests only permissions required for its reminder functionality.
Scheduled Reminders
Scheduled reminders require:
- Notifications
- Exact alarm capability where required by Android
Quick Reminders
Quick reminders do not require notification permission.

If scheduled reminders cannot currently be used, Evoca guides the user toward Quick Reminders instead of blocking the entire application.

Full-screen notification behavior is treated as an optional enhancement where supported by the device.
✦ Privacy / Architecture
Evoca currently uses local storage.

There is:
- No account system
- No backend
- No cloud database
- No login
- No unnecessary device permissions
Reminder data is stored locally using Room.
✦ Tech Stack
Technology	Purpose
Kotlin	Application language
Jetpack Compose	UI
Material 3	Compose UI components
Room	Local database
SQLite	Local persistence through Room
AlarmManager	Scheduled reminders
Android Notifications	Reminder alerts
Android Share Intent	Sharing content into Evoca
Kotlin Coroutines / Flow	Asynchronous and reactive data handling
Android SDK	Platform APIs


✦ Architecture
Evoca uses a simple local architecture separating UI, state management, and persistence.
Compose UI
    ↓
ViewModel
    ↓
Repository
    ↓
DAO
    ↓
Room Database

Scheduled reminders additionally interact with:
Reminder
    ↓
AlarmManager
    ↓
BroadcastReceiver
    ↓
Notification
    ↓
Reminder Activity

✦ Project Structure
app/
└── src/main/java/com/fabin/evoca/
    │
    ├── MainActivity.kt
    ├── ReminderAlarmActivity.kt
    ├── ReminderAlarmReceiver.kt
    │
    ├── data/
    │   ├── AppDatabase.kt
    │   ├── Reminder.kt
    │   ├── ReminderDao.kt
    │   └── ReminderRepository.kt
    │
    ├── permissions/
    │   └── EvocaPermissionManager.kt
    │
    ├── ui/
    │   └── theme/
    │
    └── viewmodel/
        └── ReminderViewModel.kt

✦ Design
Evoca follows a quiet, editorial visual direction rather than a typical productivity-app aesthetic.

The design uses:
- Warm cream backgrounds
- Earthy green accents
- Soft borders
- Rounded surfaces
- Serif editorial headings
- Manrope for functional UI text
- Minimal visual noise
The goal is to make reminders feel calm and intentional rather than demanding.
✦ Testing
Evoca has been tested on a physical Android device for core flows including:
- Scheduled reminders
- Quick reminders
- Notifications
- Custom notification sound
- Reminder screen
- Share-to-Evoca flow
- Editing
- Deleting
- Rescheduling
- Completing reminders
- Permission handling
- Empty states
- Scheduled / Quick navigation
✦ Installation
Development
Clone the repository:
git clone https://github.com/mohamedfabin962-pixel/Evoca.git

Open the project in Android Studio and allow Gradle to sync.

Then run the application on an Android emulator or physical Android device.
✦ Requirements
- Android Studio
- Android SDK
- JDK compatible with the project configuration
- Android device or emulator
- Minimum Android SDK: API 26
✦ Current Status
Evoca MVP — Functional

The current version focuses on the core reminder experience.

The project is being developed as a portfolio Android application while exploring real Android platform features such as:
- Jetpack Compose
- Room
- AlarmManager
- Notifications
- Android permissions
- Share Intent
- Lifecycle-aware UI
- Physical-device testing
✦ Future Ideas
Possible future improvements include:
- Better reminder history
- Additional reminder actions
- More notification customization
- Improved accessibility
- More device-specific optimization
- Release distribution through Google Play
These are intentionally outside the current MVP scope.
✦ Author
Mohamed Fabin

Junior MERN Stack Developer / Android Developer

GitHub:
https://github.com/mohamedfabin962-pixel

LinkedIn:
https://www.linkedin.com/in/mohamed-fabin-379aba379
License
This project is currently maintained as a personal portfolio project.