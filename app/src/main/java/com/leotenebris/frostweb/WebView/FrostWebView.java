package com.leotenebris.frostweb.WebView;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

import com.leotenebris.frostweb.R;

import androidx.annotation.Nullable;

import static com.leotenebris.frostweb.BrowserActivity.startup;
import static com.leotenebris.frostweb.BrowserActivity.tabFragments;

@SuppressWarnings("unused")
public class FrostWebView extends WebView {
    private OnScrollChangedCallback mOnScrollChangedCallback;

    public FrostWebView(Context context) {
        super(context);
    }

    public FrostWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FrostWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedCallback != null) mOnScrollChangedCallback.onScroll(l, t, oldl, oldt);
    }

    public OnScrollChangedCallback getOnScrollChangedCallback() {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(final OnScrollChangedCallback onScrollChangedCallback) {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        tabFragments.get(0).setUpdateCalled(false);
        return super.onTouchEvent(event);
    }

    @Override
    public void clearHistory() {
        super.clearHistory();
    }

    /**
     * Implement in the activity/fragment/view that you want to listen to the webview
     */
    public interface OnScrollChangedCallback {
        void onScroll(int x, int y, int oldX, int oldY);
    }

    public int getVerticalScrollRange() {
        return computeVerticalScrollRange();
    }

    @Override
    public void loadUrl(String url) {
        if (url.equals(getResources().getString(R.string.new_tab_url))) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            Context context = getContext();
            if (startup)
                context.startActivity(intent,
                        ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle());
            else
                context.startActivity(intent);
            return;
        }

        super.loadUrl(url);
    }

    @Override
    public void loadData(String data, @Nullable String mimeType, @Nullable String encoding) {
        super.loadData(data, mimeType, encoding);
    }

    @Override
    public void loadDataWithBaseURL(@Nullable String baseUrl, String data, @Nullable String mimeType, @Nullable String encoding, @Nullable String historyUrl) {
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    public boolean canGoBack() {
        return super.canGoBack();
    }

    @Override
    public void goBack() {
        super.goBack();
    }

    @Override
    public boolean canGoForward() {
        return super.canGoForward();
    }

    @Override
    public void goForward() {
        super.goForward();
    }

    @Override
    public boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY,
                                int scrollRangeX, int scrollRangeY, int maxOverScrollX,
                                int maxOverScrollY, boolean isTouchEvent) {

        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
                scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

    }
}