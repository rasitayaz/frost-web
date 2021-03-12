package com.leotenebris.frostweb.WebView;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.leotenebris.frostweb.BrowserActivity;
import com.leotenebris.frostweb.FrostWebApplication;
import com.leotenebris.frostweb.History.HistoryItem;
import com.leotenebris.frostweb.TabFragment;
import com.leotenebris.frostweb.Tabs.PageItem;
import com.leotenebris.frostweb.Tabs.TabItem;

import java.net.URISyntaxException;
import java.util.List;

import static com.leotenebris.frostweb.BrowserActivity.forceInternalMode;
import static com.leotenebris.frostweb.BrowserActivity.isInternalIntent;
import static com.leotenebris.frostweb.BrowserActivity.tabFragments;
import static com.leotenebris.frostweb.FrostWebApplication.browserHistory;
import static com.leotenebris.frostweb.FrostWebApplication.tabs;

public class FrostWebViewClient extends WebViewClient {
    private BrowserActivity browserActivity;
    private Context context;
    private TabFragment tabFragment;
    private TabItem tab;

    public FrostWebViewClient(BrowserActivity browserActivity) {
        tabFragment = tabFragments.get(0);
        tab = tabs.get(0);
        this.browserActivity = browserActivity;
        context = FrostWebApplication.getInstance();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String url = uri.toString();

        switch (browserActivity.handleUri(uri, view, tabFragment)) {
            case 0: return false;
            case 1: return true;
        }

        tabFragment.setLoading();
        tabFragment.setLinkInput(url);

        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

        tabFragment.setLinkInput(url);
        tabFragment.setUpdateCalled(false);

        tab.setPageUrl(url);
        tab.setDefaultFavicon();
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (view.getProgress() == 100) {
            tabFragment.setLoaded();
            tabFragment.setBlockBackFwd(false);
        }

        tabFragment.setPageTitle(view.getTitle());
        tabFragment.setPageTitleInput();

        tab.setPageUrl(url);
        tab.setPageTitle(view.getTitle());
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        forceInternalMode = false;

        if (!tabFragment.isUpdateCalled()) {
            WebBackForwardList list = view.copyBackForwardList();
            WebHistoryItem item = list.getCurrentItem();
            assert item != null;
            boolean foundInHistory = false;
            if (browserHistory.size() == 0)
                browserHistory.add(0, new HistoryItem(item.getTitle(), url));
            else {
                Uri uri = Uri.parse(url);
                String ssp = uri.getSchemeSpecificPart();

                for (int i = 0; i < browserHistory.size(); i++) {
                    HistoryItem historyItem = browserHistory.get(i);
                    Uri savedUri = Uri.parse(historyItem.getPageUrl());
                    String savedSsp = savedUri.getSchemeSpecificPart();
                    if (savedSsp.equals(ssp)) {
                        browserHistory.remove(historyItem);
                        historyItem.updateDate();
                        browserHistory.add(0, historyItem);
                        foundInHistory = true;
                        break;
                    }
                }

                if (!foundInHistory)
                    browserHistory.add(0, new HistoryItem(item.getTitle(), url));
            }

            if (tab.isBackup()) {
                if (tabFragment.isLoadFromBackStack()) {
                    tab.setCurrentItem(new PageItem(item.getUrl()));
                    view.clearHistory();
                    tab.clearBackupHistory();
                } else if (tabFragment.isLoadFromFwdStack()) {
                    tab.setCurrentItem(new PageItem(item.getUrl()));
                    tab.setBackupHistory(list);
                } else if (!tabFragment.isRestoreItem()) {
                    tab.setBackupHistory(list);
                    if (!isReload && !tabFragment.isBackFwdPressed())
                        tab.getFwdStack().clear();
                }
                tabFragment.setBackFwdPressed(false);
                tabFragment.setLoadFromBackStack(false);
                tabFragment.setLoadFromFwdStack(false);
                tabFragment.setRestoreItem(false);
            } else {
                tab.setHistory(list);
            }
        }

        tabFragment.arrangeBackForwardButtons();

        tabFragment.setUpdateCalled(true);

        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        super.onLoadResource(view, url);
    }
}
