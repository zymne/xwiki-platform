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
import java.util.Date;
import java.util.List;

import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.XWikiContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jmock.States;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.store.attachments.legacy.doc.internal.ExternalContentDeletedAttachment;
import org.xwiki.store.attachments.legacy.doc.internal.ListAttachmentArchive;
import org.xwiki.store.attachments.newstore.internal.DeletedAttachmentContentStore;
import org.xwiki.store.attachments.newstore.internal.DeletedAttachmentStore;
import org.xwiki.store.TransactionRunnable;
import org.suigeneris.jrcs.rcs.Version;

/**
 * Tests for AbstractAttachmentRecycleBinStoreAdapter.
 *
 * @version $Id$
 * @since TODO
 */
@RunWith(JMock.class)
public class AttachmentRecycleBinStoreAdapterTest
{
    private final Mockery jmockContext = new JUnit4Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    // Used by legacy code.
    private final XWikiContext mockContext =
        this.jmockContext.mock(XWikiContext.class);

    // The attachment objects.
    private final XWikiAttachment mockAttach =
        this.jmockContext.mock(XWikiAttachment.class);
    private final XWikiAttachmentArchive mockArchive =
        this.jmockContext.mock(XWikiAttachmentArchive.class);
    private final ExternalContentDeletedAttachment mockDeletedAttach =
        this.jmockContext.mock(ExternalContentDeletedAttachment.class);


    // needed by the trash store.
    private final EntityReferenceResolver<String> mockResolver =
        this.jmockContext.mock(EntityReferenceResolver.class);

    // What we are testing.
    private TestingAttachmentRecycleBinStoreAdapter trashStore;

    // Mock newstore implementations.
    private final DeletedAttachmentStore mockTrashMetaStore =
        this.jmockContext.mock(DeletedAttachmentStore.class);
    private final DeletedAttachmentContentStore mockTrashContentStore =
        this.jmockContext.mock(DeletedAttachmentContentStore.class);

    // The attachment.
    private final AttachmentReference attachRef =
        new AttachmentReference("file.txt", new DocumentReference("xwiki", "Main", "WebHome"));

