package com.leotenebris.frostweb.WebView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Message;

import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.leotenebris.frostweb.BrowserActivity;
import com.leotenebris.frostweb.FrostWebApplication;
import com.leotenebris.frostweb.R;
import com.leotenebris.frostweb.Settings.SettingsFragment;
import com.leotenebris.frostweb.TabFragment;
import com.leotenebris.frostweb.Tabs.TabItem;

import static com.leotenebris.frostweb.FrostWebApplication.browserHistory;
import static com.leotenebris.frostweb.BrowserActivity.sharedPreferences;
import static com.leotenebris.frostweb.BrowserActivity.tabFragments;
import static com.leotenebris.frostweb.FrostWebApplication.tabs;

public class FrostWebChromeClient extends WebChromeClient {
    private BrowserActivity browserActivity;
    private Context context;
    private WebView dialogWebView;
    private AlertDialog dialog;
    private TabFragment tabFragment;
    private TabItem tab;

    public FrostWebChromeClient(BrowserActivity browserActivity) {
        tabFragment = tabFragments.get(0);
        tab = tabs.get(0);
        this.browserActivity = browserActivity;
        context = FrostWebApplication.getInstance();
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
        LayoutInflater inflater = browserActivity.getLayoutInflater();
        View jsPromptView = inflater.inflate(R.layout.js_prompt, browserActivity.findViewById(R.id.jsPromptLayout));
        jsPromptView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText jsPromptInput = jsPromptView.findViewById(R.id.jsPromptInput);
        jsPromptInput.setSingleLine();
        jsPromptInput.setText(defaultValue);

        new AlertDialog.Builder(view.getContext(), R.style.AlertDialogTheme)
                .setTitle("").setMessage(message)
                .setView(jsPromptView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> result.confirm(jsPromptInput.getText().toString()))
                .setNegativeButton(R.string.cancel, (dialog, which) -> result.cancel())
                .setOnCancelListener(dialog -> result.cancel())
                .create().show();

        return true;
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        new AlertDialog.Builder(view.getContext(), R.style.AlertDialogTheme)
                .setTitle("")
                .setMessage(message)
                .setPositiveButton(R.string.confirm, null)
                .setCancelable(false)
                .create().show();
        // Because there is no binding event, you need to force confirm, otherwise the page will be black and the content will not be displayed.
        result.confirm();

        return true;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        new AlertDialog.Builder(view.getContext(), R.style.AlertDialogTheme)
                .setTitle("")
                .setMessage(message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> result.confirm())
                .setNegativeButton(R.string.cancel, (dialog, which) -> result.cancel())
                .setOnCancelListener(dialog -> result.cancel())
                .create().show();

        return true;
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        if (isDialog) {
            LayoutInflater inflater = browserActivity.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.web_dialog, browserActivity.findViewById(R.id.webDialogLayout));
            dialogView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
            ImageButton btClose = dialogView.findViewById(R.id.btDialogClose);
            dialogWebView = dialogView.findViewById(R.id.dialogWebView);
            dialogWebView.setVerticalScrollBarEnabled(false);
            dialogWebView.setHorizontalScrollBarEnabled(false);
            dialogWebView.setWebViewClient(new DialogWebViewClient());
            dialogWebView.setWebChromeClient(new DialogWebChromeClient());
            WebSettings webSettings = dialogWebView.getSettings();
            webSettings.setDomStorageEnabled(true);
            webSettings.setJavaScriptEnabled(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                boolean forceDarkEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_FORCE_DARK, false);

                if (forceDarkEnabled)
                    webSettings.setForceDark(WebSettings.FORCE_DARK_ON);
                else
                    webSettings.setForceDark(WebSettings.FORCE_DARK_AUTO);
            }

            String title = view.getTitle();
            if (title == null) {
                Uri uri = Uri.parse(view.getUrl());
                title = uri.getHost();
            }

            dialogTitle.setText(title);

            btClose.setOnClickListener(v -> {
                dialogWebView.destroy();
                dialog.dismiss();
            });

            dialog = new AlertDialog.Builder(browserActivity).create();

            dialog.setTitle("");
            dialog.setView(dialogView);
            dialog.show();

            Window dialogWindow = dialog.getWindow();
            assert dialogWindow != null;
            dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(dialogWebView, true);

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(dialogWebView);
            resultMsg.sendToTarget();
            return true;
        }

        browserActivity.newTab(null, resultMsg, false);

        return true;
    }

    @Override
    public void onCloseWindow(WebView window) {
        try {
            dialogWebView.destroy();
            dialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onCloseWindow(window);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);

        browserHistory.get(0).setPageTitle(view.getTitle());

        tabFragment.setPageTitle(title);
        tabFragment.setPageTitleInput();
        if (tabFragment.isCurrent())
            tab.setPageTitle(title);
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        super.onReceivedIcon(view, icon);
        if (tabFragment.isCurrent())
            tab.setFavicon(icon);
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        browserActivity.animateProgressBar(newProgress);

        if (newProgress == 100) {
            tabFragment.setLoaded();
        }
    }

    class DialogWebChromeClient extends WebChromeClient {
        @Override
        public void onCloseWindow(WebView window) {
            try {
                dialogWebView.destroy();
                dialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }

            super.onCloseWindow(window);
        }
    }

    class DialogWebViewClient extends WebViewClient {
        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            super.onPageCommitVisible(view, url);

            ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
            assert progressBar != null;
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Window dialogWindow = dialog.getWindow();
            assert dialogWindow != null;
            dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

            super.onPageFinished(view, url);
        }
    }

    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;

    @Override
    public Bitmap getDefaultVideoPoster() {
        if (mCustomView == null) {
            return null;
        }
        return BitmapFactory.decodeResource(context.getResources(), 2130837573);
    }

    @Override
    public void onHideCustomView() {
        ((FrameLayout) browserActivity.getWindow().getDecorView()).removeView(this.mCustomView);
        this.mCustomView = null;
        browserActivity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
        browserActivity.setRequestedOrientation(this.mOriginalOrientation);
        this.mCustomViewCallback.onCustomViewHidden();
        this.mCustomViewCallback = null;
        browserActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
        if (this.mCustomView != null) {
            onHideCustomView();
            return;
        }
        this.mCustomView = paramView;
        this.mOriginalSystemUiVisibility = browserActivity.getWindow().getDecorView().getSystemUiVisibility();
        this.mOriginalOrientation = browserActivity.getRequestedOrientation();
        this.mCustomViewCallback = paramCustomViewCallback;
        ((FrameLayout) browserActivity.getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
        browserActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        return browserActivity.showFileChooser(filePathCallback, fileChooserParams);
    }
}
