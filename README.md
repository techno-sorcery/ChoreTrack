# ChoreTracker

## Overview
ChoreTracker is an Android application designed to help households organize and track chores. Users can add chores, assign them to household members, categorize them with tags, and track completion history. The app supports different chore types (one-time, daily, weekly, and monthly) and automatically resets recurring chores when their time period changes.

Users can also set due dates for chores and filter chores by tag to quickly find tasks. The goal of the app is to make it easy for people living together to manage responsibilities and see what has already been completed.

---

## Figma Design
Initial UI layouts were planned using **Figma** before implementing the screens in Jetpack Compose.

Link:

---

## Android / Jetpack Compose Features Used

### Jetpack Compose UI
The interface is built entirely with Jetpack Compose using:

- `@Composable` functions
- Layout components:
    - `Column`
    - `Row`
    - `LazyColumn`
    - `Box`
- `Modifier` chains for layout control
- Material3 components:
    - `Card`
    - `AlertDialog`
    - `FloatingActionButton`
    - `Checkbox`
    - `OutlinedTextField`
    - `TabRow`

### State Management
Application state is handled using:

- `ViewModel` (`CTViewModel`)
- `mutableStateOf`
- `mutableStateListOf`
- `remember`
- `rememberSaveable`
- state hoisting between composables

### Navigation
The application supports multiple screens controlled through an app destination state system.

### Data Persistence
Data is stored locally using:

- `SharedPreferences`
- JSON serialization using **Gson**

Stored data includes:
- household members
- chore list
- completion data

### Additional Android Features
- Material3 UI components
- `DatePicker` for selecting due dates
- coroutine usage through `viewModelScope`

### Third Party Libraries
- **Gson** – used for JSON serialization and deserialization of saved data

---

## Device / SDK Requirements

To run the application:

- Android Studio Hedgehog or newer recommended
- Minimum SDK: **Android API 24**
- Target SDK: **Android API 34**

The project uses:

- Kotlin
- Jetpack Compose
- Material3

The app should run on both Android emulators and physical Android devices.

---

## Features

### Chore Management
Users can:

- Create new chores
- Add descriptions
- Assign chores to household members
- Add tags to categorize chores
- Set optional due dates

### Recurring Chores
Chores can be set as:

- One-time
- Daily
- Weekly
- Monthly

Recurring chores automatically reset when the next period begins.

### Tag Filtering
Chores can be filtered by tags to quickly find related tasks.

### Completion Tracking
The application tracks:

- completion count
- last completion time
- history of completed chores

### Household Members
Users can add multiple people to the household and assign chores to them.

---

## Above and Beyond

Additional functionality implemented beyond the core assignment requirements includes:

- Tag filtering system
- Due date support
- Overdue chore detection
- Completion history view
- Automatic recurring chore reset
- Editable chore assignments
- Persistent data storage for both chores and people

---

## How to Run

### 1. Clone the repository

```bash
git clone <repo-url>