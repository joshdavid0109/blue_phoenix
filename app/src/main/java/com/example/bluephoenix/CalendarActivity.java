package com.example.bluephoenix;

import static com.kizitonwose.calendar.core.ExtensionsKt.daysOfWeek;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class CalendarActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "CalendarActivity";

    // Calendar
    private CalendarView calendarView;
    private TextView monthYearText;
    private LocalDate selectedDate = null;
    private YearMonth currentMonth;
    private Set<LocalDate> eventDates = new HashSet<>();
    // Map to store events: Key = LocalDate, Value = List of Events for that day
    private Map<LocalDate, List<Event>> events = new HashMap<>();
    private FloatingActionButton fabAddEvent;

    // Navigation drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView menuIcon;
    private TextView userDisplayNameTextView, mainGreetingTextView;
    private EditText homeSearchField;
    private String currentUserId;
    private String currentFirstName; // Changed to String to store first name
    private TextView nav_name;

    private TextView backButton;

    // Firebase
    private FirebaseFirestore db;
    private ListenerRegistration firestoreListener;
    private FirebaseAuth mAuth;


    // Event class to store event details
    public static class Event {
        public String title;
        public String description;
        public LocalDate date; // Stored as LocalDate object
        private LocalTime alarmTime; // Stored as LocalTime object

        public Event() {
            // No-argument constructor required for Firestore
        }

        public Event(String title, String description, LocalDate date, LocalTime alarmTime) {
            this.title = title;
            this.description = description;
            this.date = date;
            this.alarmTime = alarmTime;
        }

        // Getters for Firestore serialization (and general access)
        public String getTitle() { return title; }
        public String getDescription() { return description; }

        // Convert LocalDate to String for Firestore (ISO 8601 format like "2025-06-12")
        public String getDate() { return date != null ? date.toString() : null; }

        // Convert LocalTime to the specific full string format required for Firestore or display.
        // This getter will reconstruct the full date-time-zone string from the date and time components.
        // IMPORTANT: This assumes both `date` and `alarmTime` are set.
        // If you intend to save `alarmTime` back to Firestore as a Timestamp, this getter is only for internal display/use.
        // If you save it as a String, this getter provides the correct format.
        public String getAlarmTime() {
            if (date != null && alarmTime != null) {
                // Combine LocalDate and LocalTime into LocalDateTime
                LocalDateTime combinedDateTime = date.atTime(alarmTime);
                // Attach a system default time zone to get ZonedDateTime for formatting with offset
                // Consider using a specific ZoneId like ZoneId.of("Asia/Manila") if your alarms are fixed to that timezone.
                ZonedDateTime zonedDateTime = combinedDateTime.atZone(ZoneId.systemDefault());

                // Format to the desired string format "MMMM d, yyyy 'at' hh:mm:ss a 'UTC'XXX"
                // 'XXX' generates an ISO-like offset (e.g., "+08:00").
                // If you specifically want "UTC+8" as a literal string, use 'UTC' and then the 'Z' pattern,
                // but ensure your parsing method can handle it. 'XXX' is generally more standard.
                return zonedDateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' hh:mm:ss a 'UTC'XXX", Locale.US));
            }
            return null;
        }

        // Setters for Firestore deserialization (and general access)
        public void setTitle(String title) { this.title = title; }
        public void setDescription(String description) { this.description = description; }

        // Convert String from Firestore to LocalDate
        public void setDate(String date) {
            this.date = date != null ? LocalDate.parse(date) : null;
        }

        // Convert the full alarmTime string from Firestore into a LocalTime object
        // This method is the one that needs to parse the complex string format.
        public void setAlarmTime(String alarmTimeStr) {
            if (alarmTimeStr != null) {
                try {
                    // The string from Firestore is like "June 12, 2025 at 12:00:00 AM UTC+08:00"
                    // Define a formatter that can parse this exact full string.
                    // Ensure 'XXX' or 'Z' matches how you formatted it when converting from Timestamp.
                    DateTimeFormatter fullAlarmTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' hh:mm:ss a 'UTC'XXX", Locale.US);

                    // Parse the full string into a ZonedDateTime (since it contains date, time, and timezone offset)
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(alarmTimeStr, fullAlarmTimeFormatter);

                    // Extract only the LocalTime part from the parsed ZonedDateTime
                    this.alarmTime = zonedDateTime.toLocalTime();

                } catch (DateTimeParseException e) {
                    // Log the error for debugging purposes
                    Log.e("Event", "Error parsing alarm time string '" + alarmTimeStr + "' to LocalTime: " + e.getMessage());
                    this.alarmTime = null; // Set to null on parsing error
                }
            } else {
                this.alarmTime = null;
            }
        }

        // Helper for local access (these getters already exist through the main accessors)
        public LocalDate getLocalDate() { return date; }
        public LocalTime getLocalTime() { return alarmTime; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        backButton = findViewById(R.id.add_post_back_button);

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Get current user ID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            // Fetch user's first name from Firestore or passed intent
            // For now, using passed intent if available
            currentFirstName = getIntent().getStringExtra("USER_NAME");
            if (currentFirstName == null || currentFirstName.isEmpty()) {
                fetchUserProfile(currentUserId); // Fetch if not passed
            }
        } else {
            // Handle case where user is not logged in (e.g., redirect to login)
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_LONG).show();
            finish(); // Close activity or redirect
            return;
        }


        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            View header = navigationView.getHeaderView(0);
            nav_name = header.findViewById(R.id.nav_header_title);
        }

        // Top-bar header setup
        menuIcon = findViewById(R.id.menu_icon);
        userDisplayNameTextView = findViewById(R.id.user_display_name);
        mainGreetingTextView = findViewById(R.id.main_greeting);
        homeSearchField = findViewById(R.id.home_search_field);

        menuIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });

        // Set initial user info if available from intent
        if (currentFirstName != null && !currentFirstName.isEmpty()) {
            updateUserInfoDisplay(currentFirstName);
        }

        homeSearchField.setVisibility(View.GONE);

        // Calendar setup
        calendarView = findViewById(R.id.calendarView);
        monthYearText = findViewById(R.id.monthYearText);
        ImageButton prev = findViewById(R.id.prevMonthButton);
        ImageButton next = findViewById(R.id.nextMonthButton);
        fabAddEvent = findViewById(R.id.fab_add_event);

        currentMonth = YearMonth.now();
        setupCalendar();
        setupDayTitles();
        setupMonthNavigation(prev, next);
        setupAddEventButton();
        updateMonthYearText();

        // Start listening for events from Firestore
        fetchEventsFromFirestore();
    }

    private void fetchUserProfile(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        if (firstName != null && !firstName.isEmpty()) {
                            currentFirstName = firstName;
                            updateUserInfoDisplay(currentFirstName);
                        } else {
                            currentFirstName = "Guest";
                            updateUserInfoDisplay(currentFirstName);
                        }
                    } else {
                        currentFirstName = "Guest";
                        updateUserInfoDisplay(currentFirstName);
                        Log.d(TAG, "User document does not exist for ID: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    currentFirstName = "Guest";
                    updateUserInfoDisplay(currentFirstName);
                    Log.e(TAG, "Error fetching user profile: " + e.getMessage());
                });
    }

    private void updateUserInfoDisplay(String name) {
        userDisplayNameTextView.setText("Hello, " + name);
        mainGreetingTextView.setText(getGreetingBasedOnTime());
        nav_name.setText(name);
    }


    private void addEventToFirestore(Event event) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "User not authenticated. Cannot add event.", Toast.LENGTH_SHORT).show();
            return;
        }

        String yearMonthDocId = event.getLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US));
        // String dateKey = event.getLocalDate().toString(); // No longer needed if 'dates' is just a flat array

        DocumentReference monthDocRef = db.collection("users").document(currentUserId)
                .collection("calendarEvents").document(yearMonthDocId);

        // Using Firestore transactions for atomic update
        db.runTransaction(transaction -> {
            DocumentSnapshot docSnapshot = transaction.get(monthDocRef);

            // This is the crucial change: 'dates' is an ArrayList of Maps (events)
            List<Map<String, Object>> existingEvents = (List<Map<String, Object>>) docSnapshot.get("dates");

            if (existingEvents == null) {
                existingEvents = new ArrayList<>(); // Initialize if no events exist for this month
            }

            // Convert Event object to a Map suitable for Firestore
            Map<String, Object> newEventMap = new HashMap<>();
            newEventMap.put("title", event.getTitle());
            newEventMap.put("description", event.getDescription());
            newEventMap.put("date", event.getDate()); // The date is part of the event map
            newEventMap.put("alarmTime", event.getAlarmTime());

            // Check if an event with the same title and date already exists to prevent duplicates
            // You might need a unique ID for each event for robust updates
            boolean eventExists = false;
            for (int i = 0; i < existingEvents.size(); i++) {
                Map<String, Object> existingEvent = existingEvents.get(i);
                // Assuming title and date uniquely identify an event for update
                if (event.getTitle().equals(existingEvent.get("title")) &&
                        event.getDate().equals(existingEvent.get("date"))) {
                    existingEvents.set(i, newEventMap); // Update the existing event
                    eventExists = true;
                    break;
                }
            }

            if (!eventExists) {
                existingEvents.add(newEventMap); // Add new event if it doesn't exist
            }

            // Set the entire 'dates' array back to the document
            // Use SetOptions.merge() to update only the 'dates' field
            List<Map<String, Object>> finalExistingEvents = existingEvents;
            transaction.set(monthDocRef, new HashMap<String, Object>() {{
                put("dates", finalExistingEvents);
            }}, SetOptions.merge());

            return null; // Return null if successful
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Event added/updated in Firestore for " + yearMonthDocId);
            Toast.makeText(this, "Event added successfully!", Toast.LENGTH_SHORT).show();
            // The Firestore listener will automatically update the UI
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Error adding/updating event to Firestore", e);
            Toast.makeText(this, "Failed to save event to cloud: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchEventsFromFirestore() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "Current user ID is null or empty. Cannot fetch events.");
            return;
        }

        CollectionReference calendarEventsCollectionRef = db.collection("users").document(currentUserId)
                .collection("calendarEvents");

        firestoreListener = calendarEventsCollectionRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot querySnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed for calendar events.", e);
                    Toast.makeText(CalendarActivity.this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                if (querySnapshots != null) {
                    events.clear();
                    eventDates.clear();

                    for (QueryDocumentSnapshot monthDocument : querySnapshots) {
                        Log.d(TAG, "Month document ID: " + monthDocument.getId());
                        Map<String, Object> monthData = monthDocument.getData();

                        if (monthData != null && monthData.containsKey("dates")) {
                            Object datesFieldValue = monthData.get("dates");

                            if (datesFieldValue instanceof List) {
                                List<Object> datesList = (List<Object>) datesFieldValue;

                                for (Object listItem : datesList) { // Each listItem is directly an event details map
                                    if (listItem instanceof Map) {
                                        Map<String, Object> eventDetailsMap = (Map<String, Object>) listItem;

                                        String eventTitle = (String) eventDetailsMap.get("title");
                                        String eventDescription = (String) eventDetailsMap.get("description");
                                        Object alarmTimeValue = eventDetailsMap.get("alarmTime");
                                        String dateString = (String) eventDetailsMap.get("date");

                                        String alarmTimeStr = null;
                                        if (alarmTimeValue instanceof Timestamp) {
                                            Timestamp timestamp = (Timestamp) alarmTimeValue;
                                            ZonedDateTime zonedDateTime = timestamp.toDate().toInstant()
                                                    .atZone(ZoneId.systemDefault());
                                            alarmTimeStr = zonedDateTime.format(
                                                    DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' hh:mm:ss a 'UTC'XXX", Locale.US)
                                            );
                                        } else if (alarmTimeValue instanceof String) {
                                            alarmTimeStr = (String) alarmTimeValue;
                                        } else {
                                            Log.w(TAG, "Unexpected type for 'alarmTime': " + (alarmTimeValue != null ? alarmTimeValue.getClass().getSimpleName() : "null"));
                                        }

                                        try {
                                            LocalDate eventLocalDate = null;
                                            if (dateString != null) {
                                                eventLocalDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
                                            } else {
                                                Log.w(TAG, "Event 'date' field is null for event: " + eventDetailsMap);
                                                continue;
                                            }

                                            if (eventTitle != null) {
                                                Event event = new Event();
                                                event.setTitle(eventTitle);
                                                event.setDescription(eventDescription != null ? eventDescription : "");
                                                event.setDate(eventLocalDate.toString());
                                                event.setAlarmTime(alarmTimeStr);

                                                events.computeIfAbsent(eventLocalDate, k -> new ArrayList<>()).add(event);
                                                eventDates.add(eventLocalDate);
                                            } else {
                                                Log.w(TAG, "Incomplete event data (title null) in month " + monthDocument.getId() + ", event: " + eventDetailsMap);
                                            }
                                        } catch (Exception ex) {
                                            Log.e(TAG, "Error parsing date or creating Event object for month " + monthDocument.getId() + ", event: " + eventDetailsMap, ex);
                                        }
                                    } else {
                                        Log.w(TAG, "Unexpected item type in 'dates' list for month " + monthDocument.getId() + ": " + listItem.getClass().getSimpleName());
                                    }
                                }
                            } else {
                                Log.w(TAG, "Field 'dates' is not a List in document: " + monthDocument.getId() + ". Actual type: " + datesFieldValue.getClass().getSimpleName());
                            }
                        } else {
                            Log.d(TAG, "Document " + monthDocument.getId() + " has no 'dates' field or it's null.");
                        }
                    }
                    Log.d(TAG, "Total unique event dates fetched: " + eventDates.size());
                    Log.d(TAG, "Total events fetched: " + events.values().stream().mapToInt(List::size).sum());
                    calendarView.notifyCalendarChanged();
                } else {
                    Log.d(TAG, "No calendar events found.");
                    events.clear();
                    eventDates.clear();
                    calendarView.notifyCalendarChanged();
                }
            }
        });
    }

    // Helper method to parse LocalDate from the "alarmTime" string
    private LocalDate parseDateFromAlarmTime(String alarmTimeString) {
        if (alarmTimeString == null || alarmTimeString.isEmpty()) {
            return null;
        }
        try {
            // This formatter should match the exact output format of the `alarmTimeStr` created above
            // or the string format if `alarmTime` was already a string in Firestore.
            // It should only parse the date part.
            String datePart = alarmTimeString.split(" at ")[0];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US); // Use 'yyyy' for year
            return LocalDate.parse(datePart, formatter);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date from alarmTime string: " + alarmTimeString, e);
            return null;
        }
    }

    private void setupAddEventButton() {
        fabAddEvent.setOnClickListener(v -> {
            // If a date is already selected on the calendar, use that.
            // Otherwise, prompt user to pick a date first.
            if (selectedDate != null) {
                showAddEventDialog(selectedDate);
            } else {
                showDatePickerForEvent();
            }
        });
    }

    private void showDatePickerForEvent() {
        LocalDate today = LocalDate.now();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate pickedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    showAddEventDialog(pickedDate);
                },
                today.getYear(),
                today.getMonthValue() - 1, // Month is 0-indexed for DatePickerDialog
                today.getDayOfMonth()
        );
        datePickerDialog.show();
    }

    private void showAddEventDialog(LocalDate date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.WhiteAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_event, null);

        EditText etEventTitle = dialogView.findViewById(R.id.et_event_title);
        EditText etEventDescription = dialogView.findViewById(R.id.et_event_description);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tv_selected_date);
        TextView tvAlarmTime = dialogView.findViewById(R.id.tv_alarm_time);

        tvSelectedDate.setText("Date: " + date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));

        final LocalTime[] selectedAlarmTime = {null}; // To hold the selected time

        tvAlarmTime.setOnClickListener(v -> {
            int initialHour = 0;
            int initialMinute = 0;
            boolean is24HourFormat = android.text.format.DateFormat.is24HourFormat(this);

            if (selectedAlarmTime[0] != null) {
                initialHour = selectedAlarmTime[0].getHour();
                initialMinute = selectedAlarmTime[0].getMinute();
            } else {
                final java.util.Calendar c = java.util.Calendar.getInstance();
                initialHour = c.get(java.util.Calendar.HOUR_OF_DAY);
                initialMinute = c.get(java.util.Calendar.MINUTE);
            }

            TimePickerDialog timePickerDialog = new TimePickerDialog(CalendarActivity.this,
                    (view, hourOfDay, minute) -> {
                        selectedAlarmTime[0] = LocalTime.of(hourOfDay, minute);
                        String pattern = is24HourFormat ? "HH:mm" : "hh:mm a";
                        tvAlarmTime.setText(selectedAlarmTime[0].format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())));
                        tvAlarmTime.setTextColor(ContextCompat.getColor(CalendarActivity.this, R.color.black));
                    }, initialHour, initialMinute, is24HourFormat);
            timePickerDialog.show();
        });


        builder.setView(dialogView)
                .setTitle("Add Event")
                .setPositiveButton("Add", (dialog, id) -> {
                    String title = etEventTitle.getText().toString().trim();
                    String description = etEventDescription.getText().toString().trim();

                    if (!title.isEmpty()) {
                        Event newEvent = new Event(title, description, date, selectedAlarmTime[0]);
                        addEventToFirestore(newEvent); // Add event to Firestore
                        // The calendar UI will be updated by the Firestore listener
                    } else {
                        Toast.makeText(this, "Please enter an event title", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                try {
                    int positiveBtnColor = ContextCompat.getColor(CalendarActivity.this, R.color.bp_bg);
                    int negativeBtnColor = ContextCompat.getColor(CalendarActivity.this, R.color.black);

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(positiveBtnColor);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(negativeBtnColor);

                } catch (Exception e) {
                    Log.e(TAG, "Error setting button colors programmatically: " + e.getMessage(), e);
                }
            }
        });

        dialog.show();
    }


    @SuppressLint("ResourceAsColor") // Keep this if you're using R.color.black (though Color.BLACK is safer for direct color)
    private void showEventDetailsDialog(LocalDate date) {
        List<Event> dayEvents = events.get(date);
        if (dayEvents == null || dayEvents.isEmpty()) {
            // This should ideally not happen if the dialog is only called when eventDates.contains(date)
            return;
        }

        // Use 'this' as the context for the builder.
        // No need for a specific light theme if you're setting background and text colors manually.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        StringBuilder eventDetails = new StringBuilder();

        for (int i = 0; i < dayEvents.size(); i++) {
            Event event = dayEvents.get(i);

            // Add Title Header and Value
            eventDetails.append("Title: ").append(event.getTitle());

            // Add Time if available
            if (event.getLocalTime() != null) {
                eventDetails.append(" (").append(event.getLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))).append(")");
            }

            // Add Description Header and Value if available
            if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                eventDetails.append("\nDescription: ").append(event.getDescription());
            }

            // Add new line for separation between events, but not after the last one
            if (i < dayEvents.size() - 1) {
                eventDetails.append("\n\n");
            }
        }

        builder.setTitle("Events on " + date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)))
                .setMessage(eventDetails.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Add Event", (dialog, id) -> showAddEventDialog(date));

        // Create the dialog
        AlertDialog dialog = builder.create();

        // Show the dialog first so its views are initialized and window properties can be accessed.
        dialog.show();

        // 1. Set the background of the dialog to white
        // Use Objects.requireNonNull for safety when accessing getWindow()
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        // 2. Set the title text color to black
        TextView titleTextView = dialog.findViewById(android.R.id.title);
        if (titleTextView != null) {
            // Using Color.BLACK is generally safer and more direct than R.color.black
            // if you don't need theme tinting or resource management.
            titleTextView.setTextColor(Color.BLACK);
        }

        // 3. Set the message text color to black
        TextView messageTextView = dialog.findViewById(android.R.id.message);
        if (messageTextView != null) {
            messageTextView.setTextColor(Color.BLACK);
        }

        // 4. Set the button text colors to black
        // IMPORTANT: Access buttons after show() as they are inflated.
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.BLACK);
        }

        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutralButton != null) {
            neutralButton.setTextColor(R.color.bp_name_light_blue);
        }

        // If you had a custom layout for the dialog, you'd find views using dialog.findViewById(R.id.your_custom_view_id)
    }

    private void setupCalendar() {
        YearMonth start = currentMonth.minusMonths(100);
        YearMonth end = currentMonth.plusMonths(100);
        List<DayOfWeek> days = daysOfWeek();
        DayOfWeek first = days.get(0); // Assuming Monday as the first day, adjust if needed

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }
            @Override
            public void bind(DayViewContainer container, CalendarDay data) {
                LocalDate date = data.getDate();
                container.textView.setText(String.valueOf(date.getDayOfMonth()));

                container.view.setOnClickListener(v -> {
                    // Clear previous selection visually
                    if (selectedDate != null) calendarView.notifyDateChanged(selectedDate);
                    selectedDate = date;
                    // Notify current selection visually
                    calendarView.notifyDateChanged(date);

                    // Show event details if there are events on this date
                    if (eventDates.contains(date)) {
                        showEventDetailsDialog(date);
                    } else {
                        Toast.makeText(CalendarActivity.this,
                                "Selected: " + date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                Toast.LENGTH_SHORT).show();
                    }
                });

                // Update selected state and colors
                container.view.setSelected(date.equals(selectedDate));
                boolean isMonthDate = data.getPosition().equals(com.kizitonwose.calendar.core.DayPosition.MonthDate);

                if (isMonthDate) {
                    container.textView.setTextColor(getColor(android.R.color.white)); // Your default day color
                    if (date.equals(selectedDate)) {
                        container.textView.setTextColor(getColor(R.color.white)); // Text color for selected day
                        container.textView.setBackgroundResource(R.drawable.calendar_selected_day_background); // Custom background for selected day
                    } else if (date.equals(LocalDate.now())) {
                        container.textView.setTextColor(getColor(R.color.bp_name_light_blue)); // Today's date color
                        container.textView.setBackground(null); // No background for today if not selected
                    } else {
                        container.textView.setBackground(null); // No background for other days
                    }
                } else {
                    container.textView.setTextColor(getColor(android.R.color.darker_gray)); // Out-of-month days
                    container.textView.setBackground(null);
                }

                // Show/hide event indicator based on eventDates set
                container.eventIndicators.setVisibility(
                        eventDates.contains(date) ? View.VISIBLE : View.GONE);
            }
        });

        calendarView.setup(start, end, first);
        calendarView.scrollToMonth(currentMonth);
        calendarView.setMonthScrollListener(cm -> {
            currentMonth = cm.getYearMonth();
            updateMonthYearText();
            // Optionally, if you want to automatically select the first day of the new month
            // selectedDate = currentMonth.atDay(1);
            // calendarView.notifyMonthChanged(currentMonth);
            return null;
        });
    }

    private void setupDayTitles() {
        LinearLayout titles = findViewById(R.id.titlesContainer);
        Iterator<View> it = androidx.core.view.ViewGroupKt.getChildren(titles).iterator();
        List<TextView> labels = new ArrayList<>();
        while (it.hasNext()) {
            View v = it.next();
            if (v instanceof TextView) labels.add((TextView) v);
        }
        DayOfWeek[] std = DayOfWeek.values();
        for (int i = 0; i < labels.size() && i < std.length; i++) {
            labels.get(i).setText(std[i].getDisplayName(TextStyle.SHORT, Locale.getDefault()));
        }
    }

    private void setupMonthNavigation(ImageButton prev, ImageButton next) {
        prev.setOnClickListener(v -> calendarView.smoothScrollToMonth(currentMonth.minusMonths(1)));
        next.setOnClickListener(v -> calendarView.smoothScrollToMonth(currentMonth.plusMonths(1)));
    }

    private void updateMonthYearText() {
        monthYearText.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
    }

    private String getGreetingBasedOnTime() {
        int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        if (hour < 12) return "Good morning!";
        if (hour < 18) return "Good afternoon!";
        return "Good evening!";
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent navIntent = null;

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show();
            navIntent = new Intent(this, HomeActivity.class);
        } else if (id == R.id.nav_gallery) { // Assuming nav_gallery is for Calendar
            Toast.makeText(this, "Calendar selected", Toast.LENGTH_SHORT).show();
            // no-op since already in CalendarActivity
        } else if (id == R.id.nav_slideshow) { // Assuming nav_slideshow is for Forum
            Toast.makeText(this, "Forum selected", Toast.LENGTH_SHORT).show();
            navIntent = new Intent(this, ForumActivity.class);
        }

        if (navIntent != null) {
            navIntent.putExtra("USER_ID", currentUserId);
            navIntent.putExtra("USER_NAME", currentFirstName);
            startActivity(navIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }

        drawerLayout.closeDrawer(navigationView);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    public static class DayViewContainer extends ViewContainer {
        public TextView textView;
        public LinearLayout eventIndicators; // This is the LinearLayout in calendar_day_layout for the indicator
        public View view; // The root view of the layout

        public DayViewContainer(View v) {
            super(v);
            view = v;
            textView = v.findViewById(R.id.calendarDayText); // Assuming you have a TextView with this ID
            eventIndicators = v.findViewById(R.id.eventIndicators); // Assuming a LinearLayout for indicators
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove(); // Stop listening for Firestore updates
        }
    }
}