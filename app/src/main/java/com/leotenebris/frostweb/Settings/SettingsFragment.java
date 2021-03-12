package com.leotenebris.frostweb.Settings;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import android.util.DisplayMetrics;
import android.webkit.WebSettings;

import com.leotenebris.frostweb.R;
import com.leotenebris.frostweb.TabFragment;
import com.leotenebris.frostweb.WebView.FrostWebView;

import java.util.Locale;

import static com.leotenebris.frostweb.BrowserActivity.sharedPreferences;
import static com.leotenebris.frostweb.BrowserActivity.tabFragments;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_THEME = "theme";
    public static final String KEY_HOMEPAGE = "homepage";
    public static final String KEY_SEARCH_ENGINE = "search_engine";
    public static final String KEY_JAVASCRIPT = "javascript_enabled";
    public static final String KEY_ZOOM = "zoom";
    public static final String KEY_FORCE_DARK = "force_dark";
    public static final String KEY_STARTUP = "startup";

    private SettingsActivity settingsActivity;
    private Context context;
    private EditTextPreference homePref;
    private ListPreference langPref, themePref, searchPref, startupPref;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        context = getContext();
        settingsActivity = (SettingsActivity) getActivity();

        String lang = sharedPreferences.getString(KEY_LANGUAGE, "system");

        if (lang.equals("system")) {
            lang = Resources.getSystem().getConfiguration().locale.getLanguage();
        }

        setLocale(new Locale(lang));

        setPreferencesFromResource(R.xml.pref_main, s);

        SwitchPreference forceDarkPref = findPreference(KEY_FORCE_DARK);
        assert forceDarkPref != null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            forceDarkPref.setEnabled(false);
            forceDarkPref.setTitle(getString(R.string.force_dark_requires_q));
        }

        homePref = findPreference(KEY_HOMEPAGE);
        assert homePref != null;
        String homepage = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_HOMEPAGE, getString(R.string.new_tab_url));
        homePref.setSummary(homepage);

        langPref = findPreference(KEY_LANGUAGE);
        assert langPref != null;
        String langName = langPref.getEntry().toString();
        langPref.setSummary(langName);

        themePref = findPreference(KEY_THEME);
        assert themePref != null;
        String theme = themePref.getEntry().toString();
        themePref.setSummary(theme);

        searchPref = findPreference(KEY_SEARCH_ENGINE);
        assert searchPref != null;
        String searchEngineName = searchPref.getEntry().toString();
        searchPref.setSummary(searchEngineName);

        startupPref = findPreference(KEY_STARTUP);
        assert startupPref != null;
        String startup = startupPref.getEntry().toString();
        startupPref.setSummary(startup);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Intent intent;
        WebSettings webSettings;
        switch (key) {
            case KEY_LANGUAGE:
                String lang = sharedPreferences.getString(key, "system");

                if (lang.equals("system")) {
                    lang = Resources.getSystem().getConfiguration().locale.getLanguage();
                }

                setLocale(new Locale(lang));

                String langEntry = langPref.getEntry().toString();
                langPref.setSummary(langEntry);

                intent = new Intent(context, SettingsActivity.class);
                startActivity(intent, ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle());

                settingsActivity.finish();

                break;
            case KEY_THEME:
                String theme = sharedPreferences.getString(key, "system");

                themePref = findPreference(KEY_THEME);
                assert themePref != null;
                String themeEntry = themePref.getEntry().toString();
                themePref.setSummary(themeEntry);

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

                break;
            case KEY_HOMEPAGE:
                String homepage = sharedPreferences.getString(key, getString(R.string.new_tab_url));

                if (homepage.equals("")) {
                    homepage = getString(R.string.new_tab_url);
                }

                homePref.setSummary(homepage);

                break;
            case KEY_SEARCH_ENGINE:
                searchPref = findPreference(KEY_SEARCH_ENGINE);
                assert searchPref != null;
                String searchEngineEntry = searchPref.getEntry().toString();
                searchPref.setSummary(searchEngineEntry);

                break;
            case KEY_JAVASCRIPT:
                boolean javascriptEnabled = sharedPreferences.getBoolean(key, true);

                for (int i = 0; i < tabFragments.size(); i++) {
                    webSettings = tabFragments.get(i).getWebSettings();
                    webSettings.setJavaScriptEnabled(javascriptEnabled);
                }

                break;
            case KEY_ZOOM:
                boolean zoomEnabled = sharedPreferences.getBoolean(key, true);

                for (int i = 0; i < tabFragments.size(); i++) {
                    webSettings = tabFragments.get(i).getWebSettings();
                    webSettings.setBuiltInZoomControls(zoomEnabled);
                }

                break;
            case KEY_FORCE_DARK:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    boolean forceDarkEnabled = sharedPreferences.getBoolean(key, false);

                    for (int i = 0; i < tabFragments.size(); i++) {
                        TabFragment tabFragment = tabFragments.get(i);
                        FrostWebView webView = tabFragment.getWebView();
                        webSettings = tabFragment.getWebSettings();

                        if (forceDarkEnabled) {
                            webSettings.setForceDark(WebSettings.FORCE_DARK_ON);
                            webView.setBackgroundColor(ContextCompat.getColor(context, R.color.darkerGray));
                        } else {
                            webSettings.setForceDark(WebSettings.FORCE_DARK_AUTO);
                            webView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                        }
                    }

                }
                break;

            case KEY_STARTUP:
                startupPref = findPreference(KEY_STARTUP);
                assert startupPref != null;
                String startupEntry = startupPref.getEntry().toString();
                startupPref.setSummary(startupEntry);
                break;
        }
    }

    private void setLocale(Locale locale) {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        configuration.setLocale(locale);
        settingsActivity.getApplicationContext().createConfigurationContext(configuration);
        resources.updateConfiguration(configuration, displayMetrics);
    }

    @Override
    public void onResume() {
        super.onResume();
        // documentation requires that a reference to the listener is kept as long as it may be called, which is the case as it can only be called from this Fragment
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
