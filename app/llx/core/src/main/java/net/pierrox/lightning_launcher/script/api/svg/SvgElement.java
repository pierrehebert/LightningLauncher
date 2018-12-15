package net.pierrox.lightning_launcher.script.api.svg;

import android.graphics.Matrix;

/**
 * The base class for all SVG elements.
 */

public class SvgElement {
    protected net.pierrox.lightning_launcher.views.svg.SvgElement mSvgElement;

    /**
     * @hide
     */
    /*package*/ SvgElement(net.pierrox.lightning_launcher.views.svg.SvgElement svgElement) {
        mSvgElement = svgElement;
    }

    /**
     * Return this element identifier, if any.
     */
    public String getId() {
        return mSvgElement.getId();
    }
    /**
     * Return the parent element. This will always be null for a {@link SvgSvg} element.
     */
    public SvgGroup getParent() {
        return (SvgGroup)create(mSvgElement.getParent());
    }

    /**
     * Return the transformation matrix, can be null if not set.
     */
    public Matrix getTransform() {
        return mSvgElement.getTransform();
    }

    public boolean isVisible() {
        return mSvgElement.isVisible();
    }

    public void setVisible(boolean visible) {
        mSvgElement.setVisible(visible);
    }

    /**
     * Set the transformation matrix for this element using a matrix
     * @param transform if null, use the identity matrix (no transformation)
     */
    public void setTransform(Matrix transform) {
        mSvgElement.setTransform(transform);
    }

    /**
     * Set the transformation matrix for this element using a svg specification (matrix, translate, rotate, etc.).
     * @param transform if null, use the identity matrix (no transformation)
     * @return true if the transform specification has been parsed correctly, false if couldn't be understood
     */
    public boolean setTransform(String specification) {
        return mSvgElement.setTransform(specification);
    }

    /**
     * Request an update. This can be used to refresh the drawing if a property has been modified on it's back (access to android Paint, Path, etc.)
     */
    public void invalidate() {
        mSvgElement.invalidate();
    }

    /**
     * @hide
     */
    /*package*/ static SvgElement create(net.pierrox.lightning_launcher.views.svg.SvgElement child) {
        Class<? extends net.pierrox.lightning_launcher.views.svg.SvgElement> cls = child.getClass();
        if(cls == net.pierrox.lightning_launcher.views.svg.SvgSvg.class) {
            return new SvgSvg((net.pierrox.lightning_launcher.views.svg.SvgSvg) child);
        } else if(cls == net.pierrox.lightning_launcher.views.svg.SvgGroup.class) {
            return new SvgGroup((net.pierrox.lightning_launcher.views.svg.SvgGroup) child);
        } else if(cls == net.pierrox.lightning_launcher.views.svg.SvgLinearGradient.class) {
            return new SvgLinearGradient((net.pierrox.lightning_launcher.views.svg.SvgLinearGradient) child);
        } else if(cls == net.pierrox.lightning_launcher.views.svg.SvgRadialGradient.class) {
            return new SvgRadialGradient((net.pierrox.lightning_launcher.views.svg.SvgRadialGradient) child);
        } else if(cls == net.pierrox.lightning_launcher.views.svg.SvgPath.class) {
            return new SvgPath((net.pierrox.lightning_launcher.views.svg.SvgPath) child);
        } else {
            return new SvgElement(child);
        }
    }
}
