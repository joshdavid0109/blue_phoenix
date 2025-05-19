package com.example.bluephoenix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        MaterialButton getStartedButton = findViewById(R.id.getStartedBtn);

        getStartedButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);

            // Example 2: Log a message (uncomment the Log import at the top)
             Log.d("MainActivity", "Get Started button clicked!");
             overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        });

        ViewPager2 viewPager2 = findViewById(R.id.viewPager);
        DotsIndicator dotsIndicator = findViewById(R.id.dotsIndicator);

        List<String> slides = Arrays.asList(
                "Welcome to Blue Phoenix!",
                "Illustrated Reviewers",
                "Justitia omnibus"
        );

        com.example.bluephoenix.SlideAdapter adapter = new com.example.bluephoenix.SlideAdapter(slides);
        viewPager2.setAdapter(adapter);
        dotsIndicator.setViewPager2(viewPager2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }

}