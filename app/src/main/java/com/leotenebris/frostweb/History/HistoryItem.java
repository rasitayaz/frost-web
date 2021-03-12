package com.leotenebris.frostweb.History;

import java.util.Calendar;

public class HistoryItem extends RecyclerViewItem {
    private String pageTitle, pageUrl;
    private Calendar timeVisited;
    private int visitAmount;

    public HistoryItem(String pageTitle, String pageUrl) {
        if (pageTitle == null || pageTitle.equals(""))
            pageTitle = pageUrl;

        this.pageTitle = pageTitle;
        this.pageUrl = pageUrl;

        visitAmount++;
        timeVisited = Calendar.getInstance();
    }

    public void updateDate() {
        visitAmount++;
        timeVisited = Calendar.getInstance();
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

    public Calendar getTimeVisited() {
        return timeVisited;
    }
}
