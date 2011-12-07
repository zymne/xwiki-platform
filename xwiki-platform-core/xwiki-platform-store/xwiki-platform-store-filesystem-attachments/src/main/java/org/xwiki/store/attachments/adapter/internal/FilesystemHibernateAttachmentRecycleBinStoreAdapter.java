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

import com.xpn.xwiki.XWikiContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.inject.Provider;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.store.attachments.newstore.internal.DeletedAttachmentContentStore;
import org.xwiki.store.attachments.newstore.internal.DeletedAttachmentStore;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.TransactionException;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.store.UnexpectedException;

/**
 * Realization of {@link AttachmentRecycleBinStore} for filesystem storage.
 *
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Named("file")
@Singleton
public class FilesystemHibernateAttachmentRecycleBinStoreAdapter
    extends AbstractAttachmentRecycleBinStoreAdapter<Session>
{
    /** The means of getting Hibernate transactions. */
    @Named("hibernate")
    @Inject
    private Provider<StartableTransactionRunnable<Session>> transactionProvider;

    /** The metadata store which puts empty DeletedAttachment entries in the database. */
    @Named("hibernate")
    @Inject
    private DeletedAttachmentStore<Session> metaStore;

    /** The content store, this is filesystem based. */
    @Named("file")
    @Inject
    private DeletedAttachmentContentStore contentStore;

    /** Generic String reference resolver used for the name of the deleter. */
    @Inject
    private EntityReferenceResolver<String> resolver;

    /** A means of getting the XWikiContext to get the current wiki. */
    @Inject
    private Execution execution;

    @Override
    protected StartableTransactionRunnable<Session> getTransaction()
    {
        return this.transactionProvider.get();
    }

    @Override
    protected DeletedAttachmentStore<Session> getMetaStore()
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
    protected AttachmentReference getAttachmentReferenceForId(final long id)
    {
        final StartableTransactionRunnable<Session> str = this.transactionProvider.get();
        final String[] docNameAndFileName = new String[2];
        (new TransactionRunnable<Session>() {
            @Override
            protected void onRun()
            {
                final Object[] result = (Object[]) this.getContext().createQuery(
                    "SELECT attach.docName, attach.filename FROM DeletedAttachment as attach "
                    + "WHERE attach.id = :id").setLong("id", id).uniqueResult();

                if (result != null) {
                    docNameAndFileName[0] = (String) result[0];
                    docNameAndFileName[1] = (String) result[1];
                }
            }
        }).runIn(str);

        try {
            str.start();
        } catch (TransactionException e) {
            throw new UnexpectedException("Failed to get the name of the deleted attachment "
                                          + "with id " + id, e);
        }

        if (docNameAndFileName[0] != null) {
            DocumentReference ref =
                new DocumentReference(
                    this.resolver.resolve(docNameAndFileName[0], EntityType.DOCUMENT));
            ref = ref.replaceParent(ref.getWikiReference(), this.getWikiRef());
            return new AttachmentReference(docNameAndFileName[1], ref);
        }
        return null;
    }

    /**
     * Get the current wiki reference.
     * This is required in order to get an attachment reference for an attachment ID.
     *
     * @return the current wiki reference.
     */
    private WikiReference getWikiRef()
    {
        final XWikiContext xc =
            (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        return new WikiReference(xc.getDatabase());
    }
}
