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

import java.util.ArrayList;
import java.util.List;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.model.reference.AttachmentReference;

/**
 * Utilities for manipulating XWikiAttachments.
 *
 * @version $Id$
 * @since 3.3M2
 */
public final class AttachmentTools
{
    /** Private constructor for utility class. */
    private AttachmentTools()
    {
        // never used.
    }

    /**
     * Get a list of XWikiAttachments from an attachmentArchive.
     * Versions will have content pre-loaded, getAttachment_content() will not return null.
     *
     * @param archive the archive to get from.
     * @param context the XWikiContext to use when loading revisions from the archive.
     * @return a list of all versions of the attachment contained in the archive.
     * @throws XWikiException if thrown when getting a revision of the attachment.
     */
    public static List<XWikiAttachment> getVersionsForArchive(final XWikiAttachmentArchive archive,
                                                              final XWikiContext context)
        throws XWikiException
    {
        // If the archive happens to be a VoidAttachmentArchive the single attachment version
        // will be the attachment itself, this function should return versions which have content
        // already loaded.
        archive.getAttachment().loadContent(context);

        final Version[] versions = archive.getVersions();
        final List<XWikiAttachment> out = new ArrayList<XWikiAttachment>(versions.length);

        // Add the content files which need updating and add the attachments to the list.
        for (int i = 0; i < versions.length; i++) {
            out.add(archive.getRevision(archive.getAttachment(), versions[i].toString(), context));
        }

        return out;
    }

    /**
     * Throw an XWikiException based on information which can be gleaned from an archive.
     *
     * @param message the message to show with the exception.
     * @param exception the cause of the exception to be thrown.
     * @param archive the attachment archive to get information from.
     * @throws XWikiException always
     */
    public static void throwXWikiException(final String message,
                                           final Exception exception,
                                           final XWikiAttachmentArchive archive)
        throws XWikiException
    {
        final Object[] args = {message, "Unknown Filename", "Unknown Document"};
        if (archive.getAttachment() != null) {
            args[1] = archive.getAttachment().getFilename();
            if (archive.getAttachment().getDoc() != null) {
                args[2] = archive.getAttachment().getDoc().getFullName();
            }
        }
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                 XWikiException.ERROR_XWIKI_UNKNOWN,
                                 message + "{0} [{1}] of document [{2}]",
                                 exception,
                                 args);
    }

    /**
     * Throw an XWikiException based on information which can be gleaned from an attachment.
     *
     * @param message the message to show with the exception.
     * @param exception the cause of the exception to be thrown.
     * @param attachment the attachment to collect information from.
     * @throws XWikiException always
     */
    public static void throwXWikiException(final String message,
                                           final Exception exception,
                                           final XWikiAttachment attachment)
        throws XWikiException
    {
        final Object[] args = {message, attachment.getFilename(), ("" + attachment.getDoc())};
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                 XWikiException.ERROR_XWIKI_UNKNOWN,
                                 message + "{0} [{1}] from document [{2}]",
                                 exception,
                                 args);
    }

    /**
     * Get an attachment reference for an attachment.
     * This functionality will be in the next incarnation of XWikiAttachment.
     *
     * @param attachment the attachment to get the reference to.
     * @return a reference to the attachment.
     */
    public static AttachmentReference referenceForAttachment(final XWikiAttachment attachment)
    {
        if (attachment.getDoc() == null) {
            throw new NullPointerException("This attachment must be associated with a document.");
        }
        return new AttachmentReference(attachment.getFilename(),
                                       attachment.getDoc().getDocumentReference());
    }
}
