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
package org.xwiki.extension.index;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.index.internal.DefaultExtensionConverter;
import org.xwiki.extension.index.internal.DefaultExtensionIndex;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * @version $Id$
 */
@ComponentList({DefaultExtensionConverter.class})
public class ExtensionIndexTest
{
    static final File TEST_DIRECTORY = new File("target", "index");

    static final File TEST_EXTENSION_DIRECTORY = new File("target", "extension");

    static final URI TEST_REPOSITORY_LOCATION1 = new File("src/test/resources/repo").toURI();

    static final URI TEST_REPOSITORY_LOCATION2 = new File("src/test/resources/repo2").toURI();

    static final String ALL_EXTENSIONS_QUERY = "eid:*";

    @Rule
    public final MockitoComponentMockingRule<ExtensionIndex> mocker = new MockitoComponentMockingRule<ExtensionIndex>(
        DefaultExtensionIndex.class);

    ExtensionRepositoryDescriptor mockRepositoryDescriptor;

    ExtensionRepository mockRepository;

    Extension mockExtension;

    ExtensionId extensionId;

    ExtensionManagerConfiguration mockExtensionManagerConfiguration;

    ExtensionRepositoryManager mockExtensionRepositoryManager;

    ExtensionRepositoryDescriptor mockRepositoryDescriptor2;

    ExtensionRepository mockRepository2;

    ExtensionIndex index;

    @BeforeComponent
    public void registerComponents() throws Exception
    {
        mockExtensionRepositoryManager = mocker.registerMockComponent(ExtensionRepositoryManager.class);
    }

    @Before
    public void configure() throws Exception
    {
        mockRepositoryDescriptor = mock(ExtensionRepositoryDescriptor.class);
        when(mockRepositoryDescriptor.getId()).thenReturn("testRepository");
        when(mockRepositoryDescriptor.getURI()).thenReturn(TEST_REPOSITORY_LOCATION1);

        mockRepository = mock(ExtensionRepository.class);
        when(mockRepository.getDescriptor()).thenReturn(mockRepositoryDescriptor);

        // Not a component, general mocking will be done.
        mockExtension = mock(Extension.class);
        when(mockExtension.getRepository()).thenReturn(mockRepository);

        extensionId = new ExtensionId("testGroup:testArtifact", "1.0");
        when(mockExtension.getId()).thenReturn(extensionId);
        when(mockExtension.getType()).thenReturn("xar");

        mockRepositoryDescriptor2 = mock(ExtensionRepositoryDescriptor.class);
        when(mockRepositoryDescriptor2.getId()).thenReturn("testRepository2");
        when(mockRepositoryDescriptor2.getURI()).thenReturn(TEST_REPOSITORY_LOCATION2);

        mockRepository2 = mock(ExtensionRepository.class);
        when(mockRepository2.getDescriptor()).thenReturn(mockRepositoryDescriptor2);

        mockExtensionRepositoryManager = mocker.getInstance(ExtensionRepositoryManager.class);
        when(mockExtensionRepositoryManager.getRepositories()).thenReturn(
            Arrays.asList(mockRepository, mockRepository2));
        when(mockExtensionRepositoryManager.getRepository(mockRepositoryDescriptor.getId())).thenReturn(mockRepository);
        when(mockExtensionRepositoryManager.getRepository(mockRepositoryDescriptor2.getId())).thenReturn(
            mockRepository2);

        mockExtensionManagerConfiguration = mocker.getInstance(ExtensionManagerConfiguration.class);
        when(mockExtensionManagerConfiguration.getLocalRepository()).thenReturn(TEST_EXTENSION_DIRECTORY);

        index = mocker.getComponentUnderTest();

        // Clean the test directory at each run.
        FileUtils.deleteDirectory(TEST_DIRECTORY);
    }

    @AfterClass
    public static void tearDown() throws Exception
    {
        // Clean the test directory after all tests are executed.
        FileUtils.deleteDirectory(TEST_DIRECTORY);
    }

