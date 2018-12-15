package net.pierrox.lightning_launcher.views.svg;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class SvgSvg extends SvgGroup {
    private Drawable mHolderDrawable;
    public float width;
    public float height;
    private HashMap<String,SvgElement> mElementsById;

    SvgSvg() {
        super(null, null, true, null);
        this.width = 1;
        this.height = 1;
        this.mElementsById = new HashMap<>();
    }

    public void setHolderDrawable(Drawable holderDrawable) {
        mHolderDrawable = holderDrawable;
    }

    public SvgElement getElementById(String id) {
        return mElementsById.get(id);
    }

    @Override
    public void invalidate() {
        if(mHolderDrawable != null) {
            mHolderDrawable.invalidateSelf();
        }
    }

    public void parse(InputStream is) throws IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp;
        SvgHandler svg_handler;
        try {
            sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            svg_handler=new SvgHandler();
            xr.setContentHandler(svg_handler);
            xr.parse(new InputSource(is));
        } catch (ParserConfigurationException | SAXException | IOException e1) {
            throw new IOException(e1.getMessage());
        }
    }

    private class SvgHandler extends org.xml.sax.helpers.DefaultHandler {
        private Stack<SvgGroup> mCurrentGroup;
        private SvgGradient mCurrentGradient;
        private RectF mTmpRectF = new RectF();

        public SvgHandler() {
            mCurrentGroup = new Stack<>();
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            SvgElement e = null;
            if(localName.equals("svg")) {
                float width;
                float height;
                String viewBox = atts.getValue("viewBox");
                if(viewBox != null) {
                    String[] tokens = viewBox.split(" ");
                    width = Float.parseFloat(tokens[2]);
                    height = Float.parseFloat(tokens[3]);
                } else {
                    width = Float.parseFloat(atts.getValue("width"));
                    height = Float.parseFloat(atts.getValue("height"));
                }
                SvgSvg.this.width = width;
                SvgSvg.this.height = height;
                mCurrentGroup.push(SvgSvg.this);
                e = SvgSvg.this;
            } else if(localName.equals("g")) {
                Matrix t = parseTransform(atts.getValue("transform"));
                String style = atts.getValue("style");
                try {
                    SvgSvg.ParsedStyle parsedStyle = parseStyle(style);
                    SvgGroup group = new SvgGroup(SvgSvg.this, getCurrentGroup(), parsedStyle.isVisible, t);
                    addElement(group);
                    mCurrentGroup.push(group);
                    e = group;
                } catch (Exception r) {
                    r.printStackTrace();
                }
            } else if(localName.equals("path")) {
                String d = atts.getValue("d");
                String style = composeStyle(atts);
                String transform = atts.getValue("transform");
                Matrix t = parseTransform(transform);
                ParsedStyle parsedStyle = parseStyle(style);
                Path path = parsePath(d);
                SvgPath svgPath = new SvgPath(SvgSvg.this, getCurrentGroup(), parsedStyle.isVisible, t, path, parsedStyle.fillPaint, parsedStyle.strokePaint);
                addElement(svgPath);
                e = svgPath;
            } else if(localName.equals("rect")) {
                float x = SvgSvg.parseFloat(atts, "x", 0);
                float y = SvgSvg.parseFloat(atts, "y", 0);
                float width = SvgSvg.parseFloat(atts, "width", 0);
                float height = SvgSvg.parseFloat(atts, "height", 0);
                float rx = parseFloat(atts, "rx", 0);
                float ry = parseFloat(atts, "ry", 0);

                Path p = new Path();
                p.addRoundRect(new RectF(x, y, x + width, y + height), rx, ry, Path.Direction.CW);

                String style = composeStyle(atts);
                ParsedStyle parsedStyle = parseStyle(style);

                String transform = atts.getValue("transform");
                Matrix matrix = parseTransform(transform);

                SvgPath path = new SvgPath(SvgSvg.this, getCurrentGroup(), parsedStyle.isVisible, matrix, p, parsedStyle.fillPaint, parsedStyle.strokePaint);
                addElement(path);
                e = path;
            } else if(localName.equals("ellipse") || localName.equals("circle")) {
                float centerX, centerY, radiusX, radiusY;

                centerX = SvgSvg.parseFloat(atts, "cx", 0);
                centerY = SvgSvg.parseFloat(atts, "cy", 0);
                if (localName.equals("ellipse")) {
                    radiusX = SvgSvg.parseFloat(atts, "rx", 0);
                    radiusY = SvgSvg.parseFloat(atts, "ry", 0);
                } else {
                    radiusX = radiusY = SvgSvg.parseFloat(atts, "r", 0);
                }
                mTmpRectF.set(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);

                Path p = new Path();
                p.addOval(mTmpRectF, Path.Direction.CW);

                String style = composeStyle(atts);
                ParsedStyle parsedStyle = parseStyle(style);

                String transform = atts.getValue("transform");
                Matrix matrix = parseTransform(transform);

                SvgPath path = new SvgPath(SvgSvg.this, getCurrentGroup(), parsedStyle.isVisible, matrix, p, parsedStyle.fillPaint, parsedStyle.strokePaint);
                addElement(path);
                e = path;
            } else if(localName.equals("linearGradient")) {
                SvgLinearGradient g = new SvgLinearGradient(SvgSvg.this, getCurrentGroup());
                parseGradient(g, atts);
                g.x1 = parseFloat(atts, "x1", 0);
                g.y1 = parseFloat(atts, "y1", 0);
                g.x2 = parseFloat(atts, "x2", SvgSvg.this.width);
                g.y2 = parseFloat(atts, "y2", 0);
                mCurrentGradient = g;
                e = g;
            } else if(localName.equals("radialGradient")) {
                SvgRadialGradient g = new SvgRadialGradient(SvgSvg.this, getCurrentGroup());
                parseGradient(g, atts);
                g.cx = parseFloat(atts, "cx", SvgSvg.this.width / 2);
                g.cy = parseFloat(atts, "cy", SvgSvg.this.height / 2);
                g.r = parseFloat(atts, "r", SvgSvg.this.width / 2);
                mCurrentGradient = g;
                e = g;
            } else if(localName.equals("stop")) {
                float offset = Float.parseFloat(atts.getValue("offset"));
                String style = atts.getValue("style");
                int color = Color.BLACK;
                int alpha = 255;
                for(String token : style.split(";")) {
                    String[] elems = token.split(":");
                    String name = elems[0];
                    String value = elems[1];
                    if("stop-color".equals(name)) {
                        color = Color.parseColor(value);
                    } else if("stop-opacity".equals(name)) {
                        alpha = Math.round(Float.parseFloat(value) * 255);
                    }
                }

                color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));

                mCurrentGradient.addStopOffset(offset);
                mCurrentGradient.addStopColors(color);
            }

            if(e != null) {
                String id = atts.getValue("id");
                if(id != null) {
                    e.setId(id);
                    mElementsById.put(id, e);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(localName.equals("g")) {
                mCurrentGroup.pop();
            } else if(localName.equals("defs")) {
                for(SvgElement e : mElementsById.values()) {
                    if(e instanceof SvgGradient) {
                        SvgGradient gradient = (SvgGradient) e;
                        gradient.resolveHref(mElementsById);
                        gradient.updatePaint();
                    }
                }
            } else if(localName.equals("linearGradient") || localName.equals("radialGradient")) {
                if(mCurrentGradient.getHref() == null) {
                    mCurrentGradient.updatePaint();
                }
                mCurrentGradient = null;
            }
        }

        private SvgGroup getCurrentGroup() {
            return mCurrentGroup.lastElement();
        }

        private void addElement(SvgElement e) {
            getCurrentGroup().getChildren().add(e);
        }
    }

    /*package*/ static class ParsedStyle {
        Paint strokePaint;
        Paint fillPaint;
        boolean isVisible;

        public ParsedStyle(Paint strokePaint, Paint fillPaint, boolean isVisible) {
            this.strokePaint = strokePaint;
            this.fillPaint = fillPaint;
            this.isVisible = isVisible;
        }
    }

    // Extracted from https://github.com/Pixplicity/sharp/blob/master/library/src/main/java/com/pixplicity/sharp/Sharp.java
    /*package*/ static Path parsePath(String s) {
        int n = s.length();
        SvgParserHelper ph = new SvgParserHelper(s, 0);
        ph.skipWhitespace();
        Path p = new Path();
        float lastX = 0;
        float lastY = 0;
        float lastX1 = 0;
        float lastY1 = 0;
        float subPathStartX = 0;
        float subPathStartY = 0;
        char prevCmd = 0;
        while (ph.pos < n) {
            char cmd = s.charAt(ph.pos);
            switch (cmd) {
                case '-':
                case '+':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (prevCmd == 'm' || prevCmd == 'M') {
                        cmd = (char) (((int) prevCmd) - 1);
                        break;
                    } else if (("lhvcsqta").indexOf(Character.toLowerCase(prevCmd)) >= 0) {
                        cmd = prevCmd;
                        break;
                    }
                default: {
                    ph.advance();
                    prevCmd = cmd;
                }
            }

            boolean wasCurve = false;
            switch (cmd) {
                case 'Z':
                case 'z': {
                    // Close path
                    p.close();
                    p.moveTo(subPathStartX, subPathStartY);
                    lastX = subPathStartX;
                    lastY = subPathStartY;
                    lastX1 = subPathStartX;
                    lastY1 = subPathStartY;
                    wasCurve = true;
                    break;
                }
                case 'M':
                case 'm': {
                    // Move
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        subPathStartX += x;
                        subPathStartY += y;
                        p.rMoveTo(x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        subPathStartX = x;
                        subPathStartY = y;
                        p.moveTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'L':
                case 'l': {
                    // Line
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        p.rLineTo(x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        p.lineTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'H':
                case 'h': {
                    // Horizontal line
                    float x = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        p.rLineTo(x, 0);
                        lastX += x;
                    } else {
                        p.lineTo(x, lastY);
                        lastX = x;
                    }
                    break;
                }
                case 'V':
                case 'v': {
                    // Vertical line
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        p.rLineTo(0, y);
                        lastY += y;
                    } else {
                        p.lineTo(lastX, y);
                        lastY = y;
                    }
                    break;
                }
                case 'C':
                case 'c': {
                    // Cubic Bézier (six parameters)
                    wasCurve = true;
                    float x1 = ph.nextFloat();
                    float y1 = ph.nextFloat();
                    float x2 = ph.nextFloat();
                    float y2 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        // Relative coordinates
                        x1 += lastX;
                        x2 += lastX;
                        x += lastX;
                        y1 += lastY;
                        y2 += lastY;
                        y += lastY;
                    }
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'S':
                case 's': {
                    // Shorthand cubic Bézier (four parameters)
                    wasCurve = true;
                    float x2 = ph.nextFloat();
                    float y2 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        // Relative coordinates
                        x2 += lastX;
                        x += lastX;
                        y2 += lastY;
                        y += lastY;
                    }
                    float x1 = 2 * lastX - lastX1;
                    float y1 = 2 * lastY - lastY1;
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'Q':
                case 'q': {
                    // Quadratic Bézier (four parameters)
                    wasCurve = true;
                    float x1 = ph.nextFloat();
                    float y1 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        // Relative coordinates
                        x1 += lastX;
                        x += lastX;
                        y1 += lastY;
                        y += lastY;
                    }
                    p.quadTo(x1, y1, x, y);
                    lastX1 = x1;
                    lastY1 = y1;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'T':
                case 't': {
                    // Shorthand quadratic Bézier (two parameters)
                    wasCurve = true;
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        // Relative coordinates
                        x += lastX;
                        y += lastY;
                    }
                    float x1 = 2 * lastX - lastX1;
                    float y1 = 2 * lastY - lastY1;
                    p.quadTo(x1, y1, x, y);
                    lastX1 = x1;
                    lastY1 = y1;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'A':
                case 'a': {
                    // Elliptical arc
                    float rx = ph.nextFloat();
                    float ry = ph.nextFloat();
                    float theta = ph.nextFloat();
                    int largeArc = ph.nextFlag();
                    int sweepArc = ph.nextFlag();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (Character.isLowerCase(cmd)) {
                        x += lastX;
                        y += lastY;
                    }
                    drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc, sweepArc);
                    lastX = x;
                    lastY = y;
                    break;
                }
            }
            if (!wasCurve) {
                lastX1 = lastX;
                lastY1 = lastY;
            }
            ph.skipWhitespace();
        }
        return p;
    }

    private static float angle(float y1, float x1, float y2, float x2) {
        return (float) Math.toDegrees(Math.atan2(y1, x1) - Math.atan2(y2, x2)) % 360;
    }

    private static final RectF arcRectf = new RectF();
    private static final Matrix arcMatrix = new Matrix();
    private static final Matrix arcMatrix2 = new Matrix();

    private static void drawArc(Path p, float lastX, float lastY, float x, float y,
                                float rx, float ry, float theta, int largeArc, int sweepArc) {
        //Log.d("drawArc", "from (" + lastX + "," + lastY + ") to (" + x + ","+ y + ") r=(" + rx + "," + ry + ") theta=" + theta + " flags="+ largeArc + "," + sweepArc);

        // http://www.w3.org/TR/SVG/implnote.html#ArcImplementationNotes

        if (rx == 0 || ry == 0) {
            p.lineTo(x, y);
            return;
        }

        if (x == lastX && y == lastY) {
            return; // nothing to draw
        }

        rx = Math.abs(rx);
        ry = Math.abs(ry);

        final float thrad = theta * (float) Math.PI / 180;
        final float st = (float) Math.sin(thrad);
        final float ct = (float) Math.cos(thrad);

        final float xc = (lastX - x) / 2;
        final float yc = (lastY - y) / 2;
        final float x1t = ct * xc + st * yc;
        final float y1t = -st * xc + ct * yc;

        final float x1ts = x1t * x1t;
        final float y1ts = y1t * y1t;
        float rxs = rx * rx;
        float rys = ry * ry;

        float lambda = (x1ts / rxs + y1ts / rys) * 1.001f; // add 0.1% to be sure that no out of range occurs due to limited precision
        if (lambda > 1) {
            float lambdasr = (float) Math.sqrt(lambda);
            rx *= lambdasr;
            ry *= lambdasr;
            rxs = rx * rx;
            rys = ry * ry;
        }

        final float R = (float) Math.sqrt((rxs * rys - rxs * y1ts - rys * x1ts) / (rxs * y1ts + rys * x1ts))
                * ((largeArc == sweepArc) ? -1 : 1);
        final float cxt = R * rx * y1t / ry;
        final float cyt = -R * ry * x1t / rx;
        final float cx = ct * cxt - st * cyt + (lastX + x) / 2;
        final float cy = st * cxt + ct * cyt + (lastY + y) / 2;

        final float th1 = angle(1, 0, (x1t - cxt) / rx, (y1t - cyt) / ry);
        float dth = angle((x1t - cxt) / rx, (y1t - cyt) / ry, (-x1t - cxt) / rx, (-y1t - cyt) / ry);

        if (sweepArc == 0 && dth > 0) {
            dth -= 360;
        } else if (sweepArc != 0 && dth < 0) {
            dth += 360;
        }

        // draw
        if ((theta % 360) == 0) {
            // no rotate and translate need
            arcRectf.set(cx - rx, cy - ry, cx + rx, cy + ry);
            p.arcTo(arcRectf, th1, dth);
        } else {
            // this is the hard and slow part :-)
            arcRectf.set(-rx, -ry, rx, ry);

            arcMatrix.reset();
            arcMatrix.postRotate(theta);
            arcMatrix.postTranslate(cx, cy);
            arcMatrix.invert(arcMatrix2);

            p.transform(arcMatrix2);
            p.arcTo(arcRectf, th1, dth);
            p.transform(arcMatrix);
        }
    }

    private static String composeStyle(Attributes atts) {
        String style = atts.getValue("style");
        if(style != null) {
            return style;
        }

        StringBuilder sb = new StringBuilder();
        addStyleAttribute(atts, sb, "fill");
        addStyleAttribute(atts, sb, "stroke");
        addStyleAttribute(atts, sb, "stroke-width");
        addStyleAttribute(atts, sb, "stroke-linecap");
        addStyleAttribute(atts, sb, "stroke-linejoin");
        addStyleAttribute(atts, sb, "stroke-miterlimit");
        addStyleAttribute(atts, sb, "stroke-dasharray");
        addStyleAttribute(atts, sb, "stroke-dashoffset");
        addStyleAttribute(atts, sb, "stroke-opacity");
        addStyleAttribute(atts, sb, "fill-opacity");
        addStyleAttribute(atts, sb, "display");

        return sb.toString();
    }

    private static void addStyleAttribute(Attributes atts, StringBuilder sb, String name) {
        String value = atts.getValue(name);
        if(value == null) {
            return;
        }

        if(sb.length() > 0) {
            sb.append(';');
        }
        sb.append(name).append(':').append(value);
    }

    /*package*/ ParsedStyle parseStyle(String style) {
        if(style==null || "".equals(style)) {
            Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(Paint.Style.FILL);
            return new ParsedStyle(null, fillPaint, true);
        }

        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setStyle(Paint.Style.FILL);
        boolean hasFill = true;

        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setStyle(Paint.Style.STROKE);
        boolean hasStroke = false;

        boolean isVisible = true;

        int fcolor = fp.getColor();
        int falpha = fp.getAlpha();
        int scolor = sp.getColor();
        int salpha = sp.getAlpha();
        float stroke_width = 1;
        float[] stroke_dash_array = null;
        float stroke_dash_offset = 0;

        if(style != null) {
            String[] tokens = style.split(";");
            for(String s : tokens) {
                int n = s.indexOf(":");
                String name = s.substring(0, n);
                String value = s.substring(n+1);
                if("fill".equals(name)) {
                    if("none".equals(value)) {
                        hasFill = false;
                    } else if(value.startsWith("url")) {
                        String id = value.substring(value.indexOf('#')+1, value.lastIndexOf(')'));
                        SvgGradient g = (SvgGradient) mElementsById.get(id);
                        // TODO make the link with element using this gradient so that updates to the gradient can be taken into account. See Gradient.addUser()
                        fp = new Paint(g.getPaint());
                        fp.setStyle(Paint.Style.FILL);
                        hasFill = true;
                        continue;
                    } else {
                        try {
                            fcolor = Color.parseColor(value);
                            hasFill = fcolor != 0;
                        } catch (IllegalArgumentException ex) { /*pass*/ }
                    }
                } else if("stroke".equals(name)) {
                    if("none".equals(value)) {
                        hasStroke = false;
                    } else if(value.startsWith("url")) {
                        String id = value.substring(value.indexOf('#')+1, value.lastIndexOf(')'));
                        SvgGradient g = (SvgGradient) mElementsById.get(id);
                        // TODO make the link with element using this gradient so that updates to the gradient can be taken into account. See Gradient.addUser()
                        sp = new Paint(g.getPaint());
                        sp.setStyle(Paint.Style.STROKE);
                        hasStroke = true;
                        continue;
                    } else {
                        try {
                            scolor = Color.parseColor(value);
                            hasStroke = scolor != 0;
                        } catch (IllegalArgumentException ex) { /*pass*/ }
                    }
                } else if("stroke-width".equals(name)) {
                    if(sp != null) {
                        int j;
                        for (j = value.length() - 1; j >= 0; j--) {
                            char c = value.charAt(j);
                            if (Character.isDigit(c) || c == '.') break;
                        }
                        stroke_width = Float.parseFloat(value.substring(0, j + 1));
                    }
                } else if("stroke-linecap".equals(name)) {
                    if (sp != null) {
                        Paint.Cap cap = sp.getStrokeCap();
                        if ("round".equals(value)) {
                            cap = Paint.Cap.ROUND;
                        } else if ("butt".equals(value)) {
                            cap = Paint.Cap.BUTT;
                        } else if ("square".equals(value)) {
                            cap = Paint.Cap.SQUARE;
                        }
                        sp.setStrokeCap(cap);
                    }
                } else if("stroke-linejoin".equals(name)) {
                    if(sp != null) {
                        Paint.Join join = sp.getStrokeJoin();
                        if ("miter".equals(value)) {
                            join = Paint.Join.MITER;
                        } else if ("round".equals(value)) {
                            join = Paint.Join.ROUND;
                        } else if ("bevel".equals(value)) {
                            join = Paint.Join.BEVEL;
                        }
                        sp.setStrokeJoin(join);
                    }
                } else if("stroke-miterlimit".equals(name)) {
                    if(sp != null) sp.setStrokeMiter(Float.parseFloat(value));
                } else if("stroke-dasharray".equals(name)) {
                    if(!"none".equals(value)) {
                        stroke_dash_array = parseFloats(value);
                    }
                } else if("stroke-dashoffset".equals(name)) {
                    stroke_dash_offset = Float.parseFloat(value);
                } else if("stroke-opacity".equals(name)) {
                    salpha = (int)(255* Float.parseFloat(value));
                } else if("fill-opacity".equals(name)) {
                    falpha = (int)(255* Float.parseFloat(value));
                } else if("display".equals(name)) {
                    isVisible = !"none".equals(value);
                }
            }
        }

        if(!hasFill) {
            fp = null;
        }

        if(fp != null) {
            fp.setColor(fcolor);
            fp.setAlpha(falpha);
        }

        if(!hasStroke) {
            sp = null;
        }

        if(sp != null) {
            sp.setColor(scolor);
            sp.setAlpha(salpha);
            sp.setStrokeWidth(stroke_width);
            if(stroke_dash_array != null) {
                sp.setPathEffect(new DashPathEffect(stroke_dash_array, stroke_dash_offset));
            }
        }

        return new ParsedStyle(sp, fp, isVisible);
    }

    private static void parseGradient(SvgGradient g, Attributes atts) {
        g.setTransform(parseTransform(atts.getValue("gradientTransform")));
        g.setHref(atts.getValue("xlink:href"));
        g.setTileMode(Shader.TileMode.CLAMP);
        String spreadMethod = atts.getValue("spreadMethod");
        if(spreadMethod != null) {
            if("repeat".equals(spreadMethod)) {
                g.setTileMode(Shader.TileMode.REPEAT);
            } else if("reflect".equals(spreadMethod)) {
                g.setTileMode(Shader.TileMode.MIRROR);
            }
        }
    }

    /*package*/ static Matrix parseTransform(String s) {
        if(s == null) {
            return null;
        }

        if (s.startsWith("matrix(")) {
            float[] floats = parseFloats(s, "matrix(");
            if (floats.length == 6) {
                Matrix matrix = new Matrix();
                matrix.setValues(new float[]{
                        // Row 1
                        floats[0],
                        floats[2],
                        floats[4],
                        // Row 2
                        floats[1],
                        floats[3],
                        floats[5],
                        // Row 3
                        0,
                        0,
                        1,
                });
                return matrix;
            }
        } else if (s.startsWith("translate(")) {
            float[] floats = parseFloats(s, "translate(");
            if (floats.length > 0) {
                float tx = floats[0];
                float ty = floats.length == 1 ? 0 : floats[1];
                Matrix matrix = new Matrix();
                matrix.postTranslate(tx, ty);
                return matrix;
            }
        } else if (s.startsWith("scale(")) {
            float[] floats = parseFloats(s, "scale(");
            if (floats.length > 0) {
                float sx = floats[0];
                float sy = floats.length == 1 ? 0 : floats[1];
                Matrix matrix = new Matrix();
                matrix.postScale(sx, sy);
                return matrix;
            }
        } else if (s.startsWith("skewX(")) {
            float[] floats = parseFloats(s, "skewX(");
            if (floats.length > 0) {
                float angle = floats[0];
                Matrix matrix = new Matrix();
                matrix.postSkew((float) Math.tan(angle), 0);
                return matrix;
            }
        } else if (s.startsWith("skewY(")) {
            float[] floats = parseFloats(s, "skewY(");
            if (floats.length > 0) {
                float angle = floats[0];
                Matrix matrix = new Matrix();
                matrix.postSkew(0, (float) Math.tan(angle));
                return matrix;
            }
        } else if (s.startsWith("rotate(")) {
            float[] floats = parseFloats(s, "rotate(");
            if (floats.length > 0) {
                float angle = floats[0];
                float cx = 0;
                float cy = 0;
                if (floats.length > 2) {
                    cx = floats[1];
                    cy = floats[2];
                }
                Matrix matrix = new Matrix();
                matrix.postTranslate(cx, cy);
                matrix.postRotate(angle);
                matrix.postTranslate(-cx, -cy);
                return matrix;
            }
        }
        return null;
    }

    private static Float parseFloat(Attributes atts, String name, float defaultValue) {
        String v = atts.getValue(name);
        return v == null ? defaultValue : Float.parseFloat(v);
    }

    private static float[] parseFloats(String list, String prefix) {
        return parseFloats(list.substring(prefix.length(), list.lastIndexOf(')')));
    }

    private static float[] parseFloats(String list) {
        String[] tokens = list.split(list.indexOf(',')==-1?" ":",");
        int l = tokens.length;
        float[] floats = new float[l];
        for(int i=0; i<l; i++) {
            floats[i] = Float.parseFloat(tokens[i]);
        }
        return floats;
    }

//        private static NumberParse parseNumbers(String s) {
//            //Util.debug("Parsing numbers from: '" + s + "'");
//            int n = s.length();
//            int p = 0;
//            ArrayList<Float> numbers = new ArrayList<Float>();
//            boolean skipChar = false;
//            for (int i = 1; i < n; i++) {
//                if (skipChar) {
//                    skipChar = false;
//                    continue;
//                }
//                char c = s.charAt(i);
//                switch (c) {
//                    // This ends the parsing, as we are on the next element
//                    case 'M':
//                    case 'm':
//                    case 'Z':
//                    case 'z':
//                    case 'L':
//                    case 'l':
//                    case 'H':
//                    case 'h':
//                    case 'V':
//                    case 'v':
//                    case 'C':
//                    case 'c':
//                    case 'S':
//                    case 's':
//                    case 'Q':
//                    case 'q':
//                    case 'T':
//                    case 't':
//                    case ')': {
//                        String str = s.substring(p, i);
//                        if (str.trim().length() > 0) {
//                            //Util.debug("  Last: " + str);
//                            Float f = Float.parseFloat(str);
//                            numbers.add(f);
//                        }
//                        p = i;
//                        return new NumberParse(numbers, p);
//                    }
//                    case '\n':
//                    case '\t':
//                    case ' ':
//                    case ',':
//                    case '-': {
//                        String str = s.substring(p, i);
//                        // Just keep moving if multiple whitespace
//                        if (str.trim().length() > 0) {
//                            //Util.debug("  Next: " + str);
//                            Float f = Float.parseFloat(str);
//                            numbers.add(f);
//                            if (c == '-') {
//                                p = i;
//                            } else {
//                                p = i + 1;
//                                skipChar = true;
//                            }
//                        } else {
//                            p++;
//                        }
//                        break;
//                    }
//                }
//            }
//            String last = s.substring(p);
//            if (last.length() > 0) {
//                //Util.debug("  Last: " + last);
//                try {
//                    numbers.add(Float.parseFloat(last));
//                } catch (NumberFormatException nfe) {
//                    // Just white-space, forget it
//                }
//                p = s.length();
//            }
//            return new NumberParse(numbers, p);
//        }
//
//
//        private static class NumberParse {
//            private ArrayList<Float> numbers;
//            //private int nextCmd;
//
//            public NumberParse(ArrayList<Float> numbers, int nextCmd) {
//                this.numbers = numbers;
//                //this.nextCmd = nextCmd;
//            }
//
//        /*public int getNextCmd() {
//            return nextCmd;
//        }
//
//        public float getNumber(int index) {
//            return numbers.get(index);
//        }*/
//        }

//    private static Path arcTo(float lastX, float lastY, float rx, float ry, float angle, boolean largeArcFlag, boolean sweepFlag, float x, float y) {
//        Path path = new Path();
//
//        if (lastX == x && lastY == y) {
//            // If the endpoints (x, y) and (x0, y0) are identical, then this
//            // is equivalent to omitting the elliptical arc segment entirely.
//            // (behaviour specified by the spec)
//            return null;
//        }
//
//        // Handle degenerate case (behaviour specified by the spec)
//        if (rx == 0 || ry == 0) {
//            path.lineTo(x, y);
//            return path;
//        }
//
//        // Sign of the radii is ignored (behaviour specified by the spec)
//        rx = Math.abs(rx);
//        ry = Math.abs(ry);
//
//        // Convert angle from degrees to radians
//        float  angleRad = (float) Math.toRadians(angle % 360.0);
//        double cosAngle = Math.cos(angleRad);
//        double sinAngle = Math.sin(angleRad);
//
//        // We simplify the calculations by transforming the arc so that the origin is at the
//        // midpoint calculated above followed by a rotation to line up the coordinate axes
//        // with the axes of the ellipse.
//
//        // Compute the midpoint of the line between the current and the end point
//        double dx2 = (lastX - x) / 2.0;
//        double dy2 = (lastY - y) / 2.0;
//
//        // Step 1 : Compute (x1', y1') - the transformed start point
//        double x1 = (cosAngle * dx2 + sinAngle * dy2);
//        double y1 = (-sinAngle * dx2 + cosAngle * dy2);
//
//        double rx_sq = rx * rx;
//        double ry_sq = ry * ry;
//        double x1_sq = x1 * x1;
//        double y1_sq = y1 * y1;
//
//        // Check that radii are large enough.
//        // If they are not, the spec says to scale them up so they are.
//        // This is to compensate for potential rounding errors/differences between SVG implementations.
//        double radiiCheck = x1_sq / rx_sq + y1_sq / ry_sq;
//        if (radiiCheck > 1) {
//            rx = (float) Math.sqrt(radiiCheck) * rx;
//            ry = (float) Math.sqrt(radiiCheck) * ry;
//            rx_sq = rx * rx;
//            ry_sq = ry * ry;
//        }
//
//        // Step 2 : Compute (cx1, cy1) - the transformed centre point
//        double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
//        double sq = ((rx_sq * ry_sq) - (rx_sq * y1_sq) - (ry_sq * x1_sq)) / ((rx_sq * y1_sq) + (ry_sq * x1_sq));
//        sq = (sq < 0) ? 0 : sq;
//        double coef = (sign * Math.sqrt(sq));
//        double cx1 = coef * ((rx * y1) / ry);
//        double cy1 = coef * -((ry * x1) / rx);
//
//        // Step 3 : Compute (cx, cy) from (cx1, cy1)
//        double sx2 = (lastX + x) / 2.0;
//        double sy2 = (lastY + y) / 2.0;
//        double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
//        double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);
//
//        // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
//        double ux = (x1 - cx1) / rx;
//        double uy = (y1 - cy1) / ry;
//        double vx = (-x1 - cx1) / rx;
//        double vy = (-y1 - cy1) / ry;
//        double p, n;
//
//        // Compute the angle start
//        n = Math.sqrt((ux * ux) + (uy * uy));
//        p = ux; // (1 * ux) + (0 * uy)
//        sign = (uy < 0) ? -1.0 : 1.0;
//        double angleStart = Math.toDegrees(sign * Math.acos(p / n));
//
//        // Compute the angle extent
//        n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
//        p = ux * vx + uy * vy;
//        sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
//        double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
//        if (!sweepFlag && angleExtent > 0) {
//            angleExtent -= 360f;
//        } else if (sweepFlag && angleExtent < 0) {
//            angleExtent += 360f;
//        }
//        angleExtent %= 360f;
//        angleStart %= 360f;
//
//        // Many elliptical arc implementations including the Java2D and Android ones, only
//        // support arcs that are axis aligned.  Therefore we need to substitute the arc
//        // with bezier curves.  The following method call will generate the beziers for
//        // a unit circle that covers the arc angles we want.
//        float[]  bezierPoints = arcToBeziers(angleStart, angleExtent);
//
//        // Calculate a transformation matrix that will move and scale these bezier points to the correct location.
//        Matrix m = new Matrix();
//        m.postScale(rx, ry);
//        m.postRotate(angle);
//        m.postTranslate((float) cx, (float) cy);
//        m.mapPoints(bezierPoints);
//
//        // The last point in the bezier set should match exactly the last coord pair in the arc (ie: x,y). But
//        // considering all the mathematical manipulation we have been doing, it is bound to be off by a tiny
//        // fraction. Experiments show that it can be up to around 0.00002.  So why don't we just set it to
//        // exactly what it ought to be.
//        bezierPoints[bezierPoints.length-2] = x;
//        bezierPoints[bezierPoints.length-1] = y;
//
//        // Final step is to add the bezier curves to the path
//        for (int i=0; i<bezierPoints.length; i+=6)
//        {
//            path.cubicTo(bezierPoints[i], bezierPoints[i+1], bezierPoints[i+2], bezierPoints[i+3], bezierPoints[i+4], bezierPoints[i+5]);
//        }
//
//        return path;
//    }
//
//    private static float[]  arcToBeziers(double angleStart, double angleExtent)
//    {
//        int    numSegments = (int) Math.ceil(Math.abs(angleExtent) / 90.0);
//
//        angleStart = Math.toRadians(angleStart);
//        angleExtent = Math.toRadians(angleExtent);
//        float  angleIncrement = (float) (angleExtent / numSegments);
//
//        // The length of each control point vector is given by the following formula.
//        double  controlLength = 4.0 / 3.0 * Math.sin(angleIncrement / 2.0) / (1.0 + Math.cos(angleIncrement / 2.0));
//
//        float[] coords = new float[numSegments * 6];
//        int     pos = 0;
//
//        for (int i=0; i<numSegments; i++)
//        {
//            double  angle = angleStart + i * angleIncrement;
//            // Calculate the control vector at this angle
//            double  dx = Math.cos(angle);
//            double  dy = Math.sin(angle);
//            // First control point
//            coords[pos++]   = (float) (dx - controlLength * dy);
//            coords[pos++] = (float) (dy + controlLength * dx);
//            // Second control point
//            angle += angleIncrement;
//            dx = Math.cos(angle);
//            dy = Math.sin(angle);
//            coords[pos++] = (float) (dx + controlLength * dy);
//            coords[pos++] = (float) (dy - controlLength * dx);
//            // Endpoint of bezier
//            coords[pos++] = (float) dx;
//            coords[pos++] = (float) dy;
//        }
//        return coords;
//    }
}
