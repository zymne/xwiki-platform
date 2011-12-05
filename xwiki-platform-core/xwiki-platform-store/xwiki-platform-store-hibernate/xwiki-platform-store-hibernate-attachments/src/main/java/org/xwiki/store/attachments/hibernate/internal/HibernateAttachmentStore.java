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
package org.xwiki.store.attachments.hibernate.internal;

import java.util.List;

import com.xpn.xwiki.doc.XWikiAttachment;
import javax.inject.Named;
import javax.inject.Singleton;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.store.attachments.newstore.internal.AttachmentStore;
import org.xwiki.store.TransactionRunnable;

/**
 * A means of storing the metadata of an attachment.
 * This interface is designed to seperate concerns of content, metadata, and archive.
 *
 * @version $Id$
 * @since 3.3M2
 */
@Component
@Named("hibernate")
@Singleton
public class HibernateAttachmentStore implements AttachmentStore<Session>
{
    @Override
    public TransactionRunnable<Session> getAttachmentSaveRunnable(final List<XWikiAttachment> toSave)
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public TransactionRunnable<Session> getAttachmentLoadRunnable(
        final List<AttachmentReference> refs,
        final List<XWikiAttachment> output)
    {
        throw new RuntimeException("Not implemented.");
    }

    @Override
    public TransactionRunnable<Session> getAttachmentDeleteRunnable(
        final List<XWikiAttachment> toDelete)
    {
        return (new TransactionRunnable<Session>() {
            @Override
            protected void onRun()
            {
                final Session session = this.getContext();
                for (final XWikiAttachment attach : toDelete) {
                    session.delete(attach);
                }
            }
        });
    }
}
