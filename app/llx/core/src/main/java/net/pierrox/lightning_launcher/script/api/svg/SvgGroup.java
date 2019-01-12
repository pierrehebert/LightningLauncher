package net.pierrox.lightning_launcher.script.api.svg;

import java.util.ArrayList;

/**
 * A group of SVG elements (&lt;g&gt;).
 */

public class SvgGroup extends SvgElement {
    private SvgElement[] mChildren;

    /**
     * @hide
     */
    SvgGroup(net.pierrox.android.lsvg.SvgGroup svgGroup) {
        super(svgGroup);
    }

    /**
     * Return the list of elements in this group.
     */
    public SvgElement[] getChildren() {
        if(mChildren == null) {
            net.pierrox.android.lsvg.SvgGroup group = (net.pierrox.android.lsvg.SvgGroup) mSvgElement;
            ArrayList<net.pierrox.android.lsvg.SvgElement> children = group.getChildren();
            int size = children.size();
            mChildren = new SvgElement[size];
            for(int i=0; i<size; i++) {
                mChildren[i] = SvgElement.create(children.get(i));
            }
        }

        return mChildren;
    }
}
