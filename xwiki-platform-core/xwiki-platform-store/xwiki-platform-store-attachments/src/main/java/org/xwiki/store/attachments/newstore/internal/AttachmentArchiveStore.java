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

import java.util.List;

import com.xpn.xwiki.doc.XWikiAttachment;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.store.TransactionRunnable;

/**
 * A means of storing the an attachment archive.
 *
 * @version $Id$
 * @param <T> The type of storage engine which must be available for this attachment store.
 * @since 3.3M2
 */
@ComponentRole
public interface AttachmentArchiveStore<T>
{
    /**
     * Save the archive of an attachment.
     * Neither content nor metadata will not be saved, only the archive.
     * The archive must be loaded first and the attachment must be attached to a document.
     *
     * @param versions all of the versions of an attachment, they must all be attached to the
     *                 same document and all must have content already loaded.
     * @return a new TransactionRunnable to save an attachment archive made of the given versions.
     */
    TransactionRunnable<T> getAttachmentArchiveSaveRunnable(final List<XWikiAttachment> versions);

    /**
     * Load the archive of an attachment.
     * The archive will be placed in the attachment object.
     * The attachment must be attached to a document.
     *
     * @param attachment an attachment, this must be attached to a document.
     * @return a new TransactionRunnable to load archive for the given attachment.
     */
    TransactionRunnable<T> getAttachmentArchiveLoadRunnable(final XWikiAttachment attachment);

    /**
     * Delete the attachment archive.
     * Neither metadata not content will not be deleted, only the archive.
     * The attachment must be attached to a document.
     *
     * @param attachment an attachment, this must be attached to a document.
     * @return a new TransactionRunnable to delete the attachment archive.
     */
    TransactionRunnable<T> getAttachmentArchiveDeleteRunnable(final XWikiAttachment attachment);
}
