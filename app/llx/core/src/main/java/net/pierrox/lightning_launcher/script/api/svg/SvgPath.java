package net.pierrox.lightning_launcher.script.api.svg;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * SVG path (&lt;path&gt;).
 */

public class SvgPath extends SvgElement {
    public SvgPath(net.pierrox.lightning_launcher.views.svg.SvgPath path) {
        super(path);
    }

    /**
     * Update the paint used to fill the path.
     * The paint should have been configured this way: <code>fillPaint.setStyle(Paint.Style.FILL);</code>
     * @param fillPaint paint, either a new one, or the paint returned by {@link #getFillPaint()}
     */
    public void setFillPaint(Paint fillPaint) {

        ((net.pierrox.lightning_launcher.views.svg.SvgPath)mSvgElement).setFillPaint(fillPaint);
    }

    /**
     * Return the paint used to fill the path, may be null if the path is not filled.
     * If the paint object is modified, the element need to be refreshed using {@link SvgElement#invalidate()}
     */
    public Paint getFillPaint() {
        return ((net.pierrox.lightning_launcher.views.svg.SvgPath)mSvgElement).getFillPaint();
    }

    /**
     * Update the paint used to stroke the path.
     * The paint should have been configured this way: <code>strokePaint.setStyle(Paint.Style.STROKE);</code>
     * @param strokePaint paint, either a new one, or the paint returned by {@link #getStrokePaint()}
     */
    public void setStrokePaint(Paint strokePaint) {
        ((net.pierrox.lightning_launcher.views.svg.SvgPath)mSvgElement).setStrokePaint(strokePaint);
    }

    /**
     * Return the paint used to stroke the path, may be null if the path is not stroked.
     * If the paint object is modified, the element need to be refreshed using {@link SvgElement#invalidate()}
     */
    public Paint getStrokePaint() {
        return ((net.pierrox.lightning_launcher.views.svg.SvgPath)mSvgElement).getStrokePaint();
    }

    /**
     * Update the path using the given specification
     * @param d path specification as defined here : {@link https://svgwg.org/svg2-draft/paths.html#TheDProperty}
     * @return true if the path specification has been parsed correctly, false if couldn't be understood
     */
    public boolean setPath(String d) {
        return ((net.pierrox.lightning_launcher.views.svg.SvgPath)mSvgElement).setPath(d);
    }

    /**
     * Return the android Path object, decoded from the string specification.
     * If the path object is modified, the element need to be refreshed using {@link SvgElement#invalidate()}
     */
    public Path getPath() {
        return ((net.pierrox.lightning_launcher.views.svg.SvgPath)mSvgElement).getPath();
    }

    /**
     * Update the style (fill, stroke and visibility) using the given style specification
     * @param style recognized elements are: fill, stroke, stroke-width, stroke-linecap, stroke-linejoin, stroke-miterlimit, stroke-dasharray, stroke-dashoffset, stroke-opacity, fill-opacity, display
     * @return true if the style specification has been parsed correctly, false if couldn't be understood
     */
    public boolean setStyle(String style) {
        return ((net.pierrox.lightning_launcher.views.svg.SvgPath)mSvgElement).setStyle(style);
    }
}
