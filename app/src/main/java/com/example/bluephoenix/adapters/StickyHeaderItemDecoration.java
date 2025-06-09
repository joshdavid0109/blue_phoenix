//package com.example.bluephoenix.adapters;
//
//import android.graphics.Canvas;
//import android.graphics.Rect;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.bluephoenix.R;
//import com.example.bluephoenix.adapters.ChapterContentAdapter;
//
//public class StickyHeaderItemDecoration extends RecyclerView.ItemDecoration {
//
//    private final ChapterContentAdapter adapter;
//    private final LayoutInflater inflater;
//    private View stickyHeaderView;
//    private TextView stickyHeaderTextView;
//    private int stickyHeaderHeight;
//
//    public StickyHeaderItemDecoration(ChapterContentAdapter adapter, LayoutInflater inflater) {
//        this.adapter = adapter;
//        this.inflater = inflater;
//    }
//
//    @Override
//    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
//        super.onDrawOver(c, parent, state);
//
//        if (adapter.getItemCount() == 0) {
//            return;
//        }
//
//        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
//        if (layoutManager == null) {
//            Log.e("StickyHeader", "LayoutManager is null. Cannot draw sticky headers.");
//            return;
//        }
//
//        if (!(layoutManager instanceof LinearLayoutManager)) {
//            Log.w("StickyHeader", "RecyclerView does not use LinearLayoutManager. Sticky headers might not work correctly.");
//            return; // Return here as sticky header logic heavily relies on LinearLayoutManager
//        }
//
//        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
//
//        int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
//        if (firstVisibleItemPosition == RecyclerView.NO_POSITION) {
//            return;
//        }
//
//        int headerPosition = getHeaderPosition(firstVisibleItemPosition);
//
//        if (headerPosition == RecyclerView.NO_POSITION || !adapter.isChapterTitle(headerPosition)) {
//            return;
//        }
//
//        // --- IMPORTANT CHANGE HERE ---
//        ensureStickyHeaderView(parent); // Pass the parent RecyclerView
//        // --- END IMPORTANT CHANGE ---
//
//        stickyHeaderTextView.setText(adapter.getChapterTitle(headerPosition));
//
//        // Measure and layout the sticky header view
//        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
//        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.AT_MOST);
//        stickyHeaderView.measure(widthSpec, heightSpec);
//        stickyHeaderHeight = stickyHeaderView.getMeasuredHeight();
//        stickyHeaderView.layout(parent.getPaddingLeft(), parent.getPaddingTop(),
//                parent.getWidth() - parent.getPaddingRight(), parent.getPaddingTop() + stickyHeaderHeight);
//
//
//        float translateY = 0;
//        int nextHeaderPosition = getNextHeaderPosition(headerPosition);
//
//        if (nextHeaderPosition != RecyclerView.NO_POSITION) {
//            View nextHeaderView = linearLayoutManager.findViewByPosition(nextHeaderPosition); // Use findViewByPosition
//            if (nextHeaderView != null) {
//                if (nextHeaderView.getTop() <= stickyHeaderHeight + parent.getPaddingTop()) {
//                    translateY = nextHeaderView.getTop() - stickyHeaderHeight - parent.getPaddingTop();
//                }
//            }
//        }
//
//        c.save();
//        c.translate(0, translateY + parent.getPaddingTop());
//        stickyHeaderView.draw(c);
//        c.restore();
//    }
//
//    // --- MODIFIED METHOD SIGNATURE ---
//    private void ensureStickyHeaderView(@NonNull RecyclerView parent) {
//        if (stickyHeaderView == null) {
//            // Inflate with the parent to ensure layout parameters are set correctly,
//            // but do NOT attach to root as we will draw it ourselves.
//            stickyHeaderView = inflater.inflate(R.layout.item_sticky_header, parent, false);
//            stickyHeaderTextView = stickyHeaderView.findViewById(R.id.sticky_header_text);
//        }
//    }
//    // --- END MODIFIED METHOD ---
//
//    private int getHeaderPosition(int startPosition) {
//        for (int i = startPosition; i >= 0; i--) {
//            if (adapter.isChapterTitle(i)) {
//                return i;
//            }
//        }
//        return RecyclerView.NO_POSITION;
//    }
//
//    private int getNextHeaderPosition(int currentHeaderPosition) {
//        for (int i = currentHeaderPosition + 1; i < adapter.getItemCount(); i++) {
//            if (adapter.isChapterTitle(i)) {
//                return i;
//            }
//        }
//        return RecyclerView.NO_POSITION;
//    }
//}