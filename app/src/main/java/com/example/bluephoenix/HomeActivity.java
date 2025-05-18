package com.example.bluephoenix;

import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private void setupSpinner() {
        // Get reference to your spinner
        Spinner spinner = findViewById(R.id.codals_more);

        // Create an ArrayList of items
        ArrayList<String> items = new ArrayList<>();
        items.add("Item 1");
        items.add("Item 2");
        items.add("Item 3");

        // Create an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                items
        );

        // Set the adapter to the spinner
        spinner.setAdapter(adapter);
    }

    // 2. Editing a specific item
    private void editSpinnerItem(int position, String newValue) {
        Spinner spinner = findViewById(R.id.codals_more);

        // Get the adapter and cast it to the appropriate type
        @SuppressWarnings("unchecked")
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();

        // Remove the old item and insert the new one at the same position
        adapter.remove(adapter.getItem(position));
        adapter.insert(newValue, position);

        // Notify the adapter that data has changed
        adapter.notifyDataSetChanged();
    }

    // 3. Adding a new item
    private void addSpinnerItem(String newItem) {
        Spinner spinner = findViewById(R.id.codals_more);

        @SuppressWarnings("unchecked")
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();

        // Add the new item
        adapter.add(newItem);

        // Notify the adapter that data has changed
        adapter.notifyDataSetChanged();
    }

    // 4. Removing an item
    private void removeSpinnerItem(int position) {
        Spinner spinner = findViewById(R.id.codals_more);

        @SuppressWarnings("unchecked")
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();

        // Remove the item at the specified position
        adapter.remove(adapter.getItem(position));

        // Notify the adapter that data has changed
        adapter.notifyDataSetChanged();
    }

    // 5. Replacing all items
    private void replaceAllSpinnerItems(ArrayList<String> newItems) {
        Spinner spinner = findViewById(R.id.codals_more);

        // Create a new adapter with the new items
        ArrayAdapter<String> newAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                newItems
        );

        // Set the new adapter
        spinner.setAdapter(newAdapter);
    }

    // 6. Getting the current selected item
    private String getSelectedItem() {
        Spinner spinner = findViewById(R.id.codals_more);
        return spinner.getSelectedItem().toString();
    }

    // 7. Setting a specific item as selected
    private void setSelectedItem(int position) {
        Spinner spinner = findViewById(R.id.codals_more);
        spinner.setSelection(position);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initial setup
        setupSpinner();

        // Example: Edit an item after 2 seconds
        new Handler().postDelayed(() -> {
            editSpinnerItem(1, "Updated Item 2");
        }, 2000);

    }
}