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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.index.ArtifactInfo;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.ExtensionConverter;
import org.xwiki.extension.index.IndexedExtension;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;

/**
 * Default implementation of {@link ExtensionConverter}.
 * 
 * @version $Id$
 */
@Singleton
public class DefaultExtensionConverter implements ExtensionConverter
{
    /**
     * Needed to resolve extension repositories by ID.
     */
    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @Override
    public Extension getExtension(ArtifactInfo artifactInfo)
    {
        StringBuilder extensionIdBuilder = new StringBuilder();
        extensionIdBuilder.append(artifactInfo.getGroupId());
        extensionIdBuilder.append(':');
        extensionIdBuilder.append(artifactInfo.getArtifactId());
        if (artifactInfo.getClassifier() != null) {
            extensionIdBuilder.append(':');
            extensionIdBuilder.append(artifactInfo.getClassifier());
        }

        ExtensionId extensionId = new ExtensionId(extensionIdBuilder.toString(), artifactInfo.getVersion());
        ExtensionRepository repository = extensionRepositoryManager.getRepository(artifactInfo.getRepository());
        String extensionType = artifactInfo.getPackaging();

        IndexedExtension extension = new IndexedExtension(repository, extensionId, extensionType);

        extension.setName(artifactInfo.getName());
        extension.setDescription(artifactInfo.getDescription());

        return extension;
    }

    @Override
    public ArtifactInfo getArtifactInfo(Extension extension)
    {
        String extensionId = extension.getId().getId();

        String[] components = extensionId.split(":");
        String extensionGroupId = components[0];
        String extensionArtifactId = components[1];
        String extensionClassifier = null;
        if (components.length == 3) {
            extensionClassifier = components[2];
        }

        ArtifactInfo artifactInfo =
            new ArtifactInfo(extension.getRepository().getDescriptor().getId(), extensionGroupId, extensionArtifactId,
                extension.getId().getVersion().getValue(), extensionClassifier, extension.getType());

        // // Index this to allow easy searches on extensions (i.e. to avoid parsing an extension's ID every time).
        // artifactInfo.getAttributes().put(XWIKI.EXTENSION_ID.getFieldName(), extensionId);

        return artifactInfo;
    }
}
