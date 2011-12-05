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
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.XWikiException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.FileDeleteTransactionRunnable;
import org.xwiki.store.FileSaveTransactionRunnable;
import org.xwiki.store.attachments.util.internal.AttachmentContentStreamProvider;
import org.xwiki.store.attachments.util.internal.AttachmentFileProvider;
import org.xwiki.store.attachments.util.internal.FilesystemStoreTools;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.store.legacy.doc.internal.ListAttachmentArchive;
import org.xwiki.store.serialization.SerializationStreamProvider;
import org.xwiki.store.serialization.Serializer;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.store.UnexpectedException;

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

        checkAttachedToDocument(versions.get(0));

        final AttachmentFileProvider provider =
            this.fileTools.getAttachmentFileProvider(versions.get(0));

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
    public TransactionRunnable getAttachmentArchiveLoadRunnable(final XWikiAttachment attachment)
    {
        checkAttachedToDocument(attachment);

        final AttachmentFileProvider provider =
            this.fileTools.getAttachmentFileProvider(attachment);
        final File metaFile = provider.getAttachmentVersioningMetaFile();

        // If no meta file then assume no archive and return an empty archive.
        if (!metaFile.exists()) {
            return new TransactionRunnable() {
                @Override
                protected void onRun()
                {
                    attachment.setAttachment_archive(new ListAttachmentArchive(attachment));
                }
            };
        }

        final Serializer<List<XWikiAttachment>, List<XWikiAttachment>> metaSerializer =
            this.metaSerializer;

        return new TransactionRunnable() {
            @Override
            protected void onRun() throws IOException
            {
                final InputStream is = new FileInputStream(metaFile);
                final List<XWikiAttachment> attachList = metaSerializer.parse(is);
                is.close();

                for (XWikiAttachment attach : attachList) {
                    final File contentFile =
                        provider.getAttachmentVersionContentFile(attach.getVersion());
                    attach.setAttachment_content(
                        new FilesystemAttachmentContent(contentFile, attach));
                    // Pass the document since it will be lost in the serialize/deserialize.
                    attach.setDoc(attachment.getDoc());
                }

                final ListAttachmentArchive out = new ListAttachmentArchive(attachList);
                out.setAttachment(attachment);
                attachment.setAttachment_archive(out);
            }
        };
    }

    @Override
    public TransactionRunnable getAttachmentArchiveDeleteRunnable(final XWikiAttachment attachment)
    {
        checkAttachedToDocument(attachment);

        final AttachmentFileProvider provider =
            this.fileTools.getAttachmentFileProvider(attachment);
        final TransactionRunnable out = new TransactionRunnable();

        this.fileTools.getDeleter(provider.getAttachmentVersioningMetaFile()).runIn(out);

        final List<Version> versions;
        try {
            versions = attachment.getVersionList();
        } catch (XWikiException e) {
            // Won't happen unless XWikiAttachment is modified.
            throw new UnexpectedException(e);
        }

        for (final Version ver : versions) {
            final File file = provider.getAttachmentVersionContentFile(ver.toString());
            this.fileTools.getDeleter(file).runIn(out);
        }

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
