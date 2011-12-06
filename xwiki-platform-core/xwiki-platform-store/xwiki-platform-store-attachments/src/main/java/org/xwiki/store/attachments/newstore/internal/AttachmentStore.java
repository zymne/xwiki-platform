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
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.store.TransactionRunnable;

/**
 * A means of storing the metadata of an attachment.
 * This interface is designed to seperate concerns of content, metadata, and archive.
 *
 * @version $Id$
 * @param <T> The type of storage engine which must be available for this attachment store.
 * @since 3.3M2
 */
@ComponentRole
public interface AttachmentStore<T>
{
    /**
     * Save the metadata of some attachments.
     * Content and history will not be saved.
     *
     * @param attachments the attachments to save, these must each be attached to a document
     *                    although they need not all be attached to the same document.
     * @return a new TransactionRunnable to save the given attachments.
     */
    TransactionRunnable<T> getAttachmentSaveRunnable(final List<XWikiAttachment> attachments);

    /**
     * Load the metadata of some attachments.
     * The output list will be populated with attachments.
     * Attachments WILL NOT be attached to any document, such is the responsiblity of the caller.
     *
     * @param references a lits of AttachmentReferences to the desired attachments.
     * @param output a list which will be populated with attachments for each of the references.
     *               The attachments will be in the same order as the references.
     * @return a new TransactionRunnable to load the attachments and place them in the list.
     */
    TransactionRunnable<T> getAttachmentLoadRunnable(final List<AttachmentReference> references,
                                                     final List<XWikiAttachment> output);

    /**
     * Delete an attachment.
     * Only metadata will be deleted, content and archive must be deleted seperately.
     *
     * @param toDelete the attachments to be deleted, each of these must be attached to a document
     *                 although they need not all be attached to the same document.
     * @return a new TransactionRunnable to delete the attachments.
     */
    TransactionRunnable<T> getAttachmentDeleteRunnable(final List<XWikiAttachment> toDelete);
}
