package com.leotenebris.frostweb;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.leotenebris.frostweb.History.HistoryActivity;
import com.leotenebris.frostweb.Settings.SettingsActivity;
import com.leotenebris.frostweb.Settings.SettingsFragment;
import com.leotenebris.frostweb.Tabs.TabItem;
import com.leotenebris.frostweb.Tabs.TabsActivity;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import static com.leotenebris.frostweb.BrowserActivity.isNewTab;
import static com.leotenebris.frostweb.BrowserActivity.isWelcomeLinkInputFocused;
import static com.leotenebris.frostweb.BrowserActivity.forceInternalMode;
import static com.leotenebris.frostweb.BrowserActivity.loadFromTargetUrl;
import static com.leotenebris.frostweb.BrowserActivity.sharedPreferences;
import static com.leotenebris.frostweb.BrowserActivity.tabFragments;
import static com.leotenebris.frostweb.FrostWebApplication.tabs;
import static com.leotenebris.frostweb.BrowserActivity.tabsToRemove;
import static com.leotenebris.frostweb.BrowserActivity.targetUrl;
import static com.leotenebris.frostweb.FrostWebApplication.dpToPx;

public class WelcomeActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private Context applicationContext;

    private ConstraintLayout mainLayout;
    private FrameLayout btTabsLayout;
    private ImageView frostWebIcon;
    private ImageButton btGo, btMore, btHome, btTabs;
    private EditText linkInput;
    private TextView txtTabAmount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applicationContext = getApplicationContext();

        String lang = sharedPreferences.getString(SettingsFragment.KEY_LANGUAGE, "system");

        if (lang.equals("system")) {
            lang = Resources.getSystem().getConfiguration().locale.getLanguage();
        }

        setLocale(new Locale(lang));

        setContentView(R.layout.activity_welcome);

        mainLayout = findViewById(R.id.mainLayout);
        frostWebIcon = findViewById(R.id.welcomeFrostWebIcon);
        linkInput = findViewById(R.id.linkInput);
        btGo = findViewById(R.id.btGo);
        btMore = findViewById(R.id.btMore);
        btHome = findViewById(R.id.btHome);
        btTabs = findViewById(R.id.btTabs);
        txtTabAmount = findViewById(R.id.txtTabAmount);
        btTabsLayout = findViewById(R.id.btTabsLayout);

        String tabAmountStr = tabs.size() + "";
        txtTabAmount.setText(tabAmountStr);

        linkInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                go(v);
                return true;
            }
            return false;
        });

        linkInput.setOnFocusChangeListener((v, hasFocus) -> {
            isWelcomeLinkInputFocused = hasFocus;

            TransitionDrawable transition = (TransitionDrawable) v.getBackground();
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            Resources res = getResources();
            int animDuration = 250;

            if (hasFocus) {
                params.setMargins(0, 0, dpToPx(44), 0);
                v.setElevation(0);
                linkInput.setTextColor(ContextCompat.getColor(applicationContext, R.color.white));

                btMore.setVisibility(View.GONE);
                btHome.setVisibility(View.GONE);
                btTabsLayout.setVisibility(View.GONE);
                btGo.setVisibility(View.VISIBLE);
                btGo.setColorFilter(ContextCompat.getColor(applicationContext, R.color.white));
                frostWebIcon.setVisibility(View.GONE);

                transition.startTransition(animDuration);

                ObjectAnimator.ofObject(
                        linkInput,
                        "hintTextColor",
                        new ArgbEvaluator(),
                        ContextCompat.getColor(applicationContext, R.color.colorTextHint),
                        ContextCompat.getColor(applicationContext, R.color.colorPrimaryLighter)
                ).setDuration(animDuration).start();

                ObjectAnimator.ofObject(
                        mainLayout,
                        "backgroundColor",
                        new ArgbEvaluator(),
                        ContextCompat.getColor(applicationContext, R.color.colorBg),
                        ContextCompat.getColor(applicationContext, R.color.colorPrimary)
                ).setDuration(animDuration).start();

                ObjectAnimator.ofFloat(v, "Y", dpToPx(8))
                        .setDuration(animDuration).start();

            } else {
                closeKeyboard();

                int margin = dpToPx(48);
                params.setMargins(margin, margin, margin, margin);
                v.setElevation(dpToPx(8));
                linkInput.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorText));

                btGo.setVisibility(View.GONE);
                btMore.setVisibility(View.VISIBLE);
                btHome.setVisibility(View.VISIBLE);
                btTabsLayout.setVisibility(View.VISIBLE);
                btGo.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorFgAlt));
                frostWebIcon.setVisibility(View.VISIBLE);

                transition.reverseTransition(animDuration);

                ObjectAnimator.ofObject(
                        linkInput,
                        "hintTextColor",
                        new ArgbEvaluator(),
                        ContextCompat.getColor(applicationContext, R.color.colorPrimaryLighter),
                        ContextCompat.getColor(applicationContext, R.color.colorTextHint)
                ).setDuration(animDuration).start();

                ObjectAnimator.ofObject(
                        mainLayout,
                        "backgroundColor",
                        new ArgbEvaluator(),
                        ContextCompat.getColor(applicationContext, R.color.colorPrimary),
                        ContextCompat.getColor(applicationContext, R.color.colorBg)
                ).setDuration(animDuration).start();

                float newY = (mainLayout.getHeight() - linkInput.getHeight()) / 2;
                ObjectAnimator.ofFloat(v, "Y", newY)
                        .setDuration(animDuration).start();
            }

            v.setLayoutParams(params);
        });
    }

    public void popupMore(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.more);

        Menu popupMenu = popup.getMenu();
        popupMenu.findItem(R.id.itemTabs).setVisible(false);
        popupMenu.findItem(R.id.itemHomepage).setVisible(false);

        popup.show();
    }

    public void go(View v) {
        targetUrl = linkInput.getText().toString();
        switch (targetUrl) {
            case "":
            case "frostweb://newtab":
                break;
            default:
                forceInternalMode = true;
                loadFromTargetUrl = true;
                finish();
                overridePendingTransition(0, 0);
                break;
        }
    }

    public void goHome(View v) {
        String newTabUrl = getString(R.string.new_tab_url);
        String homepage = sharedPreferences.getString(SettingsFragment.KEY_HOMEPAGE, newTabUrl);
        if (!homepage.equals(newTabUrl)) {
            targetUrl = homepage;
            forceInternalMode = true;
            loadFromTargetUrl = true;
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    public void showTabs(View v) {
        Intent intent = new Intent(applicationContext, TabsActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.itemNewTab:
                isNewTab = true;
                targetUrl = getString(R.string.new_tab_url);
                finish();
                return true;
            case R.id.itemTabs:
                showTabs(null);
                return true;
            case R.id.itemHomepage:
                goHome(null);
                return true;
            case R.id.itemHistory:
                intent = new Intent(applicationContext, HistoryActivity.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.itemSettings:
                intent = new Intent(applicationContext, SettingsActivity.class);
                startActivity(intent);
                finish();
                return true;
            default:
                return false;
        }
    }

    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(linkInput.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        TabFragment tabFragment = tabFragments.get(0);
        TabItem tab = tabs.get(0);
        if (tab.getPageUrl().equals(getString(R.string.new_tab_url))) {
            if (tabFragment.isTargetWindow() && tabs.size() > 1) {
                tabs.remove(0);
                tabsToRemove.push(tabFragment.getUniqueId());
                finish();
            } else {
                finishAffinity();
            }
        } else {
            finish();
        }
    }

    private void setLocale(Locale locale) {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        configuration.setLocale(locale);
        applicationContext.createConfigurationContext(configuration);
        resources.updateConfiguration(configuration, displayMetrics);
    }
}
