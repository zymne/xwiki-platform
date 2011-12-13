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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

//import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import javax.inject.Provider;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.xwiki.model.internal.reference.PathStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.attachments.newstore.internal.AttachmentContentStore;
import org.xwiki.store.attachments.newstore.internal.AttachmentStore;
//import org.xwiki.store.attachments.newstore.internal.FilesystemAttachmentContentStore;
//import org.xwiki.store.attachments.adapter.internal.FilesystemHibernateAttachmentStoreAdapter;
//import org.xwiki.store.attachments.util.internal.DefaultFilesystemStoreTools;
//import org.xwiki.store.attachments.util.internal.FilesystemStoreTools;
//import org.xwiki.store.locks.preemptive.internal.PreemptiveLockProvider;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.test.AbstractMockingComponentTestCase;
//import org.xwiki.store.attachments.adapter.internal.FilesystemHibernateAttachmentStoreAdapter;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.store.attachments.newstore.internal.AttachmentArchiveStore;
import org.suigeneris.jrcs.rcs.Version;

/**
 * Tests for FilesystemAttachmentStore.
 *
 * @version $Id$
 * @since 3.0M2
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
            //allowing(mockAttach).getContentInputStream(mockContext); will(returnValue(HELLO_STREAM));
            //allowing(mockDirtyContent).getContentInputStream(); will(returnValue(HELLO_STREAM));
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

        final AttachmentVersioningStore versioningStore =
            new TestingAttachmentVersioningStoreAdapter(this.mockArchiveStore, Object.class);

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockXWiki).getAttachmentVersioningStore(); will(returnValue(versioningStore));
            oneOf(mockContentStore).getAttachmentContentDeleteRunnable(mockAttach);
                will(returnValue(attachDeleteTr));

//            oneOf(mockAttachVersionStore).deleteArchive(mockAttach, mockContext, false);
        }});
        //this.createFile();

        this.attachStore.deleteXWikiAttachment(this.mockAttach, false, this.mockContext, false);
        Assert.assertTrue("Attachment metadata was not deleted.",
                          attachDeleteTr.numberOfTimesCalled == 1);
        //Assert.assertFalse("The attachment file was not deleted.", this.storeFile.exists());
    }
/*
    @Test
    public void documentUpdateOnDeleteTest() throws Exception
    {
        final List<XWikiAttachment> attachList = new ArrayList<XWikiAttachment>();
        attachList.add(this.mockAttach);
        this.doc.setAttachmentList(attachList);

        this.jmockContext.checking(new Expectations() {{
            oneOf(mockAttachVersionStore).deleteArchive(mockAttach, mockContext, false);
            oneOf(mockHibernate).saveXWikiDoc(doc, mockContext, false);
            will(new CustomAction("Make sure the attachment has been removed from the list.")
            {
                public Object invoke(final Invocation invoc)
                {
                    final XWikiDocument document = (XWikiDocument) invoc.getParameter(0);
                    Assert.assertTrue("Attachment was not removed from the list.",
                        document.getAttachmentList().size() == 0);
                    return null;
                }
            });
        }});
        //this.createFile();

        this.attachStore.deleteXWikiAttachment(this.mockAttach, true, this.mockContext, false);
        Assert.assertTrue("Attachment metadata was not deleted.",
                          this.testingAttachmentDeleteTr.numberOfTimesCalled == 1);
    }

    @Test
    public void documentUpdateOnSaveTest() throws Exception
    {
        this.jmockContext.checking(new Expectations() {{
            oneOf(mockHibernate).saveXWikiDoc(doc, mockContext, false);
        }});

        this.attachStore.saveAttachmentContent(this.mockAttach, true, this.mockContext, false);
    }
*/
    /* -------------------- Helpers -------------------- */



    /** Used for making assertions that a TR has been run. */
    private static class TestingTransactionRunnable<T> extends TransactionRunnable<T>
    {
        public int numberOfTimesCalled;

        public void onRun()
        {
            this.numberOfTimesCalled++;
        }
    }
}
