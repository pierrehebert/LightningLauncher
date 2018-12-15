package net.pierrox.lightning_launcher.views.item;

import android.content.Context;
import android.view.ViewGroup;

import net.pierrox.lightning_launcher.data.Unlocker;
import net.pierrox.lightning_launcher.views.RingUnlockerView;

public class UnlockerView extends ItemView implements RingUnlockerView.OnUnlockerListener {
    public UnlockerView(Context context, Unlocker unlocker) {
        super(context, unlocker);
    }

    @Override
    public void init() {
        RingUnlockerView uv = new RingUnlockerView(getContext());
        uv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        uv.setOnUnlockerListener(this);
        setView(uv);
    }

    @Override
    public void onUnlocked() {
        getScreen().onItemViewAction(this, 0);
    }
}
