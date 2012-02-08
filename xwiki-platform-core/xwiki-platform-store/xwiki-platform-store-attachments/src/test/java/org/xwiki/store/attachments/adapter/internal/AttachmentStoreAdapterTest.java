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
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.attachments.adapter.internal.AttachmentTools;
import org.xwiki.store.attachments.newstore.internal.AttachmentContentStore;
import org.xwiki.store.attachments.newstore.internal.AttachmentStore;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.store.attachments.newstore.internal.AttachmentArchiveStore;
import org.suigeneris.jrcs.rcs.Version;

/**
 * Tests for AbstractAttachmentStoreAdapter.
 *
 * @version $Id$
 * @since TODO
 */
@RunWith(JMock.class)
public class AttachmentStoreAdapterTest
{
    private final Mockery jmockContext = new JUnit4Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    // Used by legacy code.
    private final XWikiContext mockContext =
        this.jmockContext.mock(XWikiContext.class);
    private XWiki mockXWiki;
    private XWikiDocument mockDocument;

    // The attachment objects.
    private final XWikiAttachment mockAttach =
        this.jmockContext.mock(XWikiAttachment.class);
    private final XWikiAttachmentContent mockDirtyContent =
        this.jmockContext.mock(XWikiAttachmentContent.class);
    private final XWikiAttachmentArchive mockArchive =
        this.jmockContext.mock(XWikiAttachmentArchive.class);

    // The document which holds the attachment.
    private final DocumentReference documentRef = new DocumentReference("xwiki", "Main", "WebHome");


    // What we are testing.
    private TestingAttachmentStoreAdapter attachStore;


    // Mock newstore implementations.
    private final AttachmentStore mockMetaStore =
        this.jmockContext.mock(AttachmentStore.class);
    private final AttachmentContentStore mockContentStore =
        this.jmockContext.mock(AttachmentContentStore.class);
    private final AttachmentArchiveStore mockArchiveStore =
        this.jmockContext.mock(AttachmentArchiveStore.class);

    @Before
    public void setUp() throws Exception
    {
        // Instead of bringing the whole wiki engine up for each test, we'll just mock it.
        final ComponentManager cm = this.jmockContext.mock(ComponentManager.class);
        Utils.setComponentManager(cm);

        this.jmockContext.checking(new Expectations() {{
            allowing(cm).lookup(with(any(Class.class)));
                will(returnValue(null));
            allowing(cm).lookup(with(any(Class.class)), with(any(String.class)));
                will(returnValue(null));
        }});

        this.mockDocument = this.jmockContext.mock(XWikiDocument.class);
        this.mockXWiki = this.jmockContext.mock(XWiki.class);


        this.jmockContext.checking(new Expectations() {{
            allowing(mockContext).getWiki(); will(returnValue(mockXWiki));

            // attachment, content, archive
            allowing(mockAttach).getDoc(); will(returnValue(mockDocument));
            allowing(mockAttach).getFilename(); will(returnValue("file.name"));
            allowing(mockAttach).updateContentArchive(mockContext);
            allowing(mockAttach).loadContent(mockContext);
            allowing(mockAttach).getAttachment_archive(); will(returnValue(mockArchive));
            allowing(mockAttach).getAttachment_content(); will(returnValue(mockDirtyContent));
            allowing(mockAttach).isContentDirty(); will(returnValue(true));
            allowing(mockDirtyContent).isContentDirty(); will(returnValue(true));
            allowing(mockDirtyContent).getAttachment(); will(returnValue(mockAttach));
            allowing(mockArchive).getAttachment(); will(returnValue(mockAttach));
            allowing(mockArchive).getVersions();
                will(returnValue(new Version[] { new Version("1.1") }));
            allowing(mockArchive).getRevision(mockAttach, "1.1", mockContext);
                will(returnValue(mockAttach));
            allowing(mockDocument).getDocumentReference();
                will(returnValue(documentRef));
        }});

        this.attachStore = new TestingAttachmentStoreAdapter(this.mockContentStore,
                                                             this.mockMetaStore,
                                                             Object.class);
    }

