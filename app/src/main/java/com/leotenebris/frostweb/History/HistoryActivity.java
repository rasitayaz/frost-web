package com.leotenebris.frostweb.History;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.leotenebris.frostweb.FrostWebApplication;
import com.leotenebris.frostweb.R;

import java.util.ArrayList;
import java.util.Calendar;

import static com.leotenebris.frostweb.FrostWebApplication.browserHistory;
import static com.leotenebris.frostweb.BrowserActivity.loadFromTargetUrl;
import static com.leotenebris.frostweb.BrowserActivity.targetUrl;

public class HistoryActivity extends AppCompatActivity {
    private HistoryAdapter adapter;
    private ArrayList<RecyclerViewItem> viewItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history);

        RecyclerView historyView = findViewById(R.id.historyView);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(getResources().getString(R.string.history));

        viewItems = createRecyclerViewList();
        adapter = new HistoryAdapter(viewItems, this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        historyView.setLayoutManager(layoutManager);
        historyView.setAdapter(adapter);
    }

    public void onItemClick(int position) {
        HistoryItem historyItem = (HistoryItem) viewItems.get(position);
        targetUrl = historyItem.getPageUrl();
        loadFromTargetUrl = true;
        browserHistory.remove(historyItem);
        browserHistory.add(0, historyItem);
        finish();
    }

    public void onClickRemove(int position) {
        boolean categoryCleared = viewItems.get(position - 1) instanceof HistoryCategory
                && (viewItems.size() == position + 1
                || (viewItems.size() > position + 1 && viewItems.get(position + 1) instanceof HistoryCategory));

        HistoryItem historyItem = (HistoryItem) viewItems.remove(position);
        browserHistory.remove(historyItem);

        if (categoryCleared)
            viewItems.remove(position - 1);

        adapter.notifyItemRemoved(position);
        if (categoryCleared)
            adapter.notifyItemRemoved(position - 1);

        FrostWebApplication.getInstance().saveHistory();
    }

    private ArrayList<RecyclerViewItem> createRecyclerViewList() {
        ArrayList<RecyclerViewItem> list = new ArrayList<>();

        if (browserHistory.size() == 0) return list;

        HistoryCategory category = new HistoryCategory(browserHistory.get(0).getTimeVisited());
        list.add(category);

        for (HistoryItem item : browserHistory) {
            Calendar itemDate = item.getTimeVisited();
            int itemYear = itemDate.get(Calendar.YEAR);
            int itemDay = itemDate.get(Calendar.DAY_OF_YEAR);

            Calendar categoryDate = category.getDate();
            int categoryYear = categoryDate.get(Calendar.YEAR);
            int categoryDay = categoryDate.get(Calendar.DAY_OF_YEAR);

            if (categoryYear == itemYear && categoryDay == itemDay) {
                list.add(item);
                continue;
            }

            category = new HistoryCategory(itemDate);
            list.add(category);
            list.add(item);
        }

        return list;
    }
}
