package com.bokhaled.tv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    static class Channel {
        String name;
        String group;
        String url;
        String referrer;
        Channel(String n, String g, String u, String r) {
            name=n; group=g; url=u; referrer=r;
        }
    }

    private final List<Channel> allChannels = new ArrayList<>();
    private final List<Channel> shownChannels = new ArrayList<>();
    private GridView grid;
    private ChannelAdapter adapter;
    private LinearLayout categoryBar;
    private String activeGroup = "الكل";
    private final int BG = Color.rgb(15,17,21);
    private final int TILE = Color.rgb(31,35,43);
    private final int TILE_FOCUS = Color.rgb(71,79,92);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        loadPlaylist();
        buildUi();
    }

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(28), dp(20), dp(28), dp(20));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("قنوات الوالد");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setPadding(dp(8),0,dp(8),dp(12));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setFillViewport(true);
        categoryBar = new LinearLayout(this);
        categoryBar.setOrientation(LinearLayout.HORIZONTAL);
        categoryBar.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        categoryBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        hsv.addView(categoryBar, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(hsv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(74)));

        grid = new GridView(this);
        grid.setNumColumns(5);
        grid.setHorizontalSpacing(dp(16));
        grid.setVerticalSpacing(dp(16));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setGravity(Gravity.CENTER);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(10), 0, dp(20));
        grid.setSelector(android.R.color.transparent);

        adapter = new ChannelAdapter(this, shownChannels);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            Channel ch = shownChannels.get(position);
            Intent in = new Intent(MainActivity.this, PlayerActivity.class);
            in.putExtra("name", ch.name);
            in.putExtra("url", ch.url);
            in.putExtra("referrer", ch.referrer == null ? "" : ch.referrer);
            startActivity(in);
        });

        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        buildCategories();
        filter("الكل");
    }

    private void buildCategories() {
        Set<String> groups = new LinkedHashSet<>();
        groups.add("الكل");
        // Preferred order
        String[] preferred = new String[]{
                "الكويت","الأخبار","MBC","روتانا","الجزيرة","الشرق","الإمارات",
                "السعودية","قطر","البحرين","عمان","مصر","أطفال","دولية عربية"
        };
        for (String p : preferred) groups.add(p);
        for (Channel c : allChannels) groups.add(c.group);

        for (String g : groups) {
            if (!g.equals("الكل") && !containsGroup(g)) continue;
            Button b = new Button(this);
            b.setText(g);
            b.setTextSize(19);
            b.setTextColor(Color.WHITE);
            b.setAllCaps(false);
            b.setSingleLine(true);
            b.setFocusable(true);
            b.setPadding(dp(26), 0, dp(26), 0);
            b.setBackground(rounded(TILE, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(56));
            lp.setMargins(dp(7), dp(5), dp(7), dp(5));
            categoryBar.addView(b, lp);

            b.setOnFocusChangeListener((v, hasFocus) -> {
                v.setScaleX(hasFocus ? 1.08f : 1f);
                v.setScaleY(hasFocus ? 1.08f : 1f);
                v.setBackground(rounded(hasFocus ? TILE_FOCUS : TILE, 12));
            });
            b.setOnClickListener(v -> filter(g));
        }
    }

    private boolean containsGroup(String g) {
        for (Channel c : allChannels) if (g.equals(c.group)) return true;
        return false;
    }

    private void filter(String group) {
        activeGroup = group;
        shownChannels.clear();
        for (Channel c : allChannels) {
            if ("الكل".equals(group) || group.equals(c.group)) shownChannels.add(c);
        }
        adapter.notifyDataSetChanged();
        if (!shownChannels.isEmpty()) {
            grid.post(() -> grid.setSelection(0));
        }
    }

    private void loadPlaylist() {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(getAssets().open("channels.m3u"), "UTF-8"));
            String line;
            String name = null, group = "أخرى", referrer = "";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXTINF:")) {
                    int comma = line.lastIndexOf(',');
                    name = comma >= 0 ? line.substring(comma + 1).trim() : "قناة";
                    group = extractAttr(line, "group-title");
                    if (group == null || group.trim().isEmpty()) group = "أخرى";
                    referrer = "";
                } else if (line.startsWith("#EXTVLCOPT:http-referrer=")) {
                    referrer = line.substring("#EXTVLCOPT:http-referrer=".length()).trim();
                } else if (!line.isEmpty() && !line.startsWith("#") && name != null) {
                    allChannels.add(new Channel(name, group, line, referrer));
                    name = null; group = "أخرى"; referrer = "";
                }
            }
            br.close();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر قراءة قائمة القنوات", Toast.LENGTH_LONG).show();
        }
    }

    private String extractAttr(String line, String attr) {
        String key = attr + "=\"";
        int s = line.indexOf(key);
        if (s < 0) return "";
        s += key.length();
        int e = line.indexOf('"', s);
        return e > s ? line.substring(s, e) : "";
    }

    class ChannelAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<Channel> data;

        ChannelAdapter(Context c, List<Channel> d) { ctx=c; data=d; }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int p) { return data.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView t;
            if (convertView instanceof TextView) {
                t = (TextView) convertView;
            } else {
                t = new TextView(ctx);
                t.setTextColor(Color.WHITE);
                t.setTextSize(21);
                t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                t.setGravity(Gravity.CENTER);
                t.setPadding(dp(12), dp(12), dp(12), dp(12));
                t.setFocusable(true);
                t.setFocusableInTouchMode(false);
                t.setMinHeight(dp(112));
            }

            Channel ch = data.get(position);
            t.setText(ch.name);
            t.setBackground(rounded(TILE, 16));
            t.setScaleX(1f); t.setScaleY(1f);

            t.setOnFocusChangeListener((v, hasFocus) -> {
                TextView tv = (TextView)v;
                tv.setBackground(rounded(hasFocus ? TILE_FOCUS : TILE, 16));
                tv.setScaleX(hasFocus ? 1.08f : 1f);
                tv.setScaleY(hasFocus ? 1.08f : 1f);
                tv.setElevation(hasFocus ? dp(12) : 0);
            });
            return t;
        }
    }
}
