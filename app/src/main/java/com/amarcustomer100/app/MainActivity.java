package com.amarcustomer100.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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

    private static final int BG = Color.rgb(245, 247, 251);
    private static final int INK = Color.rgb(25, 39, 58);
    private static final int MUTED = Color.rgb(112, 125, 143);
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int PALE = Color.rgb(232, 240, 255);
    private static final int BORDER = Color.rgb(229, 234, 242);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadData();
        showCustomers();
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView label(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
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
        e.setTextColor(INK);
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
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(12));
        setContentView(root);

        LinearLayout brand = row();
        brand.setPadding(dp(15), dp(14), dp(15), dp(14));
        GradientDrawable banner = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(29, 78, 216), Color.rgb(14, 165, 233)});
        banner.setCornerRadius(dp(20));
        brand.setBackground(banner);
        brand.setElevation(dp(3));
        TextView mark = label("AC", 17, true, BLUE);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(shape(Color.WHITE, 0, 14));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titles = column();
        titles.setPadding(dp(12), 0, 0, 0);
        titles.addView(label(title, 22, true, Color.WHITE));
        TextView sub = label(subtitle, 12, false, Color.rgb(220, 235, 255));
        titles.addView(sub);
        brand.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(brand);

        search = field("⌕   নাম, ঠিকানা বা নম্বর লিখে খুঁজুন...",  android.text.InputType.TYPE_CLASS_TEXT, false);
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
        TextView add = action("+   নতুন কাস্টমার যোগ করুন", BLUE, Color.WHITE, 14);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(52));
        ap.topMargin = dp(8);
        ap.bottomMargin = dp(14);
        root.addView(add, 2, ap);
        add.setOnClickListener(v -> customerDialog(-1));

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
        String q = filter.toLowerCase(Locale.ROOT).trim();

        for (int i = 0; i < customers.size(); i++) {
            final int index = i;
            JSONObject c = customers.get(i);
            String name = c.optString("name");
            String address = c.optString("address");
            String phone = c.optString("phone");
            if (!(name + " " + address + " " + phone).toLowerCase(Locale.ROOT).contains(q)) continue;

            LinearLayout card = column();
            card.setPadding(dp(15), dp(13), dp(13), dp(12));
            card.setBackground(shape(Color.WHITE, BORDER, 16));
            card.setElevation(dp(2));

            LinearLayout top = row();
            TextView customerName = label(name, 18, true, INK);
            top.addView(customerName, new LinearLayout.LayoutParams(0, dp(34), 1));

            card.addView(top);

            TextView addr = label("⌖  " + (address.trim().isEmpty() ? "ঠিকানা যোগ করা হয়নি" : address), 13, false,
                    address.trim().isEmpty() ? MUTED : Color.rgb(76, 91, 111));
            LinearLayout.LayoutParams adp = new LinearLayout.LayoutParams(-1, -2);
            adp.topMargin = dp(5);
            card.addView(addr, adp);

            TextView count = label(productCount(c) + "টি পণ্য", 12, true, MUTED);
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

        TextView add = action("+   নতুন পণ্য যোগ করুন", BLUE, Color.WHITE, 14);
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

        renderProducts("");
        search.addTextChangedListener(watcher(s -> renderProducts(s)));
    }

    private void renderProducts(String filter) {
        listArea.removeAllViews();
        if (selectedCustomer < 0 || selectedCustomer >= customers.size()) return;
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
            card.setPadding(dp(14), dp(12), dp(10), dp(12));
            card.setBackground(shape(Color.WHITE, BORDER, 15));
            card.setElevation(dp(2));
            LinearLayout info = column();
            info.addView(label(name, 16, true, INK));
            info.addView(label("দাম: " + price, 13, false, MUTED));
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
            del.setOnClickListener(v -> {
                products.remove(ix);
                saveData();
                renderProducts(search.getText().toString());
            });

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
                        renderProducts(search.getText().toString());
                    } catch (Exception ignored) {}
                }
            })
            .setNegativeButton("বাতিল", null)
            .show();
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
