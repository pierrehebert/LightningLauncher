/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.util;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.LLAppPhone;
import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.views.MyViewPager;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AddItemDialog extends AlertDialog implements View.OnClickListener, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {
    public static final int AI_APP = 0;
    public static final int AI_SHORTCUT = 1;
    public static final int AI_TEXT = 3;
    public static final int AI_ICON = 4;
    public static final int AI_DUMMY = 5;
    public static final int AI_FOLDER = 6;
    public static final int AI_PANEL = 7;
    public static final int AI_SIDE_BAR = 8;
    public static final int AI_WIDGET = 9;
    //    private static final int AI_LLWIDGET = 10;
    public static final int AI_BADGE = 11;
    public static final int AI_DYNAMIC_TEXT = 12;
    public static final int AI_PAGE_INDICATOR = 13;
    public static final int AI_UNLOCKER = 14;
    public static final int AI_STOP_POINT = 15;
    public static final int AI_BOOKMARK = 16;
    public static final int AI_LIGHTNING_ACTION = 17;
    public static final int AI_CUSTOM_VIEW = 18;

    public interface AddItemDialogInterface {
        boolean isDialogAddItemEnabled(int id);
        void onBuiltinItemClicked(int id);
        void onPluginClicked(Plugin plugin);
        void onPluginLongClicked(Plugin plugin);
    }

    public static class Plugin {
        public CharSequence label;
        public Drawable icon;
        public Intent intent;

        public Plugin(CharSequence label, Drawable icon, Intent intent) {
            this.label = label;
            this.icon = icon;
            this.intent = intent;
        }
    }

    private AddItemDialogInterface mAddItemDialogInterface;
    private LayoutInflater mInflater;

    public AddItemDialog(final Context context, boolean showPlugins, final AddItemDialogInterface addItemDialogInterface) {
        super(context);

        mAddItemDialogInterface = addItemDialogInterface;

        String[] labels=context.getResources().getStringArray(R.array.dialog_action_values);

        mInflater = LayoutInflater.from(context);
        final ViewGroup content = (ViewGroup) mInflater.inflate(R.layout.add_dialog, null);
        final MyViewPager pager = (MyViewPager) content.findViewById(R.id.pager);
        ViewGroup builtinsView = (ViewGroup) content.findViewById(R.id.builtins);
        final ListView pluginsListView = (ListView) content.findViewById(R.id.plugins);
        final TabHost tabHost = (TabHost) content.findViewById(R.id.tab);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("B").setIndicator(context.getString(R.string.ad_b)).setContent(R.id.empty));
        tabHost.addTab(tabHost.newTabSpec("P").setIndicator(context.getString(R.string.ad_p)).setContent(R.id.empty));
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                pager.setCurrentItem("B".equals(tabId) ? 0 : 1);
            }
        });
        pager.setOnPageChangeListener(new MyViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                tabHost.setCurrentTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        pluginsListView.setOnItemClickListener(this);
        pluginsListView.setOnItemLongClickListener(this);

        setView(content);

        if(!showPlugins) {
            tabHost.setVisibility(View.GONE);
            pager.removeViewAt(1);
        } else {
            new AsyncTask<Void, Void, ArrayList<Plugin>>() {
                @Override
                protected ArrayList<Plugin> doInBackground(Void... params) {
                    Intent filter = new Intent("net.pierrox.lightning_launcher.script.PLUGIN");
                    final PackageManager pm = context.getPackageManager();
                    List<ResolveInfo> resolveInfos = pm.queryIntentActivities(filter, PackageManager.MATCH_ALL);
                    ArrayList<Plugin> plugins = new ArrayList<>(resolveInfos.size() + 1);
                    for (ResolveInfo ri : resolveInfos) {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
                        plugins.add(new Plugin(ri.loadLabel(pm), ri.loadIcon(pm), intent));
                    }
                    Collections.sort(plugins, new Comparator<Plugin>() {
                        @Override
                        public int compare(Plugin lhs, Plugin rhs) {
                            return Utils.sItemNameCollator.compare(lhs.label, rhs.label);
                        }
                    });
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(context, Dashboard.class));
                    plugins.add(0, new Plugin(context.getString(R.string.ls_b), context.getResources().getDrawable(R.drawable.icon), intent));
                    return plugins;
                }

                @Override
                protected void onPostExecute(ArrayList<Plugin> plugins) {
                    pluginsListView.setAdapter(new PluginAdapter(context, plugins));
                }
            }.execute((Void) null);
        }

        ViewGroup cat;

        cat = addDialogAddCategory(builtinsView, R.string.ac_s);
        addDialogAddItem(cat, labels[0], "h", AI_APP);
        addDialogAddItem(cat, labels[1], "e", AI_SHORTCUT);
        addDialogAddItem(cat, context.getString(R.string.ai_t), "k", AI_TEXT);
        addDialogAddItem(cat, context.getString(R.string.ai_i), "X", AI_ICON);

        cat = addDialogAddCategory(builtinsView, R.string.ac_c);
        addDialogAddItem(cat, labels[4], "f", AI_FOLDER);
        addDialogAddItem(cat, context.getString(R.string.efolder), "i", AI_PANEL);
        addDialogAddItem(cat, context.getString(R.string.ai_sb), "j", AI_SIDE_BAR);

        cat = addDialogAddCategory(builtinsView, R.string.ac_d);
        addDialogAddItem(cat, labels[2], "N", AI_WIDGET);
        addDialogAddItem(cat, context.getString(R.string.ai_b), "g", AI_BADGE);
        addDialogAddItem(cat, context.getString(R.string.dtext), "l", AI_DYNAMIC_TEXT);

        cat = addDialogAddCategory(builtinsView, R.string.ac_n);
        addDialogAddItem(cat, context.getString(R.string.ai_bo), "m", AI_BOOKMARK);
        addDialogAddItem(cat, labels[5], "c", AI_STOP_POINT);
        addDialogAddItem(cat, context.getString(R.string.pi), "d", AI_PAGE_INDICATOR);

        cat = addDialogAddCategory(builtinsView, R.string.ac_m);
        addDialogAddItem(cat, context.getString(R.string.shortcut_actions), "o", AI_LIGHTNING_ACTION);
        addDialogAddItem(cat, context.getString(R.string.ai_d), "n", AI_DUMMY);
        addDialogAddItem(cat, context.getString(R.string.i_ul), "b", AI_UNLOCKER);
        addDialogAddItem(cat, context.getString(R.string.cv), "1", AI_CUSTOM_VIEW);

    }

    @Override
    public void onClick(View v) {
        int id = (int) v.getTag();
        mAddItemDialogInterface.onBuiltinItemClicked(id);
        dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mAddItemDialogInterface.onPluginClicked((Plugin) parent.getItemAtPosition(position));
        dismiss();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        mAddItemDialogInterface.onPluginLongClicked((Plugin) parent.getItemAtPosition(position));
        dismiss();
        return true;
    }

    private ViewGroup addDialogAddCategory(ViewGroup root, int title) {
        View cat = mInflater.inflate(R.layout.add_dialog_cat, root, false);
        ((TextView)cat.findViewById(R.id.title)).setText(title);
        root.addView(cat);
        return (ViewGroup) cat.findViewById(R.id.content);
    }

    private void addDialogAddItem(ViewGroup root, String label, String icon, int id) {
        View item = mInflater.inflate(R.layout.add_dialog_item, root, false);
        TextView i = (TextView)item.findViewById(R.id.icon);
        i.setText(icon);
        LLAppPhone app = (LLAppPhone) LLApp.get();
        i.setTypeface(app.getIconsTypeface());
        ((TextView)item.findViewById(R.id.label)).setText(label);
        item.setOnClickListener(this);
        item.setTag(id);
        Utils.setEnabledStateOnViews(item, mAddItemDialogInterface.isDialogAddItemEnabled(id));
        final boolean is_locked = app.isFreeVersion() && (id == AI_BADGE || id == AI_DYNAMIC_TEXT || id == AI_PAGE_INDICATOR || id == AI_UNLOCKER || id == AI_CUSTOM_VIEW);
        app.manageAddItemDialogLockedFlag(item, is_locked);
        root.addView(item);
    }

    private static class PluginAdapter extends ArrayAdapter<Plugin> {
        private LayoutInflater mLayoutInflater;

        public PluginAdapter(Context context, List<Plugin> objects) {
            super(context, 0, objects);
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.two_lines_list_item, null);
            }

            Plugin plugin = getItem(position);

            CheckedTextView line1 = (CheckedTextView) convertView.findViewById(android.R.id.text1);
            line1.setText(plugin.label);
            convertView.findViewById(android.R.id.text2).setVisibility(View.GONE);
            ((ImageView) convertView.findViewById(android.R.id.icon)).setImageDrawable(plugin.icon);

            return convertView;
        }
    }
}