    @Test
    public void saveContentTest() throws Exception
    {
        final TestingTransactionRunnable attachmentSaveTr = new TestingTransactionRunnable();
        final TestingTransactionRunnable archiveSaveTr = new TestingTransactionRunnable();

        final AttachmentVersioningStore versioningStore =
            new TestingAttachmentVersioningStoreAdapter(this.mockArchiveStore, Object.class);

        this.jmockContext.checking(new Expectations() {{
            // Save the content
            oneOf(mockContentStore).getAttachmentContentSaveRunnable(mockDirtyContent);
                will(returnValue(attachmentSaveTr));

            // Save the archive in the same transaction.
            oneOf(mockArchiveStore).getAttachmentArchiveSaveRunnable(with(any(List.class)));
                will(returnValue(archiveSaveTr));

            // saveAttachmentContent() causes saveArchive() to be called.
            oneOf(mockXWiki).getAttachmentVersioningStore(); will(returnValue(versioningStore));
        }});

        this.attachStore.saveAttachmentContent(this.mockAttach, false, this.mockContext, false);

        Assert.assertTrue("The attachment content save TR wasn't called.",
                          attachmentSaveTr.numberOfTimesCalled == 1);

        Assert.assertTrue("The attachment archive save TR wasn't called.",
                          archiveSaveTr.numberOfTimesCalled == 1);
    }

    @Test
    public void saveContentWithIncompatableArchiveStoreTest() throws Exception
    {
        final TestingTransactionRunnable attachmentSaveTr = new TestingTransactionRunnable();

        final AttachmentVersioningStore incompatableVersioningStore =
            this.jmockContext.mock(AttachmentVersioningStore.class);

        this.jmockContext.checking(new Expectations() {{
            // Save the content
            oneOf(mockContentStore).getAttachmentContentSaveRunnable(mockDirtyContent);
                will(returnValue(attachmentSaveTr));

            // Test with a versioning store which doesn't support the same type of transactions.
            allowing(mockXWiki).getAttachmentVersioningStore();
                will(returnValue(incompatableVersioningStore));

            // Save the archive
            oneOf(incompatableVersioningStore).saveArchive(mockArchive, mockContext, false);
        }});

        this.attachStore.saveAttachmentContent(this.mockAttach, false, this.mockContext, false);

        Assert.assertTrue("The attachment content save TR wasn't called.",
                          attachmentSaveTr.numberOfTimesCalled == 1);
    }

