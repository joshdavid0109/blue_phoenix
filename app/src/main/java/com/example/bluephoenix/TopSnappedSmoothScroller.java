package com.example.bluephoenix; // Adjust this to your actual package name

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;

public class TopSnappedSmoothScroller extends LinearSmoothScroller {

    // You can adjust this value to control the speed of the smooth scroll.
    // A higher value makes it slower, a lower value makes it faster.
    private static final float MILLISECONDS_PER_INCH = 50f; // Example: 50ms per inch

    public TopSnappedSmoothScroller(Context context) {
        super(context);
    }

    @Override
    protected int getVerticalSnapPreference() {
        // This is the crucial part: SNAP_TO_START ensures the item is
        // aligned to the start (top for vertical RecyclerView) of the RecyclerView.
        return SNAP_TO_START;
    }

    // You can override this to control the scroll speed (optional, but good for consistency)
    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
    }

    // This method is called to calculate the scroll distance to the target position.
    // If the item is already partially visible but not at the top,
    // ensure it still scrolls to the top.
    @Override
    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
        // We want to align the start of the view (boxStart) with the start of the parent (viewStart).
        // So, the distance to scroll is simply (boxStart - viewStart).
        return boxStart - viewStart;
    }
}