package net.pierrox.lightning_launcher.script.api.svg;

/**
 * The root &lt;svg&gt; element.
 * Lightning Launcher supports the following subset of the SVG specification:
 * - elements: rect, circle, ellipse, path, group, svg, linearGradient, radialGradient
 * - transforms: matrix, translate, rotate, scale, skewX, skewY
 * - style: fill, fill-opacity, stroke, stroke-width, stroke-linecap, stroke-linejoin, stroke-miterlimit, stroke-dasharray, stroke-dashoffset, stroke-opacity, display (only with value "none")
 * - linear and radial: with stop offsets and gradient transform. Gradients can be used in fill and stroke using the url(#gradient_id) syntax
 */

public class SvgSvg extends SvgGroup {
    /**
     * @hide
     */
    /*package*/
    public SvgSvg(net.pierrox.android.lsvg.SvgSvg svgSvg) {
        super(svgSvg);
    }

    public SvgElement getElementById(String id) {
        return create(((net.pierrox.android.lsvg.SvgSvg)mSvgElement).getElementById(id));
    }
}
