package com.example.bluephoenix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.l4digital.fastscroll.FastScroller;
import com.l4digital.fastscroll.FastScrollRecyclerView;

import java.util.List;

// This custom smooth scroller ensures the target item always snaps to the top of the RecyclerView.
class ChapterSmoothScroller extends LinearSmoothScroller {

    public ChapterSmoothScroller(Context context) {
        super(context);
    }

    // This is the key: always snap the target item to the start (top for vertical RecyclerView)
    @Override
    protected int getVerticalSnapPreference() {
        return SNAP_TO_START;
    }

    // Optional: You can adjust the scroll speed if the default is too fast or slow
    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        // Default is 25f. A higher value makes it scroll faster.
        // Adjust this value to control the speed of the smooth scroll.
        return 100f / displayMetrics.densityDpi; // Example: 100 pixels per millisecond
    }
}


public class FastScrollTOCHandler {

    private final Context context;
    private final FastScrollRecyclerView recyclerView;
    private final View tocPopup;
    private final ViewGroup tocContainer;
    private List<String> chapterTitles;
    private List<Integer> chapterPositions; // These should be the adapter positions of chapter titles/starts

    private boolean isFastScrollingActive = false;
    private boolean isTocVisible = false;

    private Runnable hideTocRunnable;

    // Flag to indicate if scroll was initiated by TOC click
    private boolean isScrollFromTocClick = false;
    // Variable to store the chapter index targeted by a TOC click
    private int targetChapterIndexFromClick = -1;

    // Track last highlighted chapter to avoid unnecessary updates
    private int lastHighlightedChapter = -1;

