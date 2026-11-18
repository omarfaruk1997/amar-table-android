package com.amarcustomer100.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private LinearLayout root, listArea;
    private EditText search;
    private int selectedCustomer = -1;
    private boolean darkMode = false;
    private static final int REQUEST_BACKUP_EXPORT = 7101;
    private static final int REQUEST_BACKUP_IMPORT = 7102;

    private static final int BG = Color.rgb(246, 247, 250);
    private static final int INK = Color.rgb(23, 33, 52);
    private static final int MUTED = Color.rgb(112, 125, 143);
    private static final int BLUE = Color.rgb(31, 75, 145);
    private static final int PALE = Color.rgb(239, 243, 249);
    private static final int TEAL = Color.rgb(13, 125, 112);
    private static final int BORDER = Color.rgb(229, 234, 242);

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
        b.setClickable(true);
        b.setFocusable(true);
        return b;
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
        e.setBackground(shape(Color.WHITE, BORDER, 12));
        return e;
    }

    private void base(String title, String subtitle) {
        root = column();
        root.setBackgroundColor(darkMode ? Color.rgb(15, 23, 36) : BG);
        root.setPadding(dp(16), dp(16), dp(16), dp(12));
        setContentView(root);
        getWindow().setStatusBarColor(darkMode ? Color.rgb(9, 15, 25) : Color.rgb(15, 29, 52));
        getWindow().setNavigationBarColor(darkMode ? Color.rgb(15, 23, 36) : Color.rgb(246, 247, 250));

        LinearLayout brand = row();
        brand.setPadding(dp(17), dp(19), dp(15), dp(19));
        GradientDrawable banner = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(5, 19, 43), Color.rgb(12, 91, 105), Color.rgb(158, 112, 47)});
        banner.setCornerRadius(dp(22));
        brand.setBackground(banner);
        brand.setElevation(dp(10));
        TextView mark = label("সা", 23, true, Color.rgb(16, 38, 63));
        mark.setTextColor(Color.rgb(16, 38, 63));
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        GradientDrawable markBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(255, 246, 220), Color.rgb(231, 202, 145)});
        markBg.setCornerRadius(dp(17));
        markBg.setStroke(dp(1), Color.rgb(255, 226, 163));
        mark.setBackground(markBg);
        mark.setElevation(dp(3));
        mark.setContentDescription("কাস্টমার সাথী লোগো");
        brand.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout titles = column();
        titles.setPadding(dp(12), 0, 0, 0);
        titles.addView(label(title, 22, true, Color.WHITE));
        TextView sub = label(subtitle, 12, false, Color.rgb(230, 218, 194));
        titles.addView(sub);
        brand.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView version = label("11.0", 12, true, Color.rgb(255, 226, 163));
        version.setGravity(Gravity.CENTER);
        version.setContentDescription("অ্যাপ সংস্করণ ১১.০");
        version.setPadding(dp(10), dp(7), dp(10), dp(7));
        version.setBackground(shape(Color.rgb(61, 75, 94), Color.rgb(166, 132, 73), 20));
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
        search.setElevation(dp(2));
        search.setSelectAllOnFocus(false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(50));
        sp.topMargin = dp(18);
        sp.bottomMargin = dp(12);
        root.addView(search, sp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listArea = column();
        scroll.addView(listArea);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showCustomers() {
        selectedCustomer = -1;
        base("কাস্টমার সাথী", "কাস্টমার, ঠিকানা ও যোগাযোগের তথ্য");
        TextView add = action("+   নতুন কাস্টমার যোগ করুন", Color.rgb(31, 75, 110), Color.WHITE, 14);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(52));
        ap.topMargin = dp(8);
        ap.bottomMargin = dp(14);
        root.addView(add, 3, ap);
        add.setOnClickListener(v -> customerDialog(-1));

        LinearLayout dashboard = row();
        dashboard.setPadding(dp(10), dp(14), dp(10), dp(14));
        dashboard.setBackground(shape(Color.rgb(255, 255, 255), Color.rgb(224, 215, 194), 20));
        dashboard.setElevation(dp(4));
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
        dashParams.bottomMargin = dp(12);
        root.addView(dashboard, 2, dashParams);

        TextView backup = action("☁   তথ্য ব্যাকআপ ও পুনরুদ্ধার", darkMode ? Color.rgb(37, 51, 70) : Color.rgb(239, 233, 219), darkMode ? Color.rgb(255, 226, 163) : Color.rgb(112, 80, 29), 14);
        LinearLayout.LayoutParams backupParams = new LinearLayout.LayoutParams(-1, dp(46));
        backupParams.bottomMargin = dp(12);
        root.addView(backup, 4, backupParams);
        backup.setOnClickListener(v -> showBackupOptions());

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
            card.setPadding(dp(16), dp(15), dp(15), dp(14));
            card.setBackground(shape(Color.WHITE, Color.rgb(228, 232, 239), 20));
            card.setElevation(dp(7));
            card.setAlpha(0f);
            card.setTranslationY(dp(12));
            card.animate().alpha(1f).translationY(0f).setDuration(260).start();

            LinearLayout top = row();
            TextView avatar = label(name.isEmpty() ? "?" : name.substring(0, 1), 19, true, BLUE);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(shape(Color.rgb(232, 241, 252), 0, 15));
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

        if (listArea.getChildCount() == 0) {
            TextView empty = label(customers.isEmpty() ? "এখনও কোনো কাস্টমার যোগ করা হয়নি।" : "কোনো কাস্টমার পাওয়া যায়নি।", 14, false, MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(35), 0, dp(35));
            listArea.addView(empty);
        }
    }

    private LinearLayout statTile(String heading, String value, String symbol, int accent) {
        LinearLayout tile = column();
        tile.setGravity(Gravity.CENTER);
        TextView icon = label(symbol, 18, true, accent);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(Color.rgb(239, 245, 255), 0, 11));
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

        TextView add = action("+   নতুন পণ্য যোগ করুন", Color.rgb(31, 75, 110), Color.WHITE, 14);
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

        TextView shareReport = action("↗   কাস্টমার রিপোর্ট শেয়ার করুন",
                darkMode ? Color.rgb(37, 51, 70) : Color.rgb(232, 242, 255),
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
            report.append("কাস্টমার সাথী — কাস্টমার রিপোর্ট\\n\\n");
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
            card.setPadding(dp(15), dp(14), dp(11), dp(14));
            card.setBackground(shape(Color.WHITE, Color.rgb(228, 232, 239), 19));
            card.setElevation(dp(4));
            card.setAlpha(0f);
            card.setTranslationY(dp(10));
            card.animate().alpha(1f).translationY(0f).setDuration(240).start();
            TextView productMark = label("▦", 20, true, BLUE);
            productMark.setGravity(Gravity.CENTER);
            productMark.setBackground(shape(Color.rgb(232, 241, 252), 0, 12));
            LinearLayout.LayoutParams productMarkParams = new LinearLayout.LayoutParams(dp(42), dp(44));
            productMarkParams.rightMargin = dp(11);
            card.addView(productMark, productMarkParams);
            LinearLayout info = column();
            info.addView(label(name, 16, true, INK));
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

        if (listArea.getChildCount() == 0) {
            TextView empty = label(products.length() == 0 ? "এই কাস্টমারের কোনো পণ্য নেই।" : "কোনো পণ্য পাওয়া যায়নি।", 14, false, MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            listArea.addView(empty);
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
        if (requestCode == REQUEST_BACKUP_EXPORT) {
            try {
                JSONObject backup = new JSONObject();
                backup.put("app", "Customer Sathi");
                backup.put("version", 1);
                backup.put("createdAt", System.currentTimeMillis());
                JSONArray saved = new JSONArray();
                for (JSONObject customer : customers) saved.put(customer);
                backup.put("customers", saved);
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
    }
}