/*
 * $Id: PdfGraphics2D.java 2912 2007-09-06 10:30:41Z psoares33 $
 *
 * Copyright 2002 by Jim Moore <jim@scolamoore.com>.
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is 'iText, a free JAVA-PDF library'.
 *
 * The Initial Developer of the Original Code is Bruno Lowagie. Portions created by
 * the Initial Developer are Copyright (C) 1999, 2000, 2001, 2002 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000, 2001, 2002 by Paulo Soares. All Rights Reserved.
 *
 * Contributor(s): all the names of the contributors are added in the source code
 * where applicable.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * LGPL license (the "GNU LIBRARY GENERAL PUBLIC LICENSE"), in which case the
 * provisions of LGPL are applicable instead of those above.  If you wish to
 * allow use of your version of this file only under the terms of the LGPL
 * License and not to allow others to use your version of this file under
 * the MPL, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the LGPL.
 * If you do not delete the provisions above, a recipient may use your version
 * of this file under either the MPL or the GNU LIBRARY GENERAL PUBLIC LICENSE.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the MPL as stated above or under the terms of the GNU
 * Library General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library general Public License for more
 * details.
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://www.lowagie.com/iText/
 */
package com.lowagie.text.pdf;

import com.lowagie.text.pdf.internal.PolylineShape;
import java.awt.RenderingHints.Key;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.*;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class PdfGraphics2D extends Graphics2D {

    private static final int FILL = 1;
    private static final int STROKE = 2;
    private static final int CLIP = 3;
    private BasicStroke strokeOne = new BasicStroke(1);
    private static final AffineTransform IDENTITY = new AffineTransform();
    private Font font;
    private BaseFont baseFont;
    private float fontSize;
    private AffineTransform transform;
    private Paint paint;
    private Color background;
    private float width;
    private float height;
    private Area clip;
    private RenderingHints rhints = new RenderingHints(null);
    private Stroke stroke;
    private Stroke originalStroke;
    private PdfContentByte cb;
    /**
     * Storage for BaseFont objects created.
     */
    private HashMap baseFonts;
    private boolean disposeCalled = false;
    private FontMapper fontMapper;
    private ArrayList kids;
    private boolean kid = false;
    private Graphics2D dg2 = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB).createGraphics();
    private boolean onlyShapes = false;
    private Stroke oldStroke;
    private Paint paintFill;
    private Paint paintStroke;
    private MediaTracker mediaTracker;
    // Added by Jurij Bilas
    protected boolean underline;          // indicates if the font style is underlined
    public static final int AFM_DIVISOR = 1000; // used to calculate coordinates
    private boolean convertImagesToJPEG = false;
    private float jpegQuality = .95f;
    // Added by Alexej Suchov & Emmanuel Deviller
    private Composite composite;
    private int alphaFill = 255;
    private int alphaStroke = 255;

    private PdfGraphics2D() {
        dg2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /**
     * Constructor for PDFGraphics2D.
     *
     */
    PdfGraphics2D(PdfContentByte cb, float width, float height, FontMapper fontMapper, boolean onlyShapes, boolean convertImagesToJPEG, float quality) {
        super();
        dg2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        this.convertImagesToJPEG = convertImagesToJPEG;
        this.jpegQuality = quality;
        this.onlyShapes = onlyShapes;
        this.transform = new AffineTransform();
        this.baseFonts = new HashMap();
        if (!onlyShapes) {
            this.fontMapper = fontMapper;
            if (this.fontMapper == null) {
                this.fontMapper = new DefaultFontMapper();
            }
        }
        paint = Color.black;
        background = Color.white;
        setFont(new Font("sanserif", Font.PLAIN, 12));
        this.cb = cb;
        cb.saveState();
        this.width = width;
        this.height = height;
        clip = new Area(new Rectangle2D.Float(0, 0, width, height));
        clip(clip);
        originalStroke = stroke = oldStroke = strokeOne;
        setStrokeDiff(stroke, null);
        cb.saveState();
    }

    /**
     * Enable the conversion of images to JPEG in the PDF
     *
     * @param convertImagesToJPEG flag
     */
    public void convertImagesToJPEG(boolean convertImagesToJPEG) {
        this.convertImagesToJPEG = convertImagesToJPEG;
    }

    /**
     * Set the JPEG compression quality
     *
     * @param jpegQuality (0 = low quality / 1 = best quality)
     */
    public void setJpegQuality(float jpegQuality) {
        this.jpegQuality = Math.max(0, Math.min(jpegQuality, 1));
    }

    /**
     * @see Graphics2D#draw(Shape)
     */
    @Override
    public void draw(Shape s) {
        followPath(s, STROKE);
    }

    /**
     * @see Graphics2D#drawImage(Image, AffineTransform, ImageObserver)
     */
    @Override
    public boolean drawImage(java.awt.Image img, AffineTransform xform, ImageObserver obs) {
        return drawImage(img, null, xform, null, obs);
    }

    /**
     * @see Graphics2D#drawImage(BufferedImage, BufferedImageOp, int, int)
     */
    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        BufferedImage result = img;
        if (op != null) {
            result = op.createCompatibleDestImage(img, img.getColorModel());
            result = op.filter(img, result);
        }
        drawImage(result, x, y, null);
    }

    /**
     * @see Graphics2D#drawRenderedImage(RenderedImage, AffineTransform)
     */
    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        BufferedImage image = null;
        if (img instanceof BufferedImage) {
            image = (BufferedImage) img;
        }
        else {
            ColorModel cm = img.getColorModel();
            int width = img.getWidth();
            int height = img.getHeight();
            WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            Hashtable properties = new Hashtable();
            String[] keys = img.getPropertyNames();
            if (keys != null) {
                for (int i = 0; i < keys.length; i++) {
                    properties.put(keys[i], img.getProperty(keys[i]));
                }
            }
            BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
            img.copyData(raster);
            image = result;
        }
        drawImage(image, xform, null);
    }

    /**
     * @see Graphics2D#drawRenderableImage(RenderableImage, AffineTransform)
     */
    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        drawRenderedImage(img.createDefaultRendering(), xform);
    }

    /**
     * @see Graphics#drawString(String, int, int)
     */
    @Override
    public void drawString(String s, int x, int y) {
        drawString(s, (float) x, (float) y);
    }

    /**
     * Calculates position and/or stroke thickness depending on the font size
     *
     * @param d value to be converted
     * @param i font size
     * @return position and/or stroke thickness depending on the font size
     */
    public static double asPoints(double d, int i) {
        return (d * (double) i) / (double) AFM_DIVISOR;
    }

    /**
     * This routine goes through the attributes and sets the font before calling
     * the actual string drawing routine
     *
     * @param iter
     */
    protected void doAttributes(AttributedCharacterIterator iter) {
        underline = false;
        Set set = iter.getAttributes().keySet();
        for (Iterator iterator = set.iterator(); iterator.hasNext();) {
            AttributedCharacterIterator.Attribute attribute = (AttributedCharacterIterator.Attribute) iterator.next();
            if (!(attribute instanceof TextAttribute)) {
                continue;
            }
            TextAttribute textattribute = (TextAttribute) attribute;
            if (textattribute.equals(TextAttribute.FONT)) {
                Font font = (Font) iter.getAttributes().get(textattribute);
                setFont(font);
            }
            else if (textattribute.equals(TextAttribute.UNDERLINE)) {
                if (iter.getAttributes().get(textattribute) == TextAttribute.UNDERLINE_ON) {
                    underline = true;
                }
            }
            else if (textattribute.equals(TextAttribute.SIZE)) {
                Object obj = iter.getAttributes().get(textattribute);
                if (obj instanceof Integer) {
                    int i = ((Integer) obj).intValue();
                    setFont(getFont().deriveFont(getFont().getStyle(), i));
                }
                else if (obj instanceof Float) {
                    float f = ((Float) obj).floatValue();
                    setFont(getFont().deriveFont(getFont().getStyle(), f));
                }
            }
            else if (textattribute.equals(TextAttribute.FOREGROUND)) {
                setColor((Color) iter.getAttributes().get(textattribute));
            }
            else if (textattribute.equals(TextAttribute.FAMILY)) {
                Font font = getFont();
                Map fontAttributes = font.getAttributes();
                fontAttributes.put(TextAttribute.FAMILY, iter.getAttributes().get(textattribute));
                setFont(font.deriveFont(fontAttributes));
            }
            else if (textattribute.equals(TextAttribute.POSTURE)) {
                Font font = getFont();
                Map fontAttributes = font.getAttributes();
                fontAttributes.put(TextAttribute.POSTURE, iter.getAttributes().get(textattribute));
                setFont(font.deriveFont(fontAttributes));
            }
            else if (textattribute.equals(TextAttribute.WEIGHT)) {
                Font font = getFont();
                Map fontAttributes = font.getAttributes();
                fontAttributes.put(TextAttribute.WEIGHT, iter.getAttributes().get(textattribute));
                setFont(font.deriveFont(fontAttributes));
            }
        }
    }

    /**
     * @see Graphics2D#drawString(String, float, float)
     */
    @Override
    public void drawString(String s, float x, float y) {
        if (s.length() == 0) {
            return;
        }
        setFillPaint();
        if (onlyShapes) {
            drawGlyphVector(this.font.layoutGlyphVector(getFontRenderContext(), s.toCharArray(), 0, s.length(), java.awt.Font.LAYOUT_LEFT_TO_RIGHT), x, y);
//            Use the following line to compile in JDK 1.3
//            drawGlyphVector(this.font.createGlyphVector(getFontRenderContext(), s), x, y);
        }
        else {
            boolean restoreTextRenderingMode = false;
            AffineTransform at = getTransform();
            AffineTransform at2 = getTransform();
            at2.translate(x, y);
            at2.concatenate(font.getTransform());
            setTransform(at2);
            AffineTransform inverse = this.normalizeMatrix();
            AffineTransform flipper = AffineTransform.getScaleInstance(1, -1);
            inverse.concatenate(flipper);
            double[] mx = new double[6];
            inverse.getMatrix(mx);
            cb.beginText();
            cb.setFontAndSize(baseFont, fontSize);
            // Check if we need to simulate an italic font.
            // When there are different fonts for italic, bold, italic bold
            // the font.getName() will be different from the font.getFontName()
            // value. When they are the same value then we are normally dealing
            // with a single font that has been made into an italic or bold
            // font.
            if (font.isItalic() && font.getFontName().equals(font.getName())) {
                float angle = baseFont.getFontDescriptor(BaseFont.ITALICANGLE, 1000);
                float angle2 = font.getItalicAngle();
                // We don't have an italic version of this font so we need
                // to set the font angle ourselves to produce an italic font.
                if (angle2 == 0) {
                    // The JavaVM didn't have an angle setting for making
                    // the font an italic font so use a default of
                    // italic angle of 15 degrees.
                    angle2 = 15.0f;
                }
                else {
                    // This sign of the angle for Java and PDF seams
                    // seams to be reversed.
                    angle2 = -angle2;
                }
                if (angle == 0) {
                    mx[2] = angle2 / 100.0f;
                }
            }
            cb.setTextMatrix((float) mx[0], (float) mx[1], (float) mx[2], (float) mx[3], (float) mx[4], (float) mx[5]);
            Float fontTextAttributeWidth = (Float) font.getAttributes().get(TextAttribute.WIDTH);
            fontTextAttributeWidth = (fontTextAttributeWidth == null)
                    ? TextAttribute.WIDTH_REGULAR
                    : fontTextAttributeWidth;
            if (!TextAttribute.WIDTH_REGULAR.equals(fontTextAttributeWidth)) {
                cb.setHorizontalScaling(100.0f / fontTextAttributeWidth.floatValue());
            }

            // Check if we need to simulate a bold font.
            // Do nothing if the BaseFont is already bold. This test is not foolproof but it will work most of the times.
            if (baseFont.getPostscriptFontName().toLowerCase().indexOf("bold") < 0) {
                // Get the weight of the font so we can detect fonts with a weight
                // that makes them bold, but the Font.isBold() value is false.
                Float weight = (Float) font.getAttributes().get(TextAttribute.WEIGHT);
                if (weight == null) {
                    weight = (font.isBold()) ? TextAttribute.WEIGHT_BOLD
                            : TextAttribute.WEIGHT_REGULAR;
                }
                if ((font.isBold() || (weight.floatValue() >= TextAttribute.WEIGHT_SEMIBOLD.floatValue()))
                        && (font.getFontName().equals(font.getName()))) {
                    // Simulate a bold font.
                    float strokeWidth = font.getSize2D() * (weight.floatValue() - TextAttribute.WEIGHT_REGULAR.floatValue()) / 30f;
                    if (strokeWidth != 1) {
                        cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL_STROKE);
                        cb.setLineWidth(strokeWidth);
                        cb.setColorStroke(getColor());
                        restoreTextRenderingMode = true;
                    }
                }
            }

            double width = 0;
            if (font.getSize2D() > 0) {
                float scale = 1000 / font.getSize2D();
                width = font.deriveFont(AffineTransform.getScaleInstance(scale, scale)).getStringBounds(s, getFontRenderContext()).getWidth() / scale;
            }
            if (s.length() > 1) {
                float adv = ((float) width - baseFont.getWidthPoint(s, fontSize)) / (s.length() - 1);
                cb.setCharacterSpacing(adv);
            }
            cb.showText(s);
            if (s.length() > 1) {
                cb.setCharacterSpacing(0);
            }
            if (!TextAttribute.WIDTH_REGULAR.equals(fontTextAttributeWidth)) {
                cb.setHorizontalScaling(100);
            }

            // Restore the original TextRenderingMode if needed.
            if (restoreTextRenderingMode) {
                cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL);
            }

            cb.endText();
            setTransform(at);
            if (underline) {
                // These two are supposed to be taken from the .AFM file
                //int UnderlinePosition = -100;
                int UnderlineThickness = 50;
                //
                double d = asPoints((double) UnderlineThickness, (int) fontSize);
                setStroke(new BasicStroke((float) d));
                y = (float) ((double) (y) + asPoints((double) (UnderlineThickness), (int) fontSize));
                Line2D line = new Line2D.Double((double) x, (double) y, (double) (width + x), (double) y);
                draw(line);
            }
        }
    }

    /**
     * @see Graphics#drawString(AttributedCharacterIterator, int, int)
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float) x, (float) y);
    }

    /**
     * @see Graphics2D#drawString(AttributedCharacterIterator, float, float)
     */
    @Override
    public void drawString(AttributedCharacterIterator iter, float x, float y) {
        /*
         * StringBuffer sb = new StringBuffer(); for(char c = iter.first(); c !=
         * AttributedCharacterIterator.DONE; c = iter.next()) { sb.append(c); }
         * drawString(sb.toString(),x,y);
         */
        StringBuilder stringbuffer = new StringBuilder(iter.getEndIndex());
        for (char c = iter.first(); c != '\uFFFF'; c = iter.next()) {
            if (iter.getIndex() == iter.getRunStart()) {
                if (stringbuffer.length() > 0) {
                    drawString(stringbuffer.toString(), x, y);
                    FontMetrics fontmetrics = getFontMetrics();
                    x = (float) ((double) x + fontmetrics.getStringBounds(stringbuffer.toString(), this).getWidth());
                    stringbuffer.delete(0, stringbuffer.length());
                }
                doAttributes(iter);
            }
            stringbuffer.append(c);
        }

        drawString(stringbuffer.toString(), x, y);
        underline = false;
    }

    /**
     * @see Graphics2D#drawGlyphVector(GlyphVector, float, float)
     */
    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        Shape s = g.getOutline(x, y);
        fill(s);
    }

    /**
     * @see Graphics2D#fill(Shape)
     */
    @Override
    public void fill(Shape s) {
        followPath(s, FILL);
    }

    /**
     * @see Graphics2D#hit(Rectangle, Shape, boolean)
     */
    @Override
    public boolean hit(java.awt.Rectangle rect, Shape s, boolean onStroke) {
        if (onStroke) {
            s = stroke.createStrokedShape(s);
        }
        s = transform.createTransformedShape(s);
        Area area = new Area(s);
        if (clip != null) {
            area.intersect(clip);
        }
        return area.intersects(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @see Graphics2D#getDeviceConfiguration()
     */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return dg2.getDeviceConfiguration();
    }

    /**
     * Method contributed by Alexej Suchov
     *
     * @see Graphics2D#setComposite(Composite)
     */
    @Override
    public void setComposite(Composite composite) {
        this.composite = composite;
        this.paintFill = null;
        this.paintStroke = null;
    }

    /**
     * Method contributed by Alexej Suchov
     *
     * @see Graphics2D#setPaint(Paint)
     */
    @Override
    public void setPaint(Paint paint) {
        if (paint == null) {
            return;
        }
        this.paint = paint;
    }

    private Stroke transformStroke(Stroke stroke) {
        if (!(stroke instanceof BasicStroke)) {
            return stroke;
        }
        BasicStroke st = (BasicStroke) stroke;
        float scale = (float) Math.sqrt(Math.abs(transform.getDeterminant()));
        float dash[] = st.getDashArray();
        if (dash != null) {
            for (int k = 0; k < dash.length; ++k) {
                dash[k] *= scale;
            }
        }
        return new BasicStroke(st.getLineWidth() * scale, st.getEndCap(), st.getLineJoin(), st.getMiterLimit(), dash, st.getDashPhase() * scale);
    }

    private void setStrokeDiff(Stroke newStroke, Stroke oldStroke) {
        if (newStroke == oldStroke) {
            return;
        }
        if (!(newStroke instanceof BasicStroke)) {
            return;
        }
        BasicStroke nStroke = (BasicStroke) newStroke;
        boolean oldOk = (oldStroke instanceof BasicStroke);
        BasicStroke oStroke = null;
        if (oldOk) {
            oStroke = (BasicStroke) oldStroke;
        }
        if (!oldOk || nStroke.getLineWidth() != oStroke.getLineWidth()) {
            cb.setLineWidth(nStroke.getLineWidth());
        }
        if (!oldOk || nStroke.getEndCap() != oStroke.getEndCap()) {
            switch (nStroke.getEndCap()) {
                case BasicStroke.CAP_BUTT:
                    cb.setLineCap(0);
                    break;
                case BasicStroke.CAP_SQUARE:
                    cb.setLineCap(2);
                    break;
                default:
                    cb.setLineCap(1);
            }
        }
        if (!oldOk || nStroke.getLineJoin() != oStroke.getLineJoin()) {
            switch (nStroke.getLineJoin()) {
                case BasicStroke.JOIN_MITER:
                    cb.setLineJoin(0);
                    break;
                case BasicStroke.JOIN_BEVEL:
                    cb.setLineJoin(2);
                    break;
                default:
                    cb.setLineJoin(1);
            }
        }
        if (!oldOk || nStroke.getMiterLimit() != oStroke.getMiterLimit()) {
            cb.setMiterLimit(nStroke.getMiterLimit());
        }
        boolean makeDash;
        if (oldOk) {
            if (nStroke.getDashArray() != null) {
                if (nStroke.getDashPhase() != oStroke.getDashPhase()) {
                    makeDash = true;
                }
                else if (!java.util.Arrays.equals(nStroke.getDashArray(), oStroke.getDashArray())) {
                    makeDash = true;
                }
                else {
                    makeDash = false;
                }
            }
            else if (oStroke.getDashArray() != null) {
                makeDash = true;
            }
            else {
                makeDash = false;
            }
        }
        else {
            makeDash = true;
        }
        if (makeDash) {
            float dash[] = nStroke.getDashArray();
            if (dash == null) {
                cb.setLiteral("[]0 d\n");
            }
            else {
                cb.setLiteral('[');
                int lim = dash.length;
                for (int k = 0; k < lim; ++k) {
                    cb.setLiteral(dash[k]);
                    cb.setLiteral(' ');
                }
                cb.setLiteral(']');
                cb.setLiteral(nStroke.getDashPhase());
                cb.setLiteral(" d\n");
            }
        }
    }

    /**
     * @see Graphics2D#setStroke(Stroke)
     */
    @Override
    public void setStroke(Stroke s) {
        originalStroke = s;
        this.stroke = transformStroke(s);
    }

    /**
     * Sets a rendering hint
     *
     * @param arg0
     * @param arg1
     */
    @Override
    public void setRenderingHint(Key arg0, Object arg1) {
        if (arg1 != null) {
            rhints.put(arg0, arg1);
        }
        else {
            rhints.remove(arg0);
        }
    }

    /**
     * @param arg0 a key
     * @return the rendering hint
     */
    @Override
    public Object getRenderingHint(Key arg0) {
        return rhints.get(arg0);
    }

    /**
     * @see Graphics2D#setRenderingHints(Map)
     */
    @Override
    public void setRenderingHints(Map hints) {
        rhints.clear();
        rhints.putAll(hints);
    }

    /**
     * @see Graphics2D#addRenderingHints(Map)
     */
    @Override
    public void addRenderingHints(Map hints) {
        rhints.putAll(hints);
    }

    /**
     * @see Graphics2D#getRenderingHints()
     */
    @Override
    public RenderingHints getRenderingHints() {
        return rhints;
    }

    /**
     * @see Graphics#translate(int, int)
     */
    @Override
    public void translate(int x, int y) {
        translate((double) x, (double) y);
    }

    /**
     * @see Graphics2D#translate(double, double)
     */
    @Override
    public void translate(double tx, double ty) {
        transform.translate(tx, ty);
    }

    /**
     * @see Graphics2D#rotate(double)
     */
    @Override
    public void rotate(double theta) {
        transform.rotate(theta);
    }

    /**
     * @see Graphics2D#rotate(double, double, double)
     */
    @Override
    public void rotate(double theta, double x, double y) {
        transform.rotate(theta, x, y);
    }

    /**
     * @see Graphics2D#scale(double, double)
     */
    @Override
    public void scale(double sx, double sy) {
        transform.scale(sx, sy);
        this.stroke = transformStroke(originalStroke);
    }

    /**
     * @see Graphics2D#shear(double, double)
     */
    @Override
    public void shear(double shx, double shy) {
        transform.shear(shx, shy);
    }

    /**
     * @see Graphics2D#transform(AffineTransform)
     */
    @Override
    public void transform(AffineTransform tx) {
        transform.concatenate(tx);
        this.stroke = transformStroke(originalStroke);
    }

    /**
     * @see Graphics2D#setTransform(AffineTransform)
     */
    @Override
    public void setTransform(AffineTransform t) {
        transform = new AffineTransform(t);
        this.stroke = transformStroke(originalStroke);
    }

    /**
     * @see Graphics2D#getTransform()
     */
    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    /**
     * Method contributed by Alexej Suchov
     *
     * @see Graphics2D#getPaint()
     */
    @Override
    public Paint getPaint() {
        return paint;
    }

    /**
     * @see Graphics2D#getComposite()
     */
    @Override
    public Composite getComposite() {
        return composite;
    }

    /**
     * @see Graphics2D#setBackground(Color)
     */
    @Override
    public void setBackground(Color color) {
        background = color;
    }

    /**
     * @see Graphics2D#getBackground()
     */
    @Override
    public Color getBackground() {
        return background;
    }

    /**
     * @see Graphics2D#getStroke()
     */
    @Override
    public Stroke getStroke() {
        return originalStroke;
    }

    /**
     * @see Graphics2D#getFontRenderContext()
     */
    @Override
    public FontRenderContext getFontRenderContext() {
        boolean antialias = RenderingHints.VALUE_TEXT_ANTIALIAS_ON.equals(getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
        boolean fractions = RenderingHints.VALUE_FRACTIONALMETRICS_ON.equals(getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));
        return new FontRenderContext(new AffineTransform(), antialias, fractions);
    }

    /**
     * @see Graphics#create()
     */
    @Override
    public Graphics create() {
        PdfGraphics2D g2 = new PdfGraphics2D();
        g2.onlyShapes = this.onlyShapes;
        g2.transform = new AffineTransform(this.transform);
        g2.baseFonts = this.baseFonts;
        g2.fontMapper = this.fontMapper;
        g2.paint = this.paint;
        g2.background = this.background;
        g2.mediaTracker = this.mediaTracker;
        g2.convertImagesToJPEG = this.convertImagesToJPEG;
        g2.jpegQuality = this.jpegQuality;
        g2.setFont(this.font);
        g2.cb = this.cb.getDuplicate();
        g2.cb.saveState();
        g2.width = this.width;
        g2.height = this.height;
        g2.followPath(new Area(new Rectangle2D.Float(0, 0, width, height)), CLIP);
        if (this.clip != null) {
            g2.clip = new Area(this.clip);
        }
        g2.composite = composite;
        g2.stroke = stroke;
        g2.originalStroke = originalStroke;
        g2.strokeOne = (BasicStroke) g2.transformStroke(g2.strokeOne);
        g2.oldStroke = g2.strokeOne;
        g2.setStrokeDiff(g2.oldStroke, null);
        g2.cb.saveState();
        if (g2.clip != null) {
            g2.followPath(g2.clip, CLIP);
        }
        g2.kid = true;
        if (this.kids == null) {
            this.kids = new ArrayList();
        }
        this.kids.add(Integer.valueOf(cb.getInternalBuffer().size()));
        this.kids.add(g2);
        return g2;
    }

    public PdfContentByte getContent() {
        return this.cb;
    }

    /**
     * @see Graphics#getColor()
     */
    @Override
    public Color getColor() {
        if (paint instanceof Color) {
            return (Color) paint;
        }
        else {
            return Color.black;
        }
    }

    /**
     * @see Graphics#setColor(Color)
     */
    @Override
    public void setColor(Color color) {
        setPaint(color);
    }

    /**
     * @see Graphics#setPaintMode()
     */
    @Override
    public void setPaintMode() {
    }

    /**
     * @see Graphics#setXORMode(Color)
     */
    @Override
    public void setXORMode(Color c1) {
    }

    /**
     * @see Graphics#getFont()
     */
    @Override
    public Font getFont() {
        return font;
    }

    /**
     * @see Graphics#setFont(Font)
     */
    /**
     * Sets the current font.
     */
    @Override
    public void setFont(Font f) {
        if (f == null) {
            return;
        }
        if (onlyShapes) {
            font = f;
            return;
        }
        if (f == font) {
            return;
        }
        font = f;
        fontSize = f.getSize2D();
        baseFont = getCachedBaseFont(f);
    }

    private BaseFont getCachedBaseFont(Font f) {
        synchronized (baseFonts) {
            BaseFont bf = (BaseFont) baseFonts.get(f.getFontName());
            if (bf == null) {
                bf = fontMapper.awtToPdf(f);
                baseFonts.put(f.getFontName(), bf);
            }
            return bf;
        }
    }

    /**
     * @see Graphics#getFontMetrics(Font)
     */
    @Override
    public FontMetrics getFontMetrics(Font f) {
        return dg2.getFontMetrics(f);
    }

    /**
     * @see Graphics#getClipBounds()
     */
    @Override
    public java.awt.Rectangle getClipBounds() {
        if (clip == null) {
            return null;
        }
        return getClip().getBounds();
    }

    /**
     * @see Graphics#clipRect(int, int, int, int)
     */
    @Override
    public void clipRect(int x, int y, int width, int height) {
        Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
        clip(rect);
    }

    /**
     * @see Graphics#setClip(int, int, int, int)
     */
    @Override
    public void setClip(int x, int y, int width, int height) {
        Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
        setClip(rect);
    }

    /**
     * @see Graphics2D#clip(Shape)
     */
    @Override
    public void clip(Shape s) {
        if (s == null) {
            setClip(null);
            return;
        }
        s = transform.createTransformedShape(s);
        if (clip == null) {
            clip = new Area(s);
        }
        else {
            clip.intersect(new Area(s));
        }
        followPath(s, CLIP);
    }

    /**
     * @see Graphics#getClip()
     */
    @Override
    public Shape getClip() {
        try {
            return transform.createInverse().createTransformedShape(clip);
        }
        catch (NoninvertibleTransformException e) {
            return null;
        }
    }

    /**
     * @see Graphics#setClip(Shape)
     */
    @Override
    public void setClip(Shape s) {
        cb.restoreState();
        cb.saveState();
        if (s != null) {
            s = transform.createTransformedShape(s);
        }
        if (s == null) {
            clip = null;
        }
        else {
            clip = new Area(s);
            followPath(s, CLIP);
        }
        paintFill = paintStroke = null;
        alphaFill = alphaStroke = 255;
        oldStroke = strokeOne;
    }

    /**
     * @see Graphics#copyArea(int, int, int, int, int, int)
     */
    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    }

    /**
     * @see Graphics#drawLine(int, int, int, int)
     */
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        Line2D line = new Line2D.Double((double) x1, (double) y1, (double) x2, (double) y2);
        draw(line);
    }

    /**
     * @see Graphics#fillRect(int, int, int, int)
     */
    @Override
    public void drawRect(int x, int y, int width, int height) {
        draw(new java.awt.Rectangle(x, y, width, height));
    }

    /**
     * @see Graphics#fillRect(int, int, int, int)
     */
    @Override
    public void fillRect(int x, int y, int width, int height) {
        fill(new java.awt.Rectangle(x, y, width, height));
    }

    /**
     * @see Graphics#clearRect(int, int, int, int)
     */
    @Override
    public void clearRect(int x, int y, int width, int height) {
        Paint tmpPaint = paint;
        Composite tmpComposite = composite;
        setComposite(null);    // Opaque
        setPaint(background);
        fillRect(x, y, width, height);
        setPaint(tmpPaint);
        setComposite(tmpComposite);
    }

    /**
     * @see Graphics#drawRoundRect(int, int, int, int, int, int)
     */
    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight);
        draw(rect);
    }

    /**
     * @see Graphics#fillRoundRect(int, int, int, int, int, int)
     */
    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight);
        fill(rect);
    }

    /**
     * @see Graphics#drawOval(int, int, int, int)
     */
    @Override
    public void drawOval(int x, int y, int width, int height) {
        Ellipse2D oval = new Ellipse2D.Float((float) x, (float) y, (float) width, (float) height);
        draw(oval);
    }

    /**
     * @see Graphics#fillOval(int, int, int, int)
     */
    @Override
    public void fillOval(int x, int y, int width, int height) {
        Ellipse2D oval = new Ellipse2D.Float((float) x, (float) y, (float) width, (float) height);
        fill(oval);
    }

    /**
     * @see Graphics#drawArc(int, int, int, int, int, int)
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        Arc2D arc = new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN);
        draw(arc);

    }

    /**
     * @see Graphics#fillArc(int, int, int, int, int, int)
     */
    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        Arc2D arc = new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.PIE);
        fill(arc);
    }

    /**
     * @see Graphics#drawPolyline(int[], int[], int)
     */
    @Override
    public void drawPolyline(int[] x, int[] y, int nPoints) {
        PolylineShape polyline = new PolylineShape(x, y, nPoints);
        draw(polyline);
    }

    /**
     * @see Graphics#drawPolygon(int[], int[], int)
     */
    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        Polygon poly = new Polygon(xPoints, yPoints, nPoints);
        draw(poly);
    }

    /**
     * @see Graphics#fillPolygon(int[], int[], int)
     */
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        Polygon poly = new Polygon();
        for (int i = 0; i < nPoints; i++) {
            poly.addPoint(xPoints[i], yPoints[i]);
        }
        fill(poly);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, ImageObserver)
     */
    @Override
    public boolean drawImage(java.awt.Image img, int x, int y, ImageObserver observer) {
        return drawImage(img, x, y, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, ImageObserver)
     */
    @Override
    public boolean drawImage(java.awt.Image img, int x, int y, int width, int height, ImageObserver observer) {
        return drawImage(img, x, y, width, height, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, Color, ImageObserver)
     */
    @Override
    public boolean drawImage(java.awt.Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        waitForImage(img);
        return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), bgcolor, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, Color, ImageObserver)
     */
    @Override
    public boolean drawImage(java.awt.Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        waitForImage(img);
        double scalex = width / (double) img.getWidth(observer);
        double scaley = height / (double) img.getHeight(observer);
        AffineTransform tx = AffineTransform.getTranslateInstance(x, y);
        tx.scale(scalex, scaley);
        return drawImage(img, null, tx, bgcolor, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, int, int, int, int,
     * ImageObserver)
     */
    @Override
    public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, int, int, int, int,
     * Color, ImageObserver)
     */
    @Override
    public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor,
            ImageObserver observer) {
        waitForImage(img);
        double dwidth = (double) dx2 - dx1;
        double dheight = (double) dy2 - dy1;
        double swidth = (double) sx2 - sx1;
        double sheight = (double) sy2 - sy1;

        //if either width or height is 0, then there is nothing to draw
        if (dwidth == 0 || dheight == 0 || swidth == 0 || sheight == 0) {
            return true;
        }

        double scalex = dwidth / swidth;
        double scaley = dheight / sheight;

        double transx = sx1 * scalex;
        double transy = sy1 * scaley;
        AffineTransform tx = AffineTransform.getTranslateInstance(dx1 - transx, dy1 - transy);
        tx.scale(scalex, scaley);

        BufferedImage mask = new BufferedImage(img.getWidth(observer), img.getHeight(observer), BufferedImage.TYPE_BYTE_BINARY);
        Graphics g = mask.getGraphics();
        g.fillRect(sx1, sy1, (int) swidth, (int) sheight);
        g.dispose();

        try {
            com.lowagie.text.Image msk = com.lowagie.text.Image.getInstance(mask, null, true);
            msk.makeMask();
            msk.setInverted(true);
            drawImage(img, msk, tx, null, observer);
        }
        catch (Exception e) {
            throw new IllegalArgumentException();
        }

        return true;
    }

    /**
     * @see Graphics#dispose()
     */
    @Override
    public void dispose() {
        if (kid) {
            return;
        }
        if (!disposeCalled) {
            disposeCalled = true;
            cb.restoreState();
            cb.restoreState();
            dg2.dispose();
            dg2 = null;
            if (kids != null) {
                ByteBuffer buf = new ByteBuffer();
                internalDispose(buf);
                ByteBuffer buf2 = cb.getInternalBuffer();
                buf2.reset();
                buf2.append(buf);
            }
        }
    }

    private void internalDispose(ByteBuffer buf) {
        int last = 0;
        int pos = 0;
        ByteBuffer buf2 = cb.getInternalBuffer();
        if (kids != null) {
            for (int k = 0; k < kids.size(); k += 2) {
                pos = ((Integer) kids.get(k)).intValue();
                PdfGraphics2D g2 = (PdfGraphics2D) kids.get(k + 1);
                g2.cb.restoreState();
                g2.cb.restoreState();
                buf.append(buf2.getBuffer(), last, pos - last);
                g2.dg2.dispose();
                g2.dg2 = null;
                g2.internalDispose(buf);
                last = pos;
            }
        }
        buf.append(buf2.getBuffer(), last, buf2.size() - last);
    }

    ///////////////////////////////////////////////
    //
    //
    //		implementation specific methods
    //
    //
    private void followPath(Shape s, int drawType) {
        if (s == null) {
            return;
        }
        if (drawType == STROKE) {
            if (!(stroke instanceof BasicStroke)) {
                s = stroke.createStrokedShape(s);
                followPath(s, FILL);
                return;
            }
        }
        if (drawType == STROKE) {
            setStrokeDiff(stroke, oldStroke);
            oldStroke = stroke;
            setStrokePaint();
        }
        else if (drawType == FILL) {
            setFillPaint();
        }
        PathIterator points;
        int traces = 0;
        if (drawType == CLIP) {
            points = s.getPathIterator(IDENTITY);
        }
        else {
            points = s.getPathIterator(transform);
        }
        float[] coords = new float[6];
        while (!points.isDone()) {
            ++traces;
            int segtype = points.currentSegment(coords);
            normalizeY(coords);
            switch (segtype) {
                case PathIterator.SEG_CLOSE:
                    cb.closePath();
                    break;

                case PathIterator.SEG_CUBICTO:
                    cb.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;

                case PathIterator.SEG_LINETO:
                    cb.lineTo(coords[0], coords[1]);
                    break;

                case PathIterator.SEG_MOVETO:
                    cb.moveTo(coords[0], coords[1]);
                    break;

                case PathIterator.SEG_QUADTO:
                    cb.curveTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
            }
            points.next();
        }
        switch (drawType) {
            case FILL:
                if (traces > 0) {
                    if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD) {
                        cb.eoFill();
                    }
                    else {
                        cb.fill();
                    }
                }
                break;
            case STROKE:
                if (traces > 0) {
                    cb.stroke();
                }
                break;
            default: //drawType==CLIP
                if (traces == 0) {
                    cb.rectangle(0, 0, 0, 0);
                }
                if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD) {
                    cb.eoClip();
                }
                else {
                    cb.clip();
                }
                cb.newPath();
        }
    }

    private float normalizeY(float y) {
        return this.height - y;
    }

    private void normalizeY(float[] coords) {
        coords[1] = normalizeY(coords[1]);
        coords[3] = normalizeY(coords[3]);
        coords[5] = normalizeY(coords[5]);
    }

    private AffineTransform normalizeMatrix() {
        double[] mx = new double[6];
        AffineTransform result = AffineTransform.getTranslateInstance(0, 0);
        result.getMatrix(mx);
        mx[3] = -1;
        mx[5] = height;
        result = new AffineTransform(mx);
        result.concatenate(transform);
        return result;
    }

    private boolean drawImage(java.awt.Image img, com.lowagie.text.Image mask, AffineTransform xform, Color bgColor, ImageObserver obs) {
        if (xform == null) {
            return true;
        }

        xform.translate(0, img.getHeight(obs));
        xform.scale(img.getWidth(obs), img.getHeight(obs));

        AffineTransform inverse = this.normalizeMatrix();
        AffineTransform flipper = AffineTransform.getScaleInstance(1, -1);
        inverse.concatenate(xform);
        inverse.concatenate(flipper);

        double[] mx = new double[6];
        inverse.getMatrix(mx);

        int tmpAlphaFill = alphaFill;
        setFillOpacity(getAlpha());

        try {
            com.lowagie.text.Image image = null;
            if (!convertImagesToJPEG) {
                image = com.lowagie.text.Image.getInstance(img, bgColor);
            }
            else {
                BufferedImage bi;
                if (img instanceof BufferedImage) {
                    bi = (BufferedImage) img;
                }
                else {
                    bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = bi.createGraphics();
                    g2.setComposite(AlphaComposite.Src);
                    g2.drawImage(img, 0, 0, null);
                    g2.dispose();
                }

                
                ColorModel cm = bi.getColorModel();
                if (cm.hasAlpha()) {
                   
                    if (bi.getType() != BufferedImage.TYPE_INT_ARGB) {
                        bi = getRGBImage(bi, true);
                    }

                    int sum = 0;
                    int[] pixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
                    byte smask[] = new byte[pixels.length];

                    for (int i = 0; i < pixels.length; i++) {
                        smask[i] = (byte) ((pixels[i] >> 24) & 0xFF);
                        sum += smask[i];
                    }
                    if (sum < (255 * pixels.length)) {
                        mask = com.lowagie.text.Image.getInstance(bi.getWidth(), bi.getHeight(), 1, 8, smask);
                        mask.makeMask();
                    }

                    bi = getRGBImage(bi, false);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    writeJpeg(bi, baos, jpegQuality);
                }
                catch(IOException ex) {
                    System.err.println(ex);
                    writeJpeg(getRGBImage(bi, false), baos, jpegQuality);
                }
                
                image = com.lowagie.text.Image.getInstance(baos.toByteArray());
            }

            if (mask != null) {
                image.setImageMask(mask);
            }

            cb.addImage(image, (float) mx[0], (float) mx[1], (float) mx[2], (float) mx[3], (float) mx[4], (float) mx[5]);

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            setFillOpacity(tmpAlphaFill);
        }

        return true;
    }

    public static BufferedImage getRGBImage(BufferedImage image, boolean hasAlpha) {
        BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), (hasAlpha) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return img;    
    }
    
    private static void writeJpeg(BufferedImage image, Object obj, float jpegQuality) throws IOException {
        // Create output stream
        ImageOutputStream ios = null;
        ImageWriter writer = (ImageWriter) ImageIO.getImageWritersByFormatName("jpg").next();

        try {
            ios = ImageIO.createImageOutputStream(obj);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(jpegQuality);

            // Write the image
            writer.write(null, new IIOImage(image, null, null), param);
        }
        finally {
            closeQuietly(ios);
            writer.dispose();
        }
    }    
    
    private static void closeQuietly(ImageOutputStream ios) {
        try {
            if (ios != null) {
                ios.close();
            }
        }
        catch (IOException ioe) {
        }
    }    
    
    private boolean checkNewPaint(Paint oldPaint) {
        if (paint == oldPaint) {
            return false;
        }
        return !((paint instanceof Color) && paint.equals(oldPaint));
    }

    private void setFillPaint() {
        if (checkNewPaint(paintFill)) {
            paintFill = paint;
            setPdfPaint(false, 0, 0, true);
        }
    }

    private void setStrokePaint() {
        if (checkNewPaint(paintStroke)) {
            paintStroke = paint;
            setPdfPaint(false, 0, 0, false);
        }
    }

    private int getAlpha() {
        if (composite instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) composite;
            if (ac.getRule() == AlphaComposite.SRC_OVER) {
                return Math.round(255 * ac.getAlpha());
            }
        }
        return 255;  // OPAQUE
    }

    private Color getCompositeColor(Color color) {
        if (composite instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) composite;
            if (ac.getRule() == AlphaComposite.SRC_OVER && ac.getAlpha() != 1f) {
                return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(((float) color.getAlpha() * ac.getAlpha())));
            }
        }
        return color;
    }

    private void setFillOpacity(int alpha) {
        if (alpha != alphaFill) {
            alphaFill = alpha;
            PdfGState gs = new PdfGState();
            gs.setFillOpacity((float) alpha / 255f);
            cb.setGState(gs);
        }
    }

    private void setStrokeOpacity(int alpha) {
        if (alpha != alphaStroke) {
            alphaStroke = alpha;
            PdfGState gs = new PdfGState();
            gs.setStrokeOpacity((float) alpha / 255f);
            cb.setGState(gs);
        }
    }

    private void setPdfPaint(boolean invert, double xoffset, double yoffset, boolean fill) {
        if (paint instanceof Color) {
            Color color = getCompositeColor((Color) paint);
            if (fill) {
                setFillOpacity(color.getAlpha());
                cb.setColorFill(color);
            }
            else {
                setStrokeOpacity(color.getAlpha());
                cb.setColorStroke(color);
            }
        }
        else if (paint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) paint;
            Point2D p1 = gp.getPoint1();
            transform.transform(p1, p1);
            Point2D p2 = gp.getPoint2();
            transform.transform(p2, p2);
            Color c1 = (Color) getCompositeColor(gp.getColor1());
            Color c2 = (Color) getCompositeColor(gp.getColor2());
            PdfShading shading = PdfShading.simpleAxial(cb.getPdfWriter(), (float) p1.getX(), normalizeY((float) p1.getY()), (float) p2.getX(), normalizeY((float) p2.getY()), c1, c2);
            PdfShadingPattern pat = new PdfShadingPattern(shading);
            if (fill) {
                setFillOpacity(Math.round((c1.getAlpha() + c2.getAlpha()) / 2f));
                cb.setShadingFill(pat);
            }
            else {
                setStrokeOpacity(Math.round((c1.getAlpha() + c2.getAlpha()) / 2f));
                cb.setShadingStroke(pat);
            }
        }
        else if (paint instanceof TexturePaint) {
            try {
                TexturePaint tp = (TexturePaint) paint;
                BufferedImage img = tp.getImage();
                Rectangle2D rect = tp.getAnchorRect();
                com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(img, null);
                PdfPatternPainter pattern = cb.createPattern(image.getWidth(), image.getHeight());
                AffineTransform inverse = this.normalizeMatrix();
                inverse.translate(rect.getX(), rect.getY());
                inverse.scale(rect.getWidth() / image.getWidth(), -rect.getHeight() / image.getHeight());
                double[] mx = new double[6];
                inverse.getMatrix(mx);
                pattern.setPatternMatrix((float) mx[0], (float) mx[1], (float) mx[2], (float) mx[3], (float) mx[4], (float) mx[5]);
                image.setAbsolutePosition(0, 0);
                pattern.addImage(image);
                if (fill) {
                    setFillOpacity(getAlpha());
                    cb.setPatternFill(pattern);
                }
                else {
                    setStrokeOpacity(getAlpha());
                    cb.setPatternStroke(pattern);
                }
            }
            catch (Exception ex) {
                if (fill) {
                    cb.setColorFill(Color.gray);
                }
                else {
                    cb.setColorStroke(Color.gray);
                }
            }
        }
        else {
            try {
                BufferedImage img = null;
                int type = BufferedImage.TYPE_4BYTE_ABGR;
                if (paint.getTransparency() == Transparency.OPAQUE) {
                    type = BufferedImage.TYPE_3BYTE_BGR;
                }
                img = new BufferedImage((int) width, (int) height, type);
                Graphics2D g = (Graphics2D) img.getGraphics();
                g.transform(transform);
                AffineTransform inv = transform.createInverse();
                Shape fillRect = new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight());
                fillRect = inv.createTransformedShape(fillRect);
                g.setPaint(paint);
                g.fill(fillRect);
                if (invert) {
                    AffineTransform tx = new AffineTransform();
                    tx.scale(1, -1);
                    tx.translate(-xoffset, -yoffset);
                    g.drawImage(img, tx, null);
                }
                g.dispose();
                g = null;
                com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(img, null);
                PdfPatternPainter pattern = cb.createPattern(width, height);
                image.setAbsolutePosition(0, 0);
                pattern.addImage(image);
                if (fill) {
                    setFillOpacity(getAlpha());
                    cb.setPatternFill(pattern);
                }
                else {
                    setStrokeOpacity(getAlpha());
                    cb.setPatternStroke(pattern);
                }
            }
            catch (Exception ex) {
                if (fill) {
                    cb.setColorFill(Color.gray);
                }
                else {
                    cb.setColorStroke(Color.gray);
                }
            }
        }
    }

    private synchronized void waitForImage(java.awt.Image image) {
        if (mediaTracker == null) {
            mediaTracker = new MediaTracker(new PdfGraphics2D.fakeComponent());
        }

        mediaTracker.addImage(image, 0);
        try {
            mediaTracker.waitForID(0);
        }
        catch (InterruptedException e) {
            // empty on purpose
        }
        mediaTracker.removeImage(image);
    }

    static private class fakeComponent extends Component {

        private static final long serialVersionUID = 6450197945596086638L;
    }
}
