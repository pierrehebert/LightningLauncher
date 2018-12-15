package net.pierrox.lightning_launcher.views.svg;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

public class SvgPath extends SvgElement {
    private Path mPath;
    /*package*/ Paint fillPaint;
    /*package*/ Paint strokePaint;


    SvgPath(SvgSvg root, SvgGroup parent, boolean isVisible, Matrix transform, Path path, Paint fillPaint, Paint strokePaint) {
        super(root, parent, isVisible, transform);
        this.mPath = path;
        this.fillPaint = fillPaint;
        this.strokePaint = strokePaint;
    }

    public void setFillPaint(Paint fillPaint) {
        this.fillPaint = fillPaint;
        invalidate();
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setStrokePaint(Paint strokePaint) {
        this.strokePaint = strokePaint;
        invalidate();
    }

    public Paint getStrokePaint() {
        return strokePaint;
    }

    public boolean setPath(String d) {
        try {
            this.mPath = mRoot.parsePath(d);
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Path getPath() {
        return mPath;
    }

    public boolean setStyle(String style) {
        try {
            SvgSvg.ParsedStyle parsedStyle = mRoot.parseStyle(style);
            setVisible(parsedStyle.isVisible);
            this.fillPaint = parsedStyle.fillPaint;
            this.strokePaint = parsedStyle.strokePaint;
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
