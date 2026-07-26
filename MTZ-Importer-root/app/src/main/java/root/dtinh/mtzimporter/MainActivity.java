package root.dtinh.mtzimporter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MainActivity extends Activity {
    private static final int PICK_THEME = 31;
    private static final int PICK_FONT = 32;
    private static final String WEBSITE_URL =
            "https://tinhtinh1908.github.io/home/";

    private static final String THEME_PACKAGE =
            "com.android.thememanager";
    private static final String THEME_HOME_ACTIVITY =
            "com.android.thememanager.ThemeResourceTabActivity";
    private static final String THEME_DETAIL_ACTIVITY =
            "com.android.thememanager.module.detail.view.ThemeDetailActivity";

    private boolean darkMode;
    private int pageColor;
    private int cardColor;
    private int cardBorderColor;
    private int mutedSurfaceColor;
    private int textPrimary;
    private int textSecondary;
    private int accentColor;
    private int successColor;
    private int errorColor;

    private TextView statusTitle;
    private TextView statusMessage;
    private TextView statusIndicator;
    private TextView rootBadge;
    private TextView screenTitle;
    private TextView screenDescription;
    private TextView importTab;
    private TextView fontTab;
    private TextView themesTab;
    private TextView importFormatIcon;
    private TextView importCardTitle;
    private TextView importCardDescription;
    private TextView themesCountLabel;
    private TextView refreshThemesButton;
    private ProgressBar progress;
    private ProgressBar themesProgress;
    private Button chooseButton;
    private LinearLayout importSection;
    private LinearLayout themesSection;
    private LinearLayout themesList;
    private boolean fontTabActive;
    private boolean themesTabActive;
    private boolean themesLoading;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        try {
            resolvePalette();
            configureSystemBarsSafe();
            setContentView(buildUiSafe());
            checkRootAsync();

            Intent incomingIntent = getIntent();
            Uri incoming = incomingIntent == null
                    ? null
                    : incomingIntent.getData();
            if (incoming != null) {
                String incomingName = IoUtils.displayName(
                        getContentResolver(),
                        incoming
                ).toLowerCase(java.util.Locale.ROOT);
                if (incomingName.endsWith(".ttf")
                        || incomingName.endsWith(".otf")
                        || incomingName.endsWith(".ttc")) {
                    showFontTab();
                    importFont(incoming);
                } else {
                    importTheme(incoming);
                }
            }
        } catch (Throwable error) {
            showStartupError(error);
        }
    }

    private void resolvePalette() {
        int mode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        darkMode = mode == Configuration.UI_MODE_NIGHT_YES;

        pageColor = darkMode
                ? Color.rgb(12, 13, 16)
                : Color.rgb(246, 247, 249);
        cardColor = darkMode
                ? Color.rgb(30, 31, 35)
                : Color.WHITE;
        cardBorderColor = darkMode
                ? Color.rgb(48, 49, 54)
                : Color.rgb(231, 232, 236);
        mutedSurfaceColor = darkMode
                ? Color.rgb(40, 41, 46)
                : Color.rgb(244, 245, 248);
        textPrimary = darkMode
                ? Color.rgb(245, 245, 247)
                : Color.rgb(24, 25, 28);
        textSecondary = darkMode
                ? Color.rgb(170, 171, 178)
                : Color.rgb(103, 105, 113);
        accentColor = Color.rgb(10, 132, 255);
        successColor = Color.rgb(48, 176, 93);
        errorColor = Color.rgb(255, 69, 58);
    }

    private void configureSystemBarsSafe() {
        try {
            Window window = getWindow();
            window.setStatusBarColor(pageColor);
            window.setNavigationBarColor(pageColor);
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);

            int flags = window.getDecorView().getSystemUiVisibility();
            if (!darkMode) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        } catch (Throwable ignored) {
        }
    }

    private View buildUiSafe() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(pageColor);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(14), dp(18), dp(26));
        scroll.addView(
                page,
                new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT
                )
        );
        applySystemInsets(scroll, page);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(
                header,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        TextView eyebrow = makeText(
                "MTZ Tool",
                13,
                true,
                accentColor
        );
        header.addView(
                eyebrow,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        TextView websiteButton = makeText(
                "Website",
                11,
                true,
                accentColor
        );
        websiteButton.setSingleLine(true);
        websiteButton.setGravity(Gravity.CENTER);
        websiteButton.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
        );
        websiteButton.setBackground(
                roundedBackground(
                        withAlpha(accentColor, darkMode ? 34 : 22),
                        24
                )
        );
        websiteButton.setOnClickListener(v -> openWebsite());
        LinearLayout.LayoutParams websiteParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        websiteParams.rightMargin = dp(8);
        header.addView(websiteButton, websiteParams);

        rootBadge = makeText(
                "Đang kiểm tra",
                10,
                true,
                textSecondary
        );
        rootBadge.setSingleLine(true);
        rootBadge.setGravity(Gravity.CENTER);
        rootBadge.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
        );
        rootBadge.setBackground(
                roundedBackground(mutedSurfaceColor, 24)
        );
        header.addView(rootBadge);

        screenTitle = makeText(
                "Nhập chủ đề",
                28,
                true,
                textPrimary
        );
        screenTitle.setPadding(0, dp(8), 0, 0);
        page.addView(screenTitle);

        screenDescription = makeText(
                "Nhập và quản lý tài nguyên Chủ đề Xiaomi.",
                14,
                false,
                textSecondary
        );
        screenDescription.setLineSpacing(0f, 1.12f);
        screenDescription.setPadding(0, dp(6), 0, dp(14));
        page.addView(screenDescription);

        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(dp(3), dp(3), dp(3), dp(3));
        tabBar.setBackground(
                roundedBackground(mutedSurfaceColor, 17)
        );
        page.addView(
                tabBar,
                margins(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(46),
                        0,
                        0,
                        0,
                        14
                )
        );

        importTab = createTab("Chủ đề");
        importTab.setOnClickListener(v -> showImportTab());
        tabBar.addView(
                importTab,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        fontTab = createTab("Phông chữ");
        fontTab.setOnClickListener(v -> showFontTab());
        tabBar.addView(
                fontTab,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        themesTab = createTab("Đã nhập");
        themesTab.setOnClickListener(v -> showThemesTab());
        tabBar.addView(
                themesTab,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                )
        );

        importSection = new LinearLayout(this);
        importSection.setOrientation(LinearLayout.VERTICAL);
        page.addView(
                importSection,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        LinearLayout importCard = createCard();
        importCard.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );
        importSection.addView(
                importCard,
                margins(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0,
                        0,
                        0,
                        12
                )
        );

        LinearLayout importHeader = new LinearLayout(this);
        importHeader.setOrientation(LinearLayout.HORIZONTAL);
        importHeader.setGravity(Gravity.CENTER_VERTICAL);
        importCard.addView(
                importHeader,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        importFormatIcon = makeText(
                "MTZ",
                14,
                true,
                Color.WHITE
        );
        importFormatIcon.setGravity(Gravity.CENTER);
        importFormatIcon.setBackground(
                roundedBackground(accentColor, 18)
        );
        importHeader.addView(
                importFormatIcon,
                new LinearLayout.LayoutParams(
                        dp(50),
                        dp(50)
                )
        );

        LinearLayout importText = new LinearLayout(this);
        importText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams importTextParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
        importTextParams.leftMargin = dp(13);
        importHeader.addView(importText, importTextParams);

        importCardTitle = makeText(
                "Chọn tệp chủ đề",
                18,
                true,
                textPrimary
        );
        importText.addView(importCardTitle);

        importCardDescription = makeText(
                "Tệp MTZ • Xử lý trực tiếp trên máy",
                13,
                false,
                textSecondary
        );
        importCardDescription.setPadding(0, dp(3), 0, 0);
        importText.addView(importCardDescription);

        chooseButton = new Button(this);
        stylePrimaryButton(chooseButton, "Chọn tệp MTZ");
        chooseButton.setOnClickListener(v -> pickFile());
        importCard.addView(
                chooseButton,
                margins(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(52),
                        0,
                        16,
                        0,
                        0
                )
        );

        LinearLayout statusCard = createCard();
        statusCard.setPadding(
                dp(16),
                dp(15),
                dp(16),
                dp(15)
        );
        importSection.addView(
                statusCard,
                margins(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0,
                        0,
                        0,
                        0
                )
        );

        LinearLayout statusHeader = new LinearLayout(this);
        statusHeader.setOrientation(LinearLayout.HORIZONTAL);
        statusHeader.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.addView(
                statusHeader,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        statusIndicator = makeText(
                "●",
                15,
                true,
                accentColor
        );
        statusIndicator.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams indicatorParams =
                new LinearLayout.LayoutParams(
                        dp(18),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        indicatorParams.rightMargin = dp(7);
        statusHeader.addView(statusIndicator, indicatorParams);

        statusTitle = makeText(
                "Sẵn sàng",
                15,
                true,
                textPrimary
        );
        statusHeader.addView(
                statusTitle,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        progress = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleSmall
        );
        progress.setVisibility(View.GONE);
        statusHeader.addView(
                progress,
                new LinearLayout.LayoutParams(
                        dp(24),
                        dp(24)
                )
        );

        statusMessage = makeText(
                "Chọn một tệp MTZ để bắt đầu nhập.",
                13,
                false,
                textSecondary
        );
        statusMessage.setLineSpacing(0f, 1.12f);
        statusMessage.setPadding(0, dp(9), 0, 0);
        statusCard.addView(statusMessage);

        themesSection = createThemesSection();
        page.addView(
                themesSection,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );
        showImportTab();

        return scroll;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (themesTabActive
                && themesSection != null
                && !themesLoading) {
            loadThemes();
        }
    }

    private TextView createTab(String label) {
        TextView tab = makeText(
                label,
                13,
                true,
                textSecondary
        );
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine(true);
        return tab;
    }

    private LinearLayout createThemesSection() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setVisibility(View.GONE);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(dp(2), 0, dp(2), dp(10));
        section.addView(
                heading,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        themesCountLabel = makeText(
                "Chủ đề trong máy",
                14,
                true,
                textPrimary
        );
        heading.addView(
                themesCountLabel,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        themesProgress = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleSmall
        );
        themesProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(dp(20), dp(20));
        progressParams.rightMargin = dp(10);
        heading.addView(themesProgress, progressParams);

        refreshThemesButton = makeText(
                "Làm mới",
                13,
                true,
                accentColor
        );
        refreshThemesButton.setGravity(Gravity.CENTER);
        refreshThemesButton.setPadding(
                dp(10),
                dp(6),
                dp(10),
                dp(6)
        );
        refreshThemesButton.setBackground(
                roundedBackground(
                        withAlpha(accentColor, darkMode ? 34 : 22),
                        14
                )
        );
        refreshThemesButton.setOnClickListener(v -> loadThemes());
        heading.addView(refreshThemesButton);

        themesList = new LinearLayout(this);
        themesList.setOrientation(LinearLayout.VERTICAL);
        section.addView(
                themesList,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );
        return section;
    }

    private void showImportTab() {
        fontTabActive = false;
        themesTabActive = false;
        if (importSection != null) {
            importSection.setVisibility(View.VISIBLE);
        }
        if (themesSection != null) {
            themesSection.setVisibility(View.GONE);
        }
        if (screenTitle != null) {
            screenTitle.setText("Nhập chủ đề");
        }
        if (screenDescription != null) {
            screenDescription.setText(
                    "Nhập tệp MTZ vào Chủ đề Xiaomi."
            );
        }
        configureImportCard(false);
        styleTab(importTab, true);
        styleTab(fontTab, false);
        styleTab(themesTab, false);
    }

    private void showFontTab() {
        fontTabActive = true;
        themesTabActive = false;
        if (importSection != null) {
            importSection.setVisibility(View.VISIBLE);
        }
        if (themesSection != null) {
            themesSection.setVisibility(View.GONE);
        }
        if (screenTitle != null) {
            screenTitle.setText("Nhập phông chữ");
        }
        if (screenDescription != null) {
            screenDescription.setText(
                    "Nhập TTF, OTF, TTC hoặc MTZ vào Chủ đề Xiaomi."
            );
        }
        configureImportCard(true);
        styleTab(importTab, false);
        styleTab(fontTab, true);
        styleTab(themesTab, false);
    }

    private void configureImportCard(boolean fontMode) {
        if (importFormatIcon == null) {
            return;
        }
        importFormatIcon.setText(fontMode ? "Aa" : "MTZ");
        importCardTitle.setText(
                fontMode ? "Chọn tệp phông chữ" : "Chọn tệp chủ đề"
        );
        importCardDescription.setText(
                fontMode
                        ? "Định dạng .ttf, .otf, .ttc hoặc .mtz"
                        : "Tệp MTZ • Xử lý trực tiếp trên máy"
        );
        chooseButton.setText(
                fontMode ? "Chọn phông chữ hoặc MTZ" : "Chọn tệp MTZ"
        );
        setStatus(
                "Sẵn sàng",
                fontMode
                        ? "Chọn TTF/OTF/TTC hoặc gói phông chữ MTZ để bắt đầu."
                        : "Chọn một tệp MTZ để bắt đầu nhập.",
                textPrimary
        );
    }

    private void showThemesTab() {
        fontTabActive = false;
        themesTabActive = true;
        importSection.setVisibility(View.GONE);
        themesSection.setVisibility(View.VISIBLE);
        screenTitle.setText("Chủ đề đã nhập");
        screenDescription.setText(
                "Mở hoặc xóa chủ đề đã nhập."
        );
        styleTab(importTab, false);
        styleTab(fontTab, false);
        styleTab(themesTab, true);
        loadThemes();
    }

    private void styleTab(TextView tab, boolean selected) {
        if (tab == null) {
            return;
        }
        tab.setTextColor(selected ? accentColor : textSecondary);
        tab.setBackground(
                roundedBackground(
                        selected ? cardColor : Color.TRANSPARENT,
                        14
                )
        );
    }

    private void loadThemes() {
        if (themesLoading) {
            return;
        }
        themesLoading = true;
        themesProgress.setVisibility(View.VISIBLE);
        refreshThemesButton.setEnabled(false);
        refreshThemesButton.setAlpha(0.45f);
        themesCountLabel.setText("Đang đọc kho chủ đề…");

        new Thread(() -> {
            try {
                List<ThemeRecord> themes =
                        new ThemeRepository().listThemes();
                runOnUiThread(() -> {
                    themesLoading = false;
                    themesProgress.setVisibility(View.GONE);
                    refreshThemesButton.setEnabled(true);
                    refreshThemesButton.setAlpha(1f);
                    renderThemes(themes);
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    themesLoading = false;
                    themesProgress.setVisibility(View.GONE);
                    refreshThemesButton.setEnabled(true);
                    refreshThemesButton.setAlpha(1f);
                    themesCountLabel.setText("Không đọc được dữ liệu");
                    showThemesMessage(
                            error.getMessage() == null
                                    ? error.toString()
                                    : error.getMessage(),
                            errorColor
                    );
                });
            }
        }).start();
    }

    private void renderThemes(List<ThemeRecord> themes) {
        themesList.removeAllViews();
        themesCountLabel.setText(
                themes.size() + " chủ đề trong máy"
        );

        if (themes.isEmpty()) {
            showThemesMessage(
                    "Chưa có chủ đề đã nhập.",
                    textSecondary
            );
            return;
        }

        for (ThemeRecord theme : themes) {
            themesList.addView(
                    createThemeCard(theme),
                    margins(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            0,
                            0,
                            0,
                            10
                    )
            );
        }
    }

    private View createThemeCard(ThemeRecord theme) {
        LinearLayout card = createCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(13), dp(12), dp(13));

        TextView icon = makeText("T", 17, true, Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundedBackground(accentColor, 14));
        card.addView(
                icon,
                new LinearLayout.LayoutParams(dp(44), dp(44))
        );

        LinearLayout information = new LinearLayout(this);
        information.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );
        infoParams.leftMargin = dp(12);
        infoParams.rightMargin = dp(8);
        card.addView(information, infoParams);

        TextView title = makeText(
                theme.title,
                16,
                true,
                textPrimary
        );
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        information.addView(title);

        String detail = theme.components.size() + " thành phần";
        if (!theme.author.isEmpty()) {
            detail = theme.author + "  •  " + detail;
        }
        TextView subtitle = makeText(
                detail,
                12,
                false,
                textSecondary
        );
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        subtitle.setPadding(0, dp(3), 0, 0);
        information.addView(subtitle);

        TextView id = makeText(
                theme.themeId,
                10,
                false,
                textSecondary
        );
        id.setSingleLine(true);
        id.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        id.setPadding(0, dp(2), 0, 0);
        information.addView(id);

        TextView delete = makeText(
                "Xóa",
                12,
                true,
                errorColor
        );
        delete.setGravity(Gravity.CENTER);
        delete.setPadding(dp(12), dp(8), dp(12), dp(8));
        delete.setBackground(
                roundedBackground(
                        withAlpha(errorColor, darkMode ? 38 : 24),
                        16
                )
        );
        delete.setOnClickListener(v -> confirmDeleteTheme(theme));
        card.addView(delete);
        card.setOnClickListener(v -> openImportedTheme(theme.themeId));
        return card;
    }

    private void showThemesMessage(
            String message,
            int color
    ) {
        themesList.removeAllViews();
        LinearLayout card = createCard();
        card.setPadding(dp(18), dp(22), dp(18), dp(22));

        TextView text = makeText(message, 13, false, color);
        text.setGravity(Gravity.CENTER);
        text.setLineSpacing(0f, 1.12f);
        card.addView(text);
        themesList.addView(
                card,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );
    }

    private void confirmDeleteTheme(ThemeRecord theme) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa chủ đề?")
                .setMessage(
                        "Sẽ xóa toàn bộ dữ liệu của “"
                                + theme.title
                                + "”. Thao tác này không thể hoàn tác."
                )
                .setNegativeButton("Hủy", null)
                .setPositiveButton(
                        "Xóa",
                        (dialog, which) -> deleteTheme(theme)
                )
                .show();
    }

    private void deleteTheme(ThemeRecord theme) {
        if (themesLoading) {
            return;
        }
        themesLoading = true;
        themesProgress.setVisibility(View.VISIBLE);
        refreshThemesButton.setEnabled(false);
        refreshThemesButton.setAlpha(0.45f);
        themesCountLabel.setText("Đang xóa “" + theme.title + "”…");

        new Thread(() -> {
            try {
                new ThemeRepository().deleteTheme(theme);
                runOnUiThread(() -> {
                    themesLoading = false;
                    Toast.makeText(
                            MainActivity.this,
                            "Đã xóa chủ đề",
                            Toast.LENGTH_SHORT
                    ).show();
                    loadThemes();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    themesLoading = false;
                    themesProgress.setVisibility(View.GONE);
                    refreshThemesButton.setEnabled(true);
                    refreshThemesButton.setAlpha(1f);
                    themesCountLabel.setText("Xóa không thành công");
                    Toast.makeText(
                            MainActivity.this,
                            error.getMessage() == null
                                    ? error.toString()
                                    : error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private void applySystemInsets(
            ScrollView scroll,
            LinearLayout page
    ) {
        scroll.setOnApplyWindowInsetsListener(
                (view, insets) -> {
                    android.graphics.Insets safeArea =
                            insets.getInsets(
                                    WindowInsets.Type.systemBars()
                                            | WindowInsets.Type.displayCutout()
                            );
                    page.setPadding(
                            dp(18) + safeArea.left,
                            dp(14) + safeArea.top,
                            dp(18) + safeArea.right,
                            dp(26) + safeArea.bottom
                    );
                    return insets;
                }
        );
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background =
                roundedBackground(cardColor, 26);
        background.setStroke(dp(1), cardBorderColor);
        card.setBackground(background);
        return card;
    }

    private void stylePrimaryButton(
            Button button,
            String label
    ) {
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setElevation(0f);
        button.setStateListAnimator(null);
        button.setTypeface(
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.NORMAL
                )
        );
        button.setGravity(Gravity.CENTER);
        button.setBackground(
                roundedBackground(accentColor, 18)
        );
    }

    private void pickFile() {
        try {
            Intent picker = new Intent(
                    Intent.ACTION_OPEN_DOCUMENT
            );
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            picker.putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    fontTabActive
                            ? new String[]{
                            "font/ttf",
                            "font/otf",
                            "application/x-font-ttf",
                            "application/vnd.ms-opentype",
                            "application/zip",
                            "application/octet-stream"
                    }
                            : new String[]{
                            "application/zip",
                            "application/octet-stream",
                            "application/x-zip-compressed"
                    }
            );
            startActivityForResult(
                    picker,
                    fontTabActive ? PICK_FONT : PICK_THEME
            );
        } catch (Throwable error) {
            setFailure(
                    "Không mở được trình chọn file:\n"
                            + error.getMessage()
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if ((requestCode != PICK_THEME
                && requestCode != PICK_FONT)
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        if (requestCode == PICK_FONT) {
            importFont(data.getData());
        } else {
            importTheme(data.getData());
        }
    }

    private void importTheme(Uri uri) {
        setBusy(true, "Đang đọc file MTZ…");

        new Thread(() -> {
            try {
                ThemeImporter importer =
                        new ThemeImporter(
                                this,
                                message -> runOnUiThread(
                                        () -> setStatus(
                                                "Đang nhập",
                                                message,
                                                textPrimary
                                        )
                                )
                        );

                ImportResult result =
                        importer.importFromUri(uri);

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    chooseButton.setEnabled(true);
                    chooseButton.setAlpha(1f);
                    chooseButton.setText("Chọn tệp khác");

                    setStatus(
                            "Nhập thành công",
                            "Tên: "
                                    + result.title
                                    + "\nThành phần: "
                                    + result.resourceCount
                                    + "  •  Ảnh xem trước: "
                                    + result.previewCount,
                            successColor
                    );

                    Toast.makeText(
                            MainActivity.this,
                            "Đã nhập chủ đề MTZ",
                            Toast.LENGTH_LONG
                    ).show();

                    openImportedTheme(result.themeId);
                });
            } catch (Throwable error) {
                runOnUiThread(() -> setFailure(
                        error.getMessage() == null
                                ? error.toString()
                                : error.getMessage()
                ));
            }
        }).start();
    }

    private void importFont(Uri uri) {
        setBusy(true, "Đang đọc tệp phông chữ…");

        new Thread(() -> {
            try {
                FontImporter importer =
                        new FontImporter(
                                this,
                                message -> runOnUiThread(
                                        () -> setStatus(
                                                "Đang nhập phông chữ",
                                                message,
                                                textPrimary
                                        )
                                )
                        );

                ImportResult result = importer.importFromUri(uri);
                if (result.fontId == null
                        || result.fontId.trim().isEmpty()) {
                    throw new IllegalStateException(
                            "Không tìm thấy tài nguyên phông chữ trong tệp MTZ."
                    );
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    chooseButton.setEnabled(true);
                    chooseButton.setAlpha(1f);
                    chooseButton.setText("Chọn phông chữ khác");

                    setStatus(
                            "Nhập phông chữ thành công",
                            "Tên: "
                                    + result.title
                                    + "\nĐã thêm vào Chủ đề Xiaomi.",
                            successColor
                    );

                    Toast.makeText(
                            MainActivity.this,
                            "Đã thêm phông chữ vào Chủ đề Xiaomi",
                            Toast.LENGTH_LONG
                    ).show();

                    openImportedResource(
                            result.fontId,
                            "fonts"
                    );
                });
            } catch (Throwable error) {
                runOnUiThread(() -> setFailure(
                        error.getMessage() == null
                                ? error.toString()
                                : error.getMessage()
                ));
            }
        }).start();
    }

    private void setBusy(
            boolean busy,
            String message
    ) {
        progress.setVisibility(
                busy ? View.VISIBLE : View.GONE
        );
        chooseButton.setEnabled(!busy);
        chooseButton.setAlpha(
                busy ? 0.55f : 1f
        );
        if (busy) {
            chooseButton.setText("Đang xử lý…");
        }
        setStatus(
                busy ? "Đang xử lý" : "Sẵn sàng",
                message,
                textPrimary
        );
    }

    private void setFailure(String message) {
        progress.setVisibility(View.GONE);
        chooseButton.setEnabled(true);
        chooseButton.setAlpha(1f);
        chooseButton.setText(
                fontTabActive ? "Thử lại với phông chữ" : "Thử lại"
        );
        setStatus(
                "Nhập thất bại",
                message,
                errorColor
        );
    }

    private void setStatus(
            String title,
            String message,
            int titleColor
    ) {
        statusTitle.setText(title);
        statusTitle.setTextColor(titleColor);
        statusIndicator.setTextColor(titleColor);
        statusMessage.setText(message);
    }

    private void checkRootAsync() {
        new Thread(() -> {
            boolean granted = false;
            try {
                RootShell.Result result =
                        RootShell.run("id\n");
                granted = result.code == 0
                        && result.output.contains("uid=0");
            } catch (Throwable ignored) {
            }

            final boolean rootGranted = granted;
            runOnUiThread(() -> {
                if (rootBadge == null) {
                    return;
                }

                rootBadge.setText(
                        rootGranted
                                ? "Root sẵn sàng"
                                : "Chưa có root"
                );
                rootBadge.setTextColor(
                        rootGranted
                                ? successColor
                                : errorColor
                );
                rootBadge.setBackground(
                        roundedBackground(
                                rootGranted
                                        ? withAlpha(successColor, 36)
                                        : withAlpha(errorColor, 32),
                                24
                        )
                );
            });
        }).start();
    }

    private void openImportedTheme(String themeId) {
        openImportedResource(themeId, "theme");
    }

    private void openWebsite() {
        try {
            Intent website = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(WEBSITE_URL)
            );
            startActivity(website);
        } catch (Throwable ignored) {
            Toast.makeText(
                    this,
                    "Không mở được website",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openImportedResource(
            String localId,
            String resourceCode
    ) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(
                new ComponentName(
                        THEME_PACKAGE,
                        THEME_DETAIL_ACTIVITY
                )
        );
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(
                Uri.parse(
                        "ViewLocalResource://view.local.resource#"
                                + localId
                )
        );
        intent.putExtra(
                "REQUEST_RESOURCE_CODE",
                resourceCode
        );
        intent.putExtra(
                "REQUEST_SOURCE_TYPE",
                1
        );
        intent.putExtra(
                "REQUEST_APPLY_EVENT",
                false
        );
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        if (!tryStart(intent)) {
            openThemeManagerHome();
        }
    }

    private void openThemeManagerHome() {
        Intent explicit = new Intent(
                Intent.ACTION_MAIN
        );
        explicit.setComponent(
                new ComponentName(
                        THEME_PACKAGE,
                        THEME_HOME_ACTIVITY
                )
        );
        explicit.addCategory(
                Intent.CATEGORY_LAUNCHER
        );
        explicit.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        if (tryStart(explicit)) {
            return;
        }

        try {
            Intent launch =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    THEME_PACKAGE
                            );
            if (launch != null) {
                launch.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                );
                if (tryStart(launch)) {
                    return;
                }
            }
        } catch (Throwable ignored) {
        }

        new Thread(() -> {
            boolean opened = false;
            try {
                RootShell.Result result =
                        RootShell.run(
                                "am start --user 0 -n "
                                        + THEME_PACKAGE
                                        + "/"
                                        + THEME_HOME_ACTIVITY
                                        + "\n"
                        );
                opened = result.code == 0;
            } catch (Throwable ignored) {
            }

            final boolean success = opened;
            runOnUiThread(() -> {
                if (!success) {
                    Toast.makeText(
                            MainActivity.this,
                            "Không mở được ứng dụng Chủ đề",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }).start();
    }

    private boolean tryStart(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private TextView makeText(
            String value,
            int size,
            boolean bold,
            int color
    ) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(
                Typeface.create(
                        bold
                                ? "sans-serif-medium"
                                : "sans-serif",
                        Typeface.NORMAL
                )
        );
        return view;
    }

    private GradientDrawable roundedBackground(
            int color,
            int radiusDp
    ) {
        GradientDrawable drawable =
                new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private LinearLayout.LayoutParams margins(
            int width,
            int height,
            int left,
            int top,
            int right,
            int bottom
    ) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        width,
                        height
                );
        params.setMargins(
                dp(left),
                dp(top),
                dp(right),
                dp(bottom)
        );
        return params;
    }

    private int dp(int value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private void showStartupError(Throwable error) {
        try {
            resolvePalette();
        } catch (Throwable ignored) {
            pageColor = Color.WHITE;
            textPrimary = Color.BLACK;
            errorColor = Color.RED;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        root.setBackgroundColor(pageColor);

        TextView title = new TextView(this);
        title.setText("Không thể khởi động giao diện");
        title.setTextSize(22);
        title.setTextColor(errorColor);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView detail = new TextView(this);
        detail.setText(
                "\nVui lòng đóng và mở lại ứng dụng."
        );
        detail.setTextSize(14);
        detail.setTextColor(textPrimary);
        detail.setTextIsSelectable(true);
        root.addView(detail);

        setContentView(root);
    }
}
