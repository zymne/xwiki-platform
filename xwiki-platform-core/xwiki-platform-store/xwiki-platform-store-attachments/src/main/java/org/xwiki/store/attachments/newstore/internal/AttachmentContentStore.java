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
package org.xwiki.store.attachments.newstore.internal;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.store.TransactionRunnable;

/**
 * A means of storing the content of an attachment.
 * This interface is designed to seperate concerns of content, metadata, and archive.
 *
 * @version $Id$
 * @param <T> The type of storage engine which must be available for this attachment store.
 * @since 3.3M2
 */
@ComponentRole
public interface AttachmentContentStore<T>
{
    /**
     * Save the content of an attachment.
     * Metadata will not be saved, only the content.
     * The content must be loaded first and the attachment must be attached to a document.
     *
     * @param content the attachment content, this must be part of an attachment which is attached
     *                to a document, therefor getAttachment().getDoc() must not return null.
     * @return a new TransactionRunnable to store the content for the given attachment.
     */
    TransactionRunnable<T> getAttachmentContentSaveRunnable(final XWikiAttachmentContent content);

    /**
     * Load the content of an attachment.
     * Content will be placed in the attachment object.
     * The attachment must be attached to a document.
     *
     * @param attachment an attachment, this must be attached to a document.
     * @return a new TransactionRunnable to load the content for the given attachment.
     */
    TransactionRunnable<T> getAttachmentContentLoadRunnable(final XWikiAttachment attachment);

    /**
     * Delete the content of an attachment.
     * Metadata will not be deleted, only the content.
     * The attachment must be attached to a document.
     *
     * @param attachment an attachment, this must be attached to a document.
     * @return a new TransactionRunnable to delete the content for the given attachment.
     */
    TransactionRunnable<T> getAttachmentContentDeleteRunnable(final XWikiAttachment attachment);
}
