package com.example.bluephoenix;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.l4digital.fastscroll.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FastScrollTOCHandler {

    private Context context;
    private FastScrollRecyclerView recyclerView;
    private ScrollView tocPopup;
    private LinearLayout tocContent;
    private List<String> chapterTitles;
    private List<Integer> chapterPositions;
    private List<TextView> tocTextViews;
    private int currentActiveChapter = -1;

    public FastScrollTOCHandler(Context context, FastScrollRecyclerView recyclerView,
                                ScrollView tocPopup, LinearLayout tocContent,
                                List<String> chapterTitles, List<Integer> chapterPositions) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.tocPopup = tocPopup;
        this.tocContent = tocContent;
        this.chapterTitles = chapterTitles != null ? chapterTitles : new ArrayList<>();
        this.chapterPositions = chapterPositions != null ? chapterPositions : new ArrayList<>();
        this.tocTextViews = new ArrayList<>();

        buildTocContent();
    }

    public void updateChapterData(List<String> newChapterTitles, List<Integer> newChapterPositions) {
        this.chapterTitles.clear();
        this.chapterPositions.clear();

        if (newChapterTitles != null) {
            this.chapterTitles.addAll(newChapterTitles);
        }
        if (newChapterPositions != null) {
            this.chapterPositions.addAll(newChapterPositions);
        }

        buildTocContent();
    }

    private void buildTocContent() {
        // Clear existing TOC content (keep the header)
        tocTextViews.clear();

        // Remove all views except the first one (which should be the header)
        int childCount = tocContent.getChildCount();
        if (childCount > 1) {
            tocContent.removeViews(1, childCount - 1);
        }

        // Add chapter items to TOC
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < chapterTitles.size(); i++) {
            final int chapterIndex = i;
            String chapterTitle = chapterTitles.get(i);

            TextView tocItem = new TextView(context);
            tocItem.setText(chapterTitle);
            tocItem.setTextSize(14);
            tocItem.setPadding(16, 12, 16, 12);
            tocItem.setTextColor(ContextCompat.getColor(context, R.color.bp_bg));
            tocItem.setBackground(ContextCompat.getDrawable(context, android.R.drawable.list_selector_background));

            // Set click listener to scroll to chapter
            tocItem.setOnClickListener(v -> {
                if (chapterIndex < chapterPositions.size()) {
                    int position = chapterPositions.get(chapterIndex);
                    scrollToPosition(position);
                    forceHideToc();
                }
            });

            tocTextViews.add(tocItem);
            tocContent.addView(tocItem);
        }
    }

    private void scrollToPosition(int position) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(position, 0);
        }
    }

    public void updateActiveChapter(int activeChapterIndex) {
        if (activeChapterIndex < 0 || activeChapterIndex >= tocTextViews.size()) {
            return;
        }

        // Reset all chapter items to normal appearance
        for (TextView textView : tocTextViews) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.bp_bg));
            textView.setAlpha(0.7f);
            textView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Highlight the active chapter
        TextView activeTextView = tocTextViews.get(activeChapterIndex);
        activeTextView.setTextColor(ContextCompat.getColor(context, R.color.bp_name_light_blue));
        activeTextView.setAlpha(1.0f);
        activeTextView.setTypeface(null, android.graphics.Typeface.BOLD);

        // Scroll TOC to show active chapter
        scrollTocToShowActiveChapter(activeChapterIndex);

        currentActiveChapter = activeChapterIndex;
    }

    private void scrollTocToShowActiveChapter(int activeChapterIndex) {
        if (activeChapterIndex < 0 || activeChapterIndex >= tocTextViews.size()) {
            return;
        }

        TextView activeTextView = tocTextViews.get(activeChapterIndex);

        // Calculate position to scroll to
        int[] location = new int[2];
        activeTextView.getLocationInWindow(location);

        int[] tocLocation = new int[2];
        tocPopup.getLocationInWindow(tocLocation);

        int relativeY = location[1] - tocLocation[1];
        int tocHeight = tocPopup.getHeight();
        int itemHeight = activeTextView.getHeight();

        // If the active item is not visible, scroll to center it
        if (relativeY < 0 || relativeY + itemHeight > tocHeight) {
            int targetY = Math.max(0, relativeY - (tocHeight / 2) + (itemHeight / 2));
            tocPopup.smoothScrollTo(0, targetY);
        }
    }

    public void forceShowToc() {
        tocPopup.setVisibility(View.VISIBLE);
        tocPopup.setAlpha(0f);
        tocPopup.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
    }

    public void forceHideToc() {
        tocPopup.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> tocPopup.setVisibility(View.GONE))
                .start();
    }
}