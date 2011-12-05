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
import java.util.List;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.store.attachments.newstore.internal.AttachmentArchiveStore;
import org.xwiki.store.attachments.newstore.internal.AttachmentContentStore;
import org.xwiki.store.attachments.newstore.internal.AttachmentStore;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.TransactionException;
import org.xwiki.store.TransactionRunnable;

/**
 * Adapter between XWikiAttachmentStoreInterface and newstore implementations.
 *
 * @version $Id$
 * @param <T> The transaction type, this must extend the content store transaction
 *            and metadata store transaction types to guarantee both can be stored
 *            in the same transaction.
 * @since 3.3M2
 */
public abstract class AbstractAttachmentStoreAdapter<T>
    implements XWikiAttachmentStoreInterface
{
    /** Garbage spewer. */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(AbstractAttachmentStoreAdapter.class);

    /** The type of transaction so that compatibility can be checked. */
    private final Class<T> transactionType;

    /**
     * If the archive store is compatible with this transaction type, it will be set here.
     * This way the content and archive will be able to be stored in the same transaction.
     */
    private AttachmentArchiveStore<? super T> archiveStore;

    /** If false then archiveStore is assumed to be incompatible and not used. */
    private boolean useArchiveStore = true;

    /**
     * The Constructor.
     *
     * @param transactionType the type of transaction so that comparability can be checked.
     */
    protected AbstractAttachmentStoreAdapter(final Class<T> transactionType)
    {
        this.transactionType = transactionType;
    }

    /**
     * @return a transaction of the type needed by
     *         the underlying AttachmentContentStore and AttachmentMetaStore
     */
    protected abstract StartableTransactionRunnable<T> getTransaction();

    /**
     * @return the underlying AttachmentContentStore which this store adapts to.
     */
    protected abstract AttachmentContentStore<? super T> getContentStore();

    /**
     * @return the underlying AttachmentStore which this store adapts to.
     */
    protected abstract AttachmentStore<? super T> getMetaStore();

    /**
     * {@inheritDoc}
     * This implementation cannot operate in a larger transaction
     * so it starts a new transaction no matter whether bTransaction is true or false.
     */
    @Override
    public void saveAttachmentContent(final XWikiAttachment attachment,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        this.saveAttachmentContent(attachment, true, context, bTransaction);
    }

    /**
     * {@inheritDoc}
     * This implementation cannot operate in a larger transaction
     * so it starts a new transaction no matter whether bTransaction is true or false.
     */
    @Override
    public void saveAttachmentContent(final XWikiAttachment attachment,
                                      final boolean updateDocument,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        if (attachment == null) {
            LOGGER.debug("Attempted to save null attachment");
            return;
        }
        final XWikiDocument doc = attachment.getDoc();
        if (doc == null) {
            throw new IllegalArgumentException("Cannot save attachment which is not associated "
                                                + "with a document.");
        }

        this.saveAttachmentsContent(new ArrayList<XWikiAttachment>(1) { { add(attachment); } },
                                    doc,
                                    updateDocument,
                                    context,
                                    bTransaction);
    }

    /**
     * {@inheritDoc}
     * This implementation cannot operate in a larger transaction so it starts a
     * new transaction no matter whether bTransaction is true or false.
     */
    @Override
    public void saveAttachmentsContent(final List<XWikiAttachment> attachments,
                                       final XWikiDocument doc,
                                       final boolean updateDocument,
                                       final XWikiContext context,
                                       final boolean bTransaction) throws XWikiException
    {
        if (attachments == null || attachments.size() == 0) {
            return;
        }

        try {
            final StartableTransactionRunnable<T> transaction = this.getTransaction();

            for (final XWikiAttachment attachment : attachments) {

                final XWikiAttachmentContent content = attachment.getAttachment_content();
                if (content == null) {
                    // If content does not exist we should not blank the stored attachment.
                    continue;
                }

                // Save the attachment content
                this.getContentStore()
                    .getAttachmentContentSaveRunnable(content)
                        .runIn(transaction);

                // Save the archive because this interface demands it
                final XWikiAttachmentArchive archive = attachment.getAttachment_archive();
                if (this.useArchiveStore(context)) {
                    // If the attachment versioning store is also an adapter which supports the same
                    // type of transaction, we can run both in the same tx.
                    if (archive == null) {
                        // If first save then create a new archive.
                        this.archiveStore.getAttachmentArchiveSaveRunnable(
                            new ArrayList<XWikiAttachment>(1) { { add(attachment); } }
                        ).runIn(transaction);
                    } else {
                        final List<XWikiAttachment> versions =
                            AttachmentTools.getVersionsForArchive(archive, context);
                        this.archiveStore
                            .getAttachmentArchiveSaveRunnable(versions)
                                .runIn(transaction);
                    }
                } else {
                    // This is bad because there is no guarantee of transaction safety.
                    final AttachmentVersioningStore avs =
                        context.getWiki().getAttachmentVersioningStore();
                    (new TransactionRunnable() {
                        @Override
                        protected void onRun() throws XWikiException
                        {
                            avs.saveArchive(archive, context, false);
                        }
                    }).runIn(transaction);
                }
            }

            // Save the parent document only once.
            if (updateDocument) {
                (new TransactionRunnable() {
                    @Override
                    protected void onRun() throws XWikiException
                    {
                        context.getWiki().getStore().saveXWikiDoc(doc, context, false);
                    }
                }).runIn(transaction);
            }

            transaction.start();
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                                     "Exception while saving attachments " + attachments,
                                     e);
        }
    }

    @Override
    public void loadAttachmentContent(final XWikiAttachment attachment,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        final StartableTransactionRunnable<T> transaction = this.getTransaction();
        this.getContentStore().getAttachmentContentLoadRunnable(attachment).runIn(transaction);
        try {
            transaction.start();
        } catch (TransactionException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.ERROR_XWIKI_STORE_FILENOTFOUND,
                                     "Unable to load attachment content",
                                     e);
        }
    }

    @Override
    public void deleteXWikiAttachment(final XWikiAttachment attachment,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        this.deleteXWikiAttachment(attachment, true, context, bTransaction);
    }

    @Override
    public void deleteXWikiAttachment(final XWikiAttachment attachment,
                                      final boolean updateDocument,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        final StartableTransactionRunnable<T> transaction = this.getTransaction();

        this.getContentStore().getAttachmentContentDeleteRunnable(attachment).runIn(transaction);

        // If the store supports deleting in the same transaction then do it.
        if (this.useArchiveStore(context)) {
            this.archiveStore.getAttachmentArchiveDeleteRunnable(attachment).runIn(transaction);
        } else {
            final AttachmentVersioningStore avs = context.getWiki().getAttachmentVersioningStore();
            (new TransactionRunnable() {
                @Override
                protected void onRun() throws XWikiException
                {
                    avs.deleteArchive(attachment, context, false);
                }
            }).runIn(transaction);
        }

        // Update the document if required.
        if (updateDocument) {
            (new TransactionRunnable() {
                @Override
                protected void onRun() throws XWikiException
                {
                    final String filename = attachment.getFilename();
                    final List<XWikiAttachment> list = attachment.getDoc().getAttachmentList();
                    for (int i = 0; i < list.size(); i++) {
                        if (filename.equals(list.get(i).getFilename())) {
                            list.remove(i);
                            break;
                        }
                    }
                    context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, false);
                }
            }).runIn(transaction);
        }

        this.getMetaStore()
            .getAttachmentDeleteRunnable(new ArrayList<XWikiAttachment>(1) { { add(attachment); } })
                .runIn(transaction);
    }

    @Override
    public void cleanUp(XWikiContext context)
    {
        // Do nothing.
    }

    /**
     * Get the archive store to use.
     *
     * Determine whether the attachment archive store supports the same type of transaction
     * as this store.
     *
     * @param context the XWikiContext from which to get the attachment versioning store.
     * @return true if we should use the archive store, otherwise false.
     */
    private boolean useArchiveStore(final XWikiContext context)
    {
        if (this.archiveStore != null) {
            return true;
        } else if (!this.useArchiveStore) {
            return false;
        }

        final AttachmentVersioningStore avs = context.getWiki().getAttachmentVersioningStore();
        if (avs instanceof AbstractAttachmentVersioningStoreAdapter) {
            final Class<?> versioningTxType =
                ((AbstractAttachmentVersioningStoreAdapter) avs).getTransactionType();

            if (versioningTxType.isAssignableFrom(this.transactionType)) {
                this.archiveStore = (AttachmentArchiveStore<? super T>) archiveStore;
                this.useArchiveStore = true;
                return true;
            }
        }

        LOGGER.warn("The attachment versioning store [{0}] does not support transactions of the "
                    + "type as the main attachment store [{1}], so it will be impossible to save "
                    + "an attachment and attachment archive in the same transaction.\n"
                    + "Falling back on 2 transaction saves.",
                    avs.getClass().getName(),
                    this.getClass().getName());

        this.useArchiveStore = false;
        return false;
    }

    /**
     * @return the type of transaction used to test compatibility.
     */
    final Class<?> getTransactionType()
    {
        return this.transactionType;
    }
}
