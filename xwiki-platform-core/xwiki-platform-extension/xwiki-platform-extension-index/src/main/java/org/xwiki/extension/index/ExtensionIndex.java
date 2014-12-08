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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.Extension;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.result.IterableResult;

/**
 * Indexes registered repositories in order to support fast local searches, regardless of repository type and
 * capabilities.
 * 
 * @version $Id$
 */
@Role
public interface ExtensionIndex
{
    /**
     * @param extension the extension to index
     * @throws IndexException if problems occur
     */
    void index(Extension extension) throws IndexException;

    /**
     * @param repositoryDescriptor the repository whose extensions to index
     * @throws IndexException if problems occur
     */
    void index(ExtensionRepositoryDescriptor repositoryDescriptor) throws IndexException;

    /**
     * Index all the extensions of all the registered {@link org.xwiki.extension.repository.ExtensionRepository
     * ExtensionRepository}s, as returned by the {@link org.xwiki.extension.repository.ExtensionRepositoryManager
     * ExtensionRepositoryManager}.
     * 
     * @throws IndexException if problems occur
     */
    void index() throws IndexException;

    /**
     * @param extension the extension to remove from the index
     * @throws IndexException if problems occur
     */
    void clear(Extension extension) throws IndexException;

    /**
     * @param repositoryDescriptor the descriptor of the repository for which to remove all its indexed extensions
     * @throws IndexException if problems occur
     */
    void clear(ExtensionRepositoryDescriptor repositoryDescriptor) throws IndexException;

    /**
     * @param repositoryDescriptor the descriptor of the repository on which to run the query
     * @param query the query to use when clearing entries from the index
     * @throws IndexException if problems occur
     */
    void clear(ExtensionRepositoryDescriptor repositoryDescriptor, String query) throws IndexException;

    /**
     * Clear all the indexed extensions of all the registered {@link org.xwiki.extension.repository.ExtensionRepository
     * ExtensionRepository}s, as returned by the {@link org.xwiki.extension.repository.ExtensionRepositoryManager
     * ExtensionRepositoryManager}.
     * 
     * @throws IndexException if problems occur
     */
    void clear() throws IndexException;

    /**
     * Search for extensions in a list of specified indexed repositories.
     * 
     * @param query the search query, Lucene format. Leading wildcard queries are accepted. Only the fields under the
     *            "min" indexer type are guaranteed to be present, and the "u" field, of course. Additionally, there are
     *            also the fields defined by {@link org.xwiki.extension.index.internal.ExtensionIndexCreator
     *            ExtensionIndexCreator} which are XWiki specific.
     * @param repositories the list of repositories where to search for extensions
     * @param offset the start offset
     * @param limit the number of results
     * @return an {@link IterableResult} with the extensions matching the query
     * @throws IndexException if problems occur
     * @see <a href="http ://maven.apache.org/maven-indexer-archives/maven-indexer-LATEST/indexer-core/index.html">Maven
     *      Indexer's Index Fields Reference</a>
     */
    IterableResult<Extension> search(String query, List<ExtensionRepositoryDescriptor> repositories, int offset,
        int limit) throws IndexException;

    /**
     * Search for extensions in a specified indexed repository.
     * <p/>
     * Convenience method, equivalent to {@code search(query, Arrays.asList(repository), offset, limit)}.
     * 
     * @param query the search query, Lucene format. Leading wildcard queries are accepted. Only the fields under the
     *            "min" indexer type are guaranteed to be present, and the "u" field, of course. Additionally, there are
     *            also the fields defined by {@link org.xwiki.extension.index.internal.ExtensionIndexCreator
     *            ExtensionIndexCreator} which are XWiki specific.
     * @param repository the repository where to search for extensions
     * @param offset the start offset
     * @param limit the number of results
     * @return an {@link IterableResult} with the extensions matching the query
     * @throws IndexException if problems occur
     * @see <a href="http ://maven.apache.org/maven-indexer-archives/maven-indexer-LATEST/indexer-core/index.html">Maven
     *      Indexer's Index Fields Reference</a>
     * @see #search(String, List, int, int)
     */
    IterableResult<Extension> search(String query, ExtensionRepositoryDescriptor repository, int offset, int limit)
        throws IndexException;

    /**
     * Search for extensions in all the indexed repositories.
     * 
     * @param query the search query, Lucene format. Leading wildcard queries are accepted. Only the fields under the
     *            "min" indexer type are guaranteed to be present, and the "u" field, of course. Additionally, there are
     *            also the fields defined by {@link org.xwiki.extension.index.internal.ExtensionIndexCreator
     *            ExtensionIndexCreator} which are XWiki specific.
     * @param offset the start offset
     * @param limit the number of results
     * @return an {@link IterableResult} with the extensions matching the query
     * @throws IndexException if problems occur
     * @see <a href="http ://maven.apache.org/maven-indexer-archives/maven-indexer-LATEST/indexer-core/index.html">Maven
     *      Indexer's Index Fields Reference</a>
     */
    IterableResult<Extension> search(String query, int offset, int limit) throws IndexException;
}