    @Test
    public void loadContentTest() throws Exception
    {
        final TestingTransactionRunnable attachLoadTr = new TestingTransactionRunnable();

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockContentStore).getAttachmentContentLoadRunnable(mockAttach);
                will(returnValue(attachLoadTr));
        }});

        this.attachStore.loadAttachmentContent(this.mockAttach, this.mockContext, false);

        Assert.assertTrue("The attachment content load TR wasn't called.",
                          attachLoadTr.numberOfTimesCalled == 1);
    }

    @Test
    public void deleteAttachmentTest() throws Exception
    {
        final TestingTransactionRunnable attachDeleteTr = new TestingTransactionRunnable();
        final TestingTransactionRunnable contentDeleteTr = new TestingTransactionRunnable();
        final TestingTransactionRunnable archiveDeleteTr = new TestingTransactionRunnable();

        final AttachmentVersioningStore versioningStore =
            new TestingAttachmentVersioningStoreAdapter(this.mockArchiveStore, Object.class);

        final AttachmentReference ref = AttachmentTools.referenceForAttachment(mockAttach);

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockXWiki).getAttachmentVersioningStore(); will(returnValue(versioningStore));
            oneOf(mockContentStore).getAttachmentContentDeleteRunnable(mockAttach);
                will(returnValue(contentDeleteTr));
            oneOf(mockArchiveStore).getAttachmentArchiveDeleteRunnable(ref);
                will(returnValue(archiveDeleteTr));
            oneOf(mockMetaStore).getAttachmentDeleteRunnable(with(any(List.class)));
                will(new CustomAction("Make sure the list contains the actual attachment.")
                {
                    public Object invoke(final Invocation invoc)
                    {
                        final List l = (List) invoc.getParameter(0);
                        Assert.assertTrue("List was not one item long.", l.size() == 1);
                        Assert.assertEquals("Wrong element in list.", mockAttach, l.get(0));
                        return attachDeleteTr;
                    }
                });
        }});

        this.attachStore.deleteXWikiAttachment(this.mockAttach, false, this.mockContext, false);
        Assert.assertTrue("Attachment metadata was not deleted.",
                          attachDeleteTr.numberOfTimesCalled == 1);
        Assert.assertTrue("Attachment content was not deleted.",
                          contentDeleteTr.numberOfTimesCalled == 1);
        Assert.assertTrue("Attachment archive was not deleted.",
                          archiveDeleteTr.numberOfTimesCalled == 1);
    }

    @Test
    public void documentUpdateOnDeleteTest() throws Exception
    {
        final TestingTransactionRunnable attachDeleteTr = new TestingTransactionRunnable();
        final TestingTransactionRunnable contentDeleteTr = new TestingTransactionRunnable();
        final TestingTransactionRunnable archiveDeleteTr = new TestingTransactionRunnable();

        final AttachmentVersioningStore versioningStore =
            new TestingAttachmentVersioningStoreAdapter(this.mockArchiveStore, Object.class);

        final AttachmentReference ref = AttachmentTools.referenceForAttachment(mockAttach);

        final XWikiStoreInterface mockStore = this.jmockContext.mock(XWikiStoreInterface.class);

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockXWiki).getAttachmentVersioningStore();
                will(returnValue(versioningStore));
            oneOf(mockXWiki).getStore();
                will(returnValue(mockStore));
            oneOf(mockContentStore).getAttachmentContentDeleteRunnable(mockAttach);
                will(returnValue(contentDeleteTr));
            oneOf(mockArchiveStore).getAttachmentArchiveDeleteRunnable(ref);
                will(returnValue(archiveDeleteTr));
            oneOf(mockMetaStore).getAttachmentDeleteRunnable(with(any(List.class)));
                returnValue(attachDeleteTr);
            oneOf(mockDocument).getAttachmentList();
                will(returnValue(new ArrayList<XWikiAttachment>(1){{ add(mockAttach); }}));
            oneOf(mockStore).saveXWikiDoc(mockDocument, mockContext, false);
        }});

        this.attachStore.deleteXWikiAttachment(this.mockAttach, true, this.mockContext, false);
    }

    @Test
    public void documentUpdateOnSaveTest() throws Exception
    {
        final TestingTransactionRunnable attachmentSaveTr = new TestingTransactionRunnable();
        final TestingTransactionRunnable archiveSaveTr = new TestingTransactionRunnable();

        final AttachmentVersioningStore versioningStore =
            new TestingAttachmentVersioningStoreAdapter(this.mockArchiveStore, Object.class);

        final XWikiStoreInterface mockStore = this.jmockContext.mock(XWikiStoreInterface.class);

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockXWiki).getStore();
                will(returnValue(mockStore));
            oneOf(mockXWiki).getAttachmentVersioningStore();
                will(returnValue(versioningStore));
            oneOf(mockContentStore).getAttachmentContentSaveRunnable(mockDirtyContent);
                will(returnValue(attachmentSaveTr));
            oneOf(mockArchiveStore).getAttachmentArchiveSaveRunnable(with(any(List.class)));
                will(returnValue(archiveSaveTr));
            oneOf(mockStore).saveXWikiDoc(mockDocument, mockContext, false);
        }});

        this.attachStore.saveAttachmentContent(this.mockAttach, true, this.mockContext, false);
    }
}
