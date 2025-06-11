package com.example.bluephoenix;

import static com.kizitonwose.calendar.core.ExtensionsKt.daysOfWeek;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Calendar
    private CalendarView calendarView;
    private TextView monthYearText;
    private LocalDate selectedDate = null;
    private YearMonth currentMonth;
    private Set<LocalDate> eventDates = new HashSet<>();

    // Navigation drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView menuIcon;
    private TextView userDisplayNameTextView, mainGreetingTextView;
    private EditText homeSearchField;
    private String currentUserId, currentFirstName;
    private TextView nav_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Grab passed user info
        currentUserId = getIntent().getStringExtra("USER_ID");
        currentFirstName = getIntent().getStringExtra("USER_NAME");

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

        if (currentFirstName != null && !currentFirstName.isEmpty()) {
            userDisplayNameTextView.setText("Hello, " + currentFirstName);
            mainGreetingTextView.setText(getGreetingBasedOnTime());
            nav_name.setText(currentFirstName);
        } else {
            userDisplayNameTextView.setText("Hello!");
            mainGreetingTextView.setText("Welcome!");
            nav_name.setText("Guest");
        }

        homeSearchField.setVisibility(View.GONE);

        // Calendar setup
        calendarView = findViewById(R.id.calendarView);
        monthYearText = findViewById(R.id.monthYearText);
        ImageButton prev = findViewById(R.id.prevMonthButton);
        ImageButton next = findViewById(R.id.nextMonthButton);

        // Sample events—replace with real source
        eventDates.add(LocalDate.now().plusDays(1));
        eventDates.add(LocalDate.now().plusDays(3));
        eventDates.add(LocalDate.now().plusDays(7));

        currentMonth = YearMonth.now();
        setupCalendar();
        setupDayTitles();
        setupMonthNavigation(prev, next);
        updateMonthYearText();
    }

    private void setupCalendar() {
        YearMonth start = currentMonth.minusMonths(100);
        YearMonth end = currentMonth.plusMonths(100);
        List<DayOfWeek> days = daysOfWeek();
        DayOfWeek first = days.get(0);

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
                    if (selectedDate != null) calendarView.notifyDateChanged(selectedDate);
                    selectedDate = date;
                    calendarView.notifyDateChanged(date);
                    Toast.makeText(CalendarActivity.this,
                            "Selected: " + date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            Toast.LENGTH_SHORT).show();
                });

                container.view.setSelected(date.equals(selectedDate));
                boolean monthDate = data.getPosition().equals(com.kizitonwose.calendar.core.DayPosition.MonthDate);
                container.textView.setTextColor(getColor(
                        monthDate ? android.R.color.white : android.R.color.darker_gray));
                container.eventIndicators.setVisibility(
                        eventDates.contains(date) ? View.VISIBLE : View.GONE);

                if (date.equals(LocalDate.now()) && !date.equals(selectedDate)) {
                    container.textView.setTextColor(getColor(android.R.color.holo_blue_light));
                }
            }
        });

        calendarView.setup(start, end, first);
        calendarView.scrollToMonth(currentMonth);
        calendarView.setMonthScrollListener(cm -> {
            currentMonth = cm.getYearMonth();
            updateMonthYearText();
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
        } else if (id == R.id.nav_gallery) {
            Toast.makeText(this, "Calendar selected", Toast.LENGTH_SHORT).show();
            // no-op since already in CalendarActivity
        } else if (id == R.id.nav_slideshow) {
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
        public LinearLayout eventIndicators;
        public View view;
        public DayViewContainer(View v) {
            super(v);
            view = v;
            textView = v.findViewById(R.id.calendarDayText);
            eventIndicators = v.findViewById(R.id.eventIndicators);
        }
    }
}
