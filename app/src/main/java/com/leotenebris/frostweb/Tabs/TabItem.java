package com.leotenebris.frostweb.Tabs;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.webkit.WebBackForwardList;

import com.leotenebris.frostweb.FrostWebApplication;
import com.leotenebris.frostweb.R;

import java.util.ArrayList;
import java.util.Stack;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class TabItem {
    private Bitmap favicon;
    private String uniqueId, pageTitle, pageUrl;
    private boolean isBackup, isDefaultFavicon;

    private PageItem currentItem;
    private Stack<PageItem> backStack;
    private Stack<PageItem> fwdStack;

    private ArrayList<PageItem> backupHistory;
    private int backupIndex;

    public TabItem() {
        setUniqueId();
        Resources resources = FrostWebApplication.getInstance().getResources();
        pageTitle = resources.getString(R.string.empty_page);
        pageUrl = resources.getString(R.string.new_tab_url);
        setDefaultFavicon();
    }

    private void setUniqueId() {
        uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Bitmap getFavicon() {
        return favicon;
    }

    public void setFavicon(Bitmap favicon) {
        if (favicon == null) {
            setDefaultFavicon();
        } else {
            this.favicon = favicon;
            isDefaultFavicon = false;
        }
    }

    public void setDefaultFavicon() {
        favicon = BitmapFactory.decodeResource(FrostWebApplication.getInstance().getResources(), R.drawable.icon_app);
        isDefaultFavicon = true;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public boolean isBackup() {
        return isBackup;
    }

    public void setBackup(boolean backup) {
        isBackup = backup;
    }

    public boolean isDefaultFavicon() {
        return isDefaultFavicon;
    }

    public PageItem getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(PageItem currentItem) {
        this.currentItem = currentItem;
    }

    public Stack<PageItem> getBackStack() {
        return backStack;
    }

    public Stack<PageItem> getFwdStack() {
        return fwdStack;
    }

    public void setBackupHistory(WebBackForwardList list) {
        backupHistory = new ArrayList<>();
        backupIndex = list.getCurrentIndex();
        for (int i = 0; i < list.getSize(); i++)
            backupHistory.add(new PageItem(list.getItemAtIndex(i).getUrl()));
    }

    public void clearBackupHistory() {
        backupHistory = null;
        backupIndex = -1;
    }

    public void setHistory(WebBackForwardList list) {
        backStack = new Stack<>();
        fwdStack = new Stack<>();

        int index = list.getCurrentIndex();

        for (int i = 0; i < index; i++)
            backStack.push(new PageItem(list.getItemAtIndex(i).getUrl()));

        currentItem = new PageItem(list.getItemAtIndex(index).getUrl());

        for (int i = list.getSize() - 1; i > index; i--)
            fwdStack.push(new PageItem(list.getItemAtIndex(i).getUrl()));
    }

    public void pushToFwdStack(WebBackForwardList list) {
        for (int i = list.getSize() - 1; i >= 0; i--)
            fwdStack.push(new PageItem(list.getItemAtIndex(i).getUrl()));
    }

    public void mergeWithBackupHistory() {
        if (backupHistory == null) return;

        currentItem = backupHistory.get(backupIndex);
        for (int i = 0; i < backupIndex; i++)
            backStack.push(backupHistory.get(i));
        for (int i = backupHistory.size() - 1; i > backupIndex; i--)
            fwdStack.push(backupHistory.get(i));

        clearBackupHistory();
    }
}

