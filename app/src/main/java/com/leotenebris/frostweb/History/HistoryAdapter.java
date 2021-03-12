package com.leotenebris.frostweb.History;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.leotenebris.frostweb.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

@SuppressWarnings("WeakerAccess")
public class HistoryAdapter extends RecyclerView.Adapter {
    private static final int HISTORY_CATEGORY = 0;
    private static final int HISTORY_ITEM = 1;

    private HistoryActivity historyActivity;
    private ArrayList<RecyclerViewItem> viewItems;

    public HistoryAdapter(ArrayList<RecyclerViewItem> viewItems, HistoryActivity historyActivity) {
        this.viewItems = viewItems;
        this.historyActivity = historyActivity;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v;
        switch (viewType) {
            case HISTORY_CATEGORY:
                v = inflater.inflate(R.layout.history_category, parent, false);
                return new CategoryViewHolder(v);
            case HISTORY_ITEM:
                v = inflater.inflate(R.layout.history_item, parent, false);
                return new HistoryViewHolder(v, historyActivity);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof CategoryViewHolder) {
            CategoryViewHolder categoryViewHolder = (CategoryViewHolder) viewHolder;
            HistoryCategory currentCategory = (HistoryCategory) viewItems.get(position);

            Calendar currentDate = Calendar.getInstance();
            int currentYear = currentDate.get(Calendar.YEAR);
            int currentDay = currentDate.get(Calendar.DAY_OF_YEAR);

            Calendar categoryDate = currentCategory.getDate();
            int categoryYear = categoryDate.get(Calendar.YEAR);
            int categoryDay = categoryDate.get(Calendar.DAY_OF_YEAR);

            if (currentYear == categoryYear && currentDay == categoryDay)
                categoryViewHolder.categoryHeader.setText(historyActivity.getString(R.string.today));
            else if (currentYear == categoryYear && currentDay - 1 == categoryDay)
                categoryViewHolder.categoryHeader.setText(historyActivity.getString(R.string.yesterday));
            else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                String strDate = sdf.format(categoryDate.getTime());
                categoryViewHolder.categoryHeader.setText(strDate);
            }
        } else if (viewHolder instanceof HistoryViewHolder) {
            HistoryViewHolder historyViewHolder = (HistoryViewHolder) viewHolder;
            HistoryItem currentItem = (HistoryItem) viewItems.get(position);

            String pageUrl = currentItem.getPageUrl();
            String pageTitle = currentItem.getPageTitle();

            if (pageTitle == null || pageTitle.equals(""))
                pageTitle = pageUrl;

            historyViewHolder.pageTitle.setText(pageTitle);
            historyViewHolder.pageUrl.setText(pageUrl);
        }
    }

    @Override
    public int getItemViewType(int position) {
        RecyclerViewItem recyclerViewItem = viewItems.get(position);
        if (recyclerViewItem instanceof HistoryCategory)
            return HISTORY_CATEGORY;
        else if (recyclerViewItem instanceof HistoryItem)
            return HISTORY_ITEM;
        else
            return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return viewItems.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        public TextView categoryHeader;
        public RecyclerView categoryItems;

        public CategoryViewHolder(@NonNull View categoryView) {
            super(categoryView);

            categoryHeader = categoryView.findViewById(R.id.categoryHeader);
            categoryItems = categoryView.findViewById(R.id.historyView);
        }
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        public RecyclerView historyItems;
        public ImageView btRemoveItem;
        public TextView pageTitle;
        public TextView pageUrl;

        public HistoryViewHolder(@NonNull View itemView, HistoryActivity historyActivity) {
            super(itemView);

            historyItems = itemView.findViewById(R.id.historyItems);
            pageTitle = itemView.findViewById(R.id.pageTitle);
            pageUrl = itemView.findViewById(R.id.pageUrl);
            btRemoveItem = itemView.findViewById(R.id.btRemoveItem);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    historyActivity.onItemClick(position);
                }
            });

            btRemoveItem.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    historyActivity.onClickRemove(position);
                }
            });
        }
    }
}
