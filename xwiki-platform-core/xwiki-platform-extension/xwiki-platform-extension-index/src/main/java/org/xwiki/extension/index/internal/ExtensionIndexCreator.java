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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IndexerFieldVersion;
import org.apache.maven.index.creator.AbstractIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;

/**
 * Index creator used to augment the local index for each repository with XWiki specific fields.
 * 
 * @version $Id$
 */
@Singleton
@Named(ExtensionIndexCreator.ID)
public class ExtensionIndexCreator extends AbstractIndexCreator
{
    /** The ID of this {@link org.apache.maven.index.context.IndexCreator IndexCreator}. */
    public static final String ID = "xwiki-extension";

    /** Extension ID (as keyword). */
    public static final IndexerField FLD_EXTENSION_ID_KW = new IndexerField(XWIKI.EXTENSION_ID, IndexerFieldVersion.V1,
        "eid", "Extension ID (as keyword)", Store.NO, Index.NOT_ANALYZED);

    /** Extension ID (tokenized). */
    public static final IndexerField FLD_EXTENSION_ID = new IndexerField(XWIKI.EXTENSION_ID, IndexerFieldVersion.V3,
        "extensionId", "Extension ID (tokenized)", Store.NO, Index.ANALYZED);

    /** We depend on the {@link MinimalArtifactInfoIndexCreator} to do its job. */
    public static final List<String> DEPENDENCIES = Arrays.asList("min");

    /**
     * This index creator is not available as a Plexus component, we just initialize it directly using this constructor.
     */
    public ExtensionIndexCreator()
    {
        super(ID);
    }

    @Override
    public Collection<IndexerField> getIndexerFields()
    {
        return Arrays.asList(FLD_EXTENSION_ID_KW, FLD_EXTENSION_ID);
    }

    @Override
    public List<String> getCreatorDependencies()
    {
        return DEPENDENCIES;
    }

    @Override
    public void populateArtifactInfo(ArtifactContext artifactContext) throws IOException
    {
        // This would only execute if we were indexing a local repository. Since we can not rely on the fact that each
        // repository will be indexed with our IndexCreator, we have not much use for this method.
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This is called when indexing a new artifact. The document is newly created and is only populated by the previous
     * index creators. The {@link ArtifactInfo} comes from a previous indirect call to
     * {@link #updateArtifactInfo(Document, ArtifactInfo)} or from a direct call to
     * {@link org.xwiki.extension.index.ExtensionConverter#getArtifactInfo(org.xwiki.extension.Extension)
     * ExtensionConverter#getArtifactInfo(org.xwiki.extension.Extension)}.
     * <p>
     * Note: Since the data in the document has just been populated by the previous index creators, this means that
     * every field's value is accessible, regardless if it is defined as a stored field or not.
     * 
     * @see org.apache.maven.index.context.IndexCreator#updateDocument(org.apache.maven.index.ArtifactInfo,
     *      org.apache.lucene.document.Document)
     */
    @Override
    public void updateDocument(ArtifactInfo artifactInfo, Document document)
    {
        // String extensionId = artifactInfo.getAttributes().get(XWIKI.EXTENSION_ID.getFieldName());
        //
        // document.add(FLD_EXTENSION_ID.toField(extensionId));
        // document.add(FLD_EXTENSION_ID_KW.toField(extensionId));
        String extensionId = document.get(FLD_EXTENSION_ID.getKey());
        if (extensionId == null) {
            // Extract the extension ID from data populated by the MinimalArtifactInfoIndexCreator.
            String groupId = document.get(MinimalArtifactInfoIndexCreator.FLD_GROUP_ID_KW.getKey());
            String artifactId = document.get(MinimalArtifactInfoIndexCreator.FLD_ARTIFACT_ID_KW.getKey());
            String classifier = document.get(MinimalArtifactInfoIndexCreator.FLD_CLASSIFIER.getKey());

            // Build the extension ID.
            StringBuilder builder = new StringBuilder();
            builder.append(groupId);
            builder.append(':');
            builder.append(artifactId);
            if (classifier != null) {
                builder.append(':');
                builder.append(classifier);
            }
            extensionId = builder.toString();
        }

        document.add(FLD_EXTENSION_ID.toField(extensionId));
        document.add(FLD_EXTENSION_ID_KW.toField(extensionId));
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This is called when retrieving an existing artifact, generally through a search request.
     * <p/>
     * This is also called when indexing a new remote repository, i.e. when its downloaded index is unpacked and its
     * documents are processed to produce {@link ArtifactInfo}s which are then saved again as new Documents in our
     * index.
     * <p/>
     * This is also called when the list of groups is computed for a newly indexed repository (imported index). Each
     * document is read from the index and the artifact's group is extracted and added to the list.
     * 
     * @see org.apache.maven.index.context.IndexCreator#updateArtifactInfo(org.apache.lucene.document.Document,
     *      org.apache.maven.index.ArtifactInfo)
     */
    @Override
    public boolean updateArtifactInfo(Document document, ArtifactInfo artifactInfo)
    {
        boolean modified = false;

        // No point in doing anything here.

        // String extensionId = document.get(FLD_EXTENSION_ID.getKey());
        // if (extensionId == null) {
        // String[] uniqueIdComponents = document.get(ArtifactInfo.UINFO).split("[|]");
        // String groupId = uniqueIdComponents[0];
        // String artifactId = uniqueIdComponents[1];
        //
        // extensionId = String.format("%s:%s", groupId, artifactId);
        //
        // artifactInfo.getAttributes().put(XWIKI.EXTENSION_ID.getFieldName(), extensionId);
        //
        // modified = true;
        // }

        return modified;
    }
}
