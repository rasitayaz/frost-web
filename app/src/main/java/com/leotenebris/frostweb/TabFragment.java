package com.leotenebris.frostweb;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.leotenebris.frostweb.Settings.SettingsFragment;
import com.leotenebris.frostweb.Tabs.PageItem;
import com.leotenebris.frostweb.Tabs.TabItem;
import com.leotenebris.frostweb.WebView.FrostWebChromeClient;
import com.leotenebris.frostweb.WebView.FrostWebView;
import com.leotenebris.frostweb.WebView.FrostWebViewClient;

import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.DOWNLOAD_SERVICE;
import static com.leotenebris.frostweb.BrowserActivity.fileName;
import static com.leotenebris.frostweb.BrowserActivity.isLoaded;
import static com.leotenebris.frostweb.BrowserActivity.isNewTab;
import static com.leotenebris.frostweb.BrowserActivity.sharedPreferences;
import static com.leotenebris.frostweb.BrowserActivity.tabsToRemove;
import static com.leotenebris.frostweb.BrowserActivity.targetUrl;
import static com.leotenebris.frostweb.FrostWebApplication.tabs;

@SuppressWarnings("WeakerAccess")
public class TabFragment extends Fragment {
    private static final int GROUP_URL = 1;
    private static final int GROUP_IMAGE = 2;

    private static final int COPY_LINK = 1;
    private static final int SAVE_IMAGE = 2;
    private static final int OPEN_IN_NEW_TAB = 3;
    private static final int COPY_TITLE = 4;
    private static final int OPEN_IMAGE_IN_NEW_TAB = 5;

    public static final int STORAGE_PERMISSION_CODE = 1;
    private String[] downloadRequestArgs;

    private BrowserActivity browserActivity;

    private TabItem tab;
    private FrostWebView webView;
    private Message resultMsg;

