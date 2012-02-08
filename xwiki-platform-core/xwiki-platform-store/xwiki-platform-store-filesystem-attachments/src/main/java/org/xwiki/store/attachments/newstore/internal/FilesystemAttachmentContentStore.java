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

import java.io.File;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.store.attachments.adapter.internal.AttachmentTools;
import org.xwiki.store.attachments.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.store.attachments.util.internal.FilesystemStoreTools;
import org.xwiki.store.attachments.util.internal.AttachmentContentStreamProvider;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.UnexpectedException;
import org.xwiki.store.TransactionRunnable;

/**
 * Mechanism for storing attachment content on the filesystem.
 *
 * @version $Id$
 * @since 3.3M2
 */
@Component
@Named("file")
@Singleton
public class FilesystemAttachmentContentStore implements AttachmentContentStore
{
    /** Tools for getting files to store given content in. */
    @Inject
    private FilesystemStoreTools fileTools;

    /**
     * Testing Constructor.
     *
     * @param fileTools means of getting files and locks for storing attachments.
     */
    public FilesystemAttachmentContentStore(final FilesystemStoreTools fileTools)
    {
        this.fileTools = fileTools;
    }

    /** ComponentManager Constructor. */
    public FilesystemAttachmentContentStore()
    {
        // Filetools injected by component manager.
    }

    @Override
    public TransactionRunnable getAttachmentContentSaveRunnable(final XWikiAttachmentContent content)
    {
        final AttachmentReference ref =
            AttachmentTools.referenceForAttachment(content.getAttachment());

        // This is the permanent location where the attachment content will go.
        final File attachFile =
            this.fileTools.getAttachmentFileProvider(ref).getAttachmentContentFile();

        final StreamProvider provider =
            new AttachmentContentStreamProvider(content);
        return this.fileTools.getSaver(provider, attachFile);
    }

    @Override
    public TransactionRunnable getAttachmentContentLoadRunnable(final XWikiAttachment attachment)
    {
        final AttachmentReference ref = AttachmentTools.referenceForAttachment(attachment);
        final File attachFile =
            this.fileTools.getAttachmentFileProvider(ref).getAttachmentContentFile();
        return new TransactionRunnable() {
            @Override
            public void onRun()
            {
                if (!attachFile.exists()) {
                    throw new UnexpectedException("Failed to get attachment content for "
                                                  + "attachment [" + attachment.getFilename()
                                                  + "] attached to document ["
                                                  + attachment.getDoc().getFullName() + "]");
                }
                attachment.setAttachment_content(
                    new FilesystemAttachmentContent(attachFile, attachment));
            }
        };
    }

    @Override
    public TransactionRunnable getAttachmentContentDeleteRunnable(final XWikiAttachment attachment)
    {
        final AttachmentReference ref = AttachmentTools.referenceForAttachment(attachment);
        final File attachFile =
            this.fileTools.getAttachmentFileProvider(ref).getAttachmentContentFile();
        return this.fileTools.getDeleter(attachFile);
    }
}
