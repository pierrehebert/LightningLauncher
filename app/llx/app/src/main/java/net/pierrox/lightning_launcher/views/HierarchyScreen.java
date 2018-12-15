package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.data.ContainerPath;
import net.pierrox.lightning_launcher.data.Folder;
import net.pierrox.lightning_launcher.data.Item;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;
import java.util.HashSet;

public class HierarchyScreen {

    public interface HierarchyScreenListener {
        void onHierarchyScreenItemEdit(ContainerPath path, Item item);
        void onHierarchyScreenContainerSettings(ContainerPath path);
        void onHierarchyScreenItemMoved(HierarchyItem which, HierarchyItem before, HierarchyItem after);
        int[] onHierarchyScreenGetRootPages();
    }

    private LightningEngine mLightningEngine;
    private Screen mScreen;
    private View mRootView;
    private HierarchyScreenListener mListener;
    private Animation mHierarchyPaneAnimIn;
    private Animation mHierarchyPaneAnimOut;

    private DragSortListView mDragSortListView;
    private HierarchyAdapter mAdapter;

    public HierarchyScreen(LightningEngine engine, Screen screen, HierarchyScreenListener listener) {
        mLightningEngine = engine;
        mScreen = screen;
        mListener = listener;
    }

    public void destroy() {
        if(mAdapter != null) {
            mAdapter.destroy();
        }
    }

    public LightningEngine getLightningEngine() {
        return mLightningEngine;
    }

