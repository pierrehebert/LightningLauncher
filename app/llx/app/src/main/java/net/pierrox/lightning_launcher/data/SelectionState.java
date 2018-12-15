package net.pierrox.lightning_launcher.data;

import net.pierrox.lightning_launcher.views.HandleView;
import net.pierrox.lightning_launcher.views.item.ItemView;

import java.util.ArrayList;

public class SelectionState {
    public int masterSelectedItemId;
    public int[] selectedItemsIds;
    public HandleView.Mode handleMode;

    public SelectionState(Item masterSelectedItem, ArrayList<ItemView> selectedItemViews, HandleView.Mode handleMode) {
        masterSelectedItemId = masterSelectedItem == null ? Item.NO_ID : masterSelectedItem.getId();
        int n = selectedItemViews.size();
        selectedItemsIds = new int[n];
        for(int i=0; i<n; i++) {
            selectedItemsIds[i] = selectedItemViews.get(i).getItem().getId();
        }
        this.handleMode = handleMode;
    }
}
