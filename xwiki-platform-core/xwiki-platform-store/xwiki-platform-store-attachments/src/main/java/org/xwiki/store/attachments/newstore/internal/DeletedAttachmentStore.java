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

import java.util.Date;
import java.util.List;

import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.store.TransactionRunnable;

/**
 * A means of storing deleted attachments so that they can be revived.
 * This stores the deleted attachment metadata such as who deleted it and when
 * but not the content, metadata or archive of the actual attachment.
 *
 * @version $Id$
 * @param <T> The type of storage engine which must be available for this attachment store.
 * @since 3.3M2
 */
@ComponentRole
public interface DeletedAttachmentStore<T>
{
    /**
     * Save the deleted attachment metadata.
     *
     * @param attachment the attachment to store as deleted.
     * @param deleter a reference to the user document of the user which deleted the attachment.
     * @param dateOfDeletion when the attachment was deleted.
     * @return a TransactionRunnable which will save the metadata.
     */
    TransactionRunnable<T> getDeletedAttachmentSaveRunnable(final XWikiAttachment attachment,
                                                            final EntityReference deleter,
                                                            final Date dateOfDeletion);

    /**
     * Load the metadata for each of a list of deleted attachments.
     * If one of the attachments was deleted more than once, it will show up multiple times
     * in the output list.
     * These DeletedAttachments will NOT be attached to any document, attaching them is the
     * responsibility of the caller.
     *
     * @param reference a reference to an attachment to get deleted versions of.
     * @param output an empty list to be populated with the deleted versions of this attachment.
     * @return a TransactionRunnable which will load the deleted attachments and populate the list.
     */
    TransactionRunnable<T> getDeletedAttachmentLoadRunnable(final AttachmentReference reference,
                                                            final List<DeletedAttachment> output);

    /**
     * Load the metadata of all deleted attachments for a list of documents.
     * These DeletedAttachments will NOT be attached to any document, attaching them is the
     * responsibility of the caller.
     *
     * @param reference a reference to a document to get deleted attachments for.
     * @param output an empty list to be populated with the deleted attachments for this document.
     * @return a TransactionRunnable which will load the deleted attachments and populate the list.
     */
    TransactionRunnable<T> getDeletedAttachmentLoadRunnable(final DocumentReference reference,
                                                            final List<DeletedAttachment> output);

    /**
     * Get a runnable to purge the deleted attachment from the deleted attachment store.
     * Only the deleted attachment metadata such as who deleted it and when will be purged.
     *
     * @param reference a reference to the attachment which was deleted.
     * @param attach the deleted attachment to purge from storage.
     * @return a TransactionRunnable which will remove the content from the store.
     */
    TransactionRunnable<T> getDeletedAttachmentPurgeRunnable(final AttachmentReference reference,
                                                             final DeletedAttachment attach);
}
