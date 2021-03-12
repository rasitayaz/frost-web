package com.leotenebris.frostweb.Tabs;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.leotenebris.frostweb.FrostWebApplication;
import com.leotenebris.frostweb.R;
import com.leotenebris.frostweb.TabFragment;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.leotenebris.frostweb.BrowserActivity.isNewTab;
import static com.leotenebris.frostweb.BrowserActivity.isSavedTab;
import static com.leotenebris.frostweb.BrowserActivity.tabFragments;
import static com.leotenebris.frostweb.FrostWebApplication.tabs;
import static com.leotenebris.frostweb.BrowserActivity.tabsToRemove;
import static com.leotenebris.frostweb.BrowserActivity.targetUrl;

public class TabsActivity extends AppCompatActivity {
    private RecyclerView tabsView;
    private FloatingActionButton btAddTab;
    private TabsAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tabs);

        btAddTab = findViewById(R.id.btAddTab);
        tabsView = findViewById(R.id.tabsView);

        btAddTab.setOnClickListener(v -> {
            isNewTab = true;
            targetUrl = getString(R.string.new_tab_url);
            finish();
        });

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(getResources().getString(R.string.tabs));

        layoutManager = new LinearLayoutManager(this);
        adapter = new TabsAdapter(tabs, this);

        tabsView.setLayoutManager(layoutManager);
        tabsView.setAdapter(adapter);

        adapter.setOnItemClickListener(new TabsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                TabItem temp = tabs.get(position);
                tabs.remove(position);
                tabs.add(0, temp);

                for (int i = 0; i < tabFragments.size(); i++) {
                    TabFragment tempF = tabFragments.get(i);
                    if (tempF.getUniqueId().equals(temp.getUniqueId())) {
                        tabFragments.remove(i);
                        tabFragments.add(0, tempF);
                        finish();
                        return;
                    }
                }

                isSavedTab = true;
                tabFragments.add(0, new TabFragment());
                finish();
            }

            @Override
            public void onClickRemove(int position) {
                TabItem tabItem = tabs.remove(position);
                tabsToRemove.push(tabItem.getUniqueId());

                FrostWebApplication.getInstance().saveTabs();

                if (tabs.size() > 0) {
                    adapter.notifyItemRemoved(position);
                } else {
                    isNewTab = true;
                    targetUrl = getString(R.string.new_tab_url);
                    finish();
                }
            }
        });
    }
}
