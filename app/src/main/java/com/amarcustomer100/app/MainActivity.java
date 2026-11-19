package com.amarcustomer100.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
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
    private static final int REQUEST_BACKUP_EXPORT = 7101;
    private static final int REQUEST_BACKUP_IMPORT = 7102;
    private static final int REQUEST_REPORT_EXPORT = 7103;
    private String pendingReportPeriod = "month";

    private static final int BG = Color.rgb(246, 244, 238);
    private static final int INK = Color.rgb(13, 27, 48);
    private static final int MUTED = Color.rgb(126, 126, 122);
    private static final int BLUE = Color.rgb(19, 39, 67);
    private static final int PALE = Color.rgb(244, 239, 226);
    private static final int TEAL = Color.rgb(170, 128, 58);
    private static final int BORDER = Color.rgb(229, 220, 199);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        darkMode = getPreferences(MODE_PRIVATE).getBoolean("dark_mode", false);
        loadData();
        showCustomers();
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + .5f);
    }

    private int themedSurface(int color) {
        if (!darkMode) return color;
        if (color == Color.WHITE || color == Color.rgb(246, 244, 238)) return Color.rgb(27, 39, 56);
        if (color == Color.rgb(239, 243, 249) || color == Color.rgb(239, 245, 255)
                || color == Color.rgb(245, 239, 226) || color == Color.rgb(229, 234, 242)
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
        t.setFontFeatureSettings("kern");
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
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
        b.setElevation(dp(2));
        b.setLetterSpacing(0.015f);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private TextView primaryAction(String caption) {
        TextView button = action(caption, Color.rgb(18, 70, 91), Color.WHITE, 16);
        GradientDrawable premium = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(7, 20, 39), Color.rgb(17, 39, 66), Color.rgb(49, 54, 61), Color.rgb(151, 111, 49)});
        premium.setCornerRadius(dp(18));
        premium.setStroke(dp(1), Color.rgb(204, 169, 105));
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
        e.setPadding(dp(16), dp(12), dp(16), dp(12));
        e.setBackground(shape(Color.WHITE, BORDER, 17));
        e.setElevation(dp(2));
        return e;
    }

    private void base(String title, String subtitle) {
        root = column();
        GradientDrawable pageBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                darkMode ? new int[]{Color.rgb(8, 17, 31), Color.rgb(18, 30, 46), Color.rgb(25, 31, 40)}
                         : new int[]{Color.rgb(246, 244, 239), Color.rgb(239, 235, 226), Color.rgb(248, 246, 241)});
        root.setBackground(pageBg);
        root.setPadding(dp(15), dp(15), dp(15), dp(9));
        setContentView(root);
        getWindow().setStatusBarColor(darkMode ? Color.rgb(9, 15, 25) : Color.rgb(10, 23, 43));
        getWindow().setNavigationBarColor(darkMode ? Color.rgb(15, 23, 36) : Color.rgb(246, 244, 238));

        LinearLayout brand = row();
        brand.setPadding(dp(16), dp(16), dp(13), dp(16));
        GradientDrawable banner = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(5, 16, 32), Color.rgb(12, 34, 61), Color.rgb(31, 43, 55), Color.rgb(111, 79, 37)});
        banner.setCornerRadius(dp(24));
        banner.setStroke(dp(1), Color.rgb(180, 143, 79));
        brand.setBackground(banner);
        brand.setElevation(dp(8));
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_brand_mark);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setBackground(shape(Color.rgb(8, 25, 48), Color.rgb(225, 190, 119), 16));
        mark.setPadding(dp(3), dp(3), dp(3), dp(3));
        mark.setElevation(dp(5));
        mark.setContentDescription("আমার কাস্টমার নতুন ব্র্যান্ড লোগো");
        brand.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout titles = column();
        titles.setPadding(dp(10), 0, 0, 0);
        TextView heading = label(title, 21, true, Color.WHITE);
        heading.setLetterSpacing(0.012f);
        heading.setSingleLine(true);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titles.addView(heading);
        TextView sub = label(subtitle, 12, false, Color.rgb(239, 220, 177));
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titles.addView(sub);
        brand.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView version = label("50.2", 12, true, Color.rgb(255, 226, 163));
        version.setGravity(Gravity.CENTER);
        version.setContentDescription("অ্যাপ সংস্করণ ৫০.২");
        version.setPadding(dp(11), dp(7), dp(11), dp(7));
        version.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        version.setBackground(shape(Color.rgb(25, 48, 68), Color.rgb(202, 164, 91), 20));
        brand.addView(version, new LinearLayout.LayoutParams(-2, dp(34)));
        TextView theme = label(darkMode ? "☀" : "☾", 20, true, Color.rgb(255, 226, 163));
        theme.setGravity(Gravity.CENTER);
        theme.setContentDescription("থিম পরিবর্তন");
        theme.setBackground(shape(Color.rgb(61, 75, 94), Color.rgb(202, 164, 91), 18));
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
        search.setElevation(dp(2));
        search.setTextColor(darkMode ? Color.rgb(242, 235, 220) : Color.rgb(20, 35, 55));
        search.setHintTextColor(darkMode ? Color.rgb(164, 174, 188) : Color.rgb(125, 125, 119));
        search.setBackground(shape(darkMode ? Color.rgb(25, 39, 57) : Color.rgb(255, 254, 250),
                darkMode ? Color.rgb(184, 145, 79) : Color.rgb(204, 177, 123), 18));
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
        addPremiumBottomNav(title);
    }

    // Navigation reuses existing screens only; it does not add new business features.
    private void addPremiumBottomNav(String screenTitle) {
        LinearLayout nav = row();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(7), dp(7), dp(7), dp(7));
        GradientDrawable navBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                darkMode ? new int[]{Color.rgb(10, 22, 39), Color.rgb(23, 37, 54)}
                         : new int[]{Color.rgb(12, 29, 51), Color.rgb(25, 43, 63)});
        navBg.setCornerRadius(dp(21));
        navBg.setStroke(dp(1), Color.rgb(170, 132, 67));
        nav.setBackground(navBg);
        nav.setElevation(dp(8));
        TextView home = navItem("⌂", "হোম", screenTitle.equals("আমার কাস্টমার"));
        TextView reports = navItem("▤", "রিপোর্ট", screenTitle.contains("রিপোর্ট"));
        TextView expensesNav = navItem("▣", "খরচ", screenTitle.contains("খরচ"));
        nav.addView(home, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(reports, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(expensesNav, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(-1, dp(62));
        navParams.topMargin = dp(9);
        navParams.bottomMargin = dp(3);
        root.addView(nav, navParams);
        home.setOnClickListener(v -> showCustomers());
        reports.setOnClickListener(v -> showBusinessReport("month"));
        expensesNav.setOnClickListener(v -> showExpenses(""));
    }

    private TextView navItem(String symbol, String caption, boolean active) {
        TextView item = label(symbol + "\n" + caption, 12, true,
                active ? Color.rgb(255, 220, 151) : Color.rgb(211, 218, 228));
        item.setGravity(Gravity.CENTER);
        item.setLineSpacing(dp(1), 1f);
        if (active) {
            GradientDrawable selected = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.rgb(48, 54, 58), Color.rgb(35, 44, 55)});
            selected.setCornerRadius(dp(15));
            selected.setStroke(dp(1), Color.rgb(199, 158, 85));
            item.setBackground(selected);
        }
        item.setContentDescription(caption);
        return item;
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
        dashboard.setPadding(dp(10), dp(15), dp(10), dp(15));
        dashboard.setBackground(shape(darkMode ? Color.rgb(27, 40, 58) : Color.rgb(255, 254, 250),
                darkMode ? Color.rgb(76, 83, 91) : Color.rgb(215, 198, 164), 21));
        dashboard.setElevation(dp(6));
        dashboard.setClipToOutline(true);
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

        TextView section = label("আপনার কাস্টমার তালিকা  ·  " + customers.size() + " জন", 16, true, INK);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, dp(36));
        sectionParams.bottomMargin = dp(5);
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
            card.setPadding(dp(18), dp(18), dp(17), dp(16));
            card.setBackground(shape(darkMode ? Color.rgb(25, 38, 56) : Color.rgb(255, 254, 250),
                    darkMode ? Color.rgb(68, 82, 101) : Color.rgb(218, 202, 171), 22));
            card.setElevation(dp(5));
            card.setForeground(new RippleDrawable(ColorStateList.valueOf(Color.argb(38, 12, 82, 113)), null, shape(Color.WHITE, 0, 22)));
            View accentLine = new View(this);
            GradientDrawable accentGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{Color.rgb(17, 39, 67), Color.rgb(95, 77, 47), Color.rgb(207, 170, 101)});
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
            TextView avatar = label(name.isEmpty() ? "?" : name.substring(0, 1), 19, true,
                    darkMode ? Color.rgb(255, 222, 159) : Color.rgb(21, 43, 70));
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(shape(darkMode ? Color.rgb(64, 57, 43) : Color.rgb(248, 240, 221),
                    Color.rgb(205, 174, 117), 15));
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
            finance.setBackground(shape(darkMode ? Color.rgb(36, 47, 62) : Color.rgb(247, 244, 236),
                    darkMode ? Color.rgb(94, 82, 59) : Color.rgb(225, 211, 184), 13));
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
            TextView edit = action("✎", darkMode ? Color.rgb(48, 62, 80) : Color.rgb(239, 234, 222),
                    darkMode ? Color.rgb(238, 218, 179) : Color.rgb(23, 43, 70), 13);
            TextView delete = action("🗑", darkMode ? Color.rgb(72, 43, 47) : Color.rgb(255, 239, 237), Color.rgb(190, 45, 55), 13);
            TextView call = action("☎", darkMode ? Color.rgb(63, 56, 43) : Color.rgb(247, 240, 224),
                    darkMode ? Color.rgb(255, 220, 153) : Color.rgb(133, 96, 43), 13);
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
        TextView add = primaryAction("＋   নতুন কাস্টমার যোগ করুন");
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(54));
        ap.bottomMargin = dp(10);
        homeActions.addView(add, ap);
        add.setOnClickListener(v -> customerDialog(-1));

        TextView reports = action("▤   ব্যবসার বিক্রয় রিপোর্ট",
                darkMode ? Color.rgb(27, 65, 66) : Color.rgb(226, 243, 239),
                darkMode ? Color.rgb(145, 226, 210) : Color.rgb(142, 103, 44), 15);
        LinearLayout.LayoutParams reportParams = new LinearLayout.LayoutParams(-1, dp(48));
        reportParams.bottomMargin = dp(9);
        homeActions.addView(reports, reportParams);
        reports.setOnClickListener(v -> showBusinessReport("month"));

        TextView expenseButton = action("▣   দৈনিক খরচের হিসাব",
                darkMode ? Color.rgb(57, 43, 43) : Color.rgb(249, 239, 225),
                darkMode ? Color.rgb(255, 190, 170) : Color.rgb(147, 93, 49), 15);
        LinearLayout.LayoutParams expenseParams = new LinearLayout.LayoutParams(-1, dp(48));
        expenseParams.bottomMargin = dp(8);
        homeActions.addView(expenseButton, expenseParams);
        expenseButton.setOnClickListener(v -> showExpenses(""));

        TextView backup = action("☁   তথ্য ব্যাকআপ ও পুনরুদ্ধার",
                darkMode ? Color.rgb(55, 48, 36) : Color.rgb(247, 240, 224),
                darkMode ? Color.rgb(255, 226, 163) : Color.rgb(121, 88, 39), 15);
        LinearLayout.LayoutParams backupParams = new LinearLayout.LayoutParams(-1, dp(48));
        backupParams.bottomMargin = dp(8);
        homeActions.addView(backup, backupParams);
        backup.setOnClickListener(v -> showBackupOptions());

        LinearLayout.LayoutParams homeActionsParams = new LinearLayout.LayoutParams(-1, -2);
        homeActionsParams.topMargin = dp(8);
        listArea.addView(homeActions, homeActionsParams);
    }

    private LinearLayout emptyState(String symbol, String heading, String message) {
        LinearLayout panel = column();
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(20), dp(25), dp(20), dp(25));
        panel.setBackground(shape(Color.WHITE, Color.rgb(211, 190, 148), 24));
        panel.setElevation(dp(5));

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
        tile.setPadding(dp(3), dp(3), dp(3), dp(3));
        TextView icon = label(symbol, 18, true, accent);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(darkMode ? Color.rgb(58, 52, 42) : Color.rgb(248, 241, 225), Color.rgb(202, 169, 105), 13));
        icon.setElevation(dp(1));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(32)));
        TextView number = label(value, 20, true, INK);
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
                total += expenseAmount(item);
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
        form.addView(amount);
        addGap(form, 10);
        form.addView(note);
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
                    JSONObject entry = new JSONObject();
                    entry.put("type", type);
                    entry.put("amount", raw);
                    entry.put("note", note.getText().toString().trim());
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
        if (date == null || date.trim().isEmpty()) return false;
        String[] patterns = {"dd-MM-yyyy HH:mm", "dd-MM-yyyy", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat(pattern, Locale.getDefault());
                parser.setLenient(false);
                java.util.Date parsed = parser.parse(date.trim());
                if (parsed != null) return parsed.getTime() >= reportStart(period);
            } catch (Exception ignored) {}
        }
        return false;
    }

    private double expenseAmount(JSONObject item) {
        if (item == null) return 0;
        String amount = item.optString("amount", "");
        if (amount.trim().isEmpty()) amount = item.optString("value", item.optString("cost", ""));
        return parseMoney(amount);
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
        listArea.addView(add, 0, addParams);
        add.setOnClickListener(v -> expenseDialog(-1));

        TextView back = action("←   হোমে ফিরুন", Color.WHITE, INK, 12);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(40));
        backParams.bottomMargin = dp(10);
        listArea.addView(back, 1, backParams);
        back.setOnClickListener(v -> showCustomers());

        LinearLayout totals = row();
        totals.setPadding(dp(12), dp(13), dp(12), dp(13));
        totals.setBackground(shape(Color.WHITE, BORDER, 17));
        totals.addView(statTile("সর্বমোট খরচ", "৳ " + formatMoney(periodExpenseTotal("all")), "−", Color.rgb(147, 93, 49)),
                new LinearLayout.LayoutParams(0, -2, 1));
        totals.addView(statTile("এই মাসে", "৳ " + formatMoney(periodExpenseTotal("month")), "▣", BLUE),
                new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(-1, -2);
        totalParams.bottomMargin = dp(12);
        listArea.addView(totals, totalParams);

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
            top.addView(label("৳ " + formatMoney(expenseAmount(item)), 15, true,
                    darkMode ? Color.rgb(255, 190, 170) : Color.rgb(147, 93, 49)));
            card.addView(top);
            TextView detail = label(category + "  •  " + date, 12, false, MUTED);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
            detailParams.topMargin = dp(5);
            card.addView(detail, detailParams);
            LinearLayout actions = row();
            actions.setGravity(Gravity.RIGHT);
            TextView edit = action("✎ সম্পাদনা", darkMode ? Color.rgb(37, 51, 70) : Color.rgb(245, 239, 226), BLUE, 11);
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
        listArea.addView(back, 0, backParams);
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
        listArea.addView(filters);

        LinearLayout metrics = column();
        metrics.setPadding(dp(14), dp(14), dp(14), dp(14));
        metrics.setBackground(shape(Color.WHITE, Color.rgb(205, 178, 122), 22));
        metrics.setElevation(dp(6));
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
        metrics.addView(statTile("মোট খরচ", "৳ " + formatMoney(periodExpenseTotal(period)), "−", Color.rgb(147, 93, 49)));
        metrics.addView(statTile("জমা থেকে খরচ বাদে", "৳ " + formatMoney(periodTotal("payment", period) - periodExpenseTotal(period)), "=", TEAL));
        LinearLayout.LayoutParams metricParams = new LinearLayout.LayoutParams(-1, -2);
        metricParams.topMargin = dp(12);
        metricParams.bottomMargin = dp(12);
        listArea.addView(metrics, metricParams);

        TextView balance = label("সর্বমোট বকেয়া  ৳ " + formatMoney(sumAllDue()), 16, true,
                darkMode ? Color.rgb(255, 205, 166) : Color.rgb(160, 73, 47));
        balance.setGravity(Gravity.CENTER);
        balance.setPadding(dp(12), dp(14), dp(12), dp(14));
        balance.setBackground(shape(darkMode ? Color.rgb(57, 43, 43) : Color.rgb(255, 242, 235), Color.rgb(224, 198, 182), 16));
        listArea.addView(balance);

        TextView export = primaryAction("↓   Excel-এ খোলার রিপোর্ট (CSV) তৈরি করুন");
        LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(-1, dp(48));
        exportParams.topMargin = dp(12);
        listArea.addView(export, exportParams);
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
        summary.setBackground(shape(Color.rgb(255, 255, 255), Color.rgb(205, 178, 122), 21));
        summary.setElevation(dp(5));
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

        TextView shareReport = action("↗   কাস্টমার রিপোর্ট শেয়ার করুন",
                darkMode ? Color.rgb(37, 51, 70) : Color.rgb(245, 239, 226),
                darkMode ? Color.rgb(186, 216, 255) : Color.rgb(31, 75, 145), 13);
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(-1, dp(45));
        shareParams.bottomMargin = dp(12);
        root.addView(shareReport, shareParams);
        shareReport.setOnClickListener(v -> shareCustomerReport(c));

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
            if (!name.toLowerCase(Locale.ROOT).contains(q)) continue;

            LinearLayout card = row();
            card.setPadding(dp(16), dp(15), dp(12), dp(15));
            card.setBackground(shape(darkMode ? Color.rgb(24, 38, 56) : Color.rgb(255, 254, 250),
                    darkMode ? Color.rgb(91, 102, 116) : Color.rgb(213, 192, 151), 21));
            card.setElevation(dp(6));
            card.setForeground(new RippleDrawable(ColorStateList.valueOf(Color.argb(38, 12, 82, 113)), null, shape(Color.WHITE, 0, 21)));
            card.setAlpha(0f);
            card.setTranslationY(dp(10));
            card.animate().alpha(1f).translationY(0f).setDuration(240).start();
            TextView productMark = label("▦", 20, true, BLUE);
            productMark.setGravity(Gravity.CENTER);
            productMark.setBackground(shape(Color.rgb(245, 239, 226), Color.rgb(196, 220, 235), 14));
            productMark.setElevation(dp(2));
            LinearLayout.LayoutParams productMarkParams = new LinearLayout.LayoutParams(dp(42), dp(44));
            productMarkParams.rightMargin = dp(11);
            card.addView(productMark, productMarkParams);
            LinearLayout info = column();
            info.addView(label(name, 16, true, INK));
            TextView priceLabel = label("৳  " + price, 15, true, darkMode ? Color.rgb(255, 218, 145) : Color.rgb(145, 105, 40));
            priceLabel.setPadding(dp(10), dp(5), dp(10), dp(5));
            priceLabel.setBackground(shape(darkMode ? Color.rgb(61, 53, 39) : Color.rgb(249, 243, 230), Color.rgb(224, 204, 164), 10));
            priceLabel.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(-1, -2);
            priceParams.topMargin = dp(5);
            info.addView(priceLabel, priceParams);
            card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

            TextView edit = action("✎", Color.rgb(245, 239, 226), Color.rgb(23, 43, 70), 13);
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
        EditText price = field("দাম", android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL, false);
        form.addView(name);
        addGap(form, 10);
        form.addView(price);

        JSONArray arr = customers.get(selectedCustomer).optJSONArray("products");
        if (index >= 0) {
            JSONObject p = arr.optJSONObject(index);
            name.setText(p.optString("name"));
            price.setText(p.optString("price"));
        }

        new AlertDialog.Builder(this)
            .setTitle(index < 0 ? "নতুন পণ্য" : "পণ্য পরিবর্তন")
            .setView(form)
            .setPositiveButton("সংরক্ষণ", (d, w) -> {
                String n = name.getText().toString().trim();
                String pr = price.getText().toString().trim();
                if (!n.isEmpty()) {
                    try {
                        JSONObject p = index < 0 ? new JSONObject() : arr.getJSONObject(index);
                        p.put("name", n);
                        p.put("price", pr);
                        if (index < 0) arr.put(p);
                        saveData();
                        showProducts(selectedCustomer);
                    } catch (Exception ignored) {}
                }
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }


    private void showBackupOptions() {
        new AlertDialog.Builder(this).setTitle("তথ্য ব্যাকআপ ও পুনরুদ্ধার")
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
        } else if (requestCode == REQUEST_BACKUP_EXPORT) {
            try {
                JSONObject backup = new JSONObject();
                backup.put("app", "Amar Customer");
                backup.put("version", 1);
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
                    if (expense == null || expenseAmount(expense) <= 0)
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