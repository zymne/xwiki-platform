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
package org.xwiki.store.attachments.adapter.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.EntityType;
import org.xwiki.store.attachments.newstore.internal.DeletedAttachmentContentStore;
import org.xwiki.store.attachments.newstore.internal.DeletedAttachmentStore;
import org.xwiki.store.StartableTransactionRunnable;

/**
 * Realization of {@link AttachmentRecycleBinStore} for filesystem storage.
 *
 * @version $Id$
 * @param <T> the type of transaction which the underlying attachment metadata
 *            and attachment content stores use.
 * @since TODO
 */
public abstract class AbstractAttachmentRecycleBinStoreAdapter<T>
    implements AttachmentRecycleBinStore
{
    /** The core of the attachment engine, disguised as a logger. */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(AbstractAttachmentRecycleBinStoreAdapter.class);

    /**
     * @return the transaction which deleted attachments will be stored or loaded in.
     */
    protected abstract StartableTransactionRunnable<T> getTransaction();

    /**
     * @return the deleted attachment metadata store which this adapter adapts to.
     */
    protected abstract DeletedAttachmentStore<? super T> getMetaStore();

    /**
     * @return the deleted attachment content store which this adaptor adapts to.
     */
    protected abstract DeletedAttachmentContentStore<? super T> getContentStore();

    /**
     * Get an EntityResolver.
     * This resolver is used for resolving the name of the deleter.
     *
     * @return a String based EntityResolver.
     */
    protected abstract EntityReferenceResolver<String> getDeleterNameResolver();

    /**
     * Map a long to an attachment reference.
     * The legacy API allows the UI layer to access the database id of deleted attachments,
     * in order to maintain backward compatability, this method must map the id over to
     * an AttachmentReference for the attachment which was deleted.
     * What is most important is that for any DeletedAttachment which is loaded, calling
     * this method with the number given by getId() will return a reference to the attachment.
     *
     * @param id the opaque deleted attachment id number.
     * @return a reference to the attachment which was deleted or null if it cannot be found.
     */
    protected abstract AttachmentReference getAttachmentReferenceForId(final long id);

    @Override
    public void saveToRecycleBin(final XWikiAttachment attachment,
                                 final String deleter,
                                 final Date deleteDate,
                                 final XWikiContext context,
                                 final boolean bTransaction) throws XWikiException
    {
        final EntityReference deleterRef =
            this.getDeleterNameResolver().resolve(deleter, EntityType.DOCUMENT);
        final StartableTransactionRunnable<T> transaction = this.getTransaction();
        this.getMetaStore()
            .getDeletedAttachmentSaveRunnable(attachment, deleterRef, deleteDate)
                .runIn(transaction);
        final List<XWikiAttachment> versions =
            AttachmentTools.getVersionsForArchive(attachment.loadArchive(context), context);
        this.getContentStore()
            .getDeletedAttachmentContentSaveRunnable(versions, deleteDate)
                .runIn(transaction);

        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.MODULE_XWIKI,
                                     "Failed to store deleted attachment "
                                     + attachment.getFilename() + " for document: "
                                     + attachment.getDoc(), e);
        }
    }

    @Override
    public XWikiAttachment restoreFromRecycleBin(final XWikiAttachment attachment,
                                                 final long index,
                                                 final XWikiContext context,
                                                 boolean bTransaction) throws XWikiException
    {
        final DeletedAttachment delAttach = this.getDeletedAttachment(index, context, bTransaction);
        return delAttach != null ? delAttach.restoreAttachment(attachment, context) : null;
    }

    @Override
    public DeletedAttachment getDeletedAttachment(final long index,
                                                  final XWikiContext context,
                                                  final boolean bTransaction) throws XWikiException
    {
        final AttachmentReference attachRef = this.getAttachmentReferenceForId(index);
        final DeletedAttachment out = this.getDeletedAttachment(attachRef, index);
        if (out != null) {
            this.loadAttachmentContent(attachRef.getDocumentReference(),
                                       new ArrayList<DeletedAttachment>(1) { { add(out); } });
        }

        return out;
    }

    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(final XWikiAttachment attachment,
                                                            final XWikiContext context,
                                                            final boolean bTransaction)
        throws XWikiException
    {
        if (attachment.getDoc() == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.MODULE_XWIKI,
                "Cannot load deleted attachments because the given attachment "
                    + attachment.getFilename() + " is not attached to any document.");
        }

        // I don't know that there is no way to upload an attachment named ""
        // so I don't want to use isEmpty here.
        if (attachment.getFilename() == null) {
            return this.getAllDeletedAttachments(attachment.getDoc(), context, false);
        }

        final AttachmentReference attachRef =
            new AttachmentReference(attachment.getFilename(),
                                    attachment.getDoc().getDocumentReference());

        final List<DeletedAttachment> out = new ArrayList<DeletedAttachment>();
        final StartableTransactionRunnable<T> transaction = this.getTransaction();
        this.getMetaStore().getDeletedAttachmentLoadRunnable(attachRef, out).runIn(transaction);
        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.MODULE_XWIKI,
                                     "Failed to restore all deleted versions of attachment "
                                     + attachRef,
                                     e);
        }

        if (out.size() > 0) {
            this.loadAttachmentContent(attachRef.getDocumentReference(), out);
        }

        return out;
    }

    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(final XWikiDocument doc,
                                                            final XWikiContext context,
                                                            final boolean bTransaction)
        throws XWikiException
    {
        final List<DeletedAttachment> out = new ArrayList<DeletedAttachment>();
        final StartableTransactionRunnable<T> transaction = this.getTransaction();
        this.getMetaStore()
            .getDeletedAttachmentLoadRunnable(doc.getDocumentReference(), out)
                .runIn(transaction);
        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.MODULE_XWIKI,
                                     "Failed to restore deleted attachment metadata "
                                     + "for all attachments for document: " + doc,
                                     e);
        }

        if (out.size() > 0) {
            this.loadAttachmentContent(doc.getDocumentReference(), out);
        }

        return out;
    }

    @Override
    public void deleteFromRecycleBin(final long index,
                                     final XWikiContext context,
                                     final boolean bTransaction)
        throws XWikiException
    {
        final AttachmentReference ar = this.getAttachmentReferenceForId(index);
        final DeletedAttachment toPurge = this.getDeletedAttachment(ar, index);
        if (toPurge == null) {
            LOGGER.debug("attachment [{0}] to purge at index [{1}] doesn't exist.", ar, index);
        } else {
            final StartableTransactionRunnable<T> transaction = this.getTransaction();
            this.getMetaStore()
                .getDeletedAttachmentPurgeRunnable(ar, toPurge)
                    .runIn(transaction);
            this.getContentStore()
                .getDeletedAttachmentContentPurgeRunnable(ar, toPurge.getDate())
                    .runIn(transaction);
            try {
                transaction.start();
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                         XWikiException.MODULE_XWIKI,
                                         "Failed to purge deleted attachment from storage "
                                         + "id number " + index,
                                         e);
            }
        }
    }

    /**
     * Load the content for one or more deleted attachments.
     *
     * @param docRef the reference to the document to which the attachment is attached.
     * @param toLoad a list of deleted attachments to load content for.
     * @throws XWikiException if there is an unexpected exception while running the transaction.
     */
    private void loadAttachmentContent(final DocumentReference docRef,
                                       final List<DeletedAttachment> toLoad)
        throws XWikiException
    {
        final StartableTransactionRunnable<T> transaction = this.getTransaction();
        for (final DeletedAttachment attach : toLoad) {
            final AttachmentReference ref = new AttachmentReference(attach.getFilename(), docRef);
            final List<XWikiAttachment> versions = new ArrayList<XWikiAttachment>();
            this.getContentStore()
                .getDeletedAttachmentContentLoadRunnable(ref, attach.getDate(), versions)
                    .runIn(transaction);
        }

        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.MODULE_XWIKI,
                                     "Failed to restore deleted attachment content "
                                     + "for all attachments in document: "
                                     + docRef, e);
        }
    }

    /**
     * Get metadata for a deleted attachment.
     *
     * @param attachRef the reference to the attachment which was deleted.
     * @param index the opaque id number for the attachment.
     * @return the deleted attachment WITHOUT CONTENT or null if it is not found.
     * @throws XWikiException if there is an unexpected exception while running the transaction.
     */
    private DeletedAttachment getDeletedAttachment(final AttachmentReference attachRef,
                                                   final long index) throws XWikiException
    {
        final List<DeletedAttachment> list = new ArrayList<DeletedAttachment>();
        final StartableTransactionRunnable<T> transaction = this.getTransaction();
        this.getMetaStore().getDeletedAttachmentLoadRunnable(attachRef, list).runIn(transaction);
        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.MODULE_XWIKI,
                                     "Failed to restore deleted versions of attachment "
                                     + attachRef,
                                     e);
        }
        DeletedAttachment out = null;
        for (final DeletedAttachment attach : list) {
            if (attach.getId() == index) {
                out = attach;
            }
        }
        return out;
    }
}
