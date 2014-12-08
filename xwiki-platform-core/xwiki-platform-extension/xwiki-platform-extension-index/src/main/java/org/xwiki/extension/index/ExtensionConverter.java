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

import org.apache.maven.index.ArtifactInfo;
import org.xwiki.component.annotation.Role;
import org.xwiki.extension.Extension;

/**
 * Converts back an forwards between an {@link Extension} and an {@link ArtifactInfo}, which is the data type
 * abstraction used by the maven indexer.
 * 
 * @version $Id$
 */
@Role
public interface ExtensionConverter
{
    /**
     * @param artifactInfo the artifactInfo used as input
     * @return the equivalent {@link Extension} that is being indexed by the given {@link ArtifactInfo}
     */
    Extension getExtension(ArtifactInfo artifactInfo);

    /**
     * @param extension the extension used as input
     * @return the {@link ArtifactInfo} that is used to index the given {@link Extension}
     */
    ArtifactInfo getArtifactInfo(Extension extension);
}