    // New flag to differentiate manual scrolling from fast-scroller interaction
    private boolean isManualScrolling = false;


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
        this.lastHighlightedChapter = -1; // Reset highlight tracking
        if (isTocVisible) {
            populateTableOfContents();
            // After data update, apply highlight based on current scroll position
            updateTocHighlight();
        }
    }

    private void setupFastScrollListener() {
        recyclerView.setFastScrollListener(new FastScroller.FastScrollListener() {
            @Override
            public void onFastScrollStart(FastScroller fastScroller) {
                Log.d("TOC_DEBUG_ACTIVITY", "onFastScrollStart");
                isFastScrollingActive = true;
                isManualScrolling = false; // Not manual scrolling if fast scroll starts
                cancelHideToc();
                showTocPopup();
            }

            @Override
            public void onFastScrollStop(FastScroller fastScroller) {
                Log.d("TOC_DEBUG_ACTIVITY", "onFastScrollStop");
                isFastScrollingActive = false;
                // Immediately update highlight on stop to ensure it's correct
                updateTocHighlight();
                hideTocPopupDelayed();
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Only update TOC highlight if the TOC is currently visible
                // and if it's either fast-scrolling or a TOC-click scroll
                if (isTocVisible && (isFastScrollingActive || isScrollFromTocClick)) {
                    updateTocHighlight();
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Log.d("TOC_DEBUG_ACTIVITY", "onScrollStateChanged: " + newState +
                        ", isFastScrollingActive: " + isFastScrollingActive +
                        ", isTocVisible: " + isTocVisible +
                        ", isScrollFromTocClick: " + isScrollFromTocClick +
                        ", isManualScrolling: " + isManualScrolling + // Log new flag
                        ", targetChapterIndexFromClick: " + targetChapterIndexFromClick);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // When scroll stops, ensure the final highlight is correct.
                    // This applies whether it's fast scroll, click scroll, or manual scroll.
                    if (isTocVisible) {
                        if (isScrollFromTocClick && targetChapterIndexFromClick != -1) {
                            Log.d("TOC_HIGHLIGHT_DEBUG", "Scroll from TOC click settled. Forcing highlight to chapter: " + targetChapterIndexFromClick);
                            if (targetChapterIndexFromClick != lastHighlightedChapter) {
                                lastHighlightedChapter = targetChapterIndexFromClick;
                                updateTocHighlightForChapter(targetChapterIndexFromClick);
                            } else {
                                Log.d("TOC_HIGHLIGHT_DEBUG", "Target chapter already highlighted: " + targetChapterIndexFromClick);
                            }
                        } else {
                            // Otherwise (manual scroll or fast scroll), update highlight based on current scroll position
                            updateTocHighlight();
                        }
                    }

                    // Hide TOC logic:
                    // Hide if:
                    // 1. Not actively fast-scrolling.
                    // 2. Not currently engaged in a TOC-click initiated scroll.
                    // 3. TOC is visible.
                    if (!isFastScrollingActive && !isScrollFromTocClick && isTocVisible) {
                        hideTocPopupDelayed();
                    } else if (isScrollFromTocClick && isTocVisible) {
                        // If it was a TOC click, hide it after it settles
                        hideTocPopupDelayed();
                    }

                    // Reset flags for next interaction
                    isScrollFromTocClick = false;
                    targetChapterIndexFromClick = -1;
                    isManualScrolling = false; // Reset manual scrolling flag
                    Log.d("TOC_DEBUG_ACTIVITY", "Scroll settled. Flags reset.");

                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    // User is actively dragging the RecyclerView manually (not via FastScroller)
                    if (!isFastScrollingActive) { // Crucial check: only if not already in fast scroll mode
                        isManualScrolling = true;
                        isScrollFromTocClick = false; // If manual drag, cancel any pending click scroll flag
                        targetChapterIndexFromClick = -1;
                        cancelHideToc(); // Cancel any pending hide TOC runnable

                        // DO NOT show TOC popup here if it's just manual dragging.
                        // The prompt explicitly states "only when the scrollbar is dragged directly".
                        // The fast scroller listener handles that.
                        Log.d("TOC_DEBUG_ACTIVITY", "Manual dragging detected. TOC should NOT appear.");
                    } else {
                        // If fast scrolling is active, and dragging is detected, it means the
                        // user is using the fast scroller, so keep isManualScrolling false.
                        Log.d("TOC_DEBUG_ACTIVITY", "Fast scrolling active, dragging confirms FastScroller interaction.");
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    // During auto-scroll (e.g., after fling or smoothScrollBy/smoothScrollToPosition)
                    cancelHideToc(); // Ensure TOC stays visible during auto-scroll initiated by fast scroll or click
                }
            }
        });
    }

    private void cancelHideToc() {
        if (hideTocRunnable != null) {
            recyclerView.removeCallbacks(hideTocRunnable);
            hideTocRunnable = null; // Clear the runnable reference
        }
    }

    public void showTocPopup() {
        if (chapterTitles.isEmpty()) {
            Log.w("FastScrollTOCHandler", "No chapter titles available to show TOC.");
            return;
        }

        if (isTocVisible) {
            cancelHideToc(); // Cancel any pending hide if already visible
            updateTocHighlight(); // Update highlight in case scroll position changed while visible
            return;
        }

        populateTableOfContents(); // Only populate if TOC is not visible
        updateTocHighlight(); // Initial highlight before animation based on current scroll

        tocPopup.setVisibility(View.VISIBLE);
        tocPopup.setAlpha(0f); // Start completely transparent for animation
        tocPopup.setTranslationX(tocPopup.getWidth()); // Start off-screen to the right

        tocPopup.animate()
                .alpha(0.75f) // Fade in
                .translationX(0) // Slide in from right
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
        cancelHideToc(); // Ensure only one hide runnable is pending

        hideTocRunnable = () -> {
            // Only hide if none of the conditions for keeping it visible are met
            if (!isFastScrollingActive && !isScrollFromTocClick && isTocVisible && !isManualScrolling) { // Added isManualScrolling to condition
                hideTocPopup();
                Log.d("TOC_DEBUG_ACTIVITY", "hideTocPopupDelayed: Hiding TOC due to inactivity.");
            } else {
                Log.d("TOC_DEBUG_ACTIVITY", "hideTocPopupDelayed: Not hiding TOC yet. " +
                        "isFastScrollingActive: " + isFastScrollingActive +
                        ", isTocVisible: " + isTocVisible +
                        ", isScrollFromTocClick: " + isScrollFromTocClick +
                        ", isManualScrolling: " + isManualScrolling);
            }
        };
        recyclerView.postDelayed(hideTocRunnable, 1000); // 1-second delay
    }

    public void hideTocPopup() {
        if (!isTocVisible) return;

        tocPopup.animate()
                .alpha(0f) // Fade out
                .translationX(tocPopup.getWidth()) // Slide out to the right
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        tocPopup.setVisibility(View.GONE);
                        isTocVisible = false;
                        lastHighlightedChapter = -1; // Reset highlight tracking when TOC hides
                        tocContainer.removeAllViews(); // Clear views to re-populate on next show
                        Log.d("TOC_DEBUG_ACTIVITY", "TOC hidden. isTocVisible: " + isTocVisible);
                    }
                })
                .start();
    }

    private void populateTableOfContents() {
        Log.d("TOC_DEBUG_ACTIVITY", "Populating Table of Contents. Chapter count: " + chapterTitles.size());
        tocContainer.removeAllViews(); // Clear existing views before populating

        // Add "Table of Contents" title
        TextView title = new TextView(context);
        title.setText("Table of Contents");
        title.setTextColor(context.getResources().getColor(R.color.bp_bg, null));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, (int) (context.getResources().getDisplayMetrics().density * 20));
        tocContainer.addView(title);

        for (int i = 0; i < chapterTitles.size(); i++) {
            final int chapterIndex = i;
            final int positionInRecyclerView = chapterPositions.get(i); // This is the adapter position of the chapter start
            final String chapterTitle = chapterTitles.get(i);

            View tocItem = LayoutInflater.from(context).inflate(R.layout.item_toc, tocContainer, false);
            TextView tocItemText = tocItem.findViewById(R.id.toc_item_text);
            tocItemText.setText(chapterTitle);
            tocItemText.setTag(chapterIndex); // Store chapter index in the tag

            // Set initial state (unhighlighted) when populating the TOC
            tocItemText.setTextColor(context.getResources().getColor(R.color.bp_bg, null));
            tocItemText.setTypeface(null, android.graphics.Typeface.NORMAL);
            tocItem.setBackground(context.getDrawable(android.R.drawable.list_selector_background));


            tocItem.setOnClickListener(v -> {
                Log.d("TOC_DEBUG_ACTIVITY", "TOC item clicked: " + chapterTitle + " (Chapter Index: " + chapterIndex + ") at position: " + positionInRecyclerView);

                // 1. Set the flag to indicate scroll was initiated by a TOC click
                isScrollFromTocClick = true;
                // 2. Store the target chapter index
                targetChapterIndexFromClick = chapterIndex;
                // 3. Immediately apply the highlight for the clicked chapter
                updateTocHighlightDirectly(chapterIndex);
                lastHighlightedChapter = chapterIndex; // Update tracked highlight

                // 4. Perform the smooth scroll using the custom scroller for consistent "snap-to-top" behavior
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    ChapterSmoothScroller smoothScroller = new ChapterSmoothScroller(recyclerView.getContext());
                    smoothScroller.setTargetPosition(positionInRecyclerView);
                    layoutManager.startSmoothScroll(smoothScroller);
                }

                // The hide will be handled by onScrollStateChanged -> SCROLL_STATE_IDLE
                // Do not hide here immediately, let the scroll settle.
            });

            tocContainer.addView(tocItem);
        }
    }

    // This method is called specifically when a TOC item is clicked
    private void updateTocHighlightDirectly(int targetChapterIndex) {
        Log.d("TOC_HIGHLIGHT_DEBUG", "Directly applying highlight for chapter index: " + targetChapterIndex);

        // Start from index 1 to skip the "Table of Contents" title TextView
        for (int i = 1; i < tocContainer.getChildCount(); i++) {
            View tocItem = tocContainer.getChildAt(i);
            TextView tocText = tocItem.findViewById(R.id.toc_item_text);

            Object tag = tocText.getTag();
            if (tag == null || !(tag instanceof Integer)) {
                Log.w("TOC_HIGHLIGHT_DEBUG", "TOC item tag is missing or not an Integer for item: " + tocText.getText());
                continue;
            }
            int itemChapterIndex = (int) tag;

            if (itemChapterIndex == targetChapterIndex) {
                // Highlight current chapter
                tocText.setTextColor(context.getResources().getColor(R.color.bp_name_light_blue, null));
                tocText.setTypeface(null, android.graphics.Typeface.BOLD);
                tocItem.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
                Log.d("TOC_HIGHLIGHT_DEBUG", " -- Highlighted: " + tocText.getText());
            } else {
                // Normal appearance for other chapters
                tocText.setTextColor(context.getResources().getColor(R.color.bp_bg, null));
                tocText.setTypeface(null, android.graphics.Typeface.NORMAL);
                tocItem.setBackground(context.getDrawable(android.R.drawable.list_selector_background));
                Log.d("TOC_HIGHLIGHT_DEBUG", " -- De-highlighted: " + tocText.getText());
            }
        }
    }

    // This method calculates the current chapter based on RecyclerView's visible items
    // and applies the highlight *if not overridden by a click-initiated scroll*.
    private void updateTocHighlight() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null || chapterPositions.isEmpty()) {
            return;
        }

        // IMPORTANT: If scroll was initiated by a TOC click AND it's still settling,
        // we should temporarily "stick" to the clicked chapter's highlight.
        // This prevents the scroll listener from immediately overriding the intended highlight.
        if (isScrollFromTocClick && recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING) {
            Log.d("TOC_HIGHLIGHT_DEBUG", "Scroll from TOC click in progress (SETTLING). Sticking to lastHighlightedChapter: " + lastHighlightedChapter);
            return; // EXIT EARLY
        }

        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();

        int currentChapterIndex = 0; // Default to first chapter

        // Prioritize first completely visible item for more accurate chapter detection.
        // If no item is completely visible (e.g., during a fast scroll or very short screen),
        // fall back to the first partially visible item.
        int effectiveVisiblePosition = (firstCompletelyVisibleItemPosition != RecyclerView.NO_POSITION) ?
                firstCompletelyVisibleItemPosition : firstVisibleItemPosition;


        if (effectiveVisiblePosition == RecyclerView.NO_POSITION || chapterPositions.isEmpty()) {
            Log.d("TOC_HIGHLIGHT_DEBUG", "No effective visible items or chapter positions. Defaulting to chapter 0.");
            if (lastHighlightedChapter != 0) { // Only update if necessary
                lastHighlightedChapter = 0;
                updateTocHighlightForChapter(0);
            }
            return;
        }

        // Determine the current chapter by finding the highest chapter start position
        // that is less than or equal to the effective visible position.
        for (int i = chapterPositions.size() - 1; i >= 0; i--) {
            if (effectiveVisiblePosition >= chapterPositions.get(i)) {
                currentChapterIndex = i;
                break;
            }
        }

        // Only update UI if the calculated chapter has actually changed from the last time
        if (currentChapterIndex != lastHighlightedChapter) {
            Log.d("TOC_HIGHLIGHT_DEBUG", "Calculated current chapter index changed: " + currentChapterIndex +
                    " (RV first visible: " + firstVisibleItemPosition + ", first completely visible: " + firstCompletelyVisibleItemPosition + "). Updating highlight.");
            lastHighlightedChapter = currentChapterIndex;
            updateTocHighlightForChapter(currentChapterIndex); // Apply the highlight
        } else {
            Log.d("TOC_HIGHLIGHT_DEBUG", "Calculated current chapter index is still: " + currentChapterIndex +
                    " (RV first visible: " + firstVisibleItemPosition + ", first completely visible: " + firstCompletelyVisibleItemPosition + "). No highlight update needed.");
        }
    }

    // This method applies the highlight style for a given chapter index
    // It iterates through TOC items and highlights only the specified one.
    private void updateTocHighlightForChapter(int chapterToHighlightIndex) {
        Log.d("TOC_HIGHLIGHT_DEBUG", "Applying highlight for chapter index: " + chapterToHighlightIndex);

        // Iterate through the dynamically added TOC items (skip the "Table of Contents" title at index 0)
        for (int i = 1; i < tocContainer.getChildCount(); i++) {
            View tocItem = tocContainer.getChildAt(i);
            TextView tocText = tocItem.findViewById(R.id.toc_item_text);

            Object tag = tocText.getTag();
            if (tag == null || !(tag instanceof Integer)) {
                Log.w("TOC_HIGHLIGHT_DEBUG", "TOC item tag is missing or not an Integer for item: " + tocText.getText());
                continue;
            }
            int itemChapterIndex = (int) tag;

            if (itemChapterIndex == chapterToHighlightIndex) {
                Log.d("TOC_HIGHLIGHT_DEBUG", " -- Highlighting: " + tocText.getText());
                tocText.setTextColor(context.getResources().getColor(R.color.bp_name_light_blue, null));
                tocText.setTypeface(null, android.graphics.Typeface.BOLD);
                tocItem.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
            } else {
                Log.d("TOC_HIGHLIGHT_DEBUG", " -- De-highlighting: " + tocText.getText());
                tocText.setTextColor(context.getResources().getColor(R.color.bp_bg, null));
                tocText.setTypeface(null, android.graphics.Typeface.NORMAL);
                tocItem.setBackground(context.getDrawable(android.R.drawable.list_selector_background));
            }
        }
    }

    public void forceShowToc() {
        Log.d("TOC_DEBUG_ACTIVITY", "forceShowToc called.");
        // When forcing show, we want it to behave like a direct interaction
        isFastScrollingActive = false; // Not fast scrolling from direct interaction
        isScrollFromTocClick = false; // Not from TOC click
        isManualScrolling = false; // Not from manual drag
        targetChapterIndexFromClick = -1;
        showTocPopup();
    }

    public void forceHideToc() {
        Log.d("TOC_DEBUG_ACTIVITY", "forceHideToc called.");
        // When forcing hide, ensure all flags are off for clean slate
        isFastScrollingActive = false;
        isScrollFromTocClick = false;
        isManualScrolling = false;
        targetChapterIndexFromClick = -1;
        cancelHideToc();
        hideTocPopup();
    }
}