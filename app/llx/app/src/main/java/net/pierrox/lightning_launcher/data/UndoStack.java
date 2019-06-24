package net.pierrox.lightning_launcher.data;

import net.pierrox.lightning_launcher.activities.Dashboard;
import net.pierrox.lightning_launcher.configuration.FolderConfig;
import net.pierrox.lightning_launcher.configuration.ItemConfig;
import net.pierrox.lightning_launcher.engine.LightningEngine;
import net.pierrox.lightning_launcher.engine.Screen;
import net.pierrox.lightning_launcher.views.item.ItemView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class UndoStack {
    public interface UndoListener {
        public void onUndoStackStateChanged(boolean can_undo, boolean can_redo);
        public void onUndoStackItemChanged(Item item);
        public void onUndoStackPageChanged(Page page);
    }

    private abstract class Operation {
        protected SelectionState mSelectionState;

        protected Operation() {
            mSelectionState = mDashboard.getSelectionState();
        }

        protected void undo() {
            mDashboard.setSelectionState(mSelectionState);
        }

        protected void redo() {
        }

        protected abstract void clearTempStorage();

        protected File getTempStorageDirectory() {
            File dir = new File(mTempStorageDirectory, String.valueOf(hashCode()));
            if(!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            return dir;
        }

        protected void ensurePageVisible(Page page) {
            // FIXME re-implement
            /*Screen screen = mDashboard.getScreen();
            if(page.isDashboard()) {
                if (screen.getCurrentRootPage() != page) {
                    screen.loadRootItemLayout(page.id, false);
                    mDashboard.enterEditMode(page, null);
                }
            } else if(page.isFolder()) {
                Folder opener = page.findFirstOpener();
                if(opener != null) {
                    if(opener.getClass() == Folder.class) {
                        screen.openFolder(opener);
                        mDashboard.enterEditMode(page, null);
                    }
                }
            }*/
        }
    }

    private class GroupStartOperation extends Operation {

        protected GroupStartOperation(SelectionState selectionState) {
            if(selectionState != null) {
                mSelectionState = selectionState;
            }
        }

        @Override
        protected void undo() {
            super.undo();
        }

        @Override
        protected void redo() {
            Operation op;
            do {
                op = UndoStack.this.getNextRedoOperation();
                UndoStack.this.redo();
            } while(op.getClass() != GroupEndOperation.class);
            super.redo();
        }

        @Override
        protected void clearTempStorage() {

        }
    }

    private class GroupEndOperation extends Operation {

        @Override
        protected void undo() {
            super.undo();
            Operation op;
            do {
                op = UndoStack.this.getNextUndoOperation();
                // check for null because if the stack is full, older operations may have been removed, including balanced GroupStartOperation
                if(op != null) {
                    UndoStack.this.undo();
                }
            } while(op != null && op.getClass() != GroupStartOperation.class);
        }

        @Override
        protected void redo() {
            super.redo();
        }

        @Override
        protected void clearTempStorage() {

        }
    }

    private abstract class PageOperation extends Operation {
        protected Page mPage;

        protected PageOperation(Page page) {
            super();
            mPage = page;
        }

        protected void ensurePageVisible() {
            ensurePageVisible(mPage);
        }
    }

    private class PageOperationItemZOrder extends PageOperation {
        private int mOldZOrder;
        private int mNewZOrder;

        protected PageOperationItemZOrder(Page page, Item item, int oldZOrder) {
            super(page);
            mOldZOrder = oldZOrder;
            mNewZOrder = page.items.indexOf(item);
        }

        @Override
        protected void undo() {
            Item item = mPage.items.get(mNewZOrder);
            mPage.setItemZIndex(item, mOldZOrder);
            super.undo();
        }

        @Override
        protected void redo() {
            Item item = mPage.items.get(mOldZOrder);
            mPage.setItemZIndex(item, mNewZOrder);
            super.redo();
        }

        @Override
        protected void clearTempStorage() {
            // pass
        }
    }

    private class PageOperationAddOrRemoveItem extends PageOperation {
        private int mItemId;
        private boolean mForAdd;
        private JSONObject mJsonItem;
        private int mZOrder;

        protected PageOperationAddOrRemoveItem(Item item, boolean forAdd) {
            super(item.getPage());

            mItemId = item.mId;
            mForAdd = forAdd;

            if(!mForAdd) {
                saveItemState(item);
            } else {
                mZOrder = mPage.items.size();
            }
        }


        @Override
        protected void undo() {
            if(mForAdd) {
                doRemove();
                notifyPageChanged();
            } else {
                doAdd();
                mListener.onUndoStackPageChanged(mPage);
            }
            super.undo();
        }

        @Override
        protected void redo() {
            if(mForAdd) {
                doAdd();
                mListener.onUndoStackPageChanged(mPage);
            } else {
                doRemove();
                notifyPageChanged();
            }
            super.redo();
        }

        protected void notifyPageChanged() {
            mListener.onUndoStackPageChanged(mPage);
        }

        private void saveItemState(Item item) {
            exchangeFilesWithUndo(item, true);
            try { mJsonItem = item.toJSONObject(); } catch (JSONException e) { /*pass*/ }
            mZOrder = mPage.items.indexOf(item);
        }

        private void doRemove() {
            Item item = mPage.findItemById(mItemId);
            if(mForAdd) {
                saveItemState(item);
            }
            mPage.removeItem(item, false);
            ensurePageVisible();
        }

        private Item doAdd() {
            try {
                Item item = Item.loadItemFromJSONObject(mPage, mJsonItem);
                exchangeFilesWithUndo(item, false);
                // FIXME: need the item to list files to restore, but need the files to load the item... so load the item twice
                item = Item.loadItemFromJSONObject(mPage, mJsonItem);
                mPage.addItem(item, mZOrder);
                ensurePageVisible();
                return item;
            } catch (JSONException e) {
                // pass
                return null;
            }
        }

        /**
         *  param direction true: copy to undo, false retrieve from undo
         */
        private void exchangeFilesWithUndo(Item item, boolean direction) {
            byte[] buffer = new byte[4096];
            File out = getTempStorageDirectory();
            ArrayList<File> icons = new ArrayList<>();
            item.getIconFiles(mPage.getIconDir(), icons);
            int index = 0;
            for(File from : icons) {
                File to = new File(out, String.valueOf(index));
                if(direction) {
                    Utils.copyFileSafe(buffer, from, to);
                } else {
                    Utils.copyFileSafe(buffer, to, from);
                }
                index++;
            }

            if(item instanceof Folder) {
                Folder f = (Folder)item;
                Page page = f.getOrLoadFolderPage();
                exchangePageFilesWithUndo(page, direction, buffer, new ArrayList<Integer>());
            }
        }

        private void exchangePageFilesWithUndo(Page page, boolean direction, byte[] buffer, ArrayList<Integer> done) {
            try {
                File folder_dir = page.getPageDir();
                File temp_dir = new File(getTempStorageDirectory(), "page_"+page.id);
                if(direction) {
                    if(page.isModified()) page.save();
                    Utils.copyDirectory(buffer, folder_dir, temp_dir);
                } else {
                    Utils.copyDirectory(buffer, temp_dir, folder_dir);
                    page.reload();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            for(Item item : page.items) {
                if(item instanceof Folder) {
                    Folder f = (Folder)item;
                    int pageId = f.getFolderPageId();
                    if(!done.contains(pageId)) {
                        // prevent endless recursion when the folder is in itself
                        done.add(pageId);
                        exchangePageFilesWithUndo(f.getOrLoadFolderPage(), direction, buffer, done);
                    }
                }
            }
        }

        @Override
        protected void clearTempStorage() {
            Utils.deleteDirectory(getTempStorageDirectory(), true);
        }
    }

    private class PageOperationState extends PageOperation {
        protected PageOperationState(Page page) {
            super(page);

            exchangeFilesWithUndo(true, true);
        }

        @Override
        protected void undo() {
            undoOrRedo(true);
            super.undo();
        }

        @Override
        protected void redo() {
            undoOrRedo(false);
            super.redo();
        }

        private void undoOrRedo(boolean direction) {
            FolderConfig.FolderIconStyle previous_style = mPage.config.defaultFolderConfig.iconStyle;
            exchangeFilesWithUndo(!direction, true);
            exchangeFilesWithUndo(direction, false);
            mPage.reload();
            if(previous_style != mPage.config.defaultFolderConfig.iconStyle) {
                Utils.updateFolderIconStyle(mPage);
            }
            mListener.onUndoStackPageChanged(mPage);
            ensurePageVisible();
        }

        @Override
        protected void clearTempStorage() {
            Utils.deleteDirectory(getTempStorageDirectory(), true);
        }

        private void exchangeFilesWithUndo(boolean old, boolean direction) {
            mPage.save();

            File to = new File(getTempStorageDirectory(), old ? "o" : "n");
            to.mkdir();
            File from = mPage.getIconDir();
            if (!direction) {
                File tmp = from;
                from = to;
                to = tmp;
            }
            FileUtils.copyIcons(null, from, "", to, "");

            from = mPage.getPageConfigFile();
            to = new File(getTempStorageDirectory(), (old ? "co" : "cn"));
            if (!direction) {
                File tmp = from;
                from = to;
                to = tmp;
            }
            Utils.copyFileSafe(null, from, to);
        }
    }

    private abstract class ItemOperation extends Operation {
        protected int itemId;
        public ItemOperation(Item item) {
            this.itemId = item.getId();
        }

        protected void notifyItemChanged() {
            mListener.onUndoStackItemChanged(getItem());
        }

        protected Item getItem() {
            return mDashboard.getEngine().getItemById(itemId);
        }
    }

    private class ItemOperationSetGeometry extends ItemOperation {
        protected SavedItemGeometry mOldGeometry;
        protected SavedItemGeometry mNewGeometry;

        private ItemOperationSetGeometry(ItemView itemView, SavedItemGeometry oldGeometry) {
            super(itemView.getItem());

            mOldGeometry = oldGeometry;
            mNewGeometry = new SavedItemGeometry(itemView);
        }

        @Override
        protected void undo() {
            doUndo();
            notifyItemChanged();
            super.undo();
        }

        @Override
        protected void redo() {
            doRedo();
            notifyItemChanged();
            super.redo();
        }

        @Override
        protected void clearTempStorage() {
            // pass
        }

        protected void doUndo() {
            mOldGeometry.applyTo(getItem());
        }

        protected void doRedo() {
            mNewGeometry.applyTo(getItem());
        }
    }

    private class ItemOperationGridAttachment extends ItemOperationSetGeometry {
        private boolean wasAttached;
        private ItemOperationGridAttachment(ItemView itemView, boolean wasAttached, SavedItemGeometry oldGeometry) {
            super(itemView, oldGeometry);
            this.wasAttached = wasAttached;
        }

        @Override
        protected void undo() {
            getItem().getItemConfig().onGrid = wasAttached;
            doUndo();
            notifyItemChanged();
            super.undo();
        }

        @Override
        protected void redo() {
            getItem().getItemConfig().onGrid = !wasAttached;
            doRedo();
            notifyItemChanged();
            super.redo();
        }
    }

    private class ItemOperationPinMode extends ItemOperationSetGeometry {
        private ItemConfig.PinMode oldPinMode;
        private ItemConfig.PinMode newPinMode;
        private ItemOperationPinMode(ItemView itemView, ItemConfig.PinMode oldPinMode, SavedItemGeometry oldGeometry) {
            super(itemView, oldGeometry);
            this.oldPinMode = oldPinMode;
            newPinMode = getItem().getItemConfig().pinMode;
        }

        @Override
        protected void undo() {
            getItem().getItemConfig().pinMode = oldPinMode;
            doUndo();
            notifyItemChanged();
            super.undo();
        }

        @Override
        protected void redo() {
            getItem().getItemConfig().pinMode = newPinMode;
            doRedo();
            notifyItemChanged();
            super.redo();
        }
    }

    private class ItemOperationMove extends ItemOperationSetGeometry {
        private int mOldItemId;
        private int mNewItemId;
        private int mOldPage;
        private int mNewPage;

        private ItemOperationMove(ItemView itemView, int oldItemId, SavedItemGeometry oldGeometry) {
            super(itemView, oldGeometry);

            mOldItemId = oldItemId;
            mNewItemId = itemView.getItem().getId();
            mOldPage = Utils.getPageForItemId(oldItemId);
            mNewPage = Utils.getPageForItemId(mNewItemId);
        }

        @Override
        protected void undo() {
            LightningEngine engine = mDashboard.getEngine();
            Page oldPage = engine.getOrLoadPage(mOldPage);
            Item newItem = engine.getItemById(mNewItemId);
            Item oldItem = Utils.moveItem(newItem, oldPage, 0, 0, 1, mOldItemId);
            oldPage.items.remove(oldItem);
            oldPage.items.add(mOldGeometry.zOrder, oldItem);
            // FIXME hack, set the item id so that the set geometry operation can operate on the right item
            this.itemId = mOldItemId;
            doUndo();
            notifyItemChanged();
//            ensurePageVisible(page);
            super.undo();
        }

        @Override
        protected void redo() {
            Item oldItem = mDashboard.getEngine().getItemById(mOldItemId);
            Page newPage = mDashboard.getEngine().getOrLoadPage(mNewPage);
            Item newItem = Utils.moveItem(oldItem, newPage, 0, 0, 1, mNewItemId);
            newPage.items.remove(newItem);
            newPage.items.add(mNewGeometry.zOrder, newItem);

            // FIXME hack, set the item id so that the set geometry operation can operate on the right item
            this.itemId = mNewItemId;
            doRedo();
            notifyItemChanged();
//            ensurePageVisible(page);
            super.redo();
        }
    }

    private class ItemOperationState extends ItemOperation {
        private JSONObject mOldJsonItem;
        private JSONObject mNewJsonItem;

        public ItemOperationState(Item item) {
            super(item);

            try { mOldJsonItem = item.toJSONObject(); } catch (JSONException e) { /*pass*/ }

            ArrayList<File> icons = new ArrayList<>();
            Page page = item.getPage();
            item.getIconFiles(page.getIconDir(), icons);
            exchangeFilesWithUndo(icons, true, true);
        }

        @Override
        protected void undo() {
            Item new_item = getItem();
            Page page = new_item.getPage();
            ArrayList<File> icons = new ArrayList<>();
            new_item.getIconFiles(page.getIconDir(), icons);


            exchangeFilesWithUndo(icons, false, true); // save new files to temp
            try { mNewJsonItem = new_item.toJSONObject(); } catch (JSONException e) { /*pass*/ }

            try {
                int zorder = page.items.indexOf(new_item);
                page.removeItem(new_item, true);

                exchangeFilesWithUndo(icons, true, false); // restore old files from temp
                Item old_item = Item.loadItemFromJSONObject(page, mOldJsonItem);

                page.addItem(old_item, zorder);
            } catch (JSONException e) {
                // pass
            }
            notifyItemChanged();
            ensurePageVisible(page);
            super.undo();
        }

        @Override
        protected void redo() {
            Item old_item = getItem();
            Page page = old_item.getPage();
            ArrayList<File> icons = new ArrayList<>();
            old_item.getIconFiles(page.getIconDir(), icons);

            try {
                int zorder = page.items.indexOf(old_item);
                page.removeItem(old_item, true);

                exchangeFilesWithUndo(icons, false, false); // restore new files from temp
                Item new_item = Item.loadItemFromJSONObject(page, mNewJsonItem);

                page.addItem(new_item, zorder);
            } catch (JSONException e) {
                // pass
            }
            notifyItemChanged();
            super.redo();
        }

        @Override
        protected void clearTempStorage() {
            Utils.deleteDirectory(getTempStorageDirectory(), true);
        }

        private void exchangeFilesWithUndo(ArrayList<File> icons, boolean old, boolean direction) {
            byte[] buffer = new byte[4096];
            File out = new File(getTempStorageDirectory(), old?"o":"n");
            out.mkdir();
            int index = 0;
            for(File from : icons) {
                File to = new File(out, String.valueOf(index));
                if(!direction) {
                    File tmp = to;
                    to = from;
                    from = tmp;
                }
                if(from.exists()) {
                    Utils.copyFileSafe(buffer, from, to);
                } else {
                    to.delete();
                }
                index++;
            }
        }
    }

    private Dashboard mDashboard;
    private File mTempStorageDirectory;
    private int mMaxSize;
    private LinkedList<Operation> mUndoOperations = new LinkedList<>();
    private LinkedList<Operation> mRedoOperations = new LinkedList<>();
    private LinkedList<SelectionState> mRedoSelectionState = new LinkedList<>();
    private UndoListener mListener;

    public UndoStack(Dashboard dashboard, File tempStorageDirectory, int maxSize) {
        mDashboard = dashboard;
        mTempStorageDirectory = tempStorageDirectory;
        mMaxSize = maxSize;

        if(mTempStorageDirectory.exists()) {
            Utils.deleteDirectory(mTempStorageDirectory, false);
        }
    }

    public void clear() {
        if(mUndoOperations.size()>0 || mRedoOperations.size()>0) {
            Utils.deleteDirectory(mTempStorageDirectory, false);
            mUndoOperations.clear();
            mRedoOperations.clear();
            notifyUndoListener();
        }
    }

    public void setUndoListener(UndoListener listener) {
        mListener = listener;
    }

    public boolean canUndo() {
        return mUndoOperations.size() > 0;
    }

    public boolean canRedo() {
        return mRedoOperations.size() > 0;
    }

    private Operation getNextUndoOperation() {
        return mUndoOperations.isEmpty() ? null : mUndoOperations.getLast();
    }

    public void undo() {
        mRedoSelectionState.add(mDashboard.getSelectionState());
        Operation operation = mUndoOperations.removeLast();
        mRedoOperations.add(operation);

        operation.undo();

        notifyUndoListener();
    }

    public boolean willDeleteWidget(boolean for_undo) {
        Operation operation = for_undo ? mUndoOperations.getLast() : mRedoOperations.getLast();
        if(operation instanceof PageOperationAddOrRemoveItem) {
            PageOperationAddOrRemoveItem addOrRemoveItem = (PageOperationAddOrRemoveItem) operation;
            Item item = addOrRemoveItem.mPage.findItemById(addOrRemoveItem.mItemId);
            boolean widget = item instanceof Widget || (item instanceof Folder && ((Folder)item).hasWidget());
            if(for_undo && addOrRemoveItem.mForAdd && widget) return true;
            if(!for_undo && !addOrRemoveItem.mForAdd && widget) return true;
        }
        return false;
    }

    private Operation getNextRedoOperation() {
        return mRedoOperations.getLast();
    }

    public void redo() {
        Operation operation = mRedoOperations.removeLast();
        mUndoOperations.add(operation);

        operation.redo();

        notifyUndoListener();

        mDashboard.setSelectionState(mRedoSelectionState.removeLast());
    }

    public void storeGroupStart() {
        storeGroupStart(null);
    }

    public void storeGroupStart(SelectionState selectionState) {
        addOperation(new GroupStartOperation(selectionState));
    }

    public void storeGroupEnd() {
        int l = mUndoOperations.size();
        if(mUndoOperations.get(l-1).getClass() == GroupStartOperation.class) {
            // nothing between start and end: remove the start and don't add a end
            mUndoOperations.removeLast();
        } else if(mUndoOperations.get(l-2).getClass() == GroupStartOperation.class) {
            // a single operation between start and end: remove the start and don't add a end
            mUndoOperations.remove(l-2);
        } else {
            addOperation(new GroupEndOperation());
        }
    }

    public void storeItemSetCell(ItemView itemView, SavedItemGeometry oldGeometry) {
        addOperation(new ItemOperationSetGeometry(itemView, oldGeometry));
    }

    public void storeItemSetTransform(ItemView itemView, SavedItemGeometry oldGeometry) {
        addOperation(new ItemOperationSetGeometry(itemView, oldGeometry));
    }

    public void storeItemSetViewSize(ItemView itemView, SavedItemGeometry oldGeometry) {
        addOperation(new ItemOperationSetGeometry(itemView, oldGeometry));
    }

    public void storeItemGridAttachment(ItemView itemView, boolean wasAttached, SavedItemGeometry oldGeometry) {
        addOperation(new ItemOperationGridAttachment(itemView, wasAttached, oldGeometry));
    }

    public void storeItemPinMode(ItemView itemView, ItemConfig.PinMode oldPinMode, SavedItemGeometry oldGeometry) {
        addOperation(new ItemOperationPinMode(itemView, oldPinMode, oldGeometry));
    }

    public void storePageAddItem(Item item) {
        addOperation(new PageOperationAddOrRemoveItem(item, true));
    }

    public void storePageRemoveItem(Item item) {
        addOperation(new PageOperationAddOrRemoveItem(item, false));
    }

    public void storePageItemZOrder(Page page, Item item, int oldZOrder) {
        addOperation(new PageOperationItemZOrder(page, item, oldZOrder));
    }

    public void storePageItemMove(ItemView newItemView, int oldItemId, SavedItemGeometry oldGeometry) {
        addOperation(new ItemOperationMove(newItemView, oldItemId, oldGeometry));
    }

    public void storeItemState(Item item) {
        addOperation(new ItemOperationState(item));
    }

    public void storePageState(Page page) {
        addOperation(new PageOperationState(page));
    }

    private void addOperation(Operation operation) {
        for(Operation op : mRedoOperations) {
            op.clearTempStorage();
        }
        mRedoOperations.clear();

        if(mUndoOperations.size() == mMaxSize) {
            Operation op = mUndoOperations.removeFirst();
            op.clearTempStorage();
        }
        mUndoOperations.add(operation);

        notifyUndoListener();
    }

    private void notifyUndoListener() {
        mListener.onUndoStackStateChanged(canUndo(), canRedo());
    }
}
