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
import javax.inject.Provider;
import javax.inject.Singleton;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.attachments.newstore.internal.AttachmentContentStore;
import org.xwiki.store.attachments.newstore.internal.AttachmentStore;
import org.xwiki.store.StartableTransactionRunnable;

/**
 * AttachmentVersioningStore implementation.
 * Stores content in the filesystem and metadata in a Hibernate store.
 *
 * @version $Id$
 * @since 3.3M2
 */
@Component
@Named("file")
@Singleton
public class FilesystemHibernateAttachmentStoreAdapter
    extends AbstractAttachmentStoreAdapter<Session>
{
    /** The filesystem based attachment content store. */
    @Named("file")
    @Inject
    private AttachmentContentStore contentStore;

    /** The hibernate based attachment metadata store. */
    @Named("hibernate")
    @Inject
    private AttachmentStore<Session> metaStore;

    /** A means of getting a transaction to run the attachment save operation in. */
    @Named("hibernate")
    @Inject
    private Provider<StartableTransactionRunnable<Session>> provider;

    /**
     * Testing Constructor.
     *
     * @param contentStore the filesystem based store for the content.
     * @param metaStore the hibernate based store for the metadata.
     * @param provider the means of getting a transaction to run in.
     */
    public FilesystemHibernateAttachmentStoreAdapter(
        final AttachmentContentStore contentStore,
        final AttachmentStore<Session> metaStore,
        final Provider<StartableTransactionRunnable<Session>> provider)
    {
        super(Session.class);
        this.contentStore = contentStore;
        this.metaStore = metaStore;
        this.provider = provider;
    }

    /**
     * Component manager constructor.
     */
    public FilesystemHibernateAttachmentStoreAdapter()
    {
        super(Session.class);
    }

    @Override
    protected AttachmentContentStore getContentStore()
    {
        return this.contentStore;
    }

    @Override
    protected AttachmentStore<Session> getMetaStore()
    {
        return this.metaStore;
    }

    @Override
    protected StartableTransactionRunnable<Session> getTransaction()
    {
        return this.provider.get();
    }
}