    private boolean isCurrent, isTargetWindow, isExternalWindow, loadFromBackStack, loadFromFwdStack, restoreItem,
            backFwdPressed, updateCalled, blockBackFwd;
    private String pageTitle, uniqueId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_webview, container, false);

        tab = tabs.get(0);
        uniqueId = tab.getUniqueId();

        webView = v.findViewById(R.id.webView);
        browserActivity = (BrowserActivity) getActivity();
        isCurrent = true;

        /* ----------------- Web Settings ----------------- */

        final WebSettings webSettings = webView.getSettings();

        webSettings.setDatabaseEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDefaultTextEncodingName(StandardCharsets.UTF_8.name());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean forceDarkEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_FORCE_DARK, false);

            if (forceDarkEnabled) {
                webSettings.setForceDark(WebSettings.FORCE_DARK_ON);
                webView.setBackgroundColor(ContextCompat.getColor(browserActivity, R.color.darkerGray));
            } else {
                webSettings.setForceDark(WebSettings.FORCE_DARK_AUTO);
                webView.setBackgroundColor(ContextCompat.getColor(browserActivity, R.color.white));
            }
        }

        boolean javascriptEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_JAVASCRIPT, true);

        webSettings.setJavaScriptEnabled(javascriptEnabled);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(javascriptEnabled);

        boolean zoomEnabled = sharedPreferences.getBoolean(SettingsFragment.KEY_ZOOM, true);

        webSettings.setBuiltInZoomControls(zoomEnabled);
        webSettings.setDisplayZoomControls(false);

        /* ----------------- Other Settings ----------------- */

        Configuration config = new Configuration();
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // TODO: investigate cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        String ua = webSettings.getUserAgentString()
                .replace("; wv", "");

        int uaIndex = ua.indexOf("Version");

        ua = ua.substring(0, uaIndex)
                .concat(getString(R.string.app_name) + '/' + getString(R.string.app_version) + ' ')
                .concat(ua.substring(uaIndex));

        webSettings.setUserAgentString(ua);

        /* ----------------- Long Press Listener ----------------- */

        registerForContextMenu(webView);

        webView.setWebViewClient(new FrostWebViewClient(browserActivity));
        webView.setWebChromeClient(new FrostWebChromeClient(browserActivity));

        /* ----------------- Downloader ----------------- */

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(browserActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    browserActivity.downloadDialog(url, userAgent, contentDisposition, mimeType);
                } else {
                    downloadRequestArgs = new String[4];
                    downloadRequestArgs[0] = url;
                    downloadRequestArgs[1] = userAgent;
                    downloadRequestArgs[2] = contentDisposition;
                    downloadRequestArgs[3] = mimeType;
                    requestDownloadPermission();
                }
            } else {
                browserActivity.downloadDialog(url, userAgent, contentDisposition, mimeType);
            }
        });

        Intent intent = browserActivity.getIntent();

        browserActivity.toggleBtBack(false);
        browserActivity.toggleBtForward(false);

        if (resultMsg != null) {
            WebView messageWebView = new WebView(browserActivity);
            messageWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    String url = uri.toString();

                    view.destroy();

                    switch (browserActivity.handleUri(uri, webView, TabFragment.this)) {
                        case 0:
                            return false;
                        case 1:
                            tabs.remove(tab);
                            tabsToRemove.push(uniqueId);
                            browserActivity.handleTabs();
                            return true;
                    }

                    loadUrl(url);
                    return true;
                }
            });
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(messageWebView);
            resultMsg.sendToTarget();
        } else if (isNewTab) {
            loadUrl(targetUrl);
        } else {
            PageItem currentItem = tab.getCurrentItem();
            if (currentItem == null) {
                tab.setBackup(false);
                if (intent == null || intent.getData() == null)
                    loadUrl(getString(R.string.new_tab_url));
            } else {
                restoreItem = true;
                loadUrl(currentItem.getUrl());
            }
        }

        resultMsg = null;
        isNewTab = false;

        browserActivity.linkInputClearFocus();

        return v;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(browserActivity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE);
    }

    private void requestDownloadPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(browserActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(browserActivity, R.style.AlertDialogTheme)
                    .setTitle(getString(R.string.permission_required))
                    .setMessage(getString(R.string.storage_permission_explanation))
                    .setPositiveButton(R.string.confirm, (dialog, which) -> requestStoragePermission())
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            requestStoragePermission();
        }
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu contextMenu, @NonNull View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);

        final WebView.HitTestResult webViewHitTestResult = webView.getHitTestResult();
        final String url = webViewHitTestResult.getExtra();

        Message msg = new Handler().obtainMessage();
        webView.requestFocusNodeHref(msg);
        Bundle data = msg.getData();
        String linkTitle = data.getString("title");
        String linkUrl = data.getString("url");

        if (linkTitle != null)
            linkTitle = linkTitle.trim().equals("") ? null : linkTitle.trim();

        if (linkTitle != null)
            contextMenu.setHeaderTitle(linkTitle);
        else
            contextMenu.setHeaderTitle(linkUrl);

        switch (webViewHitTestResult.getType()) {
            case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                addUrlOptions(contextMenu, url, linkTitle);

                break;

            case WebView.HitTestResult.IMAGE_TYPE:
            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:

                if (linkUrl != null)
                    addUrlOptions(contextMenu, linkUrl, linkTitle);

                if (URLUtil.isValidUrl(url)) {
                    fileName = URLUtil.guessFileName(url, null, null);
                    if (fileName.endsWith(".bin")) {
                        int index = fileName.lastIndexOf(".bin");
                        fileName = fileName.substring(0, index) + ".jpeg";
                    }

                    if (linkTitle == null && linkUrl == null)
                        contextMenu.setHeaderTitle(fileName);

                    addImageOptions(contextMenu, url);
                }

                break;
        }
    }

    private void addUrlOptions(ContextMenu contextMenu, String url, String title) {
        contextMenu.add(GROUP_URL, OPEN_IN_NEW_TAB, 0, getString(R.string.open_in_new_tab))
                .setOnMenuItemClickListener(menuItem -> {
                    browserActivity.newTab(url, null, false);
                    return true;
                });

        contextMenu.add(GROUP_URL, COPY_LINK, 0, getString(R.string.copy_link_address))
                .setOnMenuItemClickListener(menuItem -> {
                    ClipboardManager clipboard = (ClipboardManager) browserActivity.getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("URL", url);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        return true;
                    }
                    return false;
                });

        if (title != null) {
            contextMenu.add(GROUP_URL, COPY_TITLE, 0, getString(R.string.copy_link_text))
                    .setOnMenuItemClickListener(menuItem -> {
                        ClipboardManager clipboard = (ClipboardManager) browserActivity.getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Text", title);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            return true;
                        }
                        return false;
                    });
        }
    }

    private void addImageOptions(ContextMenu contextMenu, String url) {
        contextMenu.add(GROUP_IMAGE, OPEN_IMAGE_IN_NEW_TAB, 0, getString(R.string.open_image_in_new_tab))
                .setOnMenuItemClickListener(menuItem -> {
                    browserActivity.newTab(url, null, false);
                    return true;
                });

        contextMenu.add(GROUP_IMAGE, SAVE_IMAGE, 0, getString(R.string.download_image))
                .setOnMenuItemClickListener(menuItem -> {

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    DownloadManager downloadManager = (DownloadManager) browserActivity.getSystemService(DOWNLOAD_SERVICE);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    if (downloadManager != null) {
                        downloadManager.enqueue(request);
                        return true;
                    }

                    return false;
                });
    }

    public void loadUrl(String url) {
        if (url == null || url.equals("")) {
            return;
        }

        url = url.trim();

        if (!url.contains("://")) {
            if (url.contains(".") && url.indexOf(".") + 2 < url.length()
                    && Character.isLetter(url.charAt(url.indexOf(".") + 1))
                    && Character.isLetter(url.charAt(url.indexOf(".") + 1))) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }
            } else if (!url.equals("about:blank")) {
                url = useSearchEngine(url);
            }
        }

        updateCalled = false;
        webView.loadUrl(url);

        if (url.equals(getString(R.string.new_tab_url))) {
            return;
        }

        pageTitle = null;
        setLinkInput(url);
        setLoading();
    }

    public void stopLoading() {
        tab.setFavicon(webView.getFavicon());
        tab.setPageTitle(webView.getTitle());
        tab.setPageUrl(webView.getUrl());
        webView.stopLoading();
        setLoaded();
    }

    private static String useSearchEngine(String url) {
        String searchEngine = sharedPreferences.getString(SettingsFragment.KEY_SEARCH_ENGINE, "Google");
        switch (searchEngine) {
            case "Google":
                url = "https://www.google.com/search?q=" + url;
                break;
            case "Yandex":
                url = "https://yandex.com/search/?text=" + url;
                break;
            case "Bing":
                url = "https://www.bing.com/search?q=" + url;
                break;
            case "Yahoo":
                url = "https://search.yahoo.com/search?q=" + url;
                break;
        }

        return url;
    }

    public void arrangeBackForwardButtons() {
        if (isCurrent) {
            if (canGoBack()) {
                browserActivity.toggleBtBack(true);
            } else {
                browserActivity.toggleBtBack(false);
            }

            if (canGoForward()) {
                browserActivity.toggleBtForward(true);
            } else {
                browserActivity.toggleBtForward(false);
            }
        }
    }

    public void setLoaded() {
        if (isCurrent) {
            browserActivity.setLoaded();
            isLoaded = true;
        }
    }

    public void setLoading() {
        if (isCurrent) {
            browserActivity.setLoading();
            isLoaded = false;
        }
    }

    public void setLinkInput(String url) {
        if (isCurrent && url != null) {
            browserActivity.setLinkInput(url);
        }
    }

    public void setPageTitleInput() {
        if (isCurrent) {
            if (pageTitle == null || pageTitle.equals("")) {
                setLinkInput(webView.getUrl());
            } else {
                browserActivity.setPageTitleInput(pageTitle);
            }
        }
    }

    public void goBack() {
        if (blockBackFwd) return;

        backFwdPressed = true;
        updateCalled = false;
        browserActivity.linkInputClearFocus();
        setLoading();

        if (tab.isBackup()) {
            if (webView.canGoBack()) webView.goBack();
            else {
                if (tab.getBackStack().size() == 0) return;
                blockBackFwd = true;
                tab.pushToFwdStack(webView.copyBackForwardList());
                loadFromBackStack = true;
                loadUrl(tab.getBackStack().pop().getUrl());
            }
        } else {
            webView.goBack();
        }
    }

    public boolean canGoBack() {
        if (tab.isBackup()) {
            if (webView.canGoBack()) return true;
            else return !tab.getBackStack().isEmpty();
        }

        return webView.canGoBack();
    }

    public void goForward() {
        if (blockBackFwd) return;

        backFwdPressed = true;
        updateCalled = false;
        browserActivity.linkInputClearFocus();
        setLoading();

        if (tab.isBackup()) {
            if (webView.canGoForward()) webView.goForward();
            else {
                if (tab.getFwdStack().size() == 0) return;
                blockBackFwd = true;
                loadFromFwdStack = true;
                loadUrl(tab.getFwdStack().pop().getUrl());
            }
        } else {
            webView.goForward();
        }
    }

    public boolean canGoForward() {
        if (tab.isBackup()) {
            if (webView.canGoForward()) return true;
            else return !tab.getFwdStack().isEmpty();
        }

        return webView.canGoForward();
    }

    @Override
    public void onResume() {
        super.onResume();
        browserActivity.setLocale();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void sendBackground() {
        isCurrent = false;

        if (webView != null)
            webView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void bringForward() {
        isCurrent = true;

        if (webView != null) {
            webView.onResume();
            webView.requestFocus();
        }
    }

    public FrostWebView getWebView() {
        return webView;
    }

    public WebSettings getWebSettings() {
        return webView.getSettings();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public boolean isTargetWindow() {
        return isTargetWindow;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public void setTargetWindow(boolean targetWindow) {
        isTargetWindow = targetWindow;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setResultMsg(Message resultMsg) {
        this.resultMsg = resultMsg;
    }

    public boolean isLoadFromBackStack() {
        return loadFromBackStack;
    }

    public void setLoadFromBackStack(boolean loadFromBackStack) {
        this.loadFromBackStack = loadFromBackStack;
    }

    public boolean isLoadFromFwdStack() {
        return loadFromFwdStack;
    }

    public void setLoadFromFwdStack(boolean loadFromFwdStack) {
        this.loadFromFwdStack = loadFromFwdStack;
    }

    public boolean isRestoreItem() {
        return restoreItem;
    }

    public void setRestoreItem(boolean restoreItem) {
        this.restoreItem = restoreItem;
    }

    public boolean isBackFwdPressed() {
        return backFwdPressed;
    }

    public void setBackFwdPressed(boolean backFwdPressed) {
        this.backFwdPressed = backFwdPressed;
    }

    public boolean isUpdateCalled() {
        return updateCalled;
    }

    public void setUpdateCalled(boolean updateCalled) {
        this.updateCalled = updateCalled;
    }

    public void setBlockBackFwd(boolean blockBackFwd) {
        this.blockBackFwd = blockBackFwd;
    }

    public String[] getDownloadRequestArgs() {
        return downloadRequestArgs;
    }

    public void setDownloadRequestArgs(String[] downloadRequestArgs) {
        this.downloadRequestArgs = downloadRequestArgs;
    }

    public boolean isExternalWindow() {
        return isExternalWindow;
    }

    public void setExternalWindow(boolean externalWindow) {
        isExternalWindow = externalWindow;
    }
}

