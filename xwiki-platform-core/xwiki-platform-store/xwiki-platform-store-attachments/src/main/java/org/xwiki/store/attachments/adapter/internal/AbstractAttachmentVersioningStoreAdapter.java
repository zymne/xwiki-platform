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

import java.util.List;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.xwiki.store.attachments.newstore.internal.AttachmentArchiveStore;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.TransactionRunnable;

/**
 * An adapter between com.xpn.xwiki.store.AttachmentVersioningStore
 * and org.xwiki.store.legacy.store.attachments.internal.AttachmentArchiveStore.
 *
 * @version $Id$
 * @param <T> The transaction type.
 * @since TODO
 */
public abstract class AbstractAttachmentVersioningStoreAdapter<T>
    implements AttachmentVersioningStore
{
    /** The type of transaction so that comparability can be checked between adapters. */
    private final Class<T> transactionType;

    /**
     * The Constructor.
     *
     * @param transactionType the type of transaction so that comparability can be checked.
     */
    protected AbstractAttachmentVersioningStoreAdapter(final Class<T> transactionType)
    {
        this.transactionType = transactionType;
    }

    /**
     * @return a transaction of the type needed by the underlying AttachmentArchiveStore.
     */
    protected abstract StartableTransactionRunnable<T> getTransaction();

    /**
     * @return the underlying AttachmentArchiveStore which this store adapts to.
     */
    protected abstract AttachmentArchiveStore<T> getArchiveStore();

    @Override
    public XWikiAttachmentArchive loadArchive(final XWikiAttachment attachment,
                                              final XWikiContext context,
                                              final boolean bTransaction)
        throws XWikiException
    {
        try {
            final StartableTransactionRunnable<T> transaction = this.getTransaction();
            final TransactionRunnable<T> tr =
                this.getArchiveStore().getAttachmentArchiveLoadRunnable(attachment);
            tr.runIn(transaction);
            (System.out).println("\n\n\n\n\n\n\n\n\nTEST2\n\n\n\n\n\n\n\n\n");
            transaction.start();
        } catch (Exception e) {
            AttachmentTools.throwXWikiException("Exception while loading attachment archive",
                                                e,
                                                attachment);
        }

        return attachment.getAttachment_archive();
    }

    /**
     * {@inheritDoc}
     * bTransaction is ignored by this implementation.
     * If you need to delete an archive inside of a larger transaction,
     * please use the AttachmentArchiveStore instead.
     */
    @Override
    public void saveArchive(final XWikiAttachmentArchive archive,
                            final XWikiContext context,
                            final boolean bTransaction)
        throws XWikiException
    {
        try {
            if (archive.getVersions().length == 0 && archive.getAttachment() != null) {
                archive.updateArchive(null, context);
            }

            final List<XWikiAttachment> versions =
                AttachmentTools.getVersionsForArchive(archive, context);
            final StartableTransactionRunnable<T> transaction = this.getTransaction();
            final TransactionRunnable<T> tr =
                this.getArchiveStore().getAttachmentArchiveSaveRunnable(versions);
            tr.runIn(transaction);
            transaction.start();
        } catch (Exception e) {
            AttachmentTools.throwXWikiException("Exception while saving attachment archive",
                                                e,
                                                archive);
        }
    }

    /**
     * {@inheritDoc}
     * bTransaction is ignored by this implementation.
     * If you need to delete an archive inside of a larger transaction,
     * please use the AttachmentArchiveStore instead.
     */
    @Override
    public void deleteArchive(final XWikiAttachment attachment,
                              final XWikiContext context,
                              final boolean bTransaction)
        throws XWikiException
    {
        if (attachment == null) {
            throw new NullPointerException("The attachment to delete cannot be null");
        }
        try {
            final StartableTransactionRunnable<T> transaction = this.getTransaction();
            final TransactionRunnable<T> tr =
                this.getArchiveStore().getAttachmentArchiveDeleteRunnable(attachment);
            tr.runIn(transaction);
            transaction.start();
        } catch (Exception e) {
            AttachmentTools.throwXWikiException("Exception while deleting attachment archive",
                                                e,
                                                attachment);
        }
    }

    /**
     * @return the type of transaction used to test compatibility.
     */
    final Class<?> getTransactionType()
    {
        return this.transactionType;
    }
}
