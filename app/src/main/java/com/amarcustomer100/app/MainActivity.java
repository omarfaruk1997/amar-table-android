package com.amarcustomer100.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private final ArrayList<JSONObject> customers = new ArrayList<>();
    private LinearLayout root, listArea, header;
    private EditText search;
    private int selectedCustomer = -1;
    private static final int INK = Color.rgb(30, 41, 59);
    private static final int BLUE = Color.rgb(37, 99, 235);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadData();
        showCustomers();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size);
        t.setTextColor(INK); if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void base(String title, String subtitle) {
        root = column(); root.setBackgroundColor(Color.rgb(248,250,252));
        root.setPadding(dp(18), dp(18), dp(18), dp(12));
        setContentView(root);
        LinearLayout top = row(); top.setPadding(0, dp(8), 0, dp(14));
        TextView name = text(title, 25, true); top.addView(name, new LinearLayout.LayoutParams(0, dp(42), 1));
        root.addView(top);
        if (subtitle != null) { TextView sub = text(subtitle, 14, false); sub.setTextColor(Color.GRAY); root.addView(sub); }
        search = new EditText(this); search.setSingleLine(true); search.setTextSize(15);
        search.setHint("খুঁজুন..."); search.setPadding(dp(14),0,dp(14),0);
        search.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(48)); sp.topMargin=dp(14); sp.bottomMargin=dp(12);
        root.addView(search, sp);
        ScrollView scroll = new ScrollView(this); listArea = column(); scroll.addView(listArea);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); return b;
    }
    private void showCustomers() {
        selectedCustomer = -1;
        base("আমার কাস্টমার", "কাস্টমারের নাম যোগ করুন, পণ্য ও দাম সংরক্ষণ করুন");
        Button add = button("+ নতুন কাস্টমার যোগ করুন"); add.setTextColor(Color.WHITE); add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(48)); ap.bottomMargin=dp(12); root.addView(add, 2, ap);
        add.setOnClickListener(v -> customerDialog(-1));
        renderCustomers("");
        search.addTextChangedListener(watcher(s -> renderCustomers(s)));
    }
    private interface FilterAction { void run(String value); }
    private TextWatcher watcher(FilterAction action) {
        return new TextWatcher() { public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){ action.run(s.toString()); } public void afterTextChanged(Editable e){} };
    }
    private void renderCustomers(String filter) {
        listArea.removeAllViews();
        for (int i=0;i<customers.size();i++) {
            final int index=i; JSONObject c=customers.get(i); String name=c.optString("name");
            if (!name.toLowerCase().contains(filter.toLowerCase())) continue;
            LinearLayout item=row(); item.setPadding(dp(14),dp(10),dp(8),dp(10)); item.setBackgroundColor(Color.WHITE);
            LinearLayout info=column(); info.addView(text(name,17,true));
            info.addView(text(c.optJSONArray("products").length()+"টি পণ্য",12,false));
            item.addView(info,new LinearLayout.LayoutParams(0,dp(58),1));
            Button edit=button("সম্পাদনা"); item.addView(edit); edit.setOnClickListener(v->customerDialog(index));
            Button del=button("মুছুন"); item.addView(del); del.setOnClickListener(v->confirmDeleteCustomer(index));
            item.setOnClickListener(v->showProducts(index));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(8); listArea.addView(item,p);
        }
        if (listArea.getChildCount()==0) listArea.addView(text("এখনও কোনো কাস্টমার যোগ করা হয়নি।",15,false));
    }
    private void customerDialog(int index) {
        EditText input=new EditText(this); input.setHint("কাস্টমারের নাম");
        if(index>=0) input.setText(customers.get(index).optString("name"));
        new AlertDialog.Builder(this).setTitle(index<0?"নতুন কাস্টমার":"কাস্টমারের নাম পরিবর্তন").setView(input)
            .setPositiveButton("সংরক্ষণ",(d,w)->{String n=input.getText().toString().trim(); if(!n.isEmpty()){ if(index<0){JSONObject c=new JSONObject(); try{c.put("name",n);c.put("products",new JSONArray());customers.add(c);}catch(Exception ignored){}} else try{customers.get(index).put("name",n);}catch(Exception ignored){} saveData(); showCustomers();}})
            .setNegativeButton("বাতিল",null).show();
    }
    private void confirmDeleteCustomer(int index) {
        new AlertDialog.Builder(this).setMessage("এই কাস্টমার ও তার পণ্য মুছে ফেলবেন?").setPositiveButton("মুছুন",(d,w)->{customers.remove(index);saveData();showCustomers();}).setNegativeButton("বাতিল",null).show();
    }
    private void showProducts(int index) {
        if(index<0||index>=customers.size()) return; selectedCustomer=index;
        JSONObject c=customers.get(index);
        base(c.optString("name"), "পণ্যের নাম ও দাম যোগ করুন");
        Button add=button("+ নতুন পণ্য যোগ করুন"); add.setTextColor(Color.WHITE); add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(48)); ap.bottomMargin=dp(12); root.addView(add,2,ap);
        add.setOnClickListener(v->productDialog(-1));
        Button back=button("← কাস্টমার তালিকায় ফিরুন"); root.addView(back,3); back.setOnClickListener(v->showCustomers());
        renderProducts("");
        search.addTextChangedListener(watcher(s->renderProducts(s)));
    }
    private void renderProducts(String filter) {
        listArea.removeAllViews(); if(selectedCustomer<0||selectedCustomer>=customers.size())return;
        JSONArray products=customers.get(selectedCustomer).optJSONArray("products"); if(products==null)return;
        for(int i=0;i<products.length();i++) {
            JSONObject p=products.optJSONObject(i); if(p==null)continue; final int ix=i;
            String n=p.optString("name"), price=p.optString("price");
            if(!n.toLowerCase().contains(filter.toLowerCase()))continue;
            LinearLayout item=row(); item.setPadding(dp(12),dp(8),dp(6),dp(8)); item.setBackgroundColor(Color.WHITE);
            LinearLayout info=column(); info.addView(text(n,16,true)); info.addView(text("দাম: "+price,14,false));
            item.addView(info,new LinearLayout.LayoutParams(0,dp(58),1));
            Button edit=button("বদল"); item.addView(edit); edit.setOnClickListener(v->productDialog(ix));
            Button del=button("মুছুন"); item.addView(del); del.setOnClickListener(v->{products.remove(ix);saveData();renderProducts(search.getText().toString());});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);listArea.addView(item,lp);
        }
        if(listArea.getChildCount()==0)listArea.addView(text("এই কাস্টমারের কোনো পণ্য নেই।",15,false));
    }
    private void productDialog(int index) {
        LinearLayout form=column(); EditText n=new EditText(this);n.setHint("পণ্যের নাম");
        EditText price=new EditText(this);price.setHint("দাম");price.setInputType(8194); form.addView(n);form.addView(price);
        JSONArray arr=customers.get(selectedCustomer).optJSONArray("products"); if(index>=0){JSONObject p=arr.optJSONObject(index);n.setText(p.optString("name"));price.setText(p.optString("price"));}
        new AlertDialog.Builder(this).setTitle(index<0?"নতুন পণ্য":"পণ্য পরিবর্তন").setView(form)
            .setPositiveButton("সংরক্ষণ",(d,w)->{String name=n.getText().toString().trim(),pr=price.getText().toString().trim();if(!name.isEmpty()){try{JSONObject p=index<0?new JSONObject():arr.getJSONObject(index);p.put("name",name);p.put("price",pr);if(index<0)arr.put(p);saveData();renderProducts(search.getText().toString());}catch(Exception ignored){}}})
            .setNegativeButton("বাতিল",null).show();
    }
    private void saveData() { JSONArray a=new JSONArray(); for(JSONObject c:customers)a.put(c); getSharedPreferences("amar_customer",MODE_PRIVATE).edit().putString("data",a.toString()).apply(); }
    private void loadData() { try {JSONArray a=new JSONArray(getSharedPreferences("amar_customer",MODE_PRIVATE).getString("data","[]"));for(int i=0;i<a.length();i++)customers.add(a.getJSONObject(i));}catch(Exception ignored){} }
}
