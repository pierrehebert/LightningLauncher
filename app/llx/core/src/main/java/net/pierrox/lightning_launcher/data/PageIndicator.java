package net.pierrox.lightning_launcher.data;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;

import net.pierrox.lightning_launcher.configuration.JsonFields;
import net.pierrox.lightning_launcher.configuration.ShortcutConfig;
import net.pierrox.lightning_launcher.configuration.ShortcutConfigStylable;
import net.pierrox.lightning_launcher.views.item.ItemView;
import net.pierrox.lightning_launcher.views.item.PageIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;

public class PageIndicator extends Item implements ShortcutConfigStylable {
    public enum Style {
        DOTS, // wrap
        LINE_X, // fill X, wrap Y
        LINE_Y,  // wrap X, fill Y
        MINIMAP, // fill
        RAW, // wrap
    }

    public enum LineGravity {
        CENTER,
        LEFT_TOP,
        RIGHT_BOTTOM
    }

    private ShortcutConfig mShortcutConfig;

    // public properties
    public Style style = Style.DOTS;
    public int dotsMarginX = 10;
    public int dotsMarginY = 10;
    public int dotsOuterRadius = 8;
    public int dotsInnerRadius = 5;
    public int dotsOuterStrokeWidth = 2;
    public int dotsOuterColor = Color.WHITE;
    public int dotsInnerColor = Color.WHITE;
    public String rawFormat = "%1$+5.0f  %2$+5.0f | %3$6.3f";
    public int miniMapOutStrokeColor = 0xffffffff;
    public int miniMapOutFillColor = 0x60ffffff;
    public int miniMapOutStrokeWidth = 0;
    public int miniMapInStrokeColor = 0xffffffff;
    public int miniMapInFillColor = 0x60ffffff;
    public int miniMapInStrokeWidth = 0;
    public int lineBgWidth = 2;
    public int lineBgColor = 0xffffffff;
    public int lineFgWidth = 5;
    public int lineFgColor = 0xffffffff;
    public LineGravity lineGravity = LineGravity.RIGHT_BOTTOM;



    public PageIndicator(Page page) {
        super(page);
    }

    @Override
    public void createFromJSONObject(JSONObject o) throws JSONException {
        readItemFromJSONObject(o);

        JSONObject json_configuration=o.optJSONObject(JsonFields.SHORTCUT_CONFIGURATION);
        ShortcutConfig defaultShortcutConfig = mPage.config.defaultShortcutConfig;
        if(json_configuration!=null) {
            mShortcutConfig=ShortcutConfig.readFromJsonObject(json_configuration, defaultShortcutConfig);
            mShortcutConfig.loadAssociatedIcons(mPage.getIconDir(), mId);
        } else {
            mShortcutConfig= defaultShortcutConfig;
        }

        JsonLoader.loadFieldsFromJSONObject(this, o, null);
    }

    public void copyFrom(PageIndicator from) {
        style = from.style;
        dotsMarginX = from.dotsMarginX;
        dotsMarginY = from.dotsMarginY;
        dotsOuterRadius = from.dotsOuterRadius;
        dotsInnerRadius = from.dotsInnerRadius;
        dotsOuterStrokeWidth = from.dotsOuterStrokeWidth;
        dotsOuterColor = from.dotsOuterColor;
        dotsInnerColor = from.dotsInnerColor;
        rawFormat = from.rawFormat;
        miniMapOutStrokeColor = from.miniMapOutStrokeColor;
        miniMapOutFillColor = from.miniMapOutFillColor;
        miniMapOutStrokeWidth = from.miniMapOutStrokeWidth;
        miniMapInStrokeColor = from.miniMapInStrokeColor;
        miniMapInFillColor = from.miniMapInFillColor;
        miniMapInStrokeWidth = from.miniMapInStrokeWidth;
        lineBgWidth = from.lineBgWidth;
        lineBgColor = from.lineBgColor;
        lineFgWidth = from.lineFgWidth;
        lineFgColor = from.lineFgColor;
        lineGravity = from.lineGravity;
    }

    @Override
    public Item clone() {
        return null;
    }

    @Override
    public ShortcutConfig getShortcutConfig() {
        return mShortcutConfig;
    }

    @Override
    public void setShortcutConfig(ShortcutConfig c) {
        mShortcutConfig=c;
    }

    @Override
    public ShortcutConfig modifyShortcutConfig() {
        ShortcutConfig sc = mShortcutConfig.clone();
        mShortcutConfig = sc;
        return sc;
    }



    @Override
    public void init(int id, Rect cell_p, Rect cell_l) {
        super.init(id, cell_p, cell_l);

        mShortcutConfig=mPage.config.defaultShortcutConfig;
    }

    @Override
    public ItemView createView(Context context) {
        return new PageIndicatorView(context, this);
    }
}
