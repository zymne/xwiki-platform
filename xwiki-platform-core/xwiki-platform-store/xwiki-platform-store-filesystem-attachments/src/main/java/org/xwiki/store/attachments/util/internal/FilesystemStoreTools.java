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
package org.xwiki.store.attachments.util.internal;

import java.io.File;
import java.util.Date;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.TransactionRunnable;

/**
 * Tools for getting files to store data in the filesystem.
 * These APIs are in flux and may change at any time without warning.
 * This should be replaced by a module which provides a secure extension of java.io.File.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Role
public interface FilesystemStoreTools
{
    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment
     * in a separate location which is repeatable only with the same attachment reference.
     *
     * @param ref the reference to the attachment to get a provider for.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     */
    AttachmentFileProvider getAttachmentFileProvider(final AttachmentReference ref);

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment
     * in a separate location which is repeatable only with the same attachment name, containing document,
     * and date of deletion.
     *
     * @param reference the reference to the attachment to get a provider for.
     * @param deleteDate the date the attachment was deleted.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     */
    DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(
        final AttachmentReference reference,
        final Date deleteDate);

    /**
     * Get a map of dates of deletion by the document where the attachment was attached.
     *
     * @param docRef a reference to the document to get deleted attachments for.
     * @return a map of maps which provide FileProviders by deletion dates and filenames.
     */
    Map<String, Map<Date, DeletedAttachmentFileProvider>>
    deletedAttachmentsForDocument(final DocumentReference docRef);

    /**
     * @return the absolute path to the directory where the files are stored.
     */
    String getStorageLocationPath();

    /**
     * Get a file which is global for the entire installation.
     *
     * @param name a unique identifier for the file.
     * @return a file unique to the given name.
     */
    File getGlobalFile(final String name);

    /**
     * Get a deleted attachment file provider from a path to the deleted attachment directory.
     *
     * @param pathToDirectory a relitive path to the directory where the deleted attachment is.
     * @return a DeletedAttachmentFileProvider which will provide files for that deleted attachment.
     */
    DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(final String pathToDirectory);

    /**
     * Get a TR to save a file.
     *
     * @param provider the means to get the content to save.
     * @param saveHere the location to save the data.
     * @return a TransactionRunnable to save the file.
     */
    TransactionRunnable getSaver(final StreamProvider provider, final File saveHere);

    /**
     * Get a TR to delete a file.
     *
     * @param toDelete the file to delete.
     * @return a TransactionRunnable to delete the file.
     */
    TransactionRunnable getDeleter(final File toDelete);
}
