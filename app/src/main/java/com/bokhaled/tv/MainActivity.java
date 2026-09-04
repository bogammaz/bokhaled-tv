package com.bokhaled.tv;

import android.app.Activity;
import android.app.AlertDialog;
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
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    static class Channel {
        String name, group, url, referrer;
        Channel(String n, String g, String u, String r) {
            name = n; group = g; url = u; referrer = r;
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
        normalizeGroups();
        replaceFreeSportsChannels();
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
        header.setPadding(dp(8), dp(4), dp(8), dp(4));
        header.setBackground(box(Color.rgb(10, 8, 3), 16, GOLD_DARK, 1));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.abk_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brand.addView(logo, new LinearLayout.LayoutParams(dp(82), dp(82)));

        TextView title = new TextView(this);
        title.setText("عبدالعزيز بوقماز");
        title.setTextColor(GOLD);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        brand.addView(title, new LinearLayout.LayoutParams(dp(150), dp(28)));

        header.addView(brand, new LinearLayout.LayoutParams(dp(160), dp(112)));

        ImageView skyline = new ImageView(this);
        skyline.setImageResource(R.drawable.abk_banner);
        skyline.setScaleType(ImageView.ScaleType.CENTER_CROP);
        skyline.setContentDescription("أبراج الكويت");
        header.addView(skyline, new LinearLayout.LayoutParams(0, dp(112), 1f));

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setFillViewport(true);

        categoryBar = new LinearLayout(this);
        categoryBar.setOrientation(LinearLayout.HORIZONTAL);
        categoryBar.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        categoryBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        hsv.addView(categoryBar, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(hsv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));

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
                "الكويت", "MBC", "الأخبارية", "الرياضة", "روتانا", "الشرق",
                "الجزيرة", "السعودية", "الإمارات", "قطر", "البحرين", "عمان",
                "مصر", "لبنان", "العراق", "المغرب", "الجزائر", "تونس",
                "فلسطين", "الأردن", "سوريا", "أطفال", "أفلام", "موسيقى"
        };

        for (String p : preferred) groups.add(p);
        for (Channel c : allChannels) groups.add(c.group);

        for (String g : groups) {
            if (!g.equals("★ المفضلة") && !g.equals("الكل") && !containsGroup(g)) continue;

            Button b = new Button(this);
            b.setText(g);
            b.setTextSize(18);
            b.setTextColor(Color.WHITE);
            b.setAllCaps(false);
            b.setSingleLine(true);
            b.setFocusable(true);
            b.setPadding(dp(22), 0, dp(22), 0);
            b.setBackground(box(TILE, 12, GOLD_DARK, 1));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
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
        for (Channel c : allChannels) if (g.equals(c.group)) return true;
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
        Intent in = new Intent(this, PlayerActivity.class);
        in.putStringArrayListExtra("names", names);
        in.putStringArrayListExtra("urls", urls);
        in.putStringArrayListExtra("referrers", referrers);
        in.putExtra("index", Math.max(0, index));
        startActivity(in);
    }

    private void toggleFavorite(Channel ch) {
        if (favorites.contains(ch.url)) favorites.remove(ch.url);
        else favorites.add(ch.url);

        prefs.edit().putStringSet("favorites", new HashSet<>(favorites)).apply();
        if ("★ المفضلة".equals(activeGroup)) filter(activeGroup);
        else adapter.notifyDataSetChanged();
    }

    private void loadPlaylist() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    getAssets().open("channels.m3u"), "UTF-8"));
            String line, name = null, group = "أخرى", referrer = "";
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

    private void normalizeGroups() {
        for (Channel c : allChannels) {
            if ("الأخبار".equals(c.group)) c.group = "الأخبارية";
            if ("رياضة".equals(c.group)) c.group = "الرياضة";
            if ("Rotana".equalsIgnoreCase(c.group)) c.group = "روتانا";
        }
    }

    private void replaceFreeSportsChannels() {
        List<Channel> keep = new ArrayList<>();
        for (Channel c : allChannels) {
            String n = c.name == null ? "" : c.name.toLowerCase(Locale.ROOT);
            boolean oldAlKass = n.contains("alkass") || n.contains("al kass")
                    || n.contains("الكاس") || n.contains("الكأس");
            if (!oldAlKass) keep.add(c);
        }
        allChannels.clear();
        allChannels.addAll(keep);

        allChannels.add(new Channel(
                "Al Kass 1 HD (مجاني)", "الرياضة",
                "https://liveakgr.alkassdigital.net/hls/live/2097037/Alkass1mhu/master.m3u8", ""));
        allChannels.add(new Channel(
                "Al Kass 2 HD (مجاني)", "الرياضة",
                "https://liveakgr.alkassdigital.net/hls/live/2097037/Alkass2hefazq/master.m3u8", ""));
        allChannels.add(new Channel(
                "Al Kass 3 HD (مجاني)", "الرياضة",
                "https://liveakgr.alkassdigital.net/hls/live/2097037/Alkass3vakazq/master.m3u8", ""));
        allChannels.add(new Channel(
                "beIN SPORTS XTRA (مجاني)", "الرياضة",
                "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ""));
    }

    private String extractAttr(String line, String attr) {
        String key = attr + "=\"";
        int s = line.indexOf(key);
        if (s < 0) return "";
        s += key.length();
        int e = line.indexOf('"', s);
        return e > s ? line.substring(s, e) : "";
    }

    @Override
    public void onBackPressed() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("الخروج من التطبيق")
                .setMessage("تبي تبقى بالتطبيق ولا تغلقه؟")
                .setPositiveButton("البقاء", (d, which) -> d.dismiss())
                .setNegativeButton("إغلاق التطبيق", (d, which) -> finishAffinity())
                .create();
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus());
        dialog.show();
    }

    class ChannelAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<Channel> data;

        ChannelAdapter(Context c, List<Channel> d) { ctx = c; data = d; }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int p) { return data.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView t = convertView instanceof TextView
                    ? (TextView) convertView : new TextView(ctx);

            t.setTextColor(Color.WHITE);
            t.setTextSize(19);
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(10), dp(10), dp(10), dp(10));
            t.setFocusable(true);
            t.setMinHeight(dp(108));

            Channel ch = data.get(position);
            t.setText((favorites.contains(ch.url) ? "★ " : "") + ch.name);
            t.setBackground(box(TILE, 15, GOLD_DARK, 1));

            t.setOnClickListener(v -> openChannel(ch));
            t.setOnLongClickListener(v -> { toggleFavorite(ch); return true; });
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
                    tv.setBackground(box(GOLD_DARK, 15, GOLD, 4));
                    tv.setElevation(dp(10));
                    grid.post(() -> {
                        int first = grid.getFirstVisiblePosition();
                        int last = grid.getLastVisiblePosition();
                        int cols = Math.max(1, grid.getNumColumns());
                        if (position >= last - cols + 1)
                            grid.smoothScrollToPosition(
                                    Math.min(data.size() - 1, position + cols));
                        else if (position <= first + cols - 1)
                            grid.smoothScrollToPosition(
                                    Math.max(0, position - cols));
                    });
                } else {
                    tv.setBackground(box(TILE, 15, GOLD_DARK, 1));
                    tv.setElevation(0);
                }
            });

            return t;
        }
    }
}
