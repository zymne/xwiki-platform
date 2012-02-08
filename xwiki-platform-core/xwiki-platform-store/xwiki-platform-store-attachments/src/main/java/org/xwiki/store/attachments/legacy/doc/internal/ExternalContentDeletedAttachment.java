/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.store.attachments.legacy.doc.internal;

import java.util.Date;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import org.xwiki.model.reference.DocumentReference;

/**
 * Deleted attachment with content stored externally.
 *
 * @version $Id$
 * @since TODO
 */
public class ExternalContentDeletedAttachment extends DeletedAttachment
{
    /**
     * The reference to the document which this attachment belongs to.
     */
    private DocumentReference docReference;

    /**
     * The attachment which was deleted.
     */
    private XWikiAttachment attachment;

    /**
     * A constructor with all the information about the deleted attachment.
     *
     * @param attachment Deleted attachment.
     * @param deleter User which deleted the attachment.
     * @param deleteDate Date of delete action.
     * @throws XWikiException won't happen unless DeletedAttachment constructor is modified.
     */
    public ExternalContentDeletedAttachment(final XWikiAttachment attachment,
                                            final String deleter,
                                            final Date deleteDate) throws XWikiException
    {
        // This relies on the fact that DeletedAttachment constructor doesn't use the context
        // for anything except to pass it to setAttachment which is overridden.
        super(attachment, deleter, deleteDate, null);
    }

    /**
     * Clone a DeletedAttachment without the content.
     *
     * @param original a DeletedAttachment to copy.
     */
    public ExternalContentDeletedAttachment(final DeletedAttachment original)
    {
        this.setId(original.getId());
        this.setDocId(original.getDocId());
        this.setDocName(original.getDocName());
        this.setFilename(original.getFilename());
        this.setDate(original.getDate());
        this.setDeleter(original.getDeleter());
    }

    @Override
    public String getXml()
    {
        return "<!-- Attachment data is now stored externally. -->";
    }

    /**
     * {@inheritDoc}
     * context is unused and may safely be null.
     */
    @Override
    public void setAttachment(final XWikiAttachment attachment, final XWikiContext context)
    {
        this.attachment = attachment;
    }

    /**
     * Get the attachment.
     * This does not clone the attachment.
     * To get a clone, use {@link #restoreAttachment(XWikiAttachment XWikiContext)}
     *
     * @return the attachment which was deleted.
     */
    public XWikiAttachment getAttachment()
    {
        return this.attachment;
    }

    /**
     * @return the DocumentReference to the document which this attachment is attached to.
     */
    public DocumentReference getDocumentReference()
    {
        return this.docReference;
    }

    /**
     * Set the document reference for the document which this attachment is attached to.
     *
     * @param docReference the reference to the document.
     */
    protected void setDocumentReference(final DocumentReference docReference)
    {
        this.docReference = docReference;
    }

    @Override
    public XWikiAttachment restoreAttachment(final XWikiAttachment attachment,
                                             final XWikiContext context)
        throws XWikiException
    {
        XWikiAttachment result = attachment;
        if (result != null) {
            // TODO Add XWikiAttachment#clone(XWikiAttachment)
            // this toXML does not copy content.
            result.fromXML(this.attachment.toXML(context));
            if (this.attachment.getAttachment_content() != null) {
                attachment.setAttachment_content(
                    (XWikiAttachmentContent) this.attachment.getAttachment_content().clone());
                attachment.getAttachment_content().setAttachment(attachment);
            }
            if (this.attachment.getAttachment_archive() != null) {
                result.setAttachment_archive(
                    (XWikiAttachmentArchive) this.attachment.getAttachment_archive().clone());
                result.getAttachment_archive().setAttachment(result);
            }
        } else {
            result = (XWikiAttachment) this.attachment.clone();
        }

        result.setDoc(context.getWiki().getDocument(this.getDocumentReference(), context));
        return result;
    }
}
