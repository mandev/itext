/*
 * $Id: RtfListItem.java 2811 2007-05-31 18:51:52Z hallm $
 * $Name:  $
 *
 * Copyright 2001, 2002, 2003, 2004, 2005 by Mark Hall
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
 * LGPL license (the ?GNU LIBRARY GENERAL PUBLIC LICENSE?), in which case the
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

package com.lowagie.text.rtf.list;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.lowagie.text.ListItem;
import com.lowagie.text.rtf.RtfBasicElement;
import com.lowagie.text.rtf.document.RtfDocument;
import com.lowagie.text.rtf.style.RtfParagraphStyle;
import com.lowagie.text.rtf.text.RtfChunk;
import com.lowagie.text.rtf.text.RtfParagraph;


/**
 * The RtfListItem acts as a wrapper for a ListItem.
 * 
 * @version $Id: RtfListItem.java 2811 2007-05-31 18:51:52Z hallm $
 * @author Mark Hall (mhall@edu.uni-klu.ac.at)
 * @author Thomas Bickel (tmb99@inode.at)
 */
public class RtfListItem extends RtfParagraph {

    /**
     * The RtfList this RtfListItem belongs to.
     */
    private RtfList parentList = null;
    /**
     * Whether this RtfListItem contains further RtfLists.
     */
    private boolean containsInnerList = false;
    
    /**
     * Constructs a RtfListItem for a ListItem belonging to a RtfDocument.
     * 
     * @param doc The RtfDocument this RtfListItem belongs to.
     * @param listItem The ListItem this RtfListItem is based on.
     */
    public RtfListItem(RtfDocument doc, ListItem listItem) {
        super(doc, listItem);
    }
    
    /**
     * Writes the content of this RtfListItem.
     * 
     * @return A byte array with the content of this RtfListItem.
     * @deprecated replaced by {@link #writeContent(OutputStream)}
     */
    public byte[] write() 
    {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
        	writeContent(result);
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return result.toByteArray();
    }
    /**
     * Writes the content of this RtfListItem.
     */    
    public void writeContent(final OutputStream result) throws IOException
    {
        if(this.paragraphStyle.getSpacingBefore() > 0) {
            result.write(RtfParagraphStyle.SPACING_BEFORE);
            result.write(intToByteArray(paragraphStyle.getSpacingBefore()));
        }
        if(this.paragraphStyle.getSpacingAfter() > 0) {
            result.write(RtfParagraphStyle.SPACING_AFTER);
            result.write(intToByteArray(this.paragraphStyle.getSpacingAfter()));
        }
        for(int i = 0; i < chunks.size(); i++) {
            RtfBasicElement rtfElement = (RtfBasicElement) chunks.get(i);
            if(rtfElement instanceof RtfChunk) {
                ((RtfChunk) rtfElement).setSoftLineBreaks(true);
            } else if(rtfElement instanceof RtfList) {
                result.write(RtfParagraph.PARAGRAPH);
                this.containsInnerList = true;
            }
            //.result.write(rtfElement.write());
            rtfElement.writeContent(result);
            if(rtfElement instanceof RtfList) {
                result.write(this.parentList.writeListBeginning());
                result.write("\\tab".getBytes());
            }
        }
    }        

    /**
     * Returns the definition of the first list contained in this RtfListItem or
     * an empty byte array if no inner RtfLists exist.
     * 
     * @return The definition of the first inner RtfList or an empty byte array.
     * @deprecated replaced by {@link #writeDefinition(OutputStream)}
     */
    public byte[] writeDefinition() {
        for(int i = 0; i < chunks.size(); i++) {
            RtfBasicElement rtfElement = (RtfBasicElement) chunks.get(i);
            if(rtfElement instanceof RtfList) {
                return ((RtfList) rtfElement).writeDefinition();
            }
        }
        return new byte[0];
    }
    /**
     * Writes the definition of the first element in this RtfListItem that is
     * an instanceof {@link RtfList} to the given stream.<br> 
     * If this item does not contain a {@link RtfList} element nothing is written
     * and the method returns <code>false</code>.
     * 
     * @param out destination stream
     * @return <code>true</code> if a RtfList definition was written, <code>false</code> otherwise
     * @throws IOException
     */
    public boolean writeDefinition(OutputStream out) throws IOException
    {
        for(int i = 0; i < chunks.size(); i++) {
            RtfBasicElement rtfElement = (RtfBasicElement)chunks.get(i);
            if(rtfElement instanceof RtfList) {
            	RtfList rl = (RtfList)rtfElement;
            	rl.writeDefinition(out);
                return(true);
            }
        }
        return(false);    	
    }
    
    /**
     * Inherit the list settings from the parent list to RtfLists that
     * are contained in this RtfListItem.
     * 
     * @param listNumber The list number to inherit.
     * @param listLevel The list level to inherit.
     */
    public void inheritListSettings(int listNumber, int listLevel) {
        for(int i = 0; i < chunks.size(); i++) {
            RtfBasicElement rtfElement = (RtfBasicElement) chunks.get(i);
            if(rtfElement instanceof RtfList) {
                ((RtfList) rtfElement).setListNumber(listNumber);
                ((RtfList) rtfElement).setListLevel(listLevel);
                ((RtfList) rtfElement).setParent(this.parentList);
            }
        }
    }
        
    /**
     * Correct the indentation of RtfLists in this RtfListItem by adding left/first line indentation
     * from the parent RtfList. Also calls correctIndentation on all child RtfLists.
     */
    protected void correctIndentation() {
        for(int i = 0; i < chunks.size(); i++) {
            RtfBasicElement rtfElement = (RtfBasicElement) chunks.get(i);
            if(rtfElement instanceof RtfList) {
                ((RtfList) rtfElement).correctIndentation();
            }
        }
    }
    
    /**
     * Set the parent RtfList.
     * 
     * @param parentList The parent RtfList to use.
     */
    public void setParent(RtfList parentList) {
        this.parentList = parentList;
    }

    /**
     * Gets whether this RtfListItem contains further RtfLists.
     * 
     * @return Whether this RtfListItem contains further RtfLists.
     */
    public boolean isContainsInnerList() {
        return this.containsInnerList;
    }
}
