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
package org.xwiki.extension.index.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchEngine;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.NexusAnalyzer;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.index.ExtensionConverter;
import org.xwiki.extension.index.ExtensionIndex;
import org.xwiki.extension.index.IndexException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;

/**
 * @version $Id$
 */
@Singleton
public class DefaultExtensionIndex implements ExtensionIndex, Initializable, Disposable
{
    /** The Lucene version used by the index. */
    public static final Version LUCENE_VERSION = Version.LUCENE_48;

    private PlexusContainer plexusContainer;

    private Indexer indexer;

    private IndexUpdater indexUpdater;

    // /**
    // * The context to use when searching across multiple indexed repositories.
    // */
    // private IndexingContext searchContext;

    private Map<String, IndexingContext> indexingContextRegistry;

    private List<IndexCreator> indexers;

    private SearchEngine searchEngine;

    private QueryParser queryParser;

    /**
     * Used to determine the location where to store the index.
     */
    @Inject
    private ExtensionManagerConfiguration extensionManagerConfiguration;

    /**
     * Used to query currently available extension repositories.
     */
    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @Inject
    private ExtensionConverter extensionConverter;

    /**
     * Everybody logs, sometimes.
     */
    @Inject
    private Logger logger;

    private File indexDirectory;

    @Override
    public void initialize() throws InitializationException
    {
        // Initialize maven-indexer
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);

        try {
            this.plexusContainer = new DefaultPlexusContainer(config);

            // lookup the indexer components from plexus
            this.indexer = plexusContainer.lookup(Indexer.class);
            this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);

            // IndexCreators we want to use (search for fields it defines)
            this.indexers = new ArrayList<IndexCreator>();
            this.indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
            this.indexers.add(new ExtensionIndexCreator());

            this.indexingContextRegistry = new HashMap<String, IndexingContext>();

            // Store repository indexes in the parent directory of the local extension repository.
            this.indexDirectory = new File(extensionManagerConfiguration.getLocalRepository().getParentFile(), "index");
            logger.info("Using [{}] as extension index directory.", indexDirectory);

            // Initialize modules needed for searching the index.
            this.searchEngine = this.plexusContainer.lookup(SearchEngine.class);

