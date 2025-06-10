package com.example.bluephoenix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.l4digital.fastscroll.FastScroller;
import com.l4digital.fastscroll.FastScrollRecyclerView;

import java.util.List;

public class FastScrollTOCHandler {

    private final Context context;
    private final FastScrollRecyclerView recyclerView;
    private final View tocPopup;
    private final ViewGroup tocContainer;
    private List<String> chapterTitles;
    private List<Integer> chapterPositions;

    private boolean isFastScrollingActive = false;
    private boolean isTocVisible = false;

    private Runnable hideTocRunnable;

    // Flag to indicate if scroll was initiated by TOC click
    private boolean isScrollFromTocClick = false;

    public FastScrollTOCHandler(Context context,
                                FastScrollRecyclerView recyclerView,
                                View tocPopup,
                                ViewGroup tocContainer,
                                List<String> chapterTitles,
                                List<Integer> chapterPositions) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.tocPopup = tocPopup;
        this.tocContainer = tocContainer;
        this.chapterTitles = chapterTitles;
        this.chapterPositions = chapterPositions;

        setupFastScrollListener();
    }

    public void updateChapterData(List<String> newChapterTitles, List<Integer> newChapterPositions) {
        Log.d("TOC_DEBUG_ACTIVITY", "Updating chapter data. New titles size: " + newChapterTitles.size() + ", New positions size: " + newChapterPositions.size());
        this.chapterTitles = newChapterTitles;
        this.chapterPositions = newChapterPositions;
        if (isTocVisible) {
            populateTableOfContents();
            updateTocHighlight();
        }
    }

    private void setupFastScrollListener() {
        recyclerView.setFastScrollListener(new FastScroller.FastScrollListener() {
            @Override
            public void onFastScrollStart(FastScroller fastScroller) {
                Log.d("TOC_DEBUG_ACTIVITY", "onFastScrollStart");
                isFastScrollingActive = true;
                recyclerView.removeCallbacks(hideTocRunnable);
                showTocPopup();
            }

            @Override
            public void onFastScrollStop(FastScroller fastScroller) {
                Log.d("TOC_DEBUG_ACTIVITY", "onFastScrollStop");
                isFastScrollingActive = false;
                hideTocPopupDelayed();
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (isTocVisible) {
                    updateTocHighlight();
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Log.d("TOC_DEBUG_ACTIVITY", "onScrollStateChanged: " + newState + ", isFastScrollingActive: " + isFastScrollingActive + ", isTocVisible: " + isTocVisible + ", isScrollFromTocClick: " + isScrollFromTocClick);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Always update highlight when scrolling stops
                    if (isTocVisible) {
                        updateTocHighlight();
                    }

                    // If scroll was initiated by TOC click, allow it to settle, then hide
                    if (isScrollFromTocClick && isTocVisible) {
                        isScrollFromTocClick = false; // Reset the flag
                        hideTocPopupDelayed(); // Hide TOC after smooth scroll settles
                    }
                    // If not from TOC click, and not fast scrolling, then hide
                    else if (!isFastScrollingActive && isTocVisible) {
                        hideTocPopupDelayed();
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    // If user starts dragging (regular scroll or initiating fast scroll),
                    // remove any pending hide calls and ensure TOC is visible if it should be
                    recyclerView.removeCallbacks(hideTocRunnable);
                    // If TOC was hidden due to previous idle state, but user is now actively scrolling,
                    // consider showing it again if it makes sense (e.g., if they were fast scrolling)
                    if (isFastScrollingActive && !isTocVisible) {
                        showTocPopup();
                    }
                }
                // When RecyclerView is SETTLING (e.g., from smoothScrollToPosition),
                // we want the TOC to stay visible and update its highlight.
                // The `onScrolled` method already handles updating the highlight.
                // The hide logic is deferred to SCROLL_STATE_IDLE.
            }
        });
    }

    public void showTocPopup() {
        if (chapterTitles.isEmpty()) {
            Log.w("FastScrollTOCHandler", "No chapter titles available to show TOC.");
            return;
        }

        if (isTocVisible) {
            recyclerView.removeCallbacks(hideTocRunnable);
            updateTocHighlight();
            return;
        }

        populateTableOfContents();
        updateTocHighlight(); // Initial highlight right before animation

        tocPopup.setVisibility(View.VISIBLE);
        tocPopup.setAlpha(0f);
        tocPopup.setTranslationX(tocPopup.getWidth());
        tocContainer.setAlpha(0.5f);

        tocPopup.animate()
                .alpha(1f)
                .translationX(0)
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isTocVisible = true;
                        Log.d("TOC_DEBUG_ACTIVITY", "TOC shown. isTocVisible: " + isTocVisible);
                    }
                })
                .start();
    }

    private void hideTocPopupDelayed() {
        recyclerView.removeCallbacks(hideTocRunnable); // Ensure only one hide runnable is pending

        hideTocRunnable = () -> {
            // Only hide if fast scrolling is not active AND it's not a scroll initiated by TOC click
            // (the TOC click scroll handling will hide it after it settles)
            // AND the TOC is currently visible.
            if (!isFastScrollingActive && !isScrollFromTocClick && isTocVisible) {
                hideTocPopup();
                Log.d("TOC_DEBUG_ACTIVITY", "hideTocPopupDelayed: Hiding TOC.");
            } else {
                Log.d("TOC_DEBUG_ACTIVITY", "hideTocPopupDelayed: Not hiding TOC. isFastScrollingActive: " + isFastScrollingActive + ", isTocVisible: " + isTocVisible + ", isScrollFromTocClick: " + isScrollFromTocClick);
            }
        };
        recyclerView.postDelayed(hideTocRunnable, 1000); // Hide after 1 second of inactivity
    }

    public void hideTocPopup() {
        if (!isTocVisible) return;

        tocPopup.animate()
                .alpha(0f)
                .translationX(tocPopup.getWidth())
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        tocPopup.setVisibility(View.GONE);
                        isTocVisible = false;
                        tocContainer.removeAllViews();
                        Log.d("TOC_DEBUG_ACTIVITY", "TOC hidden. isTocVisible: " + isTocVisible);
                    }
                })
                .start();
    }

    private void populateTableOfContents() {
        Log.d("TOC_DEBUG_ACTIVITY", "Populating Table of Contents. Chapter count: " + chapterTitles.size());
        tocContainer.removeAllViews();

        TextView title = new TextView(context);
        title.setTextColor(context.getResources().getColor(R.color.bp_bg, null));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, (int) (context.getResources().getDisplayMetrics().density * 20));
        tocContainer.addView(title);

        for (int i = 0; i < chapterTitles.size(); i++) {
            final int chapterIndex = i;
            final int positionInRecyclerView = chapterPositions.get(i);
            final String chapterTitle = chapterTitles.get(i);

            View tocItem = LayoutInflater.from(context).inflate(R.layout.item_toc, tocContainer, false);
            TextView tocItemText = tocItem.findViewById(R.id.toc_item_text);
            tocItemText.setText(chapterTitle);
            tocItemText.setTag(chapterIndex);

            tocItem.setOnClickListener(v -> {
                Log.d("TOC_DEBUG_ACTIVITY", "TOC item clicked: " + chapterTitle + " at position: " + positionInRecyclerView);
                // Set flag to indicate scroll was initiated by TOC click
                isScrollFromTocClick = true;
                recyclerView.smoothScrollToPosition(positionInRecyclerView);
                // Do NOT hide TOC immediately. It will be hidden after the scroll settles
                // via onScrollStateChanged -> SCROLL_STATE_IDLE check.
            });

            tocContainer.addView(tocItem);
        }
    }

    private void updateTocHighlight() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null || chapterPositions.isEmpty()) {
            return;
        }

        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        int currentChapterIndex = getCurrentChapterIndex(firstVisibleItemPosition);

        Log.d("TOC_HIGHLIGHT_DEBUG", "First Visible: " + firstVisibleItemPosition + ", Current Chapter Index: " + currentChapterIndex);

        for (int i = 1; i < tocContainer.getChildCount(); i++) {
            View tocItem = tocContainer.getChildAt(i);
            TextView tocText = tocItem.findViewById(R.id.toc_item_text);

            Object tag = tocText.getTag();
            if (tag == null || !(tag instanceof Integer)) {
                Log.w("TOC_HIGHLIGHT_DEBUG", "TOC item tag is missing or not an Integer for item: " + tocText.getText());
                continue;
            }
            int itemChapterIndex = (int) tag;

            Log.d("TOC_HIGHLIGHT_DEBUG", "  TOC Item: " + tocText.getText() + ", Item Chapter Index: " + itemChapterIndex + ", Match: " + (itemChapterIndex == currentChapterIndex));

            if (itemChapterIndex == currentChapterIndex) {
                tocText.setTextColor(context.getResources().getColor(R.color.bp_name_light_blue, null));
                tocText.setTypeface(null, android.graphics.Typeface.BOLD);
                tocItem.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
            } else {
                tocText.setTextColor(context.getResources().getColor(R.color.bp_bg, null));
                tocText.setTypeface(null, android.graphics.Typeface.NORMAL);
                tocItem.setBackground(context.getDrawable(android.R.drawable.list_selector_background));
            }
        }
    }

    private int getCurrentChapterIndex(int currentRecyclerViewPosition) {
        Log.d("TOC_DEBUG_ACTIVITY", "getCurrentChapterIndex called with currentRecyclerViewPosition: " + currentRecyclerViewPosition);

        for (int i = chapterPositions.size() - 1; i >= 0; i--) {
            if (currentRecyclerViewPosition >= chapterPositions.get(i)) {
                Log.d("TOC_DEBUG_ACTIVITY", "  Found chapter index: " + i + " for position: " + currentRecyclerViewPosition + " (chapter starts at: " + chapterPositions.get(i) + ")");
                return i;
            }
        }
        Log.d("TOC_DEBUG_ACTIVITY", "  No chapter found >= current position. Defaulting to 0.");
        return 0;
    }

    public void forceShowToc() {
        Log.d("TOC_DEBUG_ACTIVITY", "forceShowToc called.");
        isFastScrollingActive = false;
        showTocPopup();
    }

    public void forceHideToc() {
        Log.d("TOC_DEBUG_ACTIVITY", "forceHideToc called.");
        isFastScrollingActive = false;
        isScrollFromTocClick = false; // Ensure this is also reset
        recyclerView.removeCallbacks(hideTocRunnable);
        hideTocPopup();
    }
}