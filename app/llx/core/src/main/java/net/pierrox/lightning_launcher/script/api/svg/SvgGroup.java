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
    SvgGroup(net.pierrox.lightning_launcher.views.svg.SvgGroup svgGroup) {
        super(svgGroup);
    }

    /**
     * Return the list of elements in this group.
     */
    public SvgElement[] getChildren() {
        if(mChildren == null) {
            net.pierrox.lightning_launcher.views.svg.SvgGroup group = (net.pierrox.lightning_launcher.views.svg.SvgGroup) mSvgElement;
            ArrayList<net.pierrox.lightning_launcher.views.svg.SvgElement> children = group.getChildren();
            int size = children.size();
            mChildren = new SvgElement[size];
            for(int i=0; i<size; i++) {
                mChildren[i] = SvgElement.create(children.get(i));
            }
        }

        return mChildren;
    }
}
