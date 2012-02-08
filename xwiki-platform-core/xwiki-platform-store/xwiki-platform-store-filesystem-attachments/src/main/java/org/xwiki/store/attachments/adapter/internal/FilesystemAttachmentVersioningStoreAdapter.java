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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.attachments.newstore.internal.AttachmentArchiveStore;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.TransactionProvider;

/**
 * Filesystem based AttachmentVersioningStore implementation.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component
@Named("file")
@Singleton
public class FilesystemAttachmentVersioningStoreAdapter
    extends AbstractAttachmentVersioningStoreAdapter
{
    /** The filesystem based attachment archive store. */
    @Inject
    @Named("file")
    private AttachmentArchiveStore archiveStore;

    /** A means of getting a transaction to run the attachment save operation in. */
    @Inject
    @Named("hibernate")
    private TransactionProvider provider;

    /**
     * Testing Constructor.
     *
     * @param archiveStore the underlying attachment archive store.
     * @param provider the means of getting a transaction to synchronize with.
     */
    public FilesystemAttachmentVersioningStoreAdapter(
        final AttachmentArchiveStore archiveStore,
        final TransactionProvider provider)
    {
        super(Object.class);
        this.archiveStore = archiveStore;
        this.provider = provider;
    }

    /**
     * Component manager constructor.
     */
    public FilesystemAttachmentVersioningStoreAdapter()
    {
        super(Object.class);
    }

    @Override
    public AttachmentArchiveStore getArchiveStore()
    {
        return this.archiveStore;
    }

    @Override
    protected StartableTransactionRunnable getTransaction()
    {
        return this.provider.get();
    }
}
