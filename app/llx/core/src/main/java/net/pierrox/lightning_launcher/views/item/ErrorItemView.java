package net.pierrox.lightning_launcher.views.item;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import net.pierrox.lightning_launcher.data.Item;

public class ErrorItemView extends ItemView {
    String mMessage;

    public ErrorItemView(Context context, Item item, String message) {
        super(context, item);
        mMessage = message;
    }

    @Override
    public void init() {
        TextView tv = new TextView(getContext());
        tv.setText(mMessage);
        tv.setTextColor(Color.WHITE);
        tv.setShadowLayer(1, 0, 0, Color.BLACK);

        setView(tv);
    }
}
