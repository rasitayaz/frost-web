package com.leotenebris.frostweb;

import android.animation.ObjectAnimator;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.leotenebris.frostweb.History.HistoryActivity;
import com.leotenebris.frostweb.History.HistoryItem;
import com.leotenebris.frostweb.Settings.SettingsActivity;
import com.leotenebris.frostweb.Settings.SettingsFragment;
import com.leotenebris.frostweb.Tabs.TabItem;
import com.leotenebris.frostweb.Tabs.TabsActivity;
import com.leotenebris.frostweb.WebView.FrostWebView;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import static com.leotenebris.frostweb.FrostWebApplication.KEY_HISTORY;
import static com.leotenebris.frostweb.FrostWebApplication.KEY_TABS;
import static com.leotenebris.frostweb.FrostWebApplication.browserHistory;
import static com.leotenebris.frostweb.FrostWebApplication.tabs;
import static com.leotenebris.frostweb.Settings.SettingsFragment.KEY_HOMEPAGE;
import static com.leotenebris.frostweb.Settings.SettingsFragment.KEY_STARTUP;
import static com.leotenebris.frostweb.Settings.SettingsFragment.KEY_THEME;
import static com.leotenebris.frostweb.TabFragment.STORAGE_PERMISSION_CODE;

public class BrowserActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    public static final int FILE_CHOOSER = 1;

    private EditText linkInput;
    private ImageButton btGo, btBack, btForward, btMore;
    private ProgressBar progressBar;
    private Context applicationContext;

    public static SharedPreferences sharedPreferences;
    public static ArrayList<TabFragment> tabFragments;
    public static Stack<String> tabsToRemove;

    public static boolean isLoaded, startup, isNewTab, isSavedTab, isInternalIntent, forceInternalMode,
            loadFromTargetUrl, isWelcomeLinkInputFocused;
    public static String fileName, targetUrl;

    Drawable searchIcon, refreshIcon;

    private ValueCallback<Uri[]> filePathCallback;

    public boolean showFileChooser(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        this.filePathCallback = filePathCallback;

        try {
            Intent intent = fileChooserParams.createIntent();
            startActivityForResult(intent, FILE_CHOOSER);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_CHOOSER) {
            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
            filePathCallback = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        applicationContext = getApplicationContext();

        setContentView(R.layout.activity_browser);

        searchIcon = getResources().getDrawable(R.drawable.icon_search);
        refreshIcon = getResources().getDrawable(R.drawable.icon_refresh);
        linkInput = findViewById(R.id.linkInput);
        btGo = findViewById(R.id.btGo);
        btBack = findViewById(R.id.btBack);
        btForward = findViewById(R.id.btForward);
        btMore = findViewById(R.id.btMore);
        progressBar = findViewById(R.id.progressBar);

        startup = true;

        String theme = sharedPreferences.getString(KEY_THEME, "system");

        switch (theme) {
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        loadHistory();

        String startup = sharedPreferences.getString(KEY_STARTUP, "restore_session");
        String homepageUrl = sharedPreferences
                .getString(KEY_HOMEPAGE, getString(R.string.new_tab_url));

        tabs = new ArrayList<>();
        tabFragments = new ArrayList<>();

        switch (startup) {
            case "restore_session":
                loadTabs();
                break;
            case "load_homepage":
                newTab(homepageUrl, null, false);
                break;
            case "load_empty_page":
                newTab(getString(R.string.new_tab_url), null, false);
                break;
        }
        tabsToRemove = new Stack<>();

        linkInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                go(v);
                return true;
            }
            return false;
        });

        linkInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (tabFragments != null) {
                TabFragment tabFragment = tabFragments.get(0);
                FrostWebView webView = tabFragment.getWebView();
                if (hasFocus) {
                    if (webView != null)
                        tabFragment.setLinkInput(webView.getUrl());
                    linkInput.selectAll();
                    setSbLayout(true);
                } else {
                    tabFragment.setPageTitleInput();
                    setSbLayout(false);
                }
            }
        });
    }

    public void goBack(View v) {
        closeKeyboard();
        tabFragments.get(0).goBack();
    }

    public void goForward(View v) {
        closeKeyboard();
        tabFragments.get(0).goForward();
    }

    public void go(View v) {
        closeKeyboard();
        TabFragment tabFragment = tabFragments.get(0);

        if (linkInput.isFocused()) {
            forceInternalMode = true;
            tabFragment.loadUrl(linkInput.getText().toString());
        } else if (isLoaded) {
            // refresh
            tabFragment.setLoading();
            tabFragment.getWebView().reload();
        } else {
            // cancel
            tabFragment.stopLoading();
        }

        linkInputClearFocus();
    }

    public void popupMore(View v) {
        closeKeyboard();
        linkInput.clearFocus();

        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.more);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent;
        TabFragment tabFragment = tabFragments.get(0);
        switch (item.getItemId()) {
            case R.id.itemNewTab:
                newTab(getString(R.string.new_tab_url), null, false);
                return true;
            case R.id.itemTabs:
                intent = new Intent(applicationContext, TabsActivity.class);
                startActivity(intent);
                return true;
            case R.id.itemHomepage:
                forceInternalMode = true;
                String url = sharedPreferences
                        .getString(KEY_HOMEPAGE, getString(R.string.new_tab_url));
                tabFragment.loadUrl(url);
                return true;
            case R.id.itemHistory:
                intent = new Intent(applicationContext, HistoryActivity.class);
                startActivity(intent);
                return true;
            case R.id.itemSettings:
                intent = new Intent(applicationContext, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    private void loadHistory() {
        Gson gson = new Gson();
        String json = getSharedPreferences(KEY_HISTORY, MODE_PRIVATE).getString(KEY_HISTORY, null);
        Type type = new TypeToken<ArrayList<HistoryItem>>() {
        }.getType();
        browserHistory = gson.fromJson(json, type);

        if (browserHistory == null)
            browserHistory = new ArrayList<>();
    }

    private void cleanTabs() {
        for (int i = 0; i < tabs.size(); i++) {
            TabItem tab = tabs.get(i);
            tab.setDefaultFavicon();
        }
    }

    private void loadTabs() {
        Gson gson = new Gson();
        String json = getSharedPreferences(KEY_TABS, MODE_PRIVATE).getString(KEY_TABS, null);
        Type type = new TypeToken<ArrayList<TabItem>>() {
        }.getType();
        tabs = gson.fromJson(json, type);

        if (tabs == null) {
            tabs = new ArrayList<>();

            tabs.add(new TabItem());

            isNewTab = true;
            targetUrl = sharedPreferences.getString(KEY_HOMEPAGE, getString(R.string.new_tab_url));
        } else {
            for (TabItem tabItem : tabs) {
                tabItem.setBackup(true);
                tabItem.mergeWithBackupHistory();
            }
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        tabFragments.add(new TabFragment());

        ft.add(R.id.baseFrame, tabFragments.get(0));
        ft.commit();

        cleanTabs();
    }

    @Override
    public void onBackPressed() {
        closeKeyboard();
        linkInput.clearFocus();

        TabFragment tabFragment = tabFragments.get(0);

        if (tabFragment.canGoBack()) {
            tabFragment.goBack();
        } else {
            if (tabs.size() > 1) {
                if (tabFragment.isTargetWindow() || tabFragment.isExternalWindow()) {
                    tabs.remove(0);
                    tabsToRemove.push(tabFragment.getUniqueId());
                }

                if (tabFragment.isTargetWindow()) {
                    handleTabs();
                }
            }

            if (!tabFragment.isTargetWindow()) {
                super.onBackPressed();
            }
        }
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            setIntent(intent);
        }
    }

    private void destroyTabs() {
        if (!tabsToRemove.isEmpty()) {
            int size = tabsToRemove.size();
            for (int i = 0; i < size; i++) {
                String tabId = tabsToRemove.pop();
                for (int j = 0; j < tabFragments.size(); j++) {
                    TabFragment tabFragment = tabFragments.get(j);
                    if (tabFragment.getView() != null && tabFragment.getUniqueId().equals(tabId)) {
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                        ft.detach(tabFragment).commit();

                        tabFragments.remove(j);

                        break;
                    }
                }
            }
        }
    }

    public void handleTabs() {
        destroyTabs();

        if (isNewTab) {
            newTab(targetUrl, null, false);
        } else if (getIntent() != null && getIntent().getData() != null) {
            String url = getIntent().getData().toString();
            forceInternalMode = true;

            if (isInternalIntent && tabFragments != null) {
                tabFragments.get(0).loadUrl(url);
            } else {
                newTab(url, null, true);
            }

            isInternalIntent = false;
            getIntent().setData(null);
        } else if (tabFragments == null || tabFragments.size() == 0) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

            tabFragments.add(new TabFragment());

            ft.add(R.id.baseFrame, tabFragments.get(0));
            ft.commit();
        } else {
            TabItem currentTab = tabs.get(0);
            TabFragment currentFragment = tabFragments.get(0);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

            if (isSavedTab)
                ft.add(R.id.baseFrame, currentFragment);
            else
                ft.show(currentFragment);

            currentFragment.bringForward();

            if (tabFragments.size() > 1) {
                TabFragment oldFragment = tabFragments.get(1);
                oldFragment.sendBackground();
                ft.hide(oldFragment);
            }

            ft.commit();

            if (currentFragment.getView() != null) {
                currentFragment.arrangeBackForwardButtons();
                currentFragment.setPageTitleInput();
            }

            String url = currentTab.getPageUrl();

            if (url.equals(getString(R.string.new_tab_url)) && !loadFromTargetUrl && !isSavedTab && !startup) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                startActivity(intent);

            }

            isSavedTab = false;
        }
    }

    @Override
    protected void onStop() {
        isInternalIntent = false;
        super.onStop();
    }

    @Override
    protected void onPause() {
        FrostWebApplication app = FrostWebApplication.getInstance();
        app.saveHistory();
        app.saveTabs();
        //deleteCache(applicationContext);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        handleTabs();

        if (loadFromTargetUrl) {
            if (isWelcomeLinkInputFocused) {
                setSbLayout(true);
                Handler handler = new Handler();
                handler.postDelayed(() -> setSbLayout(false), 50);
                isWelcomeLinkInputFocused = false;
            }
            TabFragment tabFragment = tabFragments.get(0);
            tabFragment.loadUrl(targetUrl);
            loadFromTargetUrl = false;
        }

        setLocale();
        linkInput.setHint(getString(R.string.search_hint));

        startup = false;
    }

    private void setSbLayout(boolean focused) {
        if (focused) {
            btBack.setVisibility(View.GONE);
            btForward.setVisibility(View.GONE);
            btMore.setVisibility(View.GONE);
            btGo.setImageResource(R.drawable.icon_search);
        } else {
            btBack.setVisibility(View.VISIBLE);
            btForward.setVisibility(View.VISIBLE);
            btMore.setVisibility(View.VISIBLE);
            if (isLoaded) {
                btGo.setImageResource(R.drawable.icon_refresh);
            } else {
                btGo.setImageResource(R.drawable.icon_cancel);
            }
        }
    }

    public void newTab(String url, Message resultMsg, boolean externalTab) {
        tabs.add(0, new TabItem());

        isNewTab = true;
        targetUrl = url;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        if (tabFragments.size() > 0) {
            TabFragment currentTabFragment = tabFragments.get(0);
            currentTabFragment.sendBackground();
            ft.hide(currentTabFragment);
        }

        TabFragment newTabFragment = new TabFragment();
        newTabFragment.setResultMsg(resultMsg);
        newTabFragment.setExternalWindow(externalTab);
        newTabFragment.setTargetWindow(!externalTab);
        tabFragments.add(0, newTabFragment);

        ft.add(R.id.baseFrame, newTabFragment);
        ft.commit();
    }

    public int handleUri(Uri uri, WebView webView, TabFragment tabFragment) {
        String url = uri.toString();
        String uriScheme = uri.getScheme();

        String oldUrl = webView.getUrl();
        String oldHost = (oldUrl == null) ? null : Uri.parse(oldUrl).getHost();
        String host = uri.getHost();

        if (uriScheme != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);

            try {
                switch (uriScheme) {
                    case "intent":
                        try {
                            Intent parsedUri = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            String fallbackUrl = parsedUri.getStringExtra("browser_fallback_url");
                            Uri fallbackUri = Uri.parse(fallbackUrl);
                            String fallbackScheme = fallbackUri.getScheme();

                            if (fallbackScheme == null)
                                return 0;

                            switch (fallbackScheme) {
                                case "http":
                                case "https":
                                    tabFragment.loadUrl(fallbackUrl);
                                    break;
                                default:
                                    intent.setData(fallbackUri);
                                    startActivity(intent);
                                    return 1;
                            }
                            tabFragment.loadUrl(fallbackUrl);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            return 0;
                        }
                        break;
                    case "market":
                        webView.stopLoading();
                        startActivity(intent);
                        return 1;
                    case "http":
                    case "https":
                        PackageManager packageManager = getPackageManager();
                        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,
                                PackageManager.MATCH_DEFAULT_ONLY);

                        if (!forceInternalMode && host != null &&
                                (oldHost == null || !oldHost.equals(host))) {
                            if (activities.size() > 1
                                    && activities.get(0).match > activities.get(1).match) {
                                isInternalIntent = true;
                                webView.stopLoading();
                                startActivity(intent);
                                return 1;
                            } else if (host.equals("play.google.com")) {
                                webView.stopLoading();
                                startActivity(intent);
                                return 1;
                            }
                        }

                        break;
                    default:
                        webView.stopLoading();
                        tabFragment.setLoaded();
                        return 0;
                }
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                return 0;
            }
        }

        return -1;
    }

    public void linkInputClearFocus() {
        linkInput.clearFocus();
    }

    public void setLoaded() {
        progressBar.setVisibility(View.INVISIBLE);

        if (!linkInput.isFocused()) {
            btGo.setImageResource(R.drawable.icon_refresh);
        }
    }

    public void setLoading() {
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);

        if (!linkInput.isFocused()) {
            btGo.setImageResource(R.drawable.icon_cancel);
        }
    }

    public void setLinkInput(String url) {
        linkInput.setText(url);
        if (linkInput.isFocused()) {
            linkInput.selectAll();
        }

        if (!linkInput.isFocused() && url.equalsIgnoreCase("about:blank") || url.equals("")) {
            linkInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } else {
            linkInput.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
    }

    public void setPageTitleInput(String pageTitle) {
        if (!linkInput.isFocused()) {
            linkInput.setText(pageTitle);
            linkInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
    }

    public void toggleBtBack(boolean state) {
        btBack.setEnabled(state);
        btBack.setImageAlpha(state ? 1000 : 50);
    }

    public void toggleBtForward(boolean state) {
        btForward.setEnabled(state);
        btForward.setImageAlpha(state ? 1000 : 50);
    }

    public void animateProgressBar(int newProgress) {
        if (tabFragments.get(0).isCurrent()) {
            ObjectAnimator.ofInt(progressBar, "progress", newProgress)
                    .setDuration(250).start();
        }
    }

    public void setLocale() {
        String lang = sharedPreferences.getString(SettingsFragment.KEY_LANGUAGE, "system");

        if (lang.equals("system")) {
            lang = Resources.getSystem().getConfiguration().locale.getLanguage();
        }

        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        configuration.setLocale(new Locale(lang));
        applicationContext.createConfigurationContext(configuration);
        resources.updateConfiguration(configuration, displayMetrics);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                TabFragment tabFragment = tabFragments.get(0);
                String[] args = tabFragment.getDownloadRequestArgs();
                if (args != null)
                    downloadDialog(args[0], args[1], args[2], args[3]);
                tabFragment.setDownloadRequestArgs(null);
            } else {
                Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void downloadDialog(String url, String userAgent,
                               String contentDisposition, String mimeType) {

        if (url.startsWith("blob:")) {
            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle("Can't download file")
                    .setMessage("This browser currently does not support downloads from 'blob' URLs.")
                    .setPositiveButton(R.string.confirm, null)
                    .create().show();
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(R.string.download)
                .setMessage(filename)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    String cookie = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("Cookie", cookie);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(DOWNLOAD_SERVICE);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    if (downloadManager != null)
                        downloadManager.enqueue(request);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .create().show();
    }
}
