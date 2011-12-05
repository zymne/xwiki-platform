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

import com.xpn.xwiki.doc.XWikiAttachment;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.store.TransactionRunnable;

/**
 * A means of storing deleted attachments so that they can be revived.
 * This stores the content, metadata, and archive of the deleted attachment but not the
 * deleted attachment metadata such as who deleted it and when.
 *
 * @version $Id$
 * @param <T> The type of storage engine which must be available for this attachment store.
 * @since 3.3M2
 */
@ComponentRole
public interface DeletedAttachmentContentStore<T>
{
    /**
     * Save the archive of an attachment.
     * Only the content, archive, and metadata will be stored,
     * not the deleted attachment metadata such as who deleted it and when.
     * The attachment content and attachment archive must be loaded first.
     * and the attachment must be attached to a document.
     *
     * @param attachmentVersions each version of the attachment from the
     *                           first version to the current in no specific order.
     *                           These attachments must all be associated with a document.
     * @param dateOfDeletion the date when the attachment was deleted, needed since the same
     *                       attachment may be deleted and restored multiple times.
     * @return a TransactionRunnable which will save the content.
     */
    TransactionRunnable<T> getDeletedAttachmentContentSaveRunnable(
        final List<XWikiAttachment> attachmentVersions,
        final Date dateOfDeletion);

    /**
     * Load the content of an attachment.
     * This will load the content and metadata of the attachment itself but not the deleted
     * attachment metadata such as who deleted it and when.
     *
     * @param reference the reference to the attachment which was deleted.
     * @param dateOfDeletion the time when the attachment was deleted, this is used in case the
     *                       same attachment was deleted, undeleted and deleted again.
     * @param outputVersions a list which will be populated with all versions of the attachment
     *                       which are found in the store.
     * @return a TransactionRunnable which will load the content.
     */
    TransactionRunnable<T> getDeletedAttachmentContentLoadRunnable(
        final AttachmentReference reference,
        final Date dateOfDeletion,
        final List<XWikiAttachment> outputVersions);

    /**
     * Get a runnable to purge the deleted attachment from the deleted attachment store.
     * Only the content, archive and metadata of the attachment itself will be purged,
     * the deleted attachment metadata will not.
     *
     * @param reference the reference to the attachment which was deleted.
     * @param dateOfDeletion the time when the attachment was deleted, this is used in case the
     *                       same attachment was deleted, undeleted and deleted again.
     * @return a TransactionRunnable which will remove the content from the store.
     */
    TransactionRunnable<T> getDeletedAttachmentContentPurgeRunnable(
        final AttachmentReference reference,
        final Date dateOfDeletion);
}