    public void show(ContainerPath scrollToPath) {
        if(mRootView == null) {
            // setup
            final Context context = mScreen.getContext();
            mRootView = LayoutInflater.from(context).inflate(R.layout.hierarchy_pane, null);
            ViewGroup contentView = mScreen.getContentView();
            ViewGroup editControlsView = (ViewGroup) contentView.findViewById(R.id.edit_controls);
            editControlsView.addView(mRootView); // the dashboard should be responsible for inserting this view in its hierarchy

            mDragSortListView = (DragSortListView) mRootView.findViewById(android.R.id.list);
            mAdapter = new HierarchyAdapter(context, mDragSortListView);

            mHierarchyPaneAnimIn = AnimationUtils.makeInAnimation(context, true);
            mHierarchyPaneAnimOut = AnimationUtils.makeOutAnimation(context, false);

            DragSortController controller = new DragSortController(mDragSortListView) {
                @Override
                public void onSwipeLeft() {
                    hide();
                }
            };
            controller.setDragInitMode(DragSortController.ON_LONG_PRESS);
            mDragSortListView.setOnTouchListener(controller);
            mDragSortListView.setDragEnabled(true);
            mDragSortListView.setDropListener(new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    HierarchyItem which = mAdapter.getItem(from);
                    if (from == to) {
                        String name;
                        if(which.isContainer()) {
                            Page page = mLightningEngine.getOrLoadPage(which.page);
                            name = Utils.formatPageName(page, which.parent==null ? null : which.parent.item);
                        } else {
                            name = Utils.formatItemName(which.item, 0, 0);
                        }
                        Toast.makeText(context, name, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    HierarchyItem before, after;
                    int hoveredItem = mAdapter.getHoveredItem();
                    if (hoveredItem == -1) {
                        if (to < from) {
                            before = to == 0 ? null : mAdapter.getItem(to - 1);
                            after = mAdapter.getItem(to);
                        } else {
                            before = mAdapter.getItem(to);
                            after = to == mAdapter.getCount() - 1 ? null : mAdapter.getItem(to + 1);
                        }
                    } else {
                        before = after = mAdapter.getItem(hoveredItem);
                    }
                    mListener.onHierarchyScreenItemMoved(which, before, after);

                    mAdapter.setHoveredItem(-1);
                }
            });



            mDragSortListView.setDragListener(new DragSortListView.DragListener() {
                @Override
                public void drag(int from, int to) {
                }

                @Override
                public void onItemHovered(int position) {
                    mAdapter.setHoveredItem(position);
                }
            });

            mDragSortListView.setAdapter(mAdapter);

            mDragSortListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    HierarchyItem hi = mAdapter.getItem(position);
                    if (hi.isContainer()) {
                        mAdapter.toggleExpandedState(hi.path);
                    }
                }
            });
        }

        if(!isShown()) {
            if(scrollToPath != null) {
                expandPath(scrollToPath);
            }

            refresh();

            if(scrollToPath != null) {
                int pos = 0;
                for (HierarchyItem hi : mAdapter.mItems) {
                    if (hi.isContainer() && hi.path.equals(scrollToPath)) {
                        mDragSortListView.setSelection(pos);
                        break;
                    }
                    pos++;
                }
            }

            mRootView.setVisibility(View.VISIBLE);
            mRootView.startAnimation(mHierarchyPaneAnimIn);
        }
    }

    private void expandPath(ContainerPath path) {
        ContainerPath parent = path.getParent();
        if(parent != null) {
            expandPath(parent);
        }
        mAdapter.setExpandedState(path, true);
    }

    public void hide() {
        if(isShown()) {
            mRootView.setVisibility(View.GONE);
            mRootView.startAnimation(mHierarchyPaneAnimOut);
        }
    }

    public boolean isShown() {
        return mRootView != null && mRootView.getVisibility() == View.VISIBLE;
    }

    public void refresh() {
        mAdapter.updateModel();
        mAdapter.notifyDataSetChanged();
    }


    private static class ExpandedPathSet {
        private HashSet<String> mExpandedPaths = new HashSet<>();

        public boolean add(ContainerPath path) {
            return mExpandedPaths.add(path.toString());
        }

        public boolean remove(ContainerPath path) {
            return mExpandedPaths.remove(path.toString());
        }

        public boolean contains(ContainerPath path) {
            return mExpandedPaths.contains(path.toString());
        }
    }

    public static class HierarchyPath extends ContainerPath{
        public HierarchyPath(HierarchyItem item) {
            super((String)null);
            StringBuilder path = new StringBuilder();
            build(item, path);
            mPath = path.toString();
        }

        private void build(HierarchyItem item, StringBuilder out) {
            if(item.parent != null) {
                build(item.parent, out);
                out.append('/');
            }
            if(item.item == null) {
                out.append(item.page);
            } else {
                out.append(item.item.getId());
            }
        }
    }

    public static class HierarchyItem {
        public int level;
        public int page;
        public Item item;
        public HierarchyItem parent;
        public ContainerPath path;

        public HierarchyItem(HierarchyItem parent, int level, int page) {
            this.parent = parent;
            this.level = level;
            this.page = page;
            this.path = new HierarchyPath(this);
        }
        public HierarchyItem(HierarchyItem parent, int level, int page, Item item) {
            this.parent = parent;
            this.level = level;
            this.page = page;
            this.item = item;
            this.path = new HierarchyPath(this);
        }
        public boolean isContainer() {
            return item == null || item instanceof Folder;
        }

        public int getContainerId() {
            if(item == null) {
                return page;
            } else {
                return ((Folder)item).getFolderPageId();
            }
        }

        public int getParentContainerId() {
            if(item == null) {
                return Page.NONE;
            } else {
                return Utils.getPageForItem(item);
            }
        }
    }

    private class HierarchyAdapter extends BaseAdapter implements View.OnClickListener, View.OnLongClickListener {
        private Context mContext;
        private ListView mHostListView;
        private LightningEngine.PageManager mPageManager;

        private ArrayList<HierarchyItem> mItems;

        private ExpandedPathSet mExpandedPaths;

        private int mIndent;

        private int mHoveredItem = -1;

        public HierarchyAdapter(Context context, ListView hostListView) {
            mContext = context;
            mHostListView = hostListView;
            mPageManager = mLightningEngine.getPageManager();

            mLightningEngine.registerPageListener(mPageListener);

            mExpandedPaths = new ExpandedPathSet();

            mIndent = context.getResources().getDimensionPixelSize(R.dimen.hierarchy_indent);

            updateModel();
        }

        public void destroy() {
            mLightningEngine.unregisterPageListener(mPageListener);
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).isContainer() ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public HierarchyItem getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.hierarchy_item, parent, false);

                Button containerSettingsButton = (Button) convertView.findViewById(R.id.eb_cs);
                containerSettingsButton.setTypeface(LLApp.get().getIconsTypeface());
                containerSettingsButton.setOnClickListener(this);
                containerSettingsButton.setOnLongClickListener(this);
                containerSettingsButton.setContentDescription(mContext.getString(R.string.mi_es_settings));

                Button editButton = (Button) convertView.findViewById(R.id.eb_edit);
                editButton.setTypeface(LLApp.get().getIconsTypeface());
                editButton.setOnClickListener(this);
                editButton.setOnLongClickListener(this);
                editButton.setContentDescription(mContext.getString(R.string.hs_ed));
            }

            HierarchyItem hi = getItem(position);
            TextView textView = (TextView) convertView.findViewById(R.id.label);
            Page page = mLightningEngine.getOrLoadPage(hi.page);
            CharSequence label;
            if(hi.item == null) {
                label = Utils.formatPageName(page, hi.parent==null ? null : hi.parent.item);
            } else {
                label = Utils.formatItemName(hi.item, 0, 0);
            }
            if(hi.isContainer()) {
                SpannableString spannedLabel = new SpannableString(label);
                spannedLabel.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), 0);
                label = spannedLabel;
            }
            textView.setText(label);
            textView.setPadding(hi.level * mIndent, 0, 0, 0);

            Button containerSettingsButton = (Button) convertView.findViewById(R.id.eb_cs);
            containerSettingsButton.setTag(hi);
            containerSettingsButton.setVisibility(hi.isContainer() ? View.VISIBLE : View.INVISIBLE);

            Button editButton = (Button) convertView.findViewById(R.id.eb_edit);
            editButton.setTag(hi);

            return convertView;
        }

        public void setExpandedState(ContainerPath item, boolean expanded) {
            if(expanded) {
                mExpandedPaths.add(item);
            } else {
                mExpandedPaths.remove(item);
            }
        }

        public void toggleExpandedState(ContainerPath path) {
            if(mExpandedPaths.contains(path)) {
                mExpandedPaths.remove(path);
            } else {
                mExpandedPaths.add(path);
            }
            updateModel();
            notifyDataSetChanged();
        }

        public void setHoveredItem(int position) {
            if (mHoveredItem != position) {
                if(mHoveredItem != -1) {
                    setItemHoveredState(mHoveredItem, false);
                }
                if(position != -1 && getItem(position).isContainer()) {
                    mHoveredItem = position;
                    setItemHoveredState(mHoveredItem, true);
                } else {
                    mHoveredItem = -1;
                }
            }
        }

        public int getHoveredItem() {
            return mHoveredItem;
        }

        private void setItemHoveredState(int position, boolean hovered) {
            View view = mHostListView.getChildAt(position - mHostListView.getFirstVisiblePosition());
            if (view != null) {
                view.findViewById(R.id.label).setBackgroundColor(hovered ? 0xff008000 : Color.TRANSPARENT);
            }
        }

        private void updateModel() {
            setHoveredItem(-1);

            mItems = new ArrayList<>();
            int[] pages = mListener.onHierarchyScreenGetRootPages();
            for(int page : pages) {
                HierarchyItem desktopHierarchyItem = new HierarchyItem(null, 0, page);
                mItems.add(desktopHierarchyItem);
                addPageItems(desktopHierarchyItem, page, 1);
            }
        }

        private void addPageItems(HierarchyItem parent, int page, int level) {
            if(!mExpandedPaths.contains(parent.path)) {
                return;
            }

            Page p = mPageManager.getOrLoadPage(page);
            for (Item item : p.items) {
                HierarchyItem hierarchyItem = new HierarchyItem(parent, level, page, item);
                mItems.add(hierarchyItem);
                if(item instanceof Folder) {
                    Folder folder = (Folder) item;
                    addPageItems(hierarchyItem, folder.getFolderPageId(), level + 1);
                }
            }
        }

        @Override
        public void onClick(View v) {
            HierarchyItem hi = (HierarchyItem) v.getTag();
            switch (v.getId()) {
                case R.id.eb_edit: mListener.onHierarchyScreenItemEdit(hi.path, hi.item); break;
                case R.id.eb_cs: mListener.onHierarchyScreenContainerSettings(new HierarchyPath(hi)); break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            int label_res = 0;
            switch (v.getId()) {
                case R.id.eb_edit: label_res = R.string.hs_ed; break;
                case R.id.eb_cs: label_res = R.string.mi_es_settings; break;
            }
            if(label_res != 0) {
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                Toast toast = Toast.makeText(mContext, label_res, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.LEFT, location[0], location[1]);
                toast.show();
            }
            return true;
        }

        private Page.PageListener mPageListener = new Page.EmptyPageListener() {
            @Override
            public void onPageRemoved(Page page) {
                // TODO handle removals
            }

            @Override
            public void onPageLoaded(Page page) {
            }
        };
    }
}