    @Test
    public void testIndexExtenions() throws Exception
    {
        String query =
            "eid:" + QueryParser.escape(mockExtension.getId().getId()) + " AND v:"
                + mockExtension.getId().getVersion().getValue();

        IterableResult<Extension> extensions = index.search(query, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        index.index(mockExtension);

        extensions = index.search(query, 0, 99);
        Assert.assertEquals(1, extensions.getSize());

        Extension result = extensions.iterator().next();
        Assert.assertEquals(mockExtension.getId().getId(), result.getId().getId());
        Assert.assertEquals(mockExtension.getId().getVersion().getValue(), result.getId().getVersion().getValue());
        Assert.assertEquals(mockExtension.getRepository(), result.getRepository());

        /*
         * An extension that contains a classifier in its ID.
         */
        String idWithClassifier = "testGroup:testArtifact:javadoc";
        extensionId = new ExtensionId(idWithClassifier, extensionId.getVersion());
        when(mockExtension.getId()).thenReturn(extensionId);
        query =
            "eid:" + QueryParser.escape(idWithClassifier) + " AND v:" + mockExtension.getId().getVersion().getValue();

        extensions = index.search(query, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        index.index(mockExtension);

        extensions = index.search(query, 0, 99);
        Assert.assertEquals(1, extensions.getSize());

        Extension extensionWithClassifier = extensions.iterator().next();
        Assert.assertEquals(idWithClassifier, extensionWithClassifier.getId().getId());

        // Check that there we end up with 2 extensions in the index.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(2, extensions.getSize());
    }

    @Test
    public void testIndexRepository() throws Exception
    {
        IterableResult<Extension> extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        index.index(mockRepositoryDescriptor);

        // Check that all the extensions got indexed.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(12, extensions.getSize());

        // Check details for a particular extension ID.
        String pickOneExtensionId = "junit:junit";

        // Search all versions of an extension ID.
        extensions = index.search("eid:" + QueryParser.escape(pickOneExtensionId), 0, 99);
        Assert.assertEquals(2, extensions.getSize());

        // Search a particular version of the extension.
        extensions = index.search("eid:" + QueryParser.escape(pickOneExtensionId) + " AND v:4.11", 0, 99);
        Assert.assertEquals(1, extensions.getSize());

        Extension extension = extensions.iterator().next();

        Assert.assertEquals(pickOneExtensionId, extension.getId().getId());
        Assert.assertEquals("4.11", extension.getId().getVersion().getValue());
        Assert.assertEquals(mockRepositoryDescriptor.getId(), extension.getRepository().getDescriptor().getId());
    }

    @Test
    public void testIndexAllRepositories() throws Exception
    {
        IterableResult<Extension> extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        index.index();

        // Both the search and the index methods need the list of repositories.
        verify(mockExtensionRepositoryManager, times(2)).getRepositories();

        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        // 12 (first repo) + 19 (second repo)
        Assert.assertEquals(31, extensions.getSize());

        // Check details for a particular extension ID that we know occurs in both repositories.
        String pickOneExtensionId = "junit:junit";

        // Search all versions of an extension ID.
        extensions = index.search("eid:" + QueryParser.escape(pickOneExtensionId), 0, 99);
        // 2 extensions x 2 repositories
        Assert.assertEquals(4, extensions.getSize());

        // Search a particular version of the extension.
        extensions = index.search("eid:" + QueryParser.escape(pickOneExtensionId) + " AND v:4.11", 0, 99);
        // 1 extensions x 2 repositories.
        Assert.assertEquals(2, extensions.getSize());

        Iterator<Extension> iterator = extensions.iterator();

        Extension extension = iterator.next();
        Extension extension2 = iterator.next();

        // Extensions have the same ID and result as equal...
        Assert.assertEquals(extension, extension2);
        Assert.assertTrue(extension.getId().equals(extension2.getId()));
        // ...but they have different repositories.
        Assert.assertFalse(extension.getRepository().equals(extension2.getRepository()));
    }

    @Test
    public void testClearExtension() throws Exception
    {
        IterableResult<Extension> extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        index.index(mockRepositoryDescriptor);

        // Check that all the extensions got indexed.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(12, extensions.getSize());

        // Remove a particular extension.
        String specificExtensionQuery = "eid:" + QueryParser.escape("junit:junit") + " AND v:4.11";

        // The extension is there.
        extensions = index.search(specificExtensionQuery, 0, 99);
        Assert.assertEquals(1, extensions.getSize());

        index.clear(extensions.iterator().next());

        // The extension is gone.
        extensions = index.search(specificExtensionQuery, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        // Only the cleared extension is gone, everything else is still there.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(11, extensions.getSize());
    }

    @Test
    public void testClearByQuery() throws Exception
    {
        IterableResult<Extension> extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        index.index(mockRepositoryDescriptor);

        // Check that all the extensions got indexed.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(12, extensions.getSize());

        // Remove all extensions for a particular eid.
        String specificExtensionQuery = "eid:" + QueryParser.escape("junit:junit") + " AND v:4.11";

        // The extensions are there.
        extensions = index.search(specificExtensionQuery, 0, 99);
        Assert.assertEquals(1, extensions.getSize());

        index.clear(mockRepositoryDescriptor, specificExtensionQuery);

        // The extensions are gone.
        extensions = index.search(specificExtensionQuery, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        // Only the cleared extensions are gone, everything else is sitll there.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(11, extensions.getSize());
    }

    @Test
    public void testClearRepository() throws Exception
    {
        // Empty index.
        IterableResult<Extension> extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        index.index();

        // Full index.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(31, extensions.getSize());

        index.clear(mockRepositoryDescriptor2);

        // Just extensions from the first repo remain.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(12, extensions.getSize());

        index.clear(mockRepositoryDescriptor);

        // Empty index.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(0, extensions.getSize());
    }

    @Test
    public void testClearAllRepositories() throws IndexException
    {
        // Empty index.
        IterableResult<Extension> extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(0, extensions.getSize());

        index.index();

        // Full index.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(31, extensions.getSize());

        index.clear();

        // Empty index again.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(0, extensions.getSize());
    }

    @Test
    public void testSearchExtension() throws Exception
    {
        IterableResult<Extension> extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 1);
        Assert.assertEquals(0, extensions.getSize());

        index.index();

        // Search on all repositories.
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 99);
        Assert.assertEquals(31, extensions.getSize());

        // Save the ordered result for later.
        List<Extension> orderedExtensions = new ArrayList<Extension>();
        for (Extension extension : extensions) {
            orderedExtensions.add(extension);
        }

        // "Test your limits".
        extensions = index.search(ALL_EXTENSIONS_QUERY, 0, 1);
        Assert.assertEquals(1, extensions.getSize());

        // Test the offset.
        int offset = 1;
        int limit = 3;
        extensions = index.search(ALL_EXTENSIONS_QUERY, offset, limit);
        Assert.assertEquals(limit, extensions.getSize());
        // Check that the order of the results is also preserved.
        Iterator<Extension> resultIterator = extensions.iterator();
        for (int i = offset; i < offset + limit; i++) {
            Extension orderedExtension = orderedExtensions.get(i);
            Extension result = resultIterator.next();
            Assert.assertEquals(orderedExtension, result);
            Assert.assertEquals(orderedExtension.getRepository().getDescriptor().getId(), result.getRepository()
                .getDescriptor().getId());
        }

        // The two methods should be equivalent.
        extensions =
            index.search(ALL_EXTENSIONS_QUERY, Arrays.asList(mockRepositoryDescriptor, mockRepositoryDescriptor2), 0,
                99);
        Assert.assertEquals(31, extensions.getSize());

        // Search only on the first repository.
        extensions = index.search(ALL_EXTENSIONS_QUERY, mockRepositoryDescriptor, 0, 99);
        Assert.assertEquals(12, extensions.getSize());

        // Search only on the second repository.
        extensions = index.search(ALL_EXTENSIONS_QUERY, mockRepositoryDescriptor2, 0, 99);
        Assert.assertEquals(19, extensions.getSize());

        // Search by name.
        extensions = index.search("n:velocity", 0, 99);
        Assert.assertEquals(5, extensions.getSize());
        Assert.assertEquals("XWiki Commons - Velocity", extensions.iterator().next().getName());

        // Search by description.
        // Use bits from junit's description.
        extensions = index.search("d:\"regression testing framework\"", mockRepositoryDescriptor, 0, 99);
        Assert.assertEquals(4, extensions.getSize());
        String storedDescription = "JUnit is a regression testing framework written by Erich Gamma and Kent Beck. ";
        storedDescription += "It is used by the developer who implements unit tests in Java.";
        Assert.assertEquals(storedDescription, extensions.iterator().next().getDescription());
    }
}
