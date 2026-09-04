package com.bokhaled.tv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
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
            name = n;
            group = g;
            url = u;
            referrer = r;
        }
    }

    private final List<Channel> allChannels = new ArrayList<>();
    private final List<Channel> shownChannels = new ArrayList<>();

    private GridView grid;
    private ChannelAdapter adapter;
    private LinearLayout categoryBar;
    private SharedPreferences prefs;
    private Set<String> favorites = new HashSet<>();
    private String activeGroup = "★ المفضلة";

    private final int BG = Color.rgb(7, 7, 7);
    private final int TILE = Color.rgb(18, 18, 18);
    private final int GOLD = Color.rgb(212, 175, 55);
    private final int GOLD_DARK = Color.rgb(74, 54, 10);

    private static final int MAX_FAVORITES = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        prefs = getSharedPreferences("bogammaz_tv", MODE_PRIVATE);
        favorites = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));

        loadPlaylist();
        buildUi();
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable box(int fill, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (strokeWidth > 0) g.setStroke(dp(strokeWidth), strokeColor);
        return g;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(24), dp(12), dp(24), dp(18));
        root.setClipChildren(false);
        root.setClipToPadding(false);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        header.setClipChildren(false);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.abk_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("ABK IPTV");
        header.addView(logo, new LinearLayout.LayoutParams(dp(96), dp(96)));

        TextView title = new TextView(this);
        title.setText("عبدالعزيز بوقماز");
        title.setTextColor(GOLD);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setPadding(dp(18), 0, dp(8), 0);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(82), 1f));

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(102)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setFillViewport(true);
        hsv.setClipChildren(false);
        hsv.setClipToPadding(false);

        categoryBar = new LinearLayout(this);
        categoryBar.setOrientation(LinearLayout.HORIZONTAL);
        categoryBar.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        categoryBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        categoryBar.setClipChildren(false);

        hsv.addView(categoryBar, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(hsv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));

        grid = new GridView(this);
        grid.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        grid.setNumColumns(5);
        grid.setHorizontalSpacing(dp(14));
        grid.setVerticalSpacing(dp(14));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setGravity(Gravity.CENTER);
        grid.setClipToPadding(false);
        grid.setClipChildren(false);
        grid.setPadding(dp(8), dp(10), dp(8), dp(20));
        grid.setSelector(android.R.color.transparent);
        grid.setFocusable(true);
        grid.setFocusableInTouchMode(false);
        grid.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        adapter = new ChannelAdapter(this, shownChannels);
        grid.setAdapter(adapter);

        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        buildCategories();
        filter("★ المفضلة");
    }

    private void buildCategories() {
        categoryBar.removeAllViews();

        Set<String> groups = new LinkedHashSet<>();
        groups.add("★ المفضلة");
        groups.add("الكل");

        String[] preferred = new String[] {
                "الكويت", "الشرق", "الجزيرة", "MBC", "السعودية", "الإمارات", "قطر", "البحرين",
                "عمان", "مصر", "لبنان", "العراق", "المغرب", "الجزائر",
                "تونس", "فلسطين", "الأردن", "سوريا", "الأخبار",
                "رياضة", "أطفال", "أفلام", "موسيقى"
        };

        for (String p : preferred) groups.add(p);
        for (Channel c : allChannels) groups.add(c.group);

        for (String g : groups) {
            if (!g.equals("★ المفضلة") && !g.equals("الكل") && !containsGroup(g)) {
                continue;
            }

            Button b = new Button(this);
            b.setText(g);
            b.setTextSize(18);
            b.setTextColor(Color.WHITE);
            b.setAllCaps(false);
            b.setSingleLine(true);
            b.setFocusable(true);
            b.setFocusableInTouchMode(false);
            b.setPadding(dp(22), 0, dp(22), 0);
            b.setBackground(box(TILE, 12, GOLD_DARK, 1));

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            dp(54));
            lp.setMargins(dp(6), dp(4), dp(6), dp(4));
            categoryBar.addView(b, lp);

            b.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackground(box(GOLD_DARK, 12, GOLD, 3));
                    v.setElevation(dp(8));
                } else {
                    v.setBackground(box(TILE, 12, GOLD_DARK, 1));
                    v.setElevation(0);
                }
            });

            b.setOnClickListener(v -> filter(g));

            b.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusFirstChannel();
                    return true;
                }
                return false;
            });
        }
    }

    private boolean containsGroup(String g) {
        for (Channel c : allChannels) {
            if (g.equals(c.group)) return true;
        }
        return false;
    }

    private void filter(String group) {
        activeGroup = group;
        shownChannels.clear();

        for (Channel c : allChannels) {
            if ("★ المفضلة".equals(group)) {
                if (favorites.contains(c.url)) shownChannels.add(c);
            } else if ("الكل".equals(group) || group.equals(c.group)) {
                shownChannels.add(c);
            }
        }

        adapter.notifyDataSetChanged();

        if ("★ المفضلة".equals(group) && shownChannels.isEmpty()) {
            Toast.makeText(
                    this,
                    "المفضلة فاضية — اضغط ضغط مطوّل على أي قناة لإضافتها",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void focusFirstChannel() {
        if (shownChannels.isEmpty()) return;

        grid.setSelection(0);
        grid.postDelayed(() -> {
            View first = grid.getChildAt(0);
            if (first != null) first.requestFocus();
            else grid.requestFocus();
        }, 100);
    }

    private void openChannel(Channel ch) {
        int index = shownChannels.indexOf(ch);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> referrers = new ArrayList<>();

        for (Channel c : shownChannels) {
            names.add(c.name);
            urls.add(c.url);
            referrers.add(c.referrer == null ? "" : c.referrer);
        }

        Intent in = new Intent(MainActivity.this, PlayerActivity.class);
        in.putStringArrayListExtra("names", names);
        in.putStringArrayListExtra("urls", urls);
        in.putStringArrayListExtra("referrers", referrers);
        in.putExtra("index", Math.max(0, index));
        startActivity(in);
    }

    private void toggleFavorite(Channel ch) {
        if (favorites.contains(ch.url)) {
            favorites.remove(ch.url);
            Toast.makeText(
                    this,
                    "تم حذف " + ch.name + " من المفضلة",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            if (favorites.size() >= MAX_FAVORITES) {
                Toast.makeText(
                        this,
                        "المفضلة فيها 14 قناة — احذف قناة أول",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            favorites.add(ch.url);
            Toast.makeText(
                    this,
                    "★ تمت إضافة " + ch.name + " للمفضلة",
                    Toast.LENGTH_SHORT
            ).show();
        }

        prefs.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();

        if ("★ المفضلة".equals(activeGroup)) filter(activeGroup);
        else adapter.notifyDataSetChanged();
    }

    private void loadPlaylist() {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            getAssets().open("channels.m3u"),
                            "UTF-8"
                    )
            );

            String line;
            String name = null;
            String group = "أخرى";
            String referrer = "";

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("#EXTINF:")) {
                    int comma = line.lastIndexOf(',');
                    name = comma >= 0
                            ? line.substring(comma + 1).trim()
                            : "قناة";

                    group = extractAttr(line, "group-title");
                    if (group == null || group.trim().isEmpty()) group = "أخرى";
                    referrer = "";

                } else if (line.startsWith("#EXTVLCOPT:http-referrer=")) {
                    referrer = line.substring(
                            "#EXTVLCOPT:http-referrer=".length()
                    ).trim();

                } else if (!line.isEmpty() &&
                        !line.startsWith("#") &&
                        name != null) {

                    allChannels.add(
                            new Channel(name, group, line, referrer)
                    );

                    name = null;
                    group = "أخرى";
                    referrer = "";
                }
            }

            br.close();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "تعذر قراءة قائمة القنوات",
                    Toast.LENGTH_LONG
            ).show();
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

        ChannelAdapter(Context c, List<Channel> d) {
            ctx = c;
            data = d;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int p) {
            return data.get(p);
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView t;

            if (convertView instanceof TextView) {
                t = (TextView) convertView;
            } else {
                t = new TextView(ctx);
                t.setTextColor(Color.WHITE);
                t.setTextSize(19);
                t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                t.setGravity(Gravity.CENTER);
                t.setPadding(dp(10), dp(10), dp(10), dp(10));
                t.setFocusable(true);
                t.setFocusableInTouchMode(false);
                t.setMinHeight(dp(108));
            }

            Channel ch = data.get(position);
            String star = favorites.contains(ch.url) ? "★ " : "";
            t.setText(star + ch.name);

            t.setBackground(box(TILE, 15, GOLD_DARK, 1));
            t.setTextColor(Color.WHITE);
            t.setScaleX(1f);
            t.setScaleY(1f);
            t.setElevation(0);

            t.setOnClickListener(v -> openChannel(ch));

            t.setOnLongClickListener(v -> {
                toggleFavorite(ch);
                return true;
            });

            t.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_MENU) {
                    toggleFavorite(ch);
                    return true;
                }
                return false;
            });

            t.setOnFocusChangeListener((v, hasFocus) -> {
                TextView tv = (TextView) v;

                if (hasFocus) {
                    // بدون تكبير حتى ما ينقص الإطار يمين/يسار
                    tv.setBackground(box(GOLD_DARK, 15, GOLD, 4));
                    tv.setElevation(dp(10));
                } else {
                    tv.setBackground(box(TILE, 15, GOLD_DARK, 1));
                    tv.setElevation(0);
                }
            });

            return t;
        }
    }
}
