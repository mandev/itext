/*
 * $Id: RtfTOCEntry.java 2784 2007-05-24 15:43:40Z hallm $
 * $Name$
 *
 * Copyright 2004 by Mark Hall
 * Uses code Copyright 2002
 *   Steffen.Stundzig (Steffen.Stundzig@smb-tec.com) 
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

package com.lowagie.text.rtf.field;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.lowagie.text.Font;


/**
 * The RtfTOCEntry is used together with the RtfTableOfContents to generate a table of
 * contents. Add the RtfTOCEntry in those locations in the document where table of
 * contents entries should link to 
 * 
 * @version $Id: RtfTOCEntry.java 2784 2007-05-24 15:43:40Z hallm $
 * @author Mark Hall (mhall@edu.uni-klu.ac.at)
 * @author Steffen.Stundzig (Steffen.Stundzig@smb-tec.com) 
 * @author Thomas Bickel (tmb99@inode.at)
 */
public class RtfTOCEntry extends RtfField {

    /**
     * Constant for the beginning of hidden text
     */
    private static final byte[] TEXT_HIDDEN_ON = "\\v".getBytes();
    /**
     * Constant for the end of hidden text
     */
    private static final byte[] TEXT_HIDDEN_OFF = "\\v0".getBytes();
    /**
     * Constant for a TOC entry with page numbers
     */
    private static final byte[] TOC_ENTRY_PAGE_NUMBER = "\\tc".getBytes();
    /**
     * Constant for a TOC entry without page numbers
     */
    private static final byte[] TOC_ENTRY_NO_PAGE_NUMBER = "\\tcn".getBytes();
    
    /**
     * The entry text of this RtfTOCEntry
     */
    private String entry = "";
    /**
     * Whether to show page numbers in the table of contents
     */
    private boolean showPageNumber = true;
    
    /**
     * Constructs a RtfTOCEntry with a certain entry text.
     * 
     * @param entry The entry text to display
     */
    public RtfTOCEntry(String entry) {
        super(null, new Font());
        if(entry != null) {
            this.entry = entry;
        }
    }
    
    /**
     * Writes the content of the RtfTOCEntry
     * 
     * @return A byte array with the contents of the RtfTOCEntry
     * @deprecated replaced by {@link #writeContent(OutputStream)}
     */
    public byte[] write() {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
        	writeContent(result);
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return result.toByteArray();
    }
    /**
     * Writes the content of the RtfTOCEntry
     */ 
    public void writeContent(final OutputStream result) throws IOException
    {    	
        result.write(TEXT_HIDDEN_ON);
        result.write(OPEN_GROUP);
        if(this.showPageNumber) {
            result.write(TOC_ENTRY_PAGE_NUMBER);
        } else {
            result.write(TOC_ENTRY_NO_PAGE_NUMBER);
        }
        result.write(DELIMITER);
        //.result.write(this.document.filterSpecialChar(this.entry, true, false).getBytes());
        this.document.filterSpecialChar(result, this.entry, true, false);
        result.write(CLOSE_GROUP);
        result.write(TEXT_HIDDEN_OFF);
    }
    
    /**
     * Sets whether to display a page number in the table of contents, or not
     * 
     * @param showPageNumber Whether to display a page number or not
     */
    public void setShowPageNumber(boolean showPageNumber) {
        this.showPageNumber = showPageNumber;
    }
    
    /**
     * UNUSED
     * @return null
     * @deprecated
     * @throws IOException never thrown
     */
    protected byte[] writeFieldInstContent() throws IOException 
    {
        return null;
    }
    /**
     * unused
     */
    protected void writeFieldInstContent(OutputStream out) throws IOException 
    {
    }

    /**
     * UNUSED
     * @return null
     * @deprecated
     * @throws IOException never thrown
     */
    protected byte[] writeFieldResultContent() throws IOException {
        return null;
    }
    
    /*
     * unused
     * @see com.lowagie.text.rtf.field.RtfField#writeFieldResultContent(java.io.OutputStream)
     */
    protected void writeFieldResultContent(OutputStream out) throws IOException
    {
    }

}
