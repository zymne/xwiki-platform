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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.xpn.xwiki.doc.XWikiAttachment;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.store.attachments.util.internal.AttachmentContentStreamProvider;
import org.xwiki.store.attachments.util.internal.DeletedAttachmentFileProvider;
import org.xwiki.store.attachments.util.internal.FilesystemStoreTools;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.store.serialization.Serializer;
import org.xwiki.store.serialization.SerializationStreamProvider;
import org.xwiki.store.TransactionRunnable;

/**
 * Realization of {@link AttachmentRecycleBinStore} for filesystem storage.
 *
 * @version $Id$
 * @since TODO
 */
@Component
@Named("file")
@Singleton
public class FilesystemDeletedAttachmentContentStore implements DeletedAttachmentContentStore
{
    /**
     * Some utilities for getting attachment files, locks, and backup files.
     */
    @Inject
    private FilesystemStoreTools fileTools;

    /**
     * A serializer for the archive metadata.
     */
    @Inject
    @Named("attachment-list-meta/1.0")
    private Serializer<List<XWikiAttachment>, List<XWikiAttachment>> serializer;

    @Override
    public TransactionRunnable getDeletedAttachmentContentSaveRunnable(
        final List toSave,
        final Date dateOfDeletion)
    {
        final List<XWikiAttachment> attachmentVersions = (List<XWikiAttachment>) toSave;

        final TransactionRunnable out = new TransactionRunnable();
        if (attachmentVersions.size() == 0) {
            return out;
        }
        final AttachmentReference ref =
            new AttachmentReference(attachmentVersions.get(0).getFilename(),
                                    attachmentVersions.get(0).getDoc().getDocumentReference());

        final DeletedAttachmentFileProvider provider =
            this.fileTools.getDeletedAttachmentFileProvider(ref, dateOfDeletion);

        for (final XWikiAttachment attachVer : attachmentVersions) {
            this.fileTools.getSaver(
                new AttachmentContentStreamProvider(attachVer.getAttachment_content()),
                provider.getAttachmentVersionContentFile(attachVer.getVersion())
            ).runIn(out);
        }

        // We don't save the deleted attachment metadata
        // but the metadata for each attachment counts as content.
        this.fileTools.getSaver(
            new SerializationStreamProvider<List<XWikiAttachment>>(serializer, attachmentVersions),
            provider.getAttachmentVersioningMetaFile()
        ).runIn(out);

        return out;
    }

    @Override
    public TransactionRunnable getDeletedAttachmentContentLoadRunnable(
        final AttachmentReference reference,
        final Date dateOfDeletion,
        final List outputList)
    {
        final DeletedAttachmentFileProvider provider =
            this.fileTools.getDeletedAttachmentFileProvider(reference, dateOfDeletion);
        final Serializer<List<XWikiAttachment>, List<XWikiAttachment>> metaSerializer =
            this.serializer;

        return new TransactionRunnable() {
            @Override
            protected void onRun() throws IOException
            {
                final InputStream is =
                    new FileInputStream(provider.getAttachmentVersioningMetaFile());
                final List<XWikiAttachment> attachList = metaSerializer.parse(is);
                IOUtils.closeQuietly(is);

                for (XWikiAttachment attach : attachList) {
                    final File contentFile =
                        provider.getAttachmentVersionContentFile(attach.getVersion());
                    attach.setAttachment_content(
                        new FilesystemAttachmentContent(contentFile, attach));
                    outputList.add(attach);
                }
            }
        };
    }

    @Override
    public TransactionRunnable getDeletedAttachmentContentPurgeRunnable(
        final AttachmentReference reference,
        final Date dateOfDeletion)
    {
        final DeletedAttachmentFileProvider provider =
            this.fileTools.getDeletedAttachmentFileProvider(reference, dateOfDeletion);
        final File storeDir = provider.getAttachmentVersioningMetaFile().getParentFile();
        final TransactionRunnable out = new TransactionRunnable();
        for (final File f : storeDir.listFiles()) {
            this.fileTools.getDeleter(f).runIn(out);
        }
        this.fileTools.getDeleter(storeDir).runIn(out);
        return out;
    }
}
