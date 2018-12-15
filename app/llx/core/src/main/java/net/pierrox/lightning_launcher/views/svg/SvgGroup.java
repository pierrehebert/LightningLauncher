package net.pierrox.lightning_launcher.views.svg;

import android.graphics.Matrix;

import java.util.ArrayList;

public class SvgGroup extends SvgElement {
    private ArrayList<SvgElement> mChildren;

    SvgGroup(SvgSvg root, SvgGroup parent, boolean isVisible, Matrix transform) {
        super(root, parent, isVisible, transform);
        mChildren = new ArrayList<>();
    }

    public ArrayList<SvgElement> getChildren() {
        return mChildren;
    }
}