            // We build our own query in order to allow free-text queries parsed by the QueryParser.
            Analyzer analyzer = initAnalyzer();
            this.queryParser =
                new QueryParser(LUCENE_VERSION, ExtensionIndexCreator.FLD_EXTENSION_ID.getKey(), analyzer);
            // We want to allow '*something' queries, even if they can be heavy on a big index.
            this.queryParser.setAllowLeadingWildcard(true);

        } catch (Exception e) {
            throw new InitializationException("Failed to initialize Extension Index", e);
        }
    }

    /**
     * @return the {@link PerFieldAnalyzerWrapper} that maps each keyword field to the {@link KeywordAnalyzer}, while
     *         leaving all other fields to be processed by the default {@link NexusAnalyzer} used by the {@link Indexer}
     * @throws IndexException if problems occur
     */
    private Analyzer initAnalyzer() throws IndexException
    {
        List<IndexerField> exactFields = extractExactFields();

        Map<String, Analyzer> specialAnalyzers = new HashMap<String, Analyzer>();
        Analyzer preserve = new KeywordAnalyzer();
        for (IndexerField field : exactFields) {
            specialAnalyzers.put(field.getKey(), preserve);
        }

        // Manually add the uninfo field which is initialized by Maven Indexer itself and not by an index creator.
        specialAnalyzers.put(ArtifactInfo.UINFO, preserve);

        Analyzer defaultAnalyzer = new NexusAnalyzer();
        Analyzer result = new PerFieldAnalyzerWrapper(defaultAnalyzer, specialAnalyzers);

        return result;
    }

    /**
     * For each configured index creators, look at their class and extract the declared public static
     * {@code IndexerField}s that are not analyzed (i.e. that generate keyword analyzed values)
     * 
     * @return the list of exact fields
     * @throws IndexException if problems occur
     */
    private List<IndexerField> extractExactFields() throws IndexException
    {
        List<IndexerField> exactFields = new ArrayList<IndexerField>();

        try {
            for (IndexCreator indexCreator : indexers) {
                for (java.lang.reflect.Field field : indexCreator.getClass().getFields()) {
                    // public static {@code IndexerField}
                    if (field.getType().isAssignableFrom(IndexerField.class) && Modifier.isStatic(field.getModifiers())
                        && Modifier.isPublic(field.getModifiers())) {
                        IndexerField indexerField = (IndexerField) field.get(null);
                        if (indexerField.isKeyword()) {
                            exactFields.add(indexerField);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IndexException("Failed to extract the list of exact fields from existing indexers", e);
        }

        return exactFields;
    }

    private File getIndexDirectory()
    {
        return this.indexDirectory;
    }

    @Override
    public void index(Extension extension) throws IndexException
    {
        ExtensionRepository repository = extension.getRepository();
        ExtensionRepositoryDescriptor repositoryDescriptor = repository.getDescriptor();

        IndexingContext indexingContext = getIndexingContext(repositoryDescriptor, true);

        try {
            IndexWriter indexWriter = indexingContext.getIndexWriter();

            Document document = getDocument(extension, indexingContext);

            // Add the document to the index.
            indexWriter.addDocument(document);
            indexWriter.commit();
        } catch (Exception e) {
            throw new IndexException("Failed to index extension", e);
        }
    }

    private Document getDocument(Extension extension, IndexingContext indexingContext)
    {
        ArtifactInfo artifactInfo = extensionConverter.getArtifactInfo(extension);

        // Create the new document corresponding to this new extension.
        Document document = new Document();

        // Minimum require information for an indexed artifact.
        document.add(new StringField(ArtifactInfo.UINFO, artifactInfo.getUinfo(), Field.Store.YES));
        document.add(new LongField(ArtifactInfo.LAST_MODIFIED, System.currentTimeMillis(), Store.YES));
        // document.add(new Field(ArtifactInfo.UINFO, artifactInfo.getUinfo(), Field.Store.YES,
        // Field.Index.NOT_ANALYZED));
        // document.add(new Field(ArtifactInfo.LAST_MODIFIED, Long.toString(System.currentTimeMillis()), Store.YES,
        // Index.NO));

        // Run all existing indexers to populate the document to be stored to the index.
        for (IndexCreator indexCreator : indexingContext.getIndexCreators()) {
            indexCreator.updateDocument(artifactInfo, document);
        }

        return document;
    }

    @Override
    public void index(ExtensionRepositoryDescriptor repositoryDescriptor) throws IndexException
    {
        String repositoryId = repositoryDescriptor.getId();
        logger.info("Indexing repository [{}]", repositoryId);

        // Get or create context for the repository index
        IndexingContext indexingContext = getIndexingContext(repositoryDescriptor, true);

        // Create ResourceFetcher implementation to be used with IndexUpdateRequest
        // Here, we use Wagon based one as shorthand, but all we need is a ResourceFetcher implementation
        ResourceFetcher resourceFetcher = null;
        try {
            resourceFetcher = getResourceFetcher(repositoryDescriptor);
        } catch (Exception e) {
            throw new IndexException(String.format(
                "Failed to get resource fetcher for repository [%s]. Check the repository's URI.", repositoryId), e);
        }

        Date indexingContextCurrentTimestamp = indexingContext.getTimestamp();
        IndexUpdateRequest updateRequest = new IndexUpdateRequest(indexingContext, resourceFetcher);
        IndexUpdateResult updateResult = null;

        long start = System.currentTimeMillis();
        try {
            updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
        } catch (Exception e) {
            throw new IndexException("Failed to fetch and update index", e);
        }
        long time = System.currentTimeMillis() - start;

        if (updateResult.isFullUpdate()) {
            logger.info("Full index update performed for repository [{}] in {}ms", repositoryId, time);
        } else if (updateResult.getTimestamp().equals(indexingContextCurrentTimestamp)) {
            logger.info("No update was performed for repository [{}]. Index is up to date.", repositoryId);
        } else {
            logger.info("Incremental update was performed for repository [{}] in {}ms. The covered period is {} - {}.",
                repositoryId, time, indexingContextCurrentTimestamp, updateResult.getTimestamp());
        }
    }

    /**
     * @param repositoryDescriptor
     * @return
     * @throws IndexException
     */
    private IndexingContext createIndexingContext(ExtensionRepositoryDescriptor repositoryDescriptor)
        throws IndexException
    {
        String repositoryId = repositoryDescriptor.getId();
        IndexingContext indexingContext = null;
        try {
            File[] indexDirectories = getIndexDirectories(repositoryDescriptor.getId());

            indexingContext =
                indexer.createIndexingContext(String.format("%s-context", repositoryId), repositoryId,
                    indexDirectories[0], indexDirectories[1], repositoryDescriptor.getURI().toString(), null, true,
                    true, indexers);
        } catch (Exception e) {
            throw new IndexException("Failed to create indexing context for repository", e);
        }

        // Add it to the list of indexing context so we can later search on it.
        indexingContextRegistry.put(repositoryId, indexingContext);

        return indexingContext;
    }

    private IndexingContext getIndexingContext(ExtensionRepositoryDescriptor repositoryDescriptor, boolean create)
        throws IndexException
    {
        IndexingContext result = indexingContextRegistry.get(repositoryDescriptor.getId());

        if (result == null && create) {
            result = createIndexingContext(repositoryDescriptor);
        }

        return result;
    }

    private IndexingContext getIndexingContext(ExtensionRepositoryDescriptor repositoryDescriptor)
        throws IndexException
    {
        return getIndexingContext(repositoryDescriptor, false);
    }

    /**
     * @param repositoryID the ID of a repository.
     * @return The directories to use for the given repository where to store [0] the local cache (used in download
     *         operations) and [1] the Lucene index.
     */
    private File[] getIndexDirectories(String repositoryID)
    {
        File parent = getIndexDirectory();

        File repoCacheDirectory = new File(parent, String.format("%s-cache", repositoryID));
        File repoIndexDirectory = new File(parent, String.format("%s-index", repositoryID));

        File[] result = new File[] {repoCacheDirectory, repoIndexDirectory};

        return result;
    }

    private ResourceFetcher getResourceFetcher(ExtensionRepositoryDescriptor extensionRepositoryDescriptor)
        throws Exception
    {
        URI repositoryURI = extensionRepositoryDescriptor.getURI();
        String scheme = repositoryURI.getScheme();

        Wagon wagon = plexusContainer.lookup(Wagon.class, scheme);

        // TODO: Add authenticationInfo and proxy support.
        TransferListener transgerLogger = new LoggingTransferListener(this.logger);
        ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(wagon, transgerLogger, null, null);

        return resourceFetcher;
    }

    @Override
    public void index() throws IndexException
    {
        logger.info("Indexing all repositories");

        // Index/Update all the configured repositories.
        for (ExtensionRepository repository : extensionRepositoryManager.getRepositories()) {
            index(repository.getDescriptor());
        }
    }

    @Override
    public void clear(ExtensionRepositoryDescriptor repositoryDescriptor) throws IndexException
    {
        logger.info("Clearing repository [{}]", repositoryDescriptor.getId());

        IndexingContext indexingContext = getIndexingContext(repositoryDescriptor);
        if (indexingContext != null) {
            try {
                indexingContext.purge();
            } catch (Exception e) {
                throw new IndexException("Failed to clean repository", e);
            }
        }
    }

    @Override
    public void clear(Extension extension) throws IndexException
    {
        ExtensionRepositoryDescriptor repositoryDescriptor = extension.getRepository().getDescriptor();

        // Build the query.
        ExtensionId extensionId = extension.getId();
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("eid:");
        queryBuilder.append(QueryParser.escape(extensionId.getId()));
        queryBuilder.append(" AND v:");
        queryBuilder.append(QueryParser.escape(extensionId.getVersion().getValue()));

        // Delegate
        this.clear(repositoryDescriptor, queryBuilder.toString());
    }

    @Override
    public void clear(ExtensionRepositoryDescriptor repositoryDescriptor, String query) throws IndexException
    {
        logger.info("Clearing the repository [{}] by query [{}]", repositoryDescriptor.getId(), query);

        IndexingContext indexingContext = getIndexingContext(repositoryDescriptor);
        IndexWriter indexWriter = null;
        try {
            indexWriter = indexingContext.getIndexWriter();

            Query q = queryParser.parse(query);

            indexWriter.deleteDocuments(q);
            indexWriter.commit();
        } catch (Exception e) {
            try {
                if (indexWriter != null) {
                    indexWriter.rollback();
                }
            } catch (Exception ex) {
                logger.error("Failed to rollback the clearing of repository [{}] by query [{}]",
                    repositoryDescriptor.getId(), query, ex);
            }
            throw new IndexException(String.format("Failed to clear repository [%s] by query [%s]",
                repositoryDescriptor.getId(), query), e);
        }
    }

    @Override
    public void clear() throws IndexException
    {
        logger.info("Clearing all repositories");

        // Clear all the configured repositories.
        for (ExtensionRepository repository : extensionRepositoryManager.getRepositories()) {
            clear(repository.getDescriptor());
        }
    }

    @Override
    public IterableResult<Extension> search(String query, List<ExtensionRepositoryDescriptor> repositories, int offset,
        int limit) throws IndexException
    {
        logger.info("Searching: [{}], offset [{}], limit [{}]", query, offset, limit);

        IterableResult<Extension> results = null;

        try {
            // Query q = indexer.constructQuery(MAVEN.ARTIFACT_ID, new StringSearchExpression(query));

            // Parse the query.
            Query q = this.queryParser.parse(query);

            // Build the request.
            IteratorSearchRequest request = new IteratorSearchRequest(q);
            request.setCount(limit);
            request.setStart(offset);

            // Build the list of repositories where this search will be performed.
            List<IndexingContext> indexingContexts = new ArrayList<IndexingContext>();
            for (ExtensionRepositoryDescriptor repositoryDescriptor : repositories) {
                String repositoryId = repositoryDescriptor.getId();
                IndexingContext indexingContext = indexingContextRegistry.get(repositoryId);
                if (indexingContext != null) {
                    indexingContexts.add(indexingContext);
                } else {
                    logger
                        .warn("Asked to search on the repository [{}] but it is not currently indexed.", repositoryId);
                }
            }

            // Execute the request over the specified indexed repositories.
            IteratorSearchResponse response = this.searchEngine.searchIteratorPaged(request, indexingContexts);

            // Build the result.
            List<Extension> extensions = new ArrayList<Extension>(limit);
            for (ArtifactInfo artifactInfo : response) {
                Extension extension = this.extensionConverter.getExtension(artifactInfo);
                extensions.add(extension);
            }
            results = new CollectionIterableResult<Extension>(response.getTotalHitsCount(), offset, extensions);

            // IndexSearcher indexSearcher = getSearchContext().acquireIndexSearcher();
            //
            // int maxResults = offset + limit;
            //
            // try {
            // // Query q = indexer.constructQuery(MAVEN.ARTIFACT_ID, new StringSearchExpression(query));
            // Query q = indexer.constructQuery(MAVEN.ARTIFACT_ID, new SourcedSearchExpression(query));
            //
            // // Using a query parser instead allows us to use wildcard queries.
            // //Query q = getQueryParser().parse(query);
            // TopDocs searchResults = indexSearcher.search(q, maxResults);
            //
            // List<Extension> extensions = new ArrayList<Extension>(limit);
            // for (ScoreDoc scoreDoc : searchResults.scoreDocs) {
            // Document document = indexSearcher.doc(scoreDoc.doc);
            // Extension extension = getExtension(document);
            // extensions.add(extension);
            // }
            //
            // results = new CollectionIterableResult<Extension>(searchResults.totalHits, offset, extensions);
            // } finally {
            // searchContext.releaseIndexSearcher(indexSearcher);
            // }
        } catch (Exception e) {
            throw new IndexException("Search operation failed", e);
        }

        return results;
    }

    @Override
    public IterableResult<Extension> search(String query, int offset, int limit) throws IndexException
    {
        List<ExtensionRepositoryDescriptor> repositoryDescriptors = new ArrayList<ExtensionRepositoryDescriptor>();
        for (ExtensionRepository repository : extensionRepositoryManager.getRepositories()) {
            repositoryDescriptors.add(repository.getDescriptor());
        }

        IterableResult<Extension> result = this.search(query, repositoryDescriptors, offset, limit);

        return result;
    }

    @Override
    public IterableResult<Extension> search(String query, ExtensionRepositoryDescriptor repository, int offset,
        int limit) throws IndexException
    {
        IterableResult<Extension> result = this.search(query, Arrays.asList(repository), offset, limit);

        return result;
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        for (IndexingContext indexingContext : indexingContextRegistry.values()) {
            try {
                indexer.closeIndexingContext(indexingContext, false);
            } catch (IOException e) {
                logger.error("Failed to properly close indexing context for repository [{}]",
                    indexingContext.getRepositoryId());
            }
        }
    }

    // /**
    // * TO BE REMOVED, replaced by the SearchEngine.
    // *
    // * @deprecated
    // */
    // public IndexingContext getSearchContext() throws Exception
    // {
    // if (searchContext == null) {
    // File[] indexDirectories = getIndexDirectories("search");
    // searchContext =
    // indexer.createMergedIndexingContext("search", "search-index", indexDirectories[0], indexDirectories[1],
    // true, new ContextMemberProvider()
    // {
    // @Override
    // public Collection<IndexingContext> getMembers()
    // {
    // return Collections.unmodifiableCollection(indexingContextRegistry);
    // }
    // });
    // }
    //
    // return searchContext;
    // }

}
