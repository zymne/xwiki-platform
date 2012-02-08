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
import java.util.List;

import com.xpn.xwiki.doc.XWikiAttachment;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.store.attachments.adapter.internal.AttachmentTools;
import org.xwiki.store.attachments.util.internal.AttachmentContentStreamProvider;
import org.xwiki.store.attachments.util.internal.AttachmentFileProvider;
import org.xwiki.store.attachments.util.internal.FilesystemStoreTools;
import org.xwiki.store.attachments.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.store.serialization.SerializationStreamProvider;
import org.xwiki.store.serialization.Serializer;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.TransactionRunnable;

/**
 * A filesystem based attachment archive store.
 *
 * @version $Id$
 * @since 3.3M2
 */
@Component
@Named("file")
@Singleton
public class FilesystemAttachmentArchiveStore implements AttachmentArchiveStore
{
    /** Tools for getting files to store given content in. */
    @Inject
    private FilesystemStoreTools fileTools;

    /** A serializer for the list of attachment metdata. */
    @Inject
    @Named("attachment-list-meta/1.0")
    private Serializer<List<XWikiAttachment>, List<XWikiAttachment>> metaSerializer;

    /**
     * Testing Constructor.
     *
     * @param fileTools the means of getting files for the attachments.
     * @param metaSerializer serializer for attachment metadata.
     */
    public FilesystemAttachmentArchiveStore(final FilesystemStoreTools fileTools,
                                            final Serializer<List<XWikiAttachment>,
                                                             List<XWikiAttachment>> metaSerializer)
    {
        this.fileTools = fileTools;
        this.metaSerializer = metaSerializer;
    }

    /**
     * Component manager constructor.
     */
    public FilesystemAttachmentArchiveStore()
    {
        // The fields will be filled in by reflection.
    }

    @Override
    public TransactionRunnable getAttachmentArchiveSaveRunnable(final List versionList)
    {
        // For some reason this is required even though the interface specifies a
        // List<XWikiAttachment>, this appears to be a bug in javac.
        final List<XWikiAttachment> versions = (List<XWikiAttachment>) versionList;

        final TransactionRunnable out = new TransactionRunnable();

        if (versions.size() == 0) {
            return out;
        }

        final AttachmentReference ref = AttachmentTools.referenceForAttachment(versions.get(0));
        final AttachmentFileProvider provider = this.fileTools.getAttachmentFileProvider(ref);

        for (final XWikiAttachment attachVer : versions) {
            final String verName = attachVer.getVersion();
            // If the content is not dirty and the file was already saved then we will not update.
            if (attachVer.isContentDirty()
                || !provider.getAttachmentVersionContentFile(verName).exists())
            {
                this.fileTools.getSaver(
                    new AttachmentContentStreamProvider(attachVer.getAttachment_content()),
                    provider.getAttachmentVersionContentFile(verName)).runIn(out);
            }
        }

        // Then do the metadata.
        final StreamProvider sp =
            new SerializationStreamProvider<List<XWikiAttachment>>(this.metaSerializer, versions);
        this.fileTools.getSaver(sp, provider.getAttachmentVersioningMetaFile()).runIn(out);

        return out;
    }

    @Override
    public TransactionRunnable getAttachmentArchiveLoadRunnable(final AttachmentReference ref,
                                                                final List output)
    {
        final AttachmentFileProvider provider =
            this.fileTools.getAttachmentFileProvider(ref);
        final File metaFile = provider.getAttachmentVersioningMetaFile();

        // If no meta file then assume no archive and do nothing.
        if (!metaFile.exists()) {
            return new TransactionRunnable();
        }

        final Serializer<List<XWikiAttachment>,
                         List<XWikiAttachment>> mSerializer = this.metaSerializer;

        return new TransactionRunnable() {
            @Override
            protected void onRun() throws IOException
            {
                final InputStream is = new FileInputStream(metaFile);
                final List<XWikiAttachment> attachList = mSerializer.parse(is);
                is.close();
                for (XWikiAttachment attach : attachList) {
                    attach.setAttachment_content(
                        new FilesystemAttachmentContent(
                            provider.getAttachmentVersionContentFile(attach.getVersion()), attach));
                    output.add(attach);
                }
                //final ListAttachmentArchive out = new ListAttachmentArchive(attachList);
                //out.setAttachment(attachment);
                //attachment.setAttachment_archive(out);
            }
        };
    }

    @Override
    public TransactionRunnable getAttachmentArchiveDeleteRunnable(final AttachmentReference ref)
    {
        final AttachmentFileProvider provider = this.fileTools.getAttachmentFileProvider(ref);
        final TransactionRunnable out = new TransactionRunnable();
        final File archiveMeta = provider.getAttachmentVersioningMetaFile();

        final Serializer<List<XWikiAttachment>,
                         List<XWikiAttachment>> mSerializer = this.metaSerializer;

        (new TransactionRunnable() {
            @Override
            protected void onRun() throws IOException
            {
                if (archiveMeta.exists()) {
                    InputStream is = null;
                    try {
                        is = new FileInputStream(archiveMeta);
                        for (final XWikiAttachment ver : mSerializer.parse(is)) {
                            provider.getAttachmentVersionContentFile(ver.getVersion()).delete();
                        }
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                }
            }
        }).runIn(out);

        this.fileTools.getDeleter(provider.getAttachmentVersioningMetaFile()).runIn(out);

        return out;
    }

    /**
     * Make sure the attachment is associated with a document.
     *
     * @param attachment the attachment to check.
     * @throws IllegalArgumentException if attachment.getDoc() yields null.
     */
    private static void checkAttachedToDocument(final XWikiAttachment attachment)
    {
        if (attachment.getDoc() == null) {
            throw new IllegalArgumentException("In order to use this function, the attachment ["
                                               + attachment.getFilename()
                                               + "] must be associated with a document.");
        }
    }
}
