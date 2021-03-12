package com.leotenebris.frostweb.History;

import java.util.Calendar;

public class HistoryCategory extends RecyclerViewItem {
    private Calendar date;

    public HistoryCategory(Calendar date) {
        setDateWithoutTime(date);
    }

    public Calendar getDate() {
        return date;
    }

    private void setDateWithoutTime(Calendar date) {
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        this.date = date;
    }
}
