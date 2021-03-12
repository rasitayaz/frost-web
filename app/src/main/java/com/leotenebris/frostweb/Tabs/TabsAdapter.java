package com.leotenebris.frostweb.Tabs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.leotenebris.frostweb.R;

import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public class TabsAdapter extends RecyclerView.Adapter<TabsAdapter.TabViewHolder> {
    private Context context;
    private ArrayList<TabItem> tabs;
    private OnItemClickListener listener;

    public TabsAdapter(ArrayList<TabItem> tabs, TabsActivity tabsActivity) {
        this.tabs = tabs;
        context = tabsActivity;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onClickRemove(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class TabViewHolder extends RecyclerView.ViewHolder {
        public RecyclerView tabsView;
        public ImageView tabFavicon, btTabClose;
        public TextView tabTitle;
        public TextView tabUrl;

        public TabViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);

            tabsView = itemView.findViewById(R.id.tabsView);
            tabFavicon = itemView.findViewById(R.id.tabFavicon);
            tabTitle = itemView.findViewById(R.id.tabTitle);
            tabUrl = itemView.findViewById(R.id.tabUrl);
            btTabClose = itemView.findViewById(R.id.btTabClose);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });

            btTabClose.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onClickRemove(position);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tab_item, parent, false);
        return new TabViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder tabsViewHolder, int position) {
        TabItem currentPage = tabs.get(position);

        tabsViewHolder.tabFavicon.setImageBitmap(currentPage.getFavicon());
        if (currentPage.isDefaultFavicon())
            tabsViewHolder.tabFavicon.setColorFilter(ContextCompat.getColor(context, R.color.colorFg));
        else
            tabsViewHolder.tabFavicon.clearColorFilter();

        String tabUrl = currentPage.getPageUrl();
        String tabTitle = currentPage.getPageTitle();

        if (tabTitle == null || tabTitle.equals(""))
            tabTitle = tabUrl;

        switch (tabUrl) {
            case "about:blank":
            case "frostweb://newtab":
                tabsViewHolder.tabTitle.setText(context.getString(R.string.empty_page));
                break;
            default:
                tabsViewHolder.tabTitle.setText(tabTitle);
                break;
        }

        tabsViewHolder.tabUrl.setText(tabUrl);
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }
}