    // The user who deleted it.
    private final DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "User");

    // When it was deleted.
    private final Date now = new Date();

    // TR for loading the deleted attachment metadata.
    private final ListPopulatingTransactionRunnable attachLoadTr =
        new ListPopulatingTransactionRunnable(new ArrayList(1) {{ add(mockDeletedAttach); }});

    @Before
    public void setUp() throws Exception
    {
        this.jmockContext.checking(new Expectations() {{
            allowing(mockArchive).getAttachment(); will(returnValue(mockAttach));
            allowing(mockArchive).getVersions();
                will(returnValue(new Version[] { new Version("1.1") }));
            allowing(mockArchive).getRevision(mockAttach, "1.1", mockContext);
                will(returnValue(mockAttach));

            allowing(mockDeletedAttach).getFilename();
                will(returnValue("file.txt"));
            allowing(mockDeletedAttach).getDate();
                will(returnValue(now));
            allowing(mockDeletedAttach).setAttachment(with(mockAttach),
                                                      with(any(XWikiContext.class)));

            allowing(mockResolver).resolve("XWiki.User", EntityType.DOCUMENT);
                will(returnValue(userRef));

            allowing(mockAttach).getId();
                will(returnValue(1L));
            allowing(mockAttach).getFilename();
                will(returnValue("file.txt"));
            allowing(mockAttach).setAttachment_archive(with(any(ListAttachmentArchive.class)));

            allowing(mockTrashMetaStore)
                .getDeletedAttachmentLoadRunnable(with(attachRef), with(any(List.class)));
                will(new CustomAction("Return a TR which will populate the list with the "
                                      + "mock deleted attachment.")
                {
                    public Object invoke(final Invocation invoc)
                    {
                        final List l = (List) invoc.getParameter(1);
                        Assert.assertTrue("List is not empty.", l.size() == 0);
                        attachLoadTr.list = l;
                        return attachLoadTr;
                    }
                });
            allowing(mockDeletedAttach).getId(); will(returnValue(1L));
        }});

        this.trashStore = new TestingAttachmentRecycleBinStoreAdapter(this.mockTrashContentStore,
                                                                      this.mockTrashMetaStore,
                                                                      this.mockResolver);
    }

    @Test
    public void saveContentTest() throws Exception
    {
        final TestingTransactionRunnable attachmentSaveTr = new TestingTransactionRunnable();
        final TestingTransactionRunnable contentSaveTr = new TestingTransactionRunnable();
        //final TestingTransactionRunnable archiveSaveTr = new TestingTransactionRunnable();

        final States content = this.jmockContext.states("content").startsAs("unloaded");
        final States archive = this.jmockContext.states("archive").startsAs("unloaded");

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockTrashMetaStore).getDeletedAttachmentSaveRunnable(mockAttach, userRef, now);
                will(returnValue(attachmentSaveTr));

            // These must be loaded when prior to calling getDeletedAttachmentContentSaveRunnable.
            oneOf(mockAttach).loadContent(mockContext);
                then(content.is("loaded"));
            oneOf(mockAttach).loadArchive(mockContext);
                will(returnValue(mockArchive));
                then(archive.is("loaded"));

            oneOf(mockTrashContentStore)
                .getDeletedAttachmentContentSaveRunnable(with(any(List.class)), with(now));
                    will(new CustomAction("Make sure the list contains attachment versions and the "
                                          + "versions content and archives are all loaded.")
                    {
                        public Object invoke(final Invocation invoc)
                        {
                            Assert.assertTrue("Content was not loaded",
                                              content.is("loaded").isActive());
                            Assert.assertTrue("Archive was not loaded",
                                              archive.is("loaded").isActive());
                            final List l = (List) invoc.getParameter(0);
                            Assert.assertTrue("List was not one item long.", l.size() == 1);
                            Assert.assertEquals("Wrong element in list.", mockAttach, l.get(0));
                            return contentSaveTr;
                        }
                    });
        }});

        this.trashStore
            .saveToRecycleBin(this.mockAttach, "XWiki.User", this.now, this.mockContext, false);

        Assert.assertTrue("The deleted attachment content save TR wasn't called.",
                          contentSaveTr.numberOfTimesCalled == 1);

        Assert.assertTrue("The deleted attachment metadata save TR wasn't called.",
                          attachmentSaveTr.numberOfTimesCalled == 1);
    }

    @Test
    public void getDeletedAttachmentTest() throws Exception
    {
        final ListPopulatingTransactionRunnable attachContentLoadTr =
            new ListPopulatingTransactionRunnable(new ArrayList(1) {{ add(mockAttach); }});

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockTrashContentStore)
                .getDeletedAttachmentContentLoadRunnable(with(equal(attachRef)),
                                                         with(now),
                                                         with(any(List.class)));
                will(new CustomAction("Return a TR which will populate the output list "
                                      + "with the versions of the deleted attachment.")
                {
                    public Object invoke(final Invocation invoc)
                    {
                        final List l = (List) invoc.getParameter(2);
                        Assert.assertTrue("List is not empty.", l.size() == 0);
                        attachContentLoadTr.list = l;
                        return attachContentLoadTr;
                    }
                });
        }});

        final DeletedAttachment delAttach =
            this.trashStore.getDeletedAttachment(1, this.mockContext, false);

        Assert.assertNotNull("The attachment wasn't loaded.", delAttach);

        Assert.assertTrue("The attachment load TR wasn't called.",
                          this.attachLoadTr.numberOfTimesCalled == 1);
    }

    @Test
    public void deleteFromRecycleBinTest() throws Exception
    {
        final TestingTransactionRunnable attachMetaPurgeTr = new TestingTransactionRunnable();
        final TestingTransactionRunnable attachContentPurgeTr = new TestingTransactionRunnable();

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockTrashMetaStore).getDeletedAttachmentPurgeRunnable(
                with(equal(attachRef)),
                with(mockDeletedAttach.getDate()));
                    will(returnValue(attachMetaPurgeTr));
            oneOf(mockTrashContentStore)
               .getDeletedAttachmentContentPurgeRunnable(with(equal(attachRef)), with(now));
                will(returnValue(attachContentPurgeTr));
        }});

        this.trashStore.deleteFromRecycleBin(1, this.mockContext, false);

        Assert.assertTrue("The attachment content purge TR wasn't called.",
                          attachContentPurgeTr.numberOfTimesCalled == 1);

        Assert.assertTrue("The attachment meta purge TR wasn't called.",
                          attachContentPurgeTr.numberOfTimesCalled == 1);
    }

    /* -------------------- Helpers -------------------- */

    private static class ListPopulatingTransactionRunnable extends TransactionRunnable
    {
        private List source;
        public List list;
        public int numberOfTimesCalled;

        public ListPopulatingTransactionRunnable(final List source)
        {
            this.source = source;
        }

        @Override
        protected void onRun()
        {
            if (this.list != null) {
                this.list.addAll(this.source);
            }
            this.numberOfTimesCalled++;
        }
    }
}
