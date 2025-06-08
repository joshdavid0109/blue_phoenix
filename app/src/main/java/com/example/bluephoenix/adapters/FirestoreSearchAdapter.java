package com.example.bluephoenix.adapters;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bluephoenix.HomeActivity;
import com.example.bluephoenix.R;
import java.util.List;
import java.util.Locale;

public class FirestoreSearchAdapter extends RecyclerView.Adapter<FirestoreSearchAdapter.SearchViewHolder> {

    private static final String TAG = "FirestoreSearchAdapter";

    private List<HomeActivity.SearchItem> searchItems;
    private OnItemClickListener listener;
    private String currentQuery = "";

    public interface OnItemClickListener {
        void onItemClick(HomeActivity.SearchItem item);
    }

    public FirestoreSearchAdapter(List<HomeActivity.SearchItem> searchItems, OnItemClickListener listener) {
        this.searchItems = searchItems;
        this.listener = listener;
        Log.d(TAG, "Adapter initialized with " + searchItems.size() + " items.");
    }

    public void setSearchQuery(String query) {
        this.currentQuery = query.toLowerCase(Locale.getDefault());
        Log.d(TAG, "Search query updated in Adapter: " + query); // Added "in Adapter" for clarity
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        Log.d(TAG, "onCreateViewHolder: View created.");
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        if (searchItems == null || position >= searchItems.size()) {
            Log.e(TAG, "onBindViewHolder: searchItems is null or position is out of bounds: " + position);
            return;
        }

        HomeActivity.SearchItem item = searchItems.get(position);
        Log.d(TAG, "onBindViewHolder for position " + position + ". Item details: " +
                "Title: '" + item.getTitle() + "', MainTopic: '" + item.getMainTopic() +
                "', SubTopic: '" + item.getSubTopic() + "', OriginalChapterTitle: '" + item.getOriginalChapterTitle() + "'");


        // 1. Set Title with highlighting
        String originalTitle = item.getTitle();
        // Remove match indicators only for the display title, keep for match type logic
        String cleanTitle = originalTitle
                .replace(" (Content match)", "")
                .replace(" (description match)", "")
                .replace(" (keywords match)", "");

        SpannableString highlightedTitle = highlightSearchTerm(cleanTitle, currentQuery);
        holder.titleTextView.setText(highlightedTitle);
        Log.d(TAG, "Title TextView set to: '" + cleanTitle + "' (Highlighted)");


        // 2. Set Topic Path
        String topicPath = item.getMainTopic() + " > " + item.getSubTopic();
        holder.topicTextView.setText(topicPath);
        Log.d(TAG, "Topic TextView set to: '" + topicPath + "'");


        // 3. Set Match Type Indicator and its text color
        String matchType = "";
        int textColorResId;

        // Ensure match type background is dark and contrast well with @color/white text
        if (originalTitle.contains(" (Content match)")) {
            matchType = "Content Match";
            textColorResId = R.color.green;
        } else if (originalTitle.contains(" (description match)") || originalTitle.contains(" (keywords match)")) {
            matchType = "Field Match";
            textColorResId = R.color.blue;
        } else {
            matchType = "Title Match";
            textColorResId = R.color.bp_name_light_blue;
        }
        holder.matchTypeTextView.setText(matchType);
        holder.matchTypeTextView.setTextColor(holder.itemView.getContext().getResources().getColor(textColorResId));
        Log.d(TAG, "Match Type TextView set to: '" + matchType + "' with color from R.color." + holder.itemView.getContext().getResources().getResourceEntryName(textColorResId));


        // 4. Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
                Log.d(TAG, "Item clicked: " + item.getTitle());
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = searchItems != null ? searchItems.size() : 0;
        Log.d(TAG, "getItemCount called, returning: " + count);
        return count;
    }

    private SpannableString highlightSearchTerm(String text, String searchTerm) {
        SpannableString spannableString = new SpannableString(text);

        if (searchTerm == null || searchTerm.isEmpty()) {
            return spannableString;
        }

        String lowerText = text.toLowerCase(Locale.getDefault());
        String lowerSearchTerm = searchTerm.toLowerCase(Locale.getDefault());

        int startIndex = 0;
        while (startIndex < lowerText.length()) {
            int index = lowerText.indexOf(lowerSearchTerm, startIndex);
            if (index == -1) {
                break;
            }

            spannableString.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    index,
                    index + searchTerm.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            startIndex = index + searchTerm.length();
        }

        return spannableString;
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView topicTextView;
        TextView matchTypeTextView;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.search_item_title);
            topicTextView = itemView.findViewById(R.id.search_item_topic);
            matchTypeTextView = itemView.findViewById(R.id.search_item_match_type);
            Log.d(TAG, "SearchViewHolder initialized. TextViews found.");
        }
    }
}