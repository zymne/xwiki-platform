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

import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.store.attachments.newstore.internal.DeletedAttachmentContentStore;
import org.xwiki.store.attachments.newstore.internal.DeletedAttachmentStore;
import org.xwiki.store.StartableTransactionRunnable;

/**
 * Attachment store adapter for testing AbstractAttachmentRecycleBinStoreAdapter.
 *
 * @version $Id$
 * @since TODO
 */
public class TestingAttachmentRecycleBinStoreAdapter extends AbstractAttachmentRecycleBinStoreAdapter
{
    private final DeletedAttachmentContentStore contentStore;

    private final DeletedAttachmentStore metaStore;

    private final EntityReferenceResolver<String> resolver;

    public TestingAttachmentRecycleBinStoreAdapter(final DeletedAttachmentContentStore contentStore,
                                                   final DeletedAttachmentStore metaStore,
                                                   final EntityReferenceResolver<String> resolver)
    {
        this.contentStore = contentStore;
        this.metaStore = metaStore;
        this.resolver = resolver;
    }

    @Override
    protected DeletedAttachmentStore getMetaStore()
    {
        return this.metaStore;
    }

    @Override
    protected DeletedAttachmentContentStore getContentStore()
    {
        return this.contentStore;
    }

    @Override
    protected EntityReferenceResolver<String> getDeleterNameResolver()
    {
        return this.resolver;
    }

    @Override
    protected AttachmentReference getAttachmentReferenceForId(final long id)
    {
        return new AttachmentReference("file.txt", new DocumentReference("xwiki", "Main", "WebHome"));
    }

    @Override
    protected StartableTransactionRunnable getTransaction()
    {
        return new StartableTransactionRunnable();
    }
}
