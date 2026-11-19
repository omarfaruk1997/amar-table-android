package com.amarcustomer100.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private final ArrayList<JSONObject> customers = new ArrayList<>();
    private JSONArray expenses = new JSONArray();
    private LinearLayout root, listArea;
    private EditText search;
    private int selectedCustomer = -1;
    private boolean darkMode = false;
    private int accentTheme = 0;
    private static final int[] ACCENTS = {Color.rgb(15, 111, 119), Color.rgb(43, 91, 177), Color.rgb(132, 82, 155), Color.rgb(27, 132, 94), Color.rgb(190, 132, 42), Color.rgb(194, 76, 102), Color.rgb(54, 112, 145), Color.rgb(91, 103, 70)};
    private static final int REQUEST_BACKUP_EXPORT = 7101;
    private static final int REQUEST_BACKUP_IMPORT = 7102;
    private static final int REQUEST_REPORT_EXPORT = 7103;
    private static final int REQUEST_INVOICE_PDF = 7104;
    private JSONObject pendingInvoiceCustomer;
    private String pendingReportPeriod = "month";

    private static final int BG = Color.rgb(242, 246, 250);
    private static final int INK = Color.rgb(20, 32, 51);
    private static final int MUTED = Color.rgb(112, 125, 143);
    private static final int BLUE = Color.rgb(27,  70, 132);
    private static final int PALE = Color.rgb(239, 243, 249);
    private static final int TEAL = Color.rgb(8, 137, 126);
    private static final int BORDER = Color.rgb(229, 234, 242);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        darkMode = getPreferences(MODE_PRIVATE).getBoolean("dark_mode", false);
        accentTheme = getPreferences(MODE_PRIVATE).getInt("accent_theme", 0);
        loadData();
        showCustomers();
        if (!getPreferences(MODE_PRIVATE).getString("app_pin_hash", "").isEmpty()) promptPinLock();
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + .5f);
    }

    private int themedSurface(int color) {
        if (!darkMode) return color;
        if (color == Color.WHITE || color == Color.rgb(246, 247, 250)) return Color.rgb(27, 39, 56);
        if (color == Color.rgb(239, 243, 249) || color == Color.rgb(239, 245, 255)
                || color == Color.rgb(232, 242, 255) || color == Color.rgb(229, 234, 242)
                || color == Color.rgb(228, 232, 239)) return Color.rgb(37, 51, 70);
        return color;
    }

    private int themedText(int color) {
        if (!darkMode || color == Color.WHITE) return color;
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        if (max < 125 && (max - min) < 95) return Color.rgb(232, 238, 247);
        if (color == Color.rgb(112, 125, 143)) return Color.rgb(165, 178, 195);
        return color;
    }

    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(themedSurface(color));
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), darkMode ? Color.rgb(61, 76, 97) : stroke);
        return d;
    }

    private TextView label(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(themedText(color));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);
        t.setLetterSpacing(0.005f);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private TextView action(String caption, int bg, int fg, int radius) {
        TextView b = label(caption, 14, true, fg);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(11), dp(14), dp(11));
        b.setBackground(shape(bg, 0, radius));
        b.setForeground(new RippleDrawable(ColorStateList.valueOf(Color.argb(55, 20, 80, 120)), null, shape(Color.WHITE, 0, radius)));
        b.setElevation(dp(3));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private TextView primaryAction(String caption) {
        TextView button = action(caption, Color.rgb(18, 70, 91), Color.WHITE, 16);
        GradientDrawable premium = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(10, 43, 78), ACCENTS[accentTheme], Color.rgb(126, 91, 45)});
        premium.setCornerRadius(dp(16));
        premium.setStroke(dp(1), Color.rgb(55, 119, 132));
        button.setBackground(premium);
        button.setElevation(dp(6));
        button.setForeground(new RippleDrawable(ColorStateList.valueOf(Color.argb(75, 255, 255, 255)), null, shape(Color.WHITE, 0, 16)));
        return button;
    }

    private EditText field(String hint, int inputType, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(15);
        e.setTextColor(themedText(INK));
        e.setHintTextColor(Color.rgb(151, 162, 178));
        e.setInputType(inputType);
        e.setSingleLine(!multiline);
        if (multiline) {
            e.setMinLines(2);
            e.setMaxLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        }
        e.setPadding(dp(14), dp(11), dp(14), dp(11));
        e.setBackground(shape(Color.WHITE, BORDER, 16));
        e.setElevation(dp(1));
        return e;
    }

    private void base(String title, String subtitle) {
        root = column();
        root.setBackgroundColor(darkMode ? Color.rgb(15, 23, 36) : BG);
        root.setPadding(dp(16), dp(16), dp(16), dp(14));
        setContentView(root);
        getWindow().setStatusBarColor(darkMode ? Color.rgb(9, 15, 25) : Color.rgb(15, 29, 52));
        getWindow().setNavigationBarColor(darkMode ? Color.rgb(15, 23, 36) : Color.rgb(246, 247, 250));

        LinearLayout brand = row();
        brand.setPadding(dp(16), dp(18), dp(13), dp(18));
        GradientDrawable banner = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(5, 19, 43), Color.rgb(7, 91, 105), Color.rgb(157, 111, 48)});
        banner.setCornerRadius(dp(22));
        brand.setBackground(banner);
        brand.setElevation(dp(12));
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_brand_mark);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setBackground(shape(Color.rgb(7, 26, 53), Color.rgb(224, 188, 115), 17));
        mark.setPadding(dp(3), dp(3), dp(3), dp(3));
        mark.setElevation(dp(5));
        mark.setContentDescription("আমার কাস্টমার নতুন ব্র্যান্ড লোগো");
        brand.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout titles = column();
        titles.setPadding(dp(10), 0, 0, 0);
        TextView heading = label(title, 19, true, Color.WHITE);
        heading.setSingleLine(true);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titles.addView(heading);
        TextView sub = label(subtitle, 11, false, Color.rgb(230, 220, 201));
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titles.addView(sub);
        brand.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView version = label("50.0", 12, true, Color.rgb(255, 226, 163));
        version.setGravity(Gravity.CENTER);
        version.setContentDescription("অ্যাপ সংস্করণ ৫০.০");
        version.setPadding(dp(11), dp(7), dp(11), dp(7));
        version.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        version.setBackground(shape(Color.rgb(25, 48, 68), Color.rgb(190, 153, 91), 20));
        brand.addView(version, new LinearLayout.LayoutParams(-2, dp(34)));
        TextView theme = label(darkMode ? "☀" : "☾", 20, true, Color.rgb(255, 226, 163));
        theme.setGravity(Gravity.CENTER);
        theme.setContentDescription("থিম পরিবর্তন");
        theme.setBackground(shape(Color.rgb(61, 75, 94), Color.rgb(166, 132, 73), 18));
        LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        themeParams.leftMargin = dp(8);
        brand.addView(theme, themeParams);
        theme.setOnClickListener(v -> {
            darkMode = !darkMode;
            getPreferences(MODE_PRIVATE).edit().putBoolean("dark_mode", darkMode).apply();
            if (selectedCustomer >= 0 && selectedCustomer < customers.size()) showProducts(selectedCustomer);
            else showCustomers();
        });
        root.addView(brand);

        search = field(selectedCustomer >= 0 ? "⌕   পণ্যের নাম লিখে খুঁজুন..." : "⌕   নাম, ঠিকানা বা নম্বর লিখে খুঁজুন...",  android.text.InputType.TYPE_CLASS_TEXT, false);
        search.setElevation(dp(3));
        search.setBackground(shape(Color.WHITE, Color.rgb(207, 220, 234), 18));
        search.setSelectAllOnFocus(false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(52));
        sp.topMargin = dp(15);
        sp.bottomMargin = dp(12);
        root.addView(search, sp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listArea = column();
        scroll.addView(listArea);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        // Keep Backup & Restore as the final home action; show bottom navigation on inner screens.
        if (!"আমার কাস্টমার".equals(title)) addPremiumBottomNavigation(title);
    }

    private void addPremiumBottomNavigation(String currentTitle) {
        LinearLayout nav = row();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(7), dp(6), dp(7));
        GradientDrawable shell = new GradientDrawable();
        shell.setColor(darkMode ? Color.rgb(8, 17, 32) : Color.rgb(12, 27, 51));
        shell.setCornerRadius(dp(21));
        shell.setStroke(dp(1), Color.rgb(171, 132, 65));
        nav.setBackground(shell);
        nav.setElevation(dp(10));
        String[] icons = {"⌂", "▤", "◷", "✦"};
        String[] labels = {"হোম", "অ্যানালিটিক্স", "বকেয়া", "টুলস"};
        int selected = currentTitle.contains("অ্যানালিটিক্স") ? 1 : currentTitle.contains("রিমাইন্ডার") ? 2 : -1;
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(3), dp(5), dp(3), dp(5));
            boolean active = i == selected;
            if (active) {
                GradientDrawable activeBg = new GradientDrawable();
                activeBg.setColor(Color.rgb(118, 88, 43));
                activeBg.setCornerRadius(dp(15));
                activeBg.setStroke(dp(1), Color.rgb(225, 190, 116));
                item.setBackground(activeBg);
            }
            TextView icon = label(icons[i], 19, true, active ? Color.rgb(255, 231, 179) : Color.rgb(205, 184, 143));
            icon.setGravity(Gravity.CENTER);
            item.addView(icon);
            TextView caption = label(labels[i], 10, true, active ? Color.WHITE : Color.rgb(218, 224, 234));
            caption.setGravity(Gravity.CENTER);
            item.addView(caption);
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(48), 1));
            item.setClickable(true);
            item.setFocusable(true);
            item.setOnClickListener(v -> {
                if (tab == 0) showCustomers();
                else if (tab == 1) showAnalytics("month");
                else if (tab == 2) showDueReminders();
                else showPremiumToolsMenu();
            });
        }
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(62));
        np.topMargin = dp(9);
        np.bottomMargin = dp(2);
        root.addView(nav, np);
    }

    private void showPremiumToolsMenu() {
        String[] tools = {"দৈনিক খরচের হিসাব", "পণ্যের স্টক ব্যবস্থাপনা", "বিক্রয় রিপোর্ট", "থিম ও রঙ কাস্টমাইজ", "অ্যাপ PIN নিরাপত্তা", "তথ্য ব্যাকআপ ও পুনরুদ্ধার"};
        new AlertDialog.Builder(this).setTitle("ব্যবসার প্রয়োজনীয় টুলস").setItems(tools, (dialog, which) -> {
            if (which == 0) showExpenses("");
            else if (which == 1) showStockManagement();
            else if (which == 2) showBusinessReport("month");
            else if (which == 3) showThemePicker();
            else if (which == 4) managePinSecurity();
            else showBackupOptions();
        }).setNegativeButton("বন্ধ", null).show();
    }

    private void showCustomers() {
        selectedCustomer = -1;
        base("আমার কাস্টমার", "আপনার ব্যবসার স্মার্ট কাস্টমার খাতা");

        renderCustomers("");
        search.addTextChangedListener(watcher(s -> renderCustomers(s)));
    }

    private interface FilterAction { void run(String value); }

    private TextWatcher watcher(FilterAction action) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { action.run(s.toString()); }
            public void afterTextChanged(Editable e) {}
        };
    }

    private void renderCustomers(String filter) {
        listArea.removeAllViews();
        LinearLayout dashboard = row();
        dashboard.setPadding(dp(10), dp(14), dp(10), dp(14));
        dashboard.setBackground(shape(Color.rgb(255, 255, 255), Color.rgb(224, 215, 194), 20));
        dashboard.setElevation(dp(5));
        dashboard.addView(statTile("মোট কাস্টমার", String.valueOf(customers.size()), "♙", BLUE),
                new LinearLayout.LayoutParams(0, -2, 1));
        View statDivider = new View(this);
        statDivider.setBackgroundColor(BORDER);
        dashboard.addView(statDivider, new LinearLayout.LayoutParams(dp(1), dp(42)));
        dashboard.addView(statTile("মোট পণ্য", String.valueOf(countAllProducts()), "▦", TEAL),
                new LinearLayout.LayoutParams(0, -2, 1));
        View priceDivider = new View(this);
        priceDivider.setBackgroundColor(BORDER);
        dashboard.addView(priceDivider, new LinearLayout.LayoutParams(dp(1), dp(42)));
        dashboard.addView(statTile("মোট দাম", "৳ " + formatMoney(sumAllProductPrices()), "৳", Color.rgb(157, 112, 43)),
                new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams dashParams = new LinearLayout.LayoutParams(-1, -2);
        dashParams.bottomMargin = dp(14);
        listArea.addView(dashboard, dashParams);
        addPremiumOverview();
        addHomeInsights();

        TextView section = label("আপনার কাস্টমার তালিকা", 17, true, INK);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, dp(36));
        sectionParams.bottomMargin = dp(7);
        listArea.addView(section, sectionParams);
        String q = filter.toLowerCase(Locale.ROOT).trim();

        for (int i = 0; i < customers.size(); i++) {
            final int index = i;
            JSONObject c = customers.get(i);
            String name = c.optString("name");
            String address = c.optString("address");
            String phone = c.optString("phone");
            if (!(name + " " + address + " " + phone).toLowerCase(Locale.ROOT).contains(q)) continue;

            LinearLayout card = column();
            card.setPadding(dp(19), dp(18), dp(18), dp(17));
            card.setBackground(shape(Color.WHITE, Color.rgb(205, 219, 237), 24));
            card.setElevation(dp(6));
            card.setForeground(new RippleDrawable(ColorStateList.valueOf(Color.argb(38, 12, 82, 113)), null, shape(Color.WHITE, 0, 22)));
            View accentLine = new View(this);
            GradientDrawable accentGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{Color.rgb(8, 67, 112), Color.rgb(12, 151, 139), Color.rgb(210, 158, 75)});
            accentGradient.setCornerRadius(dp(3));
            accentLine.setBackground(accentGradient);
            LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(-1, dp(3));
            accentParams.bottomMargin = dp(12);
            card.addView(accentLine, accentParams);
            card.setClipToOutline(true);
            card.setAlpha(0f);
            card.setTranslationY(dp(12));
            card.animate().alpha(1f).translationY(0f).setDuration(260).start();

            LinearLayout top = row();
            TextView avatar = label(name.isEmpty() ? "?" : name.substring(0, 1), 19, true, BLUE);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(shape(Color.rgb(226, 239, 251), Color.rgb(209, 224, 241), 15));
            avatar.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            avatar.setElevation(dp(1));
            top.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));
            TextView customerName = label(name, 18, true, INK);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, dp(42), 1);
            nameParams.leftMargin = dp(11);
            top.addView(customerName, nameParams);

            card.addView(top);

            TextView addr = label("⌖  " + (address.trim().isEmpty() ? "ঠিকানা যোগ করা হয়নি" : address), 13, false,
                    address.trim().isEmpty() ? MUTED : Color.rgb(76, 91, 111));
            LinearLayout.LayoutParams adp = new LinearLayout.LayoutParams(-1, -2);
            adp.topMargin = dp(5);
            card.addView(addr, adp);
            if (!phone.trim().isEmpty()) {
                TextView mobile = label("☎  " + phone, 13, false, TEAL);
                LinearLayout.LayoutParams mobileParams = new LinearLayout.LayoutParams(-1, -2);
                mobileParams.topMargin = dp(4);
                card.addView(mobile, mobileParams);
            }

            TextView count = label("▦  " + productCount(c) + "টি পণ্য", 12, true, BLUE);
            LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-1, dp(30));
            countParams.topMargin = dp(4);
            card.addView(count, countParams);

            LinearLayout finance = row();
            finance.setPadding(dp(10), dp(8), dp(10), dp(8));
            finance.setBackground(shape(darkMode ? Color.rgb(31, 47, 62) : Color.rgb(241, 247, 250), darkMode ? Color.rgb(57, 76, 92) : Color.rgb(225, 235, 240), 12));
            TextView dueInfo = label("বকেয়া ৳ " + formatMoney(customerDue(c)), 12, true,
                    customerDue(c) > 0 ? (darkMode ? Color.rgb(255, 174, 164) : Color.rgb(178, 63, 55)) : TEAL);
            finance.addView(dueInfo, new LinearLayout.LayoutParams(0, -2, 1));
            TextView salesInfo = label("বিক্রি ৳ " + formatMoney(sumTransactions(c, "sale")), 11, true, darkMode ? Color.rgb(180, 210, 235) : BLUE);
            salesInfo.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            finance.addView(salesInfo, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams financeParams = new LinearLayout.LayoutParams(-1, -2);
            financeParams.topMargin = dp(7);
            card.addView(finance, financeParams);

            LinearLayout actions = row();
            actions.setGravity(Gravity.CENTER);
            actions.setPadding(0, dp(8), 0, dp(2));
            TextView edit = action("✎", Color.rgb(232, 242, 255), Color.rgb(25, 95, 210), 13);
            TextView delete = action("🗑", Color.rgb(255, 239, 240), Color.rgb(190, 45, 55), 13);
            TextView call = action("☎", Color.rgb(229, 248, 244), Color.rgb(13, 148, 110), 13);
            edit.setTextSize(25);
            delete.setTextSize(21);
            call.setTextSize(23);
            edit.setContentDescription("সম্পাদনা");
            delete.setContentDescription("মুছুন");
            call.setContentDescription("ফোন করুন");
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(54), dp(48));
            bp.setMargins(dp(7), dp(2), dp(7), 0);
            actions.addView(edit, bp);
            actions.addView(delete, bp);
            actions.addView(call, bp);
            card.addView(actions);
            call.setOnClickListener(v -> dialCustomer(phone));

            card.setOnClickListener(v -> showProducts(index));
            edit.setOnClickListener(v -> customerDialog(index));
            delete.setOnClickListener(v -> confirmDeleteCustomer(index));

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(10);
            listArea.addView(card, cp);
        }

        if (listArea.getChildCount() == 1) {
            boolean noCustomers = customers.isEmpty() && q.isEmpty();
            LinearLayout empty = emptyState(noCustomers ? "♙" : "⌕",
                    noCustomers ? "এখনও কোনো কাস্টমার নেই" : "কোনো কাস্টমার পাওয়া যায়নি",
                    noCustomers ? "নতুন কাস্টমার যোগ করে আপনার তালিকা শুরু করুন।" : "নাম, ঠিকানা বা মোবাইল নম্বর দিয়ে আবার খুঁজুন।");
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(-1, -2);
            emptyParams.topMargin = dp(12);
            listArea.addView(empty, emptyParams);
        }
        // Place the requested home actions at the bottom of the customer list.
        LinearLayout homeActions = column();
        homeActions.setPadding(0, dp(16), 0, dp(8));
        TextView shortcutsTitle = label("আপনার ব্যবসার স্মার্ট টুলস", 17, true, INK);
        LinearLayout.LayoutParams shortcutsTitleParams = new LinearLayout.LayoutParams(-1, dp(34));
        shortcutsTitleParams.bottomMargin = dp(7);
        homeActions.addView(shortcutsTitle, shortcutsTitleParams);
        TextView add = primaryAction("＋   নতুন কাস্টমার যোগ করুন");
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(54));
        ap.bottomMargin = dp(10);
        homeActions.addView(add, ap);
        add.setOnClickListener(v -> customerDialog(-1));

        TextView reports = action("▤   ব্যবসার বিক্রয় রিপোর্ট",
                darkMode ? Color.rgb(27, 65, 66) : Color.rgb(226, 243, 239),
                darkMode ? Color.rgb(145, 226, 210) : Color.rgb(12, 112, 100), 15);
        LinearLayout.LayoutParams reportParams = new LinearLayout.LayoutParams(-1, dp(48));
        reportParams.bottomMargin = dp(9);
        homeActions.addView(reports, reportParams);
        reports.setOnClickListener(v -> showBusinessReport("month"));

        TextView expenseButton = action("▣   দৈনিক খরচের হিসাব",
                darkMode ? Color.rgb(57, 43, 43) : Color.rgb(255, 238, 232),
                darkMode ? Color.rgb(255, 190, 170) : Color.rgb(164, 72, 49), 15);
        LinearLayout.LayoutParams expenseParams = new LinearLayout.LayoutParams(-1, dp(48));
        expenseParams.bottomMargin = dp(9);
        homeActions.addView(expenseButton, expenseParams);
        expenseButton.setOnClickListener(v -> showExpenses(""));

        TextView analytics = action("▥   স্মার্ট ব্যবসা অ্যানালিটিক্স", darkMode ? Color.rgb(37, 51, 70) : Color.rgb(232, 242, 255), darkMode ? Color.rgb(186, 216, 255) : BLUE, 15);
        LinearLayout.LayoutParams analyticsParams = new LinearLayout.LayoutParams(-1, dp(48));
        analyticsParams.bottomMargin = dp(9);
        homeActions.addView(analytics, analyticsParams);
        analytics.setOnClickListener(v -> showAnalytics("month"));

        TextView reminders = action("◷   বকেয়া ও পেমেন্ট রিমাইন্ডার", darkMode ? Color.rgb(57, 43, 43) : Color.rgb(255, 242, 232), darkMode ? Color.rgb(255, 190, 170) : Color.rgb(164, 72, 49), 15);
        LinearLayout.LayoutParams reminderParams = new LinearLayout.LayoutParams(-1, dp(48));
        reminderParams.bottomMargin = dp(9);
        homeActions.addView(reminders, reminderParams);
        reminders.setOnClickListener(v -> showDueReminders());

        TextView themes = action("✦   থিম ও রঙ কাস্টমাইজ করুন", darkMode ? Color.rgb(48, 43, 65) : Color.rgb(243, 237, 252), darkMode ? Color.rgb(216, 190, 255) : Color.rgb(106, 72, 150), 15);
        LinearLayout.LayoutParams themeMenuParams = new LinearLayout.LayoutParams(-1, dp(48));
        themeMenuParams.bottomMargin = dp(9);
        homeActions.addView(themes, themeMenuParams);
        themes.setOnClickListener(v -> showThemePicker());

        TextView security = action("▣   অ্যাপ PIN নিরাপত্তা", darkMode ? Color.rgb(34, 57, 75) : Color.rgb(231, 242, 251), darkMode ? Color.rgb(176, 218, 245) : BLUE, 15);
        LinearLayout.LayoutParams securityParams = new LinearLayout.LayoutParams(-1, dp(48));
        securityParams.bottomMargin = dp(9);
        homeActions.addView(security, securityParams);
        security.setOnClickListener(v -> managePinSecurity());

        // Backup and restore must always remain the final home-screen action.
        TextView stockButton = action("▦   পণ্যের স্টক ব্যবস্থাপনা",
                darkMode ? Color.rgb(34, 57, 75) : Color.rgb(231, 242, 251),
                darkMode ? Color.rgb(176, 218, 245) : Color.rgb(27, 70, 132), 15);
        LinearLayout.LayoutParams stockParams = new LinearLayout.LayoutParams(-1, dp(48));
        stockParams.bottomMargin = dp(9);
        homeActions.addView(stockButton, stockParams);
        stockButton.setOnClickListener(v -> showStockManagement());

        TextView backup = action("☁   তথ্য ব্যাকআপ ও পুনরুদ্ধার",
                darkMode ? Color.rgb(55, 48, 36) : Color.rgb(247, 241, 229),
                darkMode ? Color.rgb(255, 226, 163) : Color.rgb(112, 80, 29), 15);
        LinearLayout.LayoutParams backupParams = new LinearLayout.LayoutParams(-1, dp(48));
        backupParams.bottomMargin = dp(8);
        homeActions.addView(backup, backupParams);
        backup.setOnClickListener(v -> showBackupOptions());

        LinearLayout.LayoutParams homeActionsParams = new LinearLayout.LayoutParams(-1, -2);
        homeActionsParams.topMargin = dp(8);
        listArea.addView(homeActions, homeActionsParams);
    }

    private void addPremiumOverview() {
        double sales = 0;
        double due = 0;
        for (JSONObject customer : customers) {
            sales += sumTransactions(customer, "sale");
            due += customerDue(customer);
        }
        LinearLayout hero = column();
        hero.setPadding(dp(20), dp(19), dp(20), dp(18));
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                darkMode
                    ? new int[]{Color.rgb(15, 25, 48), ACCENTS[accentTheme], Color.rgb(9, 117, 123)}
                    : new int[]{Color.rgb(5, 20, 48), ACCENTS[accentTheme], Color.rgb(9, 139, 133), Color.rgb(185, 132, 55)});
        gradient.setCornerRadius(dp(26));
        gradient.setStroke(dp(1), darkMode ? Color.rgb(67, 103, 116) : Color.rgb(183, 157, 111));
        hero.setBackground(gradient);
        hero.setElevation(dp(10));

        LinearLayout heading = row();
        TextView title = label("আপনার ব্যবসার ড্যাশবোর্ড", 16, true, Color.WHITE);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = label("PREMIUM", 9, true, Color.rgb(255, 225, 164));
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.argb(45, 255, 255, 255));
        badgeBg.setCornerRadius(dp(20));
        badgeBg.setStroke(dp(1), Color.argb(100, 255, 225, 164));
        badge.setBackground(badgeBg);
        heading.addView(badge);
        hero.addView(heading);

        LinearLayout metrics = row();
        metrics.setPadding(0, dp(13), 0, dp(2));
        addHeroMetric(metrics, "মোট বিক্রি", "৳ " + formatMoney(sales));
        View dividerOne = new View(this);
        dividerOne.setBackgroundColor(Color.argb(95, 255, 255, 255));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(dp(1), dp(42));
        dividerParams.setMargins(dp(8), 0, dp(8), 0);
        metrics.addView(dividerOne, dividerParams);
        addHeroMetric(metrics, "মোট বকেয়া", "৳ " + formatMoney(due));
        View dividerTwo = new View(this);
        dividerTwo.setBackgroundColor(Color.argb(95, 255, 255, 255));
        metrics.addView(dividerTwo, dividerParams);
        addHeroMetric(metrics, "কম স্টক", String.valueOf(countLowStock()) + " টি");
        hero.addView(metrics);
        TextView hint = label("কাস্টমার • বিক্রি • বকেয়া • মজুত — সব এক জায়গায়", 11, false,
                Color.rgb(226, 232, 228));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(9);
        hero.addView(hint, hintParams);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(15);
        listArea.addView(hero, params);
        hero.setAlpha(0f);
        hero.setTranslationY(dp(8));
        hero.animate().alpha(1f).translationY(0f).setDuration(320).start();
    }

    private void addHomeInsights() {
        double sales = sumAllTransactions("sale");
        double expenseTotal = 0;
        for (int i=0;i<expenses.length();i++) { JSONObject e=expenses.optJSONObject(i); if(e!=null) expenseTotal+=parseMoney(e.optString("amount")); }
        final double expenseTotalFinal = expenseTotal;
        LinearLayout panel=column(); panel.setPadding(dp(16),dp(15),dp(16),dp(14));
        panel.setBackground(shape(darkMode?Color.rgb(27,39,56):Color.WHITE,BORDER,22)); panel.setElevation(dp(5));
        LinearLayout head=row(); head.addView(label("ব্যবসার এক নজরের সারাংশ",15,true,INK),new LinearLayout.LayoutParams(0,-2,1)); head.addView(label("LIVE",10,true,ACCENTS[accentTheme])); panel.addView(head);
        panel.addView(label("বিক্রি বনাম খরচ",11,false,MUTED));
        View chart=new View(this) {
            private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas); float w=getWidth(),h=getHeight(),base=h-dp(8);
                double max=Math.max(1,Math.max(sales,expenseTotalFinal));
                float sh=(float)(sales/max)*(h-dp(24)), eh=(float)(expenseTotalFinal/max)*(h-dp(24));
                p.setColor(ACCENTS[accentTheme]); canvas.drawRoundRect(dp(30),base-sh,dp(88),base,dp(9),dp(9),p);
                p.setColor(Color.rgb(204,139,65)); canvas.drawRoundRect(w-dp(88),base-eh,w-dp(30),base,dp(9),dp(9),p);
                p.setColor(themedText(MUTED)); p.setTextSize(dp(10)); canvas.drawText("বিক্রি",dp(35),h-dp(1),p); canvas.drawText("খরচ",w-dp(82),h-dp(1),p);
            }
        };
        panel.addView(chart,new LinearLayout.LayoutParams(-1,dp(94)));
        LinearLayout foot=row(); foot.addView(label("বিক্রি ৳ "+formatMoney(sales),11,true,ACCENTS[accentTheme]),new LinearLayout.LayoutParams(0,-2,1)); foot.addView(label("খরচ ৳ "+formatMoney(expenseTotal),11,true,Color.rgb(190,120,55))); panel.addView(foot);
        TextView quick=action("✦   বিস্তারিত অ্যানালিটিক্স দেখুন",darkMode?Color.rgb(35,54,72):Color.rgb(235,244,247),darkMode?Color.rgb(210,229,244):BLUE,14);
        LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,dp(44)); qp.topMargin=dp(10); panel.addView(quick,qp); quick.setOnClickListener(v->showAnalytics("month"));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2); pp.bottomMargin=dp(14); listArea.addView(panel,pp);
    }

    private void addHeroMetric(LinearLayout parent, String caption, String value) {
        LinearLayout metric = column();
        TextView amount = label(value, 14, true, Color.WHITE);
        amount.setMaxLines(1);
        amount.setTextSize(13);
        metric.addView(amount);
        TextView metricLabel = label(caption, 10, false, Color.rgb(221, 230, 231));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(5);
        metric.addView(metricLabel, labelParams);
        parent.addView(metric, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private int countLowStock() {
        int count = 0;
        for (JSONObject customer : customers) {
            JSONArray products = customer.optJSONArray("products");
            if (products == null) continue;
            for (int i = 0; i < products.length(); i++) {
                JSONObject product = products.optJSONObject(i);
                if (product != null && parseMoney(product.optString("stock", "0")) <= 3) count++;
            }
        }
        return count;
    }

    private void showStockManagement() {
        selectedCustomer = -1;
        base("পণ্যের স্টক ব্যবস্থাপনা", "মজুত দেখুন, পরিমাণ সংশোধন ও কম স্টক শনাক্ত করুন");
        search.setVisibility(View.GONE);
        TextView back = action("←   হোমে ফিরুন", Color.WHITE, INK, 12);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(42));
        backParams.bottomMargin = dp(12);
        root.addView(back, 2, backParams);
        back.setOnClickListener(v -> showCustomers());

        LinearLayout summary = row();
        summary.setPadding(dp(12), dp(14), dp(12), dp(14));
        summary.setBackground(shape(Color.WHITE, BORDER, 18));
        int total = countAllProducts();
        int low = countLowStock();
        summary.addView(statTile("মোট পণ্য", String.valueOf(total), "▦", BLUE),
                new LinearLayout.LayoutParams(0, -2, 1));
        summary.addView(statTile("কম / শেষ স্টক", String.valueOf(low), "!", Color.rgb(190, 85, 55)),
                new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.bottomMargin = dp(12);
        root.addView(summary, summaryParams);

        listArea.removeAllViews();
        listArea.addView(label("সব কাস্টমারের পণ্যের মজুত", 16, true, INK));
        boolean found = false;
        for (int ci = 0; ci < customers.size(); ci++) {
            JSONObject customer = customers.get(ci);
            JSONArray products = customer.optJSONArray("products");
            if (products == null) continue;
            for (int pi = 0; pi < products.length(); pi++) {
                JSONObject product = products.optJSONObject(pi);
                if (product == null) continue;
                found = true;
                String stock = product.optString("stock", "0");
                boolean lowStock = parseMoney(stock) <= 3;
                LinearLayout card = column();
                card.setPadding(dp(14), dp(13), dp(14), dp(13));
                card.setBackground(shape(Color.WHITE, BORDER, 17));
                card.setElevation(dp(3));
                LinearLayout top = row();
                top.addView(label(product.optString("name"), 15, true, INK),
                        new LinearLayout.LayoutParams(0, -2, 1));
                top.addView(label("মজুত: " + stock + " টি", 14, true,
                        lowStock ? Color.rgb(190, 75, 55) : TEAL));
                card.addView(top);
                TextView owner = label("কাস্টমার: " + customer.optString("name")
                        + (lowStock ? "   •   স্টক কম" : ""), 12, false, MUTED);
                LinearLayout.LayoutParams ownerParams = new LinearLayout.LayoutParams(-1, -2);
                ownerParams.topMargin = dp(5);
                card.addView(owner, ownerParams);
                TextView adjust = action("＋   স্টক যোগ / পরিমাণ পরিবর্তন",
                        darkMode ? Color.rgb(37, 51, 70) : Color.rgb(232, 242, 255),
                        darkMode ? Color.rgb(186, 216, 255) : BLUE, 12);
                LinearLayout.LayoutParams adjustParams = new LinearLayout.LayoutParams(-1, dp(40));
                adjustParams.topMargin = dp(9);
                card.addView(adjust, adjustParams);
                final int customerIndex = ci, productIndex = pi;
                adjust.setOnClickListener(v -> stockDialog(customerIndex, productIndex));
                TextView historyButton = action("◷   স্টক পরিবর্তনের ইতিহাস",
                        darkMode ? Color.rgb(48, 53, 68) : Color.rgb(245, 246, 250),
                        darkMode ? Color.rgb(205, 215, 230) : MUTED, 12);
                LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(-1, dp(38));
                historyParams.topMargin = dp(6);
                card.addView(historyButton, historyParams);
                historyButton.setOnClickListener(v -> showStockHistory(product));
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
                cardParams.bottomMargin = dp(9);
                listArea.addView(card, cardParams);
            }
        }
        if (!found) listArea.addView(emptyState("▦", "কোনো পণ্য নেই", "কাস্টমারের তালিকা থেকে পণ্য যোগ করুন।"));
    }

    private void stockDialog(int customerIndex, int productIndex) {
        JSONObject customer = customers.get(customerIndex);
        JSONArray products = customer.optJSONArray("products");
        if (products == null) return;
        JSONObject product = products.optJSONObject(productIndex);
        if (product == null) return;
        int oldStock = (int) parseMoney(product.optString("stock", "0"));
        EditText quantity = field("নতুন মোট মজুতের পরিমাণ", android.text.InputType.TYPE_CLASS_NUMBER, false);
        quantity.setText(String.valueOf(oldStock));
        EditText reason = field("কারণ (যেমন: নতুন মাল কেনা / সংশোধন)", android.text.InputType.TYPE_CLASS_TEXT, false);
        LinearLayout form = column();
        form.addView(quantity);
        addGap(form, 8);
        form.addView(reason);
        new AlertDialog.Builder(this).setTitle(product.optString("name") + " — স্টক")
            .setMessage("বর্তমান মোট মজুতের নতুন সংখ্যা লিখুন। পরিবর্তনের ইতিহাসও সংরক্ষণ হবে।")
            .setView(form)
            .setPositiveButton("সংরক্ষণ", (d, w) -> {
                String value = quantity.getText().toString().trim();
                if (value.isEmpty() || parseMoney(value) < 0) {
                    Toast.makeText(this, "সঠিক স্টকের পরিমাণ লিখুন।", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    int newStock = (int) parseMoney(value);
                    int delta = newStock - oldStock;
                    product.put("stock", String.valueOf(newStock));
                    if (delta != 0) recordStockMovement(product, delta,
                            reason.getText().toString().trim().isEmpty() ? "স্টক সংশোধন" : reason.getText().toString().trim());
                    saveData();
                    showStockManagement();
                    Toast.makeText(this, "স্টকের পরিমাণ আপডেট হয়েছে।", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "স্টক আপডেট করা যায়নি।", Toast.LENGTH_SHORT).show();
                }
            }).setNegativeButton("বাতিল", null).show();
    }

    private void recordStockMovement(JSONObject product, int delta, String reason) {
        try {
            JSONArray history = product.optJSONArray("stockHistory");
            if (history == null) history = new JSONArray();
            JSONObject movement = new JSONObject();
            movement.put("delta", delta);
            movement.put("reason", reason);
            movement.put("date", new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                    .format(new java.util.Date()));
            movement.put("stockAfter", product.optString("stock", "0"));
            history.put(movement);
            product.put("stockHistory", history);
        } catch (Exception ignored) {}
    }

    private void showStockHistory(JSONObject product) {
        JSONArray history = product.optJSONArray("stockHistory");
        if (history == null || history.length() == 0) {
            new AlertDialog.Builder(this).setTitle("স্টক ইতিহাস")
                    .setMessage("এই পণ্যের জন্য এখনো কোনো স্টক পরিবর্তনের ইতিহাস নেই।")
                    .setPositiveButton("ঠিক আছে", null).show();
            return;
        }
        StringBuilder details = new StringBuilder();
        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject item = history.optJSONObject(i);
            if (item == null) continue;
            int delta = item.optInt("delta");
            details.append(item.optString("date")).append("\n")
                    .append(delta > 0 ? "+" : "").append(delta).append(" টি — ")
                    .append(item.optString("reason")).append("\n")
                    .append("পরিবর্তনের পর মজুত: ").append(item.optString("stockAfter")).append(" টি\n\n");
        }
        new AlertDialog.Builder(this).setTitle(product.optString("name") + " — স্টক ইতিহাস")
                .setMessage(details.toString()).setPositiveButton("বন্ধ", null).show();
    }

        private LinearLayout emptyState(String symbol, String heading, String message) {
        LinearLayout panel = column();
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(20), dp(25), dp(20), dp(25));
        panel.setBackground(shape(Color.WHITE, Color.rgb(222, 229, 239), 22));
        panel.setElevation(dp(3));

        TextView icon = label(symbol, 30, true, Color.rgb(15, 111, 119));
        icon.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(231, 244, 247), Color.rgb(242, 237, 224)});
        iconBg.setCornerRadius(dp(22));
        icon.setBackground(iconBg);
        panel.addView(icon, new LinearLayout.LayoutParams(dp(66), dp(66)));

        TextView title = label(heading, 16, true, INK);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(13);
        panel.addView(title, titleParams);

        TextView detail = label(message, 13, false, MUTED);
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(6);
        panel.addView(detail, detailParams);
        return panel;
    }

    private LinearLayout statTile(String heading, String value, String symbol, int accent) {
        LinearLayout tile = column();
        tile.setGravity(Gravity.CENTER);
        TextView icon = label(symbol, 18, true, accent);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(Color.rgb(232, 241, 252), Color.rgb(222, 232, 244), 12));
        icon.setElevation(dp(1));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(32)));
        TextView number = label(value, 21, true, INK);
        number.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(5);
        tile.addView(number, np);
        TextView caption = label(heading, 11, false, MUTED);
        caption.setGravity(Gravity.CENTER);
        tile.addView(caption);
        return tile;
    }

    private int countAllProducts() {
        int total = 0;
        for (JSONObject c : customers) total += productCount(c);
        return total;
    }

    private double sumAllProductPrices() {
        double total = 0;
        for (JSONObject customer : customers) total += sumProductPrices(customer);
        return total;
    }


    private JSONArray transactions(JSONObject customer) {
        JSONArray list = customer.optJSONArray("transactions");
        if (list == null) {
            list = new JSONArray();
            try { customer.put("transactions", list); } catch (Exception ignored) {}
        }
        return list;
    }

    private double parseMoney(String raw) {
        StringBuilder normalized = new StringBuilder();
        if (raw == null) return 0;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch >= 0x09E6 && ch <= 0x09EF) normalized.append((char)('0' + ch - 0x09E6));
            else if (ch >= 0x0660 && ch <= 0x0669) normalized.append((char)('0' + ch - 0x0660));
            else if (ch != ',' && ch != '৳' && ch != ' ') normalized.append(ch);
        }
        try { return Double.parseDouble(normalized.toString()); }
        catch (Exception ignored) { return 0; }
    }

    private double sumTransactions(JSONObject customer, String type) {
        double total = 0;
        JSONArray list = transactions(customer);
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item != null && type.equals(item.optString("type")))
                total += parseMoney(item.optString("amount"));
        }
        return total;
    }

    private double customerDue(JSONObject customer) {
        return sumTransactions(customer, "sale") - sumTransactions(customer, "payment");
    }

    private double sumAllTransactions(String type) {
        double total = 0;
        for (JSONObject customer : customers) total += sumTransactions(customer, type);
        return total;
    }

    private double sumAllDue() {
        double total = 0;
        for (JSONObject customer : customers) total += customerDue(customer);
        return total;
    }

    private void transactionDialog(String type) {
        if (selectedCustomer < 0 || selectedCustomer >= customers.size()) return;
        JSONObject customer = customers.get(selectedCustomer);
        boolean sale = "sale".equals(type);
        LinearLayout form = column();
        form.setPadding(dp(4), dp(5), dp(4), 0);
        EditText amount = field("টাকার পরিমাণ", android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL, false);
        EditText note = field(sale ? "বিবরণ / পণ্যের নাম (ঐচ্ছিক)" : "জমার বিবরণ (ঐচ্ছিক)",
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        Spinner productPicker = null;
        EditText quantity = null;
        JSONArray products = customer.optJSONArray("products");
        if (sale && products != null && products.length() > 0) {
            ArrayList<String> productNames = new ArrayList<>();
            productNames.add("পণ্য নির্বাচন (ঐচ্ছিক — স্টক কমবে)");
            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.optJSONObject(i);
                productNames.add(p == null ? "অজানা পণ্য" : p.optString("name"));
            }
            productPicker = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, productNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            productPicker.setAdapter(adapter);
            quantity = field("বিক্রির পরিমাণ (ডিফল্ট ১)", android.text.InputType.TYPE_CLASS_NUMBER, false);
            quantity.setText("1");
            form.addView(label("স্টক থেকে বিক্রিত পণ্য", 13, true, INK));
            addGap(form, 5);
            form.addView(productPicker);
            addGap(form, 8);
            form.addView(quantity);
            addGap(form, 10);
        }
        form.addView(amount);
        addGap(form, 10);
        form.addView(note);
        final Spinner selectedProduct = productPicker;
        final EditText soldQuantity = quantity;
        new AlertDialog.Builder(this)
            .setTitle(sale ? "নতুন বিক্রি / বাকি যোগ" : "টাকা জমা যোগ")
            .setView(form)
            .setPositiveButton("সংরক্ষণ", (dialog, which) -> {
                String raw = amount.getText().toString().trim();
                if (parseMoney(raw) <= 0) {
                    Toast.makeText(this, "সঠিক টাকার পরিমাণ লিখুন।", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    JSONObject linkedProduct = null;
                    int sold = 0;
                    if (sale && selectedProduct != null && selectedProduct.getSelectedItemPosition() > 0) {
                        int productIndex = selectedProduct.getSelectedItemPosition() - 1;
                        linkedProduct = products.optJSONObject(productIndex);
                        sold = soldQuantity == null ? 1 : Integer.parseInt(soldQuantity.getText().toString().trim());
                        if (linkedProduct == null || sold <= 0) {
                            Toast.makeText(this, "সঠিক পণ্য ও পরিমাণ নির্বাচন করুন।", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int available = (int) parseMoney(linkedProduct.optString("stock", "0"));
                        if (sold > available) {
                            Toast.makeText(this, "পর্যাপ্ত স্টক নেই। বর্তমান মজুত: " + available + " টি", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    JSONObject entry = new JSONObject();
                    entry.put("type", type);
                    entry.put("amount", raw);
                    String details = note.getText().toString().trim();
                    if (linkedProduct != null) {
                        String productName = linkedProduct.optString("name");
                        entry.put("productName", productName);
                        entry.put("quantity", sold);
                        if (details.isEmpty()) details = productName + " × " + sold;
                        int remaining = (int) parseMoney(linkedProduct.optString("stock", "0")) - sold;
                        linkedProduct.put("stock", String.valueOf(remaining));
                        recordStockMovement(linkedProduct, -sold, "বিক্রি: " + productName);
                    }
                    entry.put("note", details);
                    entry.put("date", new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                            .format(new java.util.Date()));
                    transactions(customer).put(entry);
                    saveData();
                    showProducts(selectedCustomer);
                    Toast.makeText(this, sale ? "বিক্রির হিসাব যোগ হয়েছে।" : "টাকা জমার হিসাব যোগ হয়েছে।",
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "হিসাব সংরক্ষণ করা যায়নি।", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }


    private long reportStart(String period) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        if ("week".equals(period)) cal.set(java.util.Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        else if ("month".equals(period)) cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    private boolean inReportPeriod(String date, String period) {
        if ("all".equals(period)) return true;
        try {
            java.util.Date parsed = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).parse(date);
            return parsed != null && parsed.getTime() >= reportStart(period);
        } catch (Exception ignored) { return false; }
    }

    private double periodTotal(String type, String period) {
        double total = 0;
        for (JSONObject customer : customers) {
            JSONArray entries = transactions(customer);
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.optJSONObject(i);
                if (entry != null && type.equals(entry.optString("type")) && inReportPeriod(entry.optString("date"), period))
                    total += parseMoney(entry.optString("amount"));
            }
        }
        return total;
    }

    private double periodExpenseTotal(String period) {
        double total = 0;
        for (int i = 0; i < expenses.length(); i++) {
            JSONObject item = expenses.optJSONObject(i);
            if (item != null && inReportPeriod(item.optString("date"), period))
                total += parseMoney(item.optString("amount"));
        }
        return total;
    }

    private int periodExpenseCount(String period) {
        int count = 0;
        for (int i = 0; i < expenses.length(); i++) {
            JSONObject item = expenses.optJSONObject(i);
            if (item != null && inReportPeriod(item.optString("date"), period)) count++;
        }
        return count;
    }

    private void saveExpenses() {
        getSharedPreferences("amar_customer", MODE_PRIVATE).edit().putString("expenses", expenses.toString()).apply();
    }

    private void showExpenses(String filter) {
        selectedCustomer = -1;
        base("দৈনিক খরচের হিসাব", "ব্যবসার খরচ লিখুন ও নিয়ন্ত্রণ করুন");
        TextView add = primaryAction("＋   নতুন খরচ যোগ করুন");
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(-1, dp(50));
        addParams.topMargin = dp(8);
        addParams.bottomMargin = dp(10);
        root.addView(add, 2, addParams);
        add.setOnClickListener(v -> expenseDialog(-1));

        TextView back = action("←   হোমে ফিরুন", Color.WHITE, INK, 12);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(40));
        backParams.bottomMargin = dp(10);
        root.addView(back, 3, backParams);
        back.setOnClickListener(v -> showCustomers());

        LinearLayout totals = row();
        totals.setPadding(dp(12), dp(13), dp(12), dp(13));
        totals.setBackground(shape(Color.WHITE, BORDER, 17));
        totals.addView(statTile("সর্বমোট খরচ", "৳ " + formatMoney(periodExpenseTotal("all")), "−", Color.rgb(164, 72, 49)),
                new LinearLayout.LayoutParams(0, -2, 1));
        totals.addView(statTile("এই মাসে", "৳ " + formatMoney(periodExpenseTotal("month")), "▣", BLUE),
                new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(-1, -2);
        totalParams.bottomMargin = dp(12);
        root.addView(totals, totalParams);

        String q = filter == null ? "" : filter.toLowerCase(Locale.ROOT).trim();
        listArea.removeAllViews();
        listArea.addView(label("খরচের তালিকা  ·  " + expenses.length() + "টি", 16, true, INK));
        for (int i = expenses.length() - 1; i >= 0; i--) {
            JSONObject item = expenses.optJSONObject(i);
            if (item == null) continue;
            final int index = i;
            String title = item.optString("title");
            String category = item.optString("category");
            String date = item.optString("date");
            if (!(title + " " + category + " " + date).toLowerCase(Locale.ROOT).contains(q)) continue;
            LinearLayout card = column();
            card.setPadding(dp(14), dp(13), dp(14), dp(12));
            card.setBackground(shape(Color.WHITE, BORDER, 17));
            card.setElevation(dp(3));
            LinearLayout top = row();
            top.addView(label(title, 15, true, INK), new LinearLayout.LayoutParams(0, -2, 1));
            top.addView(label("৳ " + formatMoney(parseMoney(item.optString("amount"))), 15, true,
                    darkMode ? Color.rgb(255, 190, 170) : Color.rgb(164, 72, 49)));
            card.addView(top);
            TextView detail = label(category + "  •  " + date, 12, false, MUTED);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
            detailParams.topMargin = dp(5);
            card.addView(detail, detailParams);
            LinearLayout actions = row();
            actions.setGravity(Gravity.RIGHT);
            TextView edit = action("✎ সম্পাদনা", darkMode ? Color.rgb(37, 51, 70) : Color.rgb(232, 242, 255), BLUE, 11);
            TextView remove = action("মুছুন", Color.rgb(255, 239, 240), Color.rgb(190, 45, 55), 11);
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-2, dp(38));
            actionParams.setMargins(dp(5), dp(8), 0, 0);
            actions.addView(edit, actionParams);
            actions.addView(remove, actionParams);
            card.addView(actions);
            edit.setOnClickListener(v -> expenseDialog(index));
            remove.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("এই খরচের তথ্য মুছে ফেলবেন?")
                .setPositiveButton("মুছুন", (d, w) -> {
                    expenses.remove(index);
                    saveExpenses();
                    showExpenses("");
                }).setNegativeButton("বাতিল", null).show());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(9);
            listArea.addView(card, cardParams);
        }
        if (listArea.getChildCount() == 1) {
            listArea.addView(emptyState("▣", "এখনও কোনো খরচ যোগ করা হয়নি",
                    "দোকান ভাড়া, যাতায়াত, কর্মচারী বা অন্যান্য খরচ লিখে রাখুন।"));
        }
        search.setHint("খরচের নাম, খাত বা তারিখ লিখে খুঁজুন...");
        search.setText(filter == null ? "" : filter);
        search.addTextChangedListener(watcher(this::showExpenses));
    }

    private void expenseDialog(int index) {
        LinearLayout form = column();
        form.setPadding(dp(4), dp(5), dp(4), 0);
        EditText title = field("খরচের নাম / বিবরণ", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        EditText amount = field("টাকার পরিমাণ", android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL, false);
        Spinner category = new Spinner(this);
        String[] categories = {"দোকান ভাড়া", "যাতায়াত", "কর্মচারী বেতন", "বিদ্যুৎ / গ্যাস", "মালামাল কেনা", "ইন্টারনেট / ফোন", "অন্যান্য"};
        category.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories));
        form.addView(title);
        addGap(form, 9);
        form.addView(amount);
        addGap(form, 7);
        form.addView(category, new LinearLayout.LayoutParams(-1, dp(48)));
        if (index >= 0 && index < expenses.length()) {
            JSONObject item = expenses.optJSONObject(index);
            if (item != null) {
                title.setText(item.optString("title"));
                amount.setText(item.optString("amount"));
                for (int i = 0; i < categories.length; i++)
                    if (categories[i].equals(item.optString("category"))) category.setSelection(i);
            }
        }
        new AlertDialog.Builder(this).setTitle(index < 0 ? "নতুন ব্যবসায়িক খরচ" : "খরচ পরিবর্তন")
            .setView(form)
            .setPositiveButton("সংরক্ষণ", (d, w) -> {
                String t = title.getText().toString().trim();
                String a = amount.getText().toString().trim();
                if (t.isEmpty() || parseMoney(a) <= 0) {
                    Toast.makeText(this, "খরচের বিবরণ ও সঠিক টাকা লিখুন।", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    JSONObject item = index < 0 ? new JSONObject() : expenses.getJSONObject(index);
                    item.put("title", t);
                    item.put("amount", a);
                    item.put("category", category.getSelectedItem().toString());
                    if (index < 0) item.put("date", new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new java.util.Date()));
                    if (index < 0) expenses.put(item);
                    saveExpenses();
                    showExpenses("");
                    Toast.makeText(this, "খরচের হিসাব সংরক্ষণ হয়েছে।", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "খরচ সংরক্ষণ করা যায়নি।", Toast.LENGTH_SHORT).show();
                }
            }).setNegativeButton("বাতিল", null).show();
    }

    private int periodCount(String period) {
        int count = 0;
        for (JSONObject customer : customers) {
            JSONArray entries = transactions(customer);
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.optJSONObject(i);
                if (entry != null && inReportPeriod(entry.optString("date"), period)) count++;
            }
        }
        return count;
    }

    private void showBusinessReport(String period) {
        base("ব্যবসার রিপোর্ট", "বিক্রি, জমা ও বকেয়ার সারসংক্ষেপ");
        search.setVisibility(View.GONE);
        TextView back = action("←   কাস্টমার তালিকায় ফিরুন", Color.WHITE, INK, 12);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(42));
        backParams.bottomMargin = dp(12);
        root.addView(back, 2, backParams);
        back.setOnClickListener(v -> showCustomers());

        LinearLayout filters = row();
        String[] keys = {"today", "week", "month", "all"};
        String[] names = {"আজ", "এই সপ্তাহ", "এই মাস", "সব"};
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            boolean active = key.equals(period);
            TextView chip = action(names[i], active ? Color.rgb(12, 92, 111) : (darkMode ? Color.rgb(37, 51, 70) : Color.WHITE),
                    active ? Color.WHITE : (darkMode ? Color.rgb(220, 231, 242) : INK), 12);
            chip.setTextSize(11);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(0, dp(42), 1);
            chipParams.setMargins(dp(2), 0, dp(2), 0);
            filters.addView(chip, chipParams);
            chip.setOnClickListener(v -> showBusinessReport(key));
        }
        root.addView(filters, 3);

        LinearLayout metrics = column();
        metrics.setPadding(dp(14), dp(14), dp(14), dp(14));
        metrics.setBackground(shape(Color.WHITE, Color.rgb(218, 228, 237), 20));
        metrics.setElevation(dp(4));
        metrics.addView(statTile("মোট বিক্রি", "৳ " + formatMoney(periodTotal("sale", period)), "↗", BLUE));
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams dv = new LinearLayout.LayoutParams(-1, dp(1));
        dv.setMargins(dp(8), dp(10), dp(8), dp(10));
        metrics.addView(divider, dv);
        LinearLayout pair = row();
        pair.addView(statTile("মোট জমা", "৳ " + formatMoney(periodTotal("payment", period)), "✓", TEAL),
                new LinearLayout.LayoutParams(0, -2, 1));
        pair.addView(statTile("লেনদেন", String.valueOf(periodCount(period)), "≋", Color.rgb(157, 112, 43)),
                new LinearLayout.LayoutParams(0, -2, 1));
        metrics.addView(pair);
        View expenseDivider = new View(this);
        expenseDivider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams expenseDividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        expenseDividerParams.setMargins(dp(8), dp(10), dp(8), dp(10));
        metrics.addView(expenseDivider, expenseDividerParams);
        metrics.addView(statTile("মোট খরচ", "৳ " + formatMoney(periodExpenseTotal(period)), "−", Color.rgb(164, 72, 49)));
        metrics.addView(statTile("জমা থেকে খরচ বাদে", "৳ " + formatMoney(periodTotal("payment", period) - periodExpenseTotal(period)), "=", TEAL));
        metrics.addView(statTile("বিক্রি থেকে খরচ বাদে", "৳ " + formatMoney(periodTotal("sale", period) - periodExpenseTotal(period)), "∑", Color.rgb(34, 132, 93)));
        LinearLayout.LayoutParams metricParams = new LinearLayout.LayoutParams(-1, -2);
        metricParams.topMargin = dp(12);
        metricParams.bottomMargin = dp(12);
        root.addView(metrics, 4, metricParams);

        TextView balance = label("সর্বমোট বকেয়া  ৳ " + formatMoney(sumAllDue()), 16, true,
                darkMode ? Color.rgb(255, 205, 166) : Color.rgb(160, 73, 47));
        balance.setGravity(Gravity.CENTER);
        balance.setPadding(dp(12), dp(14), dp(12), dp(14));
        balance.setBackground(shape(darkMode ? Color.rgb(57, 43, 43) : Color.rgb(255, 242, 235), Color.rgb(224, 198, 182), 16));
        root.addView(balance, 5);

        TextView export = primaryAction("↓   Excel-এ খোলার রিপোর্ট (CSV) তৈরি করুন");
        LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(-1, dp(48));
        exportParams.topMargin = dp(12);
        root.addView(export, 6, exportParams);
        export.setOnClickListener(v -> createReportCsv(period));
        listArea.addView(label("রিপোর্টের সময়সীমা: " + ("today".equals(period) ? "আজ" : "week".equals(period) ? "এই সপ্তাহ" : "month".equals(period) ? "এই মাস" : "সব সময়"), 14, true, INK));
        listArea.addView(label("রিপোর্টে " + periodCount(period) + "টি বিক্রি ও জমার লেনদেন এবং " + periodExpenseCount(period) + "টি খরচ অন্তর্ভুক্ত হয়েছে।", 12, false, MUTED));
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private void createReportCsv(String period) {
        pendingReportPeriod = period;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        String label = "today".equals(period) ? "Today" : "week".equals(period) ? "Weekly" : "month".equals(period) ? "Monthly" : "All";
        intent.putExtra(Intent.EXTRA_TITLE, "AmarCustomer-Report-" + label + ".csv");
        startActivityForResult(intent, REQUEST_REPORT_EXPORT);
    }

    private void writeReportCsv(Uri uri, String period) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new Exception("Output unavailable");
            StringBuilder csv = new StringBuilder("\uFEFFকাস্টমারের নাম,ধরন,পরিমাণ,তারিখ,বিবরণ\r\n");
            for (JSONObject customer : customers) {
                JSONArray entries = transactions(customer);
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.optJSONObject(i);
                    if (entry == null || !inReportPeriod(entry.optString("date"), period)) continue;
                    csv.append(csvCell(customer.optString("name"))).append(',')
                       .append(csvCell("sale".equals(entry.optString("type")) ? "বিক্রি" : "জমা")).append(',')
                       .append(csvCell(entry.optString("amount"))).append(',')
                       .append(csvCell(entry.optString("date"))).append(',')
                       .append(csvCell(entry.optString("note"))).append("\r\n");
                }
            }
            for (int i = 0; i < expenses.length(); i++) {
                JSONObject expense = expenses.optJSONObject(i);
                if (expense == null || !inReportPeriod(expense.optString("date"), period)) continue;
                csv.append(csvCell("ব্যবসার খরচ")).append(',')
                   .append(csvCell("খরচ - " + expense.optString("category"))).append(',')
                   .append(csvCell(expense.optString("amount"))).append(',')
                   .append(csvCell(expense.optString("date"))).append(',')
                   .append(csvCell(expense.optString("title"))).append("\r\n");
            }
            csv.append("\r\nসর্বমোট বিক্রি,").append(formatMoney(periodTotal("sale", period))).append("\r\n");
            csv.append("সর্বমোট জমা,").append(formatMoney(periodTotal("payment", period))).append("\r\n");
            csv.append("সর্বমোট খরচ,").append(formatMoney(periodExpenseTotal(period))).append("\r\n");
            csv.append("জমা থেকে খরচ বাদে,").append(formatMoney(periodTotal("payment", period) - periodExpenseTotal(period))).append("\r\n");
            csv.append("সর্বমোট বকেয়া,").append(formatMoney(sumAllDue())).append("\r\n");
            out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
            Toast.makeText(this, "রিপোর্ট CSV ফাইলে সংরক্ষিত হয়েছে।", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "রিপোর্ট সংরক্ষণ করা যায়নি।", Toast.LENGTH_LONG).show();
        }
    }

    private void showLedger() {
        if (selectedCustomer < 0 || selectedCustomer >= customers.size()) return;
        JSONObject customer = customers.get(selectedCustomer);
        JSONArray list = transactions(customer);
        StringBuilder report = new StringBuilder();
        report.append("মোট বিক্রি: ৳ ").append(formatMoney(sumTransactions(customer, "sale")));
        report.append("\\nমোট জমা: ৳ ").append(formatMoney(sumTransactions(customer, "payment")));
        report.append("\\nবর্তমান বকেয়া: ৳ ").append(formatMoney(customerDue(customer)));
        report.append("\\n\\nলেনদেনের ইতিহাস\\n");
        if (list.length() == 0) report.append("এখনও কোনো লেনদেন যোগ করা হয়নি।");
        for (int i = list.length() - 1; i >= 0; i--) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) continue;
            report.append("\\n").append("sale".equals(item.optString("type")) ? "বিক্রি" : "জমা");
            report.append("  •  ৳ ").append(formatMoney(parseMoney(item.optString("amount"))));
            String date = item.optString("date", "");
            if (!date.isEmpty()) report.append("\\n").append(date);
            String note = item.optString("note", "").trim();
            if (!note.isEmpty()) report.append("  |  ").append(note);
            report.append("\\n");
        }
        TextView content = label(report.toString(), 14, false, INK);
        content.setGravity(Gravity.TOP | Gravity.START);
        content.setPadding(dp(12), dp(12), dp(12), dp(12));
        ScrollView scroller = new ScrollView(this);
        scroller.addView(content);
        new AlertDialog.Builder(this)
            .setTitle(customer.optString("name") + " — হিসাব")
            .setView(scroller)
            .setPositiveButton("বন্ধ", null)
            .setNeutralButton("রিপোর্ট শেয়ার", (d, w) -> shareCustomerReport(customer))
            .show();
    }

    private double sumProductPrices(JSONObject customer) {
        JSONArray products = customer.optJSONArray("products");
        if (products == null) return 0;
        double total = 0;
        for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.optJSONObject(i);
            if (product == null) continue;
            String raw = product.optString("price", "").trim();
            StringBuilder normalized = new StringBuilder();
            for (int j = 0; j < raw.length(); j++) {
                char ch = raw.charAt(j);
                if (ch >= 0x09E6 && ch <= 0x09EF) normalized.append((char)('0' + ch - 0x09E6));
                else if (ch >= 0x0660 && ch <= 0x0669) normalized.append((char)('0' + ch - 0x0660));
                else if (ch != ',' && ch != '৳' && ch != ' ') normalized.append(ch);
            }
            try { total += Double.parseDouble(normalized.toString()); }
            catch (Exception ignored) {}
        }
        return total;
    }

    private String formatMoney(double amount) {
        if (amount == Math.floor(amount)) return String.format(Locale.US, "%,.0f", amount);
        return String.format(Locale.US, "%,.2f", amount);
    }

    private int productCount(JSONObject c) {
        JSONArray a = c.optJSONArray("products");
        return a == null ? 0 : a.length();
    }

    private void dialCustomer(String phone) {
        String number = phone == null ? "" : phone.trim();
        if (number.isEmpty()) {
            Toast.makeText(this, "এই কাস্টমারের মোবাইল নম্বর দেওয়া নেই।", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder dial = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char ch = number.charAt(i);
            if (ch >= '\u09e6' && ch <= '\u09ef') dial.append((char)('0' + ch - '\u09e6'));
            else if (ch >= '\u0660' && ch <= '\u0669') dial.append((char)('0' + ch - '\u0660'));
            else dial.append(ch);
        }
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", dial.toString(), null)));
        } catch (Exception e) {
            Toast.makeText(this, "ডায়ালার খোলা যাচ্ছে না।", Toast.LENGTH_SHORT).show();
        }
    }

    private void customerDialog(int index) {
        LinearLayout form = column();
        form.setPadding(dp(4), dp(5), dp(4), 0);
        EditText name = field("কাস্টমারের নাম", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        EditText address = field("কাস্টমারের ঠিকানা", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE, true);
        EditText phone = field("মোবাইল নম্বর", android.text.InputType.TYPE_CLASS_PHONE, false);

        if (index >= 0) {
            JSONObject c = customers.get(index);
            name.setText(c.optString("name"));
            address.setText(c.optString("address"));
            phone.setText(c.optString("phone"));
        }

        form.addView(name);
        addGap(form, 10);
        form.addView(address, new LinearLayout.LayoutParams(-1, dp(82)));
        addGap(form, 10);
        form.addView(phone);

        new AlertDialog.Builder(this)
            .setTitle(index < 0 ? "নতুন কাস্টমার যোগ করুন" : "কাস্টমারের তথ্য পরিবর্তন")
            .setView(form)
            .setPositiveButton("সংরক্ষণ", (d, w) -> {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) {
                    Toast.makeText(this, "কাস্টমারের নাম লিখুন।", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (index < 0) {
                    JSONObject c = new JSONObject();
                    try {
                        c.put("name", n);
                        c.put("address", address.getText().toString().trim());
                        c.put("phone", phone.getText().toString().trim());
                        c.put("products", new JSONArray());
                        customers.add(c);
                    } catch (Exception ignored) {}
                } else {
                    try {
                        JSONObject c = customers.get(index);
                        c.put("name", n);
                        c.put("address", address.getText().toString().trim());
                        c.put("phone", phone.getText().toString().trim());
                    } catch (Exception ignored) {}
                }
                saveData();
                showCustomers();
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void addGap(LinearLayout parent, int height) {
        View gap = new View(this);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void confirmDeleteCustomer(int index) {
        new AlertDialog.Builder(this)
            .setMessage("এই কাস্টমার ও তার পণ্য মুছে ফেলবেন?")
            .setPositiveButton("মুছুন", (d, w) -> {
                customers.remove(index);
                saveData();
                showCustomers();
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void showProducts(int index) {
        if (index < 0 || index >= customers.size()) return;
        selectedCustomer = index;
        JSONObject c = customers.get(index);
        base(c.optString("name"), "এই কাস্টমারের পণ্যের তালিকা");

        TextView add = primaryAction("+   নতুন পণ্য যোগ করুন");
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(52));
        ap.topMargin = dp(8);
        ap.bottomMargin = dp(10);
        root.addView(add, 2, ap);
        add.setOnClickListener(v -> productDialog(-1));

        TextView back = action("←   কাস্টমার তালিকায় ফিরুন", Color.WHITE, INK, 12);
        back.setBackground(shape(Color.WHITE, BORDER, 12));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(42));
        bp.bottomMargin = dp(12);
        root.addView(back, 3, bp);
        back.setOnClickListener(v -> showCustomers());

        LinearLayout summary = row();
        summary.setPadding(dp(13), dp(12), dp(13), dp(12));
        summary.setBackground(shape(Color.rgb(255, 255, 255), Color.rgb(224, 215, 194), 19));
        summary.setElevation(dp(4));
        JSONArray currentProducts = c.optJSONArray("products");
        int productTotal = currentProducts == null ? 0 : currentProducts.length();
        summary.addView(statTile("মোট পণ্য", String.valueOf(productTotal), "▦", BLUE),
                new LinearLayout.LayoutParams(0, -2, 1));
        View summaryDivider = new View(this);
        summaryDivider.setBackgroundColor(BORDER);
        summary.addView(summaryDivider, new LinearLayout.LayoutParams(dp(1), dp(42)));
        summary.addView(statTile("পণ্যের মোট দাম", "৳ " + formatMoney(sumProductPrices(c)), "৳", TEAL),
                new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.bottomMargin = dp(12);
        root.addView(summary, 4, summaryParams);

        TextView profile = primaryAction("◉   কাস্টমার প্রোফাইল ও সম্পূর্ণ হিসাব");
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(-1, dp(46));
        profileParams.bottomMargin = dp(9);
        root.addView(profile, 5, profileParams);
        profile.setOnClickListener(v -> showCustomerProfile(selectedCustomer));

        TextView shareReport = action("↗   কাস্টমার রিপোর্ট শেয়ার করুন",
                darkMode ? Color.rgb(37, 51, 70) : Color.rgb(232, 242, 255),
                darkMode ? Color.rgb(186, 216, 255) : Color.rgb(31, 75, 145), 13);
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(-1, dp(45));
        shareParams.bottomMargin = dp(12);
        root.addView(shareReport, shareParams);
        shareReport.setOnClickListener(v -> shareCustomerReport(c));

        TextView invoice = primaryAction("▤   PDF ইনভয়েস তৈরি করুন");
        LinearLayout.LayoutParams invoiceParams = new LinearLayout.LayoutParams(-1, dp(46));
        invoiceParams.bottomMargin = dp(10);
        root.addView(invoice, invoiceParams);
        invoice.setOnClickListener(v -> shareDigitalInvoice(c));

        renderProducts("");
        search.addTextChangedListener(watcher(s -> renderProducts(s)));
    }

    private void shareCustomerReport(JSONObject customer) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("আমার কাস্টমার — কাস্টমার রিপোর্ট\\n\\n");
            report.append("নাম: ").append(customer.optString("name")).append('\n');
            String address = customer.optString("address", "").trim();
            String phone = customer.optString("phone", "").trim();
            if (!address.isEmpty()) report.append("ঠিকানা: ").append(address).append('\n');
            if (!phone.isEmpty()) report.append("মোবাইল: ").append(phone).append('\n');
            report.append("\\nপণ্যের তালিকা:\\n");
            JSONArray items = customer.optJSONArray("products");
            if (items != null && items.length() > 0) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item != null) report.append(i + 1).append(". ")
                        .append(item.optString("name")).append(" — ৳ ")
                        .append(item.optString("price")).append('\n');
                }
            } else {
                report.append("এখনও কোনো পণ্য যোগ করা হয়নি।\\n");
            }
            report.append("\\nমোট পণ্য: ").append(productCount(customer));
            report.append("\\nপণ্যের মোট দাম: ৳ ").append(formatMoney(sumProductPrices(customer)));
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "কাস্টমার রিপোর্ট — " + customer.optString("name"));
            send.putExtra(Intent.EXTRA_TEXT, report.toString());
            startActivity(Intent.createChooser(send, "কাস্টমার রিপোর্ট শেয়ার করুন"));
        } catch (Exception e) {
            Toast.makeText(this, "রিপোর্ট শেয়ার করা যাচ্ছে না।", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareDigitalInvoice(JSONObject customer) {
        pendingInvoiceCustomer = customer;
        String safeName = customer.optString("name", "Customer").replaceAll("[^\\p{L}\\p{N}._-]", "_");
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "AmarCustomer-Invoice-" + safeName + "-" +
                new java.text.SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new java.util.Date()) + ".pdf");
        startActivityForResult(intent, REQUEST_INVOICE_PDF);
    }

    private void writeInvoicePdf(Uri uri, JSONObject customer) {
        PdfDocument document = new PdfDocument();
        OutputStream output = null;
        try {
            JSONArray items = customer.optJSONArray("products");
            int itemCount = items == null ? 0 : items.length();
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(12);
            paint.setColor(Color.rgb(49, 62, 78));
            double total = 0;
            for (int i = 0; i < itemCount; i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null) total += parseMoney(item.optString("price"));
            }
            int pageNumber = 1;
            int y = 0;
            PdfDocument.Page page = null;
            android.graphics.Canvas canvas = null;
            for (int i = 0; i < Math.max(1, itemCount); i++) {
                if (page == null || y > 755) {
                    if (page != null) document.finishPage(page);
                    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create();
                    page = document.startPage(info);
                    canvas = page.getCanvas();
                    canvas.drawColor(Color.WHITE);
                    Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
                    band.setColor(Color.rgb(8, 48, 75));
                    canvas.drawRect(0, 0, 595, 145, band);
                    Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
                    white.setColor(Color.WHITE);
                    white.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                    white.setTextSize(25);
                    canvas.drawText("আমার কাস্টমার", 42, 58, white);
                    white.setTextSize(14);
                    white.setTypeface(Typeface.DEFAULT);
                    canvas.drawText("বিক্রয় ইনভয়েস / রসিদ", 42, 88, white);
                    paint.setColor(Color.rgb(230, 207, 160));
                    canvas.drawText("PREMIUM BUSINESS RECORD", 42, 119, paint);
                    paint.setColor(Color.rgb(49, 62, 78));
                    canvas.drawText("কাস্টমার: " + customer.optString("name"), 42, 180, paint);
                    canvas.drawText("মোবাইল: " + customer.optString("phone", "—"), 42, 203, paint);
                    canvas.drawText("ঠিকানা: " + customer.optString("address", "—"), 42, 226, paint);
                    canvas.drawText("তারিখ: " + new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new java.util.Date()), 42, 249, paint);
                    Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
                    line.setColor(Color.rgb(218, 226, 234));
                    canvas.drawRect(42, 270, 553, 272, line);
                    paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                    canvas.drawText("নং", 45, 297, paint);
                    canvas.drawText("পণ্যের নাম", 95, 297, paint);
                    canvas.drawText("একক দাম", 430, 297, paint);
                    canvas.drawRect(42, 308, 553, 309, line);
                    paint.setTypeface(Typeface.DEFAULT);
                    y = 338;
                }
                if (itemCount > 0) {
                    JSONObject item = items.optJSONObject(i);
                    if (item == null) continue;
                    canvas.drawText(String.valueOf(i + 1), 48, y, paint);
                    canvas.drawText(item.optString("name"), 95, y, paint);
                    canvas.drawText("৳ " + formatMoney(parseMoney(item.optString("price"))), 430, y, paint);
                    y += 28;
                }
            }
            Paint totalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            totalPaint.setColor(Color.rgb(8, 91, 91));
            totalPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            totalPaint.setTextSize(17);
            canvas.drawRect(42, y + 5, 553, y + 7, totalPaint);
            canvas.drawText("সর্বমোট: ৳ " + formatMoney(total), 355, y + 36, totalPaint);
            Paint footer = new Paint(Paint.ANTI_ALIAS_FLAG);
            footer.setColor(Color.rgb(112, 125, 143));
            footer.setTextSize(11);
            canvas.drawText("ধন্যবাদ। আপনার ব্যবসার বিশ্বস্ত সঙ্গী — আমার কাস্টমার", 42, 805, footer);
            document.finishPage(page);
            output = getContentResolver().openOutputStream(uri);
            if (output == null) throw new Exception("PDF destination unavailable");
            document.writeTo(output);
            output.flush();
            Toast.makeText(this, "PDF ইনভয়েস সফলভাবে তৈরি হয়েছে।", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "PDF ইনভয়েস তৈরি করা যায়নি।", Toast.LENGTH_LONG).show();
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) {}
            document.close();
        }
    }

    private void renderProducts(String filter) {
        listArea.removeAllViews();
        if (selectedCustomer < 0 || selectedCustomer >= customers.size()) return;
        TextView section = label("পণ্যের তালিকা  ·  " + productCount(customers.get(selectedCustomer)) + "টি", 16, true, INK);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, dp(36));
        sectionParams.bottomMargin = dp(5);
        listArea.addView(section, sectionParams);
        JSONArray products = customers.get(selectedCustomer).optJSONArray("products");
        if (products == null) return;
        String q = filter.toLowerCase(Locale.ROOT).trim();

        for (int i = 0; i < products.length(); i++) {
            JSONObject p = products.optJSONObject(i);
            if (p == null) continue;
            final int ix = i;
            String name = p.optString("name");
            String price = p.optString("price");
            String stock = p.optString("stock", "0");
            if (!name.toLowerCase(Locale.ROOT).contains(q)) continue;

            LinearLayout card = row();
            card.setPadding(dp(16), dp(15), dp(12), dp(15));
            card.setBackground(shape(Color.WHITE, Color.rgb(214, 225, 237), 21));
            card.setElevation(dp(7));
            card.setForeground(new RippleDrawable(ColorStateList.valueOf(Color.argb(38, 12, 82, 113)), null, shape(Color.WHITE, 0, 21)));
            card.setAlpha(0f);
            card.setTranslationY(dp(10));
            card.animate().alpha(1f).translationY(0f).setDuration(240).start();
            TextView productMark = label("▦", 20, true, BLUE);
            productMark.setGravity(Gravity.CENTER);
            productMark.setBackground(shape(Color.rgb(232, 241, 252), Color.rgb(196, 220, 235), 14));
            productMark.setElevation(dp(2));
            LinearLayout.LayoutParams productMarkParams = new LinearLayout.LayoutParams(dp(42), dp(44));
            productMarkParams.rightMargin = dp(11);
            card.addView(productMark, productMarkParams);
            LinearLayout info = column();
            info.addView(label(name, 16, true, INK));
            String category = p.optString("category", "অন্যান্য");
            info.addView(label("ধরন: " + category + "   •   মজুত: " + stock + " টি", 12, true,
                    parseMoney(stock) <= 3 ? Color.rgb(190, 90, 48) : TEAL));
            TextView priceLabel = label("৳  " + price, 15, true, darkMode ? Color.rgb(255, 218, 145) : Color.rgb(143, 101, 29));
            priceLabel.setPadding(dp(10), dp(5), dp(10), dp(5));
            priceLabel.setBackground(shape(darkMode ? Color.rgb(61, 53, 39) : Color.rgb(250, 244, 230), Color.rgb(224, 204, 164), 10));
            priceLabel.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(-1, -2);
            priceParams.topMargin = dp(5);
            info.addView(priceLabel, priceParams);
            card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

            TextView edit = action("✎", Color.rgb(232, 242, 255), Color.rgb(25, 95, 210), 13);
            TextView del = action("🗑", Color.rgb(255, 239, 240), Color.rgb(190, 45, 55), 13);
            edit.setTextSize(25);
            del.setTextSize(21);
            edit.setContentDescription("বদল");
            del.setContentDescription("মুছুন");
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(52), dp(48));
            bp.leftMargin = dp(8);
            card.addView(edit, bp);
            card.addView(del, bp);
            edit.setOnClickListener(v -> productDialog(ix));
            del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("এই পণ্যটি মুছে ফেলবেন?")
                .setPositiveButton("মুছুন", (dialog, which) -> {
                    products.remove(ix);
                    saveData();
                    showProducts(selectedCustomer);
                })
                .setNegativeButton("বাতিল", null)
                .show());

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(9);
            listArea.addView(card, cp);
        }

        if (listArea.getChildCount() == 1) {
            boolean noProducts = products.length() == 0 && q.isEmpty();
            LinearLayout empty = emptyState(noProducts ? "▦" : "⌕",
                    noProducts ? "এখনও কোনো পণ্য নেই" : "কোনো পণ্য পাওয়া যায়নি",
                    noProducts ? "নতুন পণ্য যোগ করলে এখানে দেখা যাবে।" : "পণ্যের নাম লিখে আবার খুঁজে দেখুন।");
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(-1, -2);
            emptyParams.topMargin = dp(12);
            listArea.addView(empty, emptyParams);
        }
    }

    private void productDialog(int index) {
        LinearLayout form = column();
        form.setPadding(dp(4), dp(5), dp(4), 0);
        EditText name = field("পণ্যের নাম", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        EditText price = field("একক দাম", android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL, false);
        EditText stock = field("বর্তমান মজুতের পরিমাণ (ঐচ্ছিক)", android.text.InputType.TYPE_CLASS_NUMBER, false);
        Spinner category = new Spinner(this);
        String[] categories = {"সাধারণ", "খাদ্যপণ্য", "পোশাক", "ইলেকট্রনিক্স", "প্রসাধনী", "গৃহস্থালি", "অন্যান্য"};
        category.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories));
        form.addView(name);
        addGap(form, 10);
        form.addView(price);
        addGap(form, 10);
        form.addView(stock);
        addGap(form, 8);
        form.addView(category, new LinearLayout.LayoutParams(-1, dp(48)));

        JSONArray arr = customers.get(selectedCustomer).optJSONArray("products");
        if (index >= 0) {
            JSONObject p = arr.optJSONObject(index);
            name.setText(p.optString("name"));
            price.setText(p.optString("price"));
            stock.setText(p.optString("stock"));
            for (int i = 0; i < categories.length; i++) if (categories[i].equals(p.optString("category", "অন্যান্য"))) category.setSelection(i);
        }

        new AlertDialog.Builder(this)
            .setTitle(index < 0 ? "নতুন পণ্য" : "পণ্য পরিবর্তন")
            .setView(form)
            .setPositiveButton("সংরক্ষণ", (d, w) -> {
                String n = name.getText().toString().trim();
                String pr = price.getText().toString().trim();
                String st = stock.getText().toString().trim();
                if (!n.isEmpty()) {
                    try {
                        JSONObject p = index < 0 ? new JSONObject() : arr.getJSONObject(index);
                        p.put("name", n);
                        p.put("price", pr);
                        p.put("stock", st.isEmpty() ? "0" : st);
                        p.put("category", category.getSelectedItem().toString());
                        if (index < 0) arr.put(p);
                        saveData();
                        showProducts(selectedCustomer);
                    } catch (Exception ignored) {}
                }
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }



    private void showCustomerProfile(int index) {
        if (index < 0 || index >= customers.size()) return;
        selectedCustomer = index;
        JSONObject customer = customers.get(index);
        base("কাস্টমার প্রোফাইল", customer.optString("name") + " • সম্পূর্ণ ব্যবসায়িক সম্পর্ক");
        search.setVisibility(View.GONE);
        TextView back = action("←   পণ্যের তালিকায় ফিরুন", Color.WHITE, INK, 12);
        root.addView(back, 2, new LinearLayout.LayoutParams(-1, dp(42)));
        back.setOnClickListener(v -> showProducts(index));
        listArea.removeAllViews();
        LinearLayout hero = column();
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(7, 35, 69), ACCENTS[accentTheme], Color.rgb(18, 105, 105)});
        heroBg.setCornerRadius(dp(22));
        hero.setBackground(heroBg);
        hero.setElevation(dp(7));
        hero.addView(label(customer.optString("name"), 22, true, Color.WHITE));
        hero.addView(label("⌖  " + customer.optString("address", "ঠিকানা দেওয়া হয়নি"), 13, false, Color.WHITE));
        hero.addView(label("☎  " + customer.optString("phone", "মোবাইল দেওয়া হয়নি"), 13, true, Color.WHITE));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2); hp.bottomMargin = dp(12);
        listArea.addView(hero, hp);
        LinearLayout metrics = row();
        metrics.setPadding(dp(12), dp(14), dp(12), dp(14));
        metrics.setBackground(shape(Color.WHITE, BORDER, 18));
        metrics.addView(statTile("মোট বিক্রি", "৳ " + formatMoney(sumTransactions(customer, "sale")), "↗", BLUE), new LinearLayout.LayoutParams(0,-2,1));
        metrics.addView(statTile("মোট জমা", "৳ " + formatMoney(sumTransactions(customer, "payment")), "✓", TEAL), new LinearLayout.LayoutParams(0,-2,1));
        metrics.addView(statTile("বকেয়া", "৳ " + formatMoney(customerDue(customer)), "!", Color.rgb(180,75,55)), new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1,-2); mp.bottomMargin = dp(12);
        listArea.addView(metrics, mp);
        LinearLayout actions = column();
        TextView reminder = primaryAction("◷   পেমেন্ট রিমাইন্ডার শেয়ার করুন");
        actions.addView(reminder, new LinearLayout.LayoutParams(-1, dp(46)));
        reminder.setOnClickListener(v -> shareDueReminder(customer));
        TextView invoice = action("▤   PDF ইনভয়েস তৈরি করুন", darkMode ? Color.rgb(37,51,70) : Color.rgb(232,242,255), BLUE, 13);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(44)); ip.topMargin = dp(8);
        actions.addView(invoice, ip); invoice.setOnClickListener(v -> shareDigitalInvoice(customer));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1,-2); ap.bottomMargin = dp(12);
        listArea.addView(actions, ap);
        listArea.addView(label("লেনদেনের সম্পূর্ণ ইতিহাস", 17, true, INK));
        JSONArray entries = transactions(customer);
        if (entries.length() == 0) listArea.addView(emptyState("≋","এখনো লেনদেন নেই","বিক্রি বা জমার হিসাব যোগ করলে এখানে দেখা যাবে।"));
        for (int i=entries.length()-1;i>=0;i--) {
            JSONObject e=entries.optJSONObject(i); if(e==null) continue;
            boolean sale="sale".equals(e.optString("type"));
            LinearLayout card=column(); card.setPadding(dp(13),dp(12),dp(13),dp(12));
            card.setBackground(shape(Color.WHITE,BORDER,15)); card.setElevation(dp(2));
            LinearLayout top=row();
            top.addView(label(sale ? "বিক্রয়" : "টাকা জমা",14,true,sale?BLUE:TEAL),new LinearLayout.LayoutParams(0,-2,1));
            top.addView(label("৳ "+formatMoney(parseMoney(e.optString("amount"))),15,true,sale?BLUE:TEAL));
            card.addView(top);
            card.addView(label(e.optString("date")+"  •  "+e.optString("note",""),12,false,MUTED));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(7);
            listArea.addView(card,cp);
        }
    }

    private void showAnalytics(String period) {
        selectedCustomer = -1;
        base("স্মার্ট ব্যবসা অ্যানালিটিক্স", "বিক্রি • জমা • খরচ • নিট ফলাফল");
        search.setVisibility(View.GONE);
        TextView back=action("←   হোমে ফিরুন",Color.WHITE,INK,12);
        root.addView(back,2,new LinearLayout.LayoutParams(-1,dp(42)));
        back.setOnClickListener(v->showCustomers());
        LinearLayout filters=row();
        String[] keys={"today","week","month","all"}, names={"আজ","সপ্তাহ","মাস","সব"};
        for(int i=0;i<keys.length;i++){ final String k=keys[i]; TextView chip=action(names[i],k.equals(period)?ACCENTS[accentTheme]:(darkMode?Color.rgb(37,51,70):Color.WHITE),k.equals(period)?Color.WHITE:INK,12); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(40),1); cp.setMargins(dp(2),dp(9),dp(2),dp(9)); filters.addView(chip,cp); chip.setOnClickListener(v->showAnalytics(k)); }
        root.addView(filters,3);
        LinearLayout grid=column(); grid.setPadding(dp(14),dp(14),dp(14),dp(14)); grid.setBackground(shape(Color.WHITE,BORDER,20)); grid.setElevation(dp(4));
        addAnalyticsMetric(grid,"বিক্রি",periodTotal("sale",period),BLUE);
        addAnalyticsMetric(grid,"জমা",periodTotal("payment",period),TEAL);
        addAnalyticsMetric(grid,"ব্যবসার খরচ",periodExpenseTotal(period),Color.rgb(180,75,55));
        addAnalyticsMetric(grid,"বিক্রি − খরচ (আনুমানিক)",periodTotal("sale",period)-periodExpenseTotal(period),ACCENTS[accentTheme]);
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2); gp.topMargin=dp(12); gp.bottomMargin=dp(12); root.addView(grid,4,gp);
        listArea.removeAllViews();
        listArea.addView(label("বিক্রির পারফরম্যান্স",16,true,INK));
        LinearLayout performance=column(); performance.setPadding(dp(14),dp(13),dp(14),dp(13)); performance.setBackground(shape(Color.WHITE,BORDER,18)); performance.setElevation(dp(3));
        double totalSales=periodTotal("sale",period), totalExpenses=periodExpenseTotal(period);
        performance.addView(label("নির্বাচিত সময়ের মোট বিক্রি",12,false,MUTED));
        performance.addView(label("৳ "+formatMoney(totalSales),22,true,ACCENTS[accentTheme]));
        performance.addView(label("খরচ বাদে আনুমানিক ফলাফল  ৳ "+formatMoney(totalSales-totalExpenses),12,true,totalSales>=totalExpenses?TEAL:Color.rgb(190,75,65)));
        LinearLayout.LayoutParams perfp=new LinearLayout.LayoutParams(-1,-2); perfp.bottomMargin=dp(12); listArea.addView(performance,perfp);
        listArea.addView(label("পণ্যভিত্তিক মজুত ও মূল্য",16,true,INK));
        java.util.HashMap<String,Integer> categories=new java.util.HashMap<>();
        for(JSONObject c:customers){JSONArray ps=c.optJSONArray("products"); if(ps==null)continue; for(int i=0;i<ps.length();i++){JSONObject p=ps.optJSONObject(i);if(p==null)continue;String cat=p.optString("category","অন্যান্য");categories.put(cat,categories.containsKey(cat)?categories.get(cat)+1:1);}}
        if(categories.isEmpty()) listArea.addView(emptyState("▦","পণ্য নেই","পণ্য যোগ করলে ক্যাটাগরি অনুযায়ী বিশ্লেষণ দেখা যাবে."));
        for(String cat:categories.keySet()){LinearLayout tile=row();tile.setPadding(dp(12),dp(11),dp(12),dp(11));tile.setBackground(shape(Color.WHITE,BORDER,13));tile.addView(label(cat,14,true,INK),new LinearLayout.LayoutParams(0,-2,1));tile.addView(label(categories.get(cat)+"টি পণ্য",13,true,ACCENTS[accentTheme]));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(6);listArea.addView(tile,tp);}
        listArea.addView(label("খরচের খাতভিত্তিক সারাংশ",16,true,INK));
        java.util.HashMap<String,Double> expenseMap=new java.util.HashMap<>();
        for(int i=0;i<expenses.length();i++){JSONObject e=expenses.optJSONObject(i);if(e==null||!inReportPeriod(e.optString("date"),period))continue;String cat=e.optString("category","অন্যান্য");expenseMap.put(cat,(expenseMap.containsKey(cat)?expenseMap.get(cat):0)+parseMoney(e.optString("amount")));}
        for(String cat:expenseMap.keySet()){listArea.addView(label(cat+"  •  ৳ "+formatMoney(expenseMap.get(cat)),13,false,MUTED));}
    }

    private void addAnalyticsMetric(LinearLayout parent,String title,double amount,int color){
        TextView metric=label(title+"     ৳ "+formatMoney(amount),15,true,color);
        metric.setPadding(dp(7),dp(9),dp(7),dp(9)); parent.addView(metric);
    }

    private void showDueReminders(){
        selectedCustomer=-1; base("বকেয়া রিমাইন্ডার","যাদের কাছে টাকা পাওনা আছে");
        search.setVisibility(View.GONE);
        TextView back=action("←   হোমে ফিরুন",Color.WHITE,INK,12);root.addView(back,2,new LinearLayout.LayoutParams(-1,dp(42)));back.setOnClickListener(v->showCustomers());
        listArea.removeAllViews(); int found=0;
        for(JSONObject c:customers){double due=customerDue(c);if(due<=0)continue;found++;
            LinearLayout card=column();card.setPadding(dp(15),dp(14),dp(15),dp(14));card.setBackground(shape(Color.WHITE,BORDER,18));card.setElevation(dp(3));
            card.addView(label(c.optString("name"),16,true,INK));
            card.addView(label("বকেয়া ৳ "+formatMoney(due)+"   •   "+c.optString("phone"),13,true,Color.rgb(180,75,55)));
            TextView send=primaryAction("↗   রিমাইন্ডার পাঠান");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(42));sp.topMargin=dp(9);card.addView(send,sp);send.setOnClickListener(v->shareDueReminder(c));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(9);listArea.addView(card,cp);
        }
        if(found==0)listArea.addView(emptyState("✓","কোনো বকেয়া নেই","সব কাস্টমারের হিসাব পরিশোধিত।"));
    }

    private void shareDueReminder(JSONObject customer){
        double due=customerDue(customer);
        String phone=customer.optString("phone","");
        String msg="প্রিয় "+customer.optString("name")+", আপনার কাছে আমাদের বকেয়া রয়েছে ৳ "+formatMoney(due)+". অনুগ্রহ করে সুবিধামতো পরিশোধ করবেন। ধন্যবাদ — আমার কাস্টমার";
        Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,msg);
        try{startActivity(Intent.createChooser(send,"পেমেন্ট রিমাইন্ডার শেয়ার করুন"));}catch(Exception e){Toast.makeText(this,"শেয়ার করা যাচ্ছে না।",Toast.LENGTH_SHORT).show();}
    }

    private void showThemePicker(){
        String[] names={"সমুদ্র নীল","রাজকীয় নীল","বেগুনি","প্রকৃতি সবুজ","গোল্ডেন অ্যাম্বার","রোজ পিংক","স্লেট ব্লু","অলিভ ন্যাচার"};
        new AlertDialog.Builder(this).setTitle("আপনার পছন্দের থিম").setSingleChoiceItems(names,accentTheme,(d,which)->{accentTheme=which;getPreferences(MODE_PRIVATE).edit().putInt("accent_theme",which).apply();d.dismiss();showCustomers();}).setNegativeButton("বন্ধ",null).show();
    }

    private String pinHash(String pin){
        try{java.security.MessageDigest md=java.security.MessageDigest.getInstance("SHA-256");byte[] bytes=md.digest(pin.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.US,"%02x",b));return out.toString();}catch(Exception e){return pin;}
    }

    private void promptPinLock(){
        EditText pin=field("৪-৬ সংখ্যার PIN লিখুন",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD,false);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("অ্যাপ লক").setMessage("আপনার তথ্য দেখতে PIN দিন।").setView(pin).setCancelable(false).setPositiveButton("খুলুন",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{String value=pin.getText().toString();if(pinHash(value).equals(getPreferences(MODE_PRIVATE).getString("app_pin_hash",""))){dialog.dismiss();}else{pin.setError("PIN সঠিক নয়");}}));
        dialog.show();
    }

    private void managePinSecurity(){
        String existing=getPreferences(MODE_PRIVATE).getString("app_pin_hash","");
        if(existing.isEmpty()){EditText pin=field("নতুন ৪-৬ সংখ্যার PIN",android.text.InputType.TYPE_CLASS_NUMBER,false);new AlertDialog.Builder(this).setTitle("PIN চালু করুন").setView(pin).setPositiveButton("চালু", (d,w)->{String p=pin.getText().toString();if(p.length()<4||p.length()>6){Toast.makeText(this,"৪ থেকে ৬ সংখ্যার PIN দিন।",Toast.LENGTH_SHORT).show();return;}getPreferences(MODE_PRIVATE).edit().putString("app_pin_hash",pinHash(p)).apply();Toast.makeText(this,"PIN নিরাপত্তা চালু হয়েছে।",Toast.LENGTH_SHORT).show();}).setNegativeButton("বাতিল",null).show();}
        else{EditText current=field("বর্তমান PIN",android.text.InputType.TYPE_CLASS_NUMBER,false);new AlertDialog.Builder(this).setTitle("PIN যাচাই").setView(current).setPositiveButton("পরবর্তী", (d,w)->{if(!pinHash(current.getText().toString()).equals(existing)){Toast.makeText(this,"PIN সঠিক নয়।",Toast.LENGTH_SHORT).show();return;}new AlertDialog.Builder(this).setTitle("PIN ব্যবস্থাপনা").setItems(new String[]{"PIN পরিবর্তন","PIN বন্ধ করুন"},(dd,which)->{if(which==1){getPreferences(MODE_PRIVATE).edit().remove("app_pin_hash").apply();Toast.makeText(this,"PIN বন্ধ হয়েছে।",Toast.LENGTH_SHORT).show();}else{EditText next=field("নতুন ৪-৬ সংখ্যার PIN",android.text.InputType.TYPE_CLASS_NUMBER,false);new AlertDialog.Builder(this).setTitle("নতুন PIN").setView(next).setPositiveButton("সংরক্ষণ",(ddd,www)->{String p=next.getText().toString();if(p.length()<4||p.length()>6){Toast.makeText(this,"৪-৬ সংখ্যার PIN দিন।",Toast.LENGTH_SHORT).show();return;}getPreferences(MODE_PRIVATE).edit().putString("app_pin_hash",pinHash(p)).apply();Toast.makeText(this,"PIN পরিবর্তন হয়েছে।",Toast.LENGTH_SHORT).show();}).setNegativeButton("বাতিল",null).show();}}).show();}).setNegativeButton("বাতিল",null).show();}
    }

    private void showBackupOptions() {
        long lastBackup=getPreferences(MODE_PRIVATE).getLong("last_backup_success",0);
        String backupStatus=lastBackup==0?"এখনো কোনো সফল ব্যাকআপ নেই":"সর্বশেষ ব্যাকআপ: "+new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.getDefault()).format(new java.util.Date(lastBackup));
        new AlertDialog.Builder(this).setTitle("তথ্য ব্যাকআপ ও পুনরুদ্ধার").setMessage(backupStatus)
            .setItems(new String[]{"ব্যাকআপ তৈরি করুন", "ব্যাকআপ থেকে তথ্য ফিরিয়ে আনুন"}, (dialog, which) -> {
                if (which == 0) createBackupFile(); else chooseBackupFile();
            }).setNegativeButton("বন্ধ", null).show();
    }

    private void createBackupFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "CustomerSathi-Backup-" +
            new java.text.SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new java.util.Date()) + ".json");
        startActivityForResult(intent, REQUEST_BACKUP_EXPORT);
    }

    private void chooseBackupFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_BACKUP_IMPORT);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_REPORT_EXPORT) {
            writeReportCsv(uri, pendingReportPeriod);
        } else if (requestCode == REQUEST_INVOICE_PDF) {
            if (pendingInvoiceCustomer != null) writeInvoicePdf(uri, pendingInvoiceCustomer);
            pendingInvoiceCustomer = null;
        } else if (requestCode == REQUEST_BACKUP_EXPORT) {
            try {
                JSONObject backup = new JSONObject();
                backup.put("app", "Amar Customer");
                backup.put("version", 2);
                backup.put("backupFormat", "AmarCustomer50");
                backup.put("createdAt", System.currentTimeMillis());
                JSONArray saved = new JSONArray();
                for (JSONObject customer : customers) saved.put(customer);
                backup.put("customers", saved);
                backup.put("expenses", expenses);
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new Exception("File unavailable");
                    out.write(backup.toString(2).getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
                getPreferences(MODE_PRIVATE).edit().putLong("last_backup_success", System.currentTimeMillis()).apply();
                Toast.makeText(this, "ব্যাকআপ সফলভাবে সংরক্ষণ হয়েছে।", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "ব্যাকআপ সংরক্ষণ ব্যর্থ হয়েছে।", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_BACKUP_IMPORT) importBackupFile(uri);
    }

    private void importBackupFile(Uri uri) {
        try {
            StringBuilder content = new StringBuilder();
            try (InputStream in = getContentResolver().openInputStream(uri);
                 java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) content.append(line);
            }
            JSONObject backup = new JSONObject(content.toString());
            JSONArray incoming = backup.optJSONArray("customers");
            if (incoming == null) throw new Exception("Invalid backup");
            JSONArray incomingExpenses = backup.optJSONArray("expenses");
            JSONArray checkedExpenses = new JSONArray();
            if (incomingExpenses != null) {
                for (int i = 0; i < incomingExpenses.length(); i++) {
                    JSONObject expense = incomingExpenses.optJSONObject(i);
                    if (expense == null || parseMoney(expense.optString("amount")) <= 0)
                        throw new Exception("Invalid expense");
                    checkedExpenses.put(expense);
                }
            }
            ArrayList<JSONObject> checked = new ArrayList<>();
            for (int i = 0; i < incoming.length(); i++) {
                JSONObject customer = incoming.optJSONObject(i);
                if (customer == null || customer.optString("name").trim().isEmpty())
                    throw new Exception("Invalid customer");
                if (customer.optJSONArray("products") == null) customer.put("products", new JSONArray());
                checked.add(customer);
            }
            new AlertDialog.Builder(this).setTitle("ব্যাকআপ পুনরুদ্ধার করবেন?")
                .setMessage("বর্তমান " + customers.size() + " জন কাস্টমারের তথ্যের জায়গায় ব্যাকআপের " +
                    checked.size() + " জন কাস্টমারের তথ্য বসবে। বর্তমান তথ্য আগে ব্যাকআপ করে রাখুন।")
                .setPositiveButton("পুনরুদ্ধার", (d, w) -> {
                    customers.clear();
                    customers.addAll(checked);
                    if (incomingExpenses != null) {
                        expenses = checkedExpenses;
                        saveExpenses();
                    }
                    saveData();
                    showCustomers();
                    Toast.makeText(this, "ব্যাকআপের তথ্য সফলভাবে ফিরিয়ে আনা হয়েছে।", Toast.LENGTH_LONG).show();
                }).setNegativeButton("বাতিল", null).show();
        } catch (Exception e) {
            Toast.makeText(this, "ফাইলটি সঠিক Customer Sathi ব্যাকআপ নয়।", Toast.LENGTH_LONG).show();
        }
    }

    private void saveData() {
        JSONArray a = new JSONArray();
        for (JSONObject c : customers) a.put(c);
        getSharedPreferences("amar_customer", MODE_PRIVATE).edit().putString("data", a.toString()).apply();
    }

    private void loadData() {
        try {
            JSONArray a = new JSONArray(getSharedPreferences("amar_customer", MODE_PRIVATE).getString("data", "[]"));
            for (int i = 0; i < a.length(); i++) customers.add(a.getJSONObject(i));
        } catch (Exception ignored) {}
        try {
            expenses = new JSONArray(getSharedPreferences("amar_customer", MODE_PRIVATE).getString("expenses", "[]"));
        } catch (Exception ignored) { expenses = new JSONArray(); }
    }
}