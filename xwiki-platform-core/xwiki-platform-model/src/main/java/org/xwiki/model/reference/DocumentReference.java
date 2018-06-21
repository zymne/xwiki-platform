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
package org.xwiki.model.reference;

import java.beans.Transient;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Provider;

import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;

/**
 * Represents a reference to a document (wiki, space and document names).
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DocumentReference extends AbstractLocalizedEntityReference
{
    /**
     * The {@link Type} for a {@code Provider<DocumentReference>}.
     * 
     * @since 7.2M1
     */
    public static final Type TYPE_PROVIDER =
        new DefaultParameterizedType(null, Provider.class, DocumentReference.class);

    /**
     * Parameter key for the locale.
     */
    static final String LOCALE = AbstractLocalizedEntityReference.LOCALE;

    /**
     * Cache the {@link LocalDocumentReference} corresponding to this {@link DocumentReference}.
     */
    private LocalDocumentReference localDocumentReference;

    /**
     * Special constructor that transforms a generic entity reference into a {@link DocumentReference}. It checks the
     * validity of the passed reference (ie correct type and correct parent).
     *
     * @param reference the reference to convert
     * @exception IllegalArgumentException if the passed reference is not a valid document reference
     */
    public DocumentReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Clone an DocumentReference, but replace one of the parent in the chain by a new one.
     *
     * @param reference the reference that is cloned
     * @param oldReference the old parent that will be replaced
     * @param newReference the new parent that will replace oldReference in the chain
     * @since 3.3M2
     */
    protected DocumentReference(EntityReference reference, EntityReference oldReference, EntityReference newReference)
    {
        super(reference, oldReference, newReference);
    }

    /**
     * Clone the provided reference and change the Locale.
     *
     * @param reference the reference to clone
     * @param locale the new locale for this reference, if null, locale is removed
     * @exception IllegalArgumentException if the passed reference is not a valid document reference
     */
    public DocumentReference(EntityReference reference, Locale locale)
    {
        super(reference);
        setLocale(locale);
    }

    /**
     * Create a new Document reference from wiki, space and page name.
     *
     * @param wikiName the name of the wiki containing the document, must not be null
     * @param spaceName the name of the space containing the document, must not be null
     * @param pageName the name of the document
     */
    public DocumentReference(String wikiName, String spaceName, String pageName)
    {
        this(pageName, new SpaceReference(spaceName, new WikiReference(wikiName)));
    }

    /**
     * Create a new Document reference from wiki name, space name, page name and locale.
     *
     * @param wikiName the name of the wiki containing the document, must not be null
     * @param spaceName the name of the space containing the document, must not be null
     * @param pageName the name of the document
     * @param locale the locale of the document reference, may be null
     */
    public DocumentReference(String wikiName, String spaceName, String pageName, Locale locale)
    {
        this(pageName, new SpaceReference(spaceName, new WikiReference(wikiName)), locale);
    }

    /**
     * Create a new Document reference from wiki name, space name, page name and language. This is an helper function
     * during transition from language to locale, it will be deprecated ASAP.
     *
     * @param wikiName the name of the wiki containing the document, must not be null
     * @param spaceName the name of the space containing the document, must not be null
     * @param pageName the name of the document
     * @param language the language of the document reference, may be null
     */
    public DocumentReference(String wikiName, String spaceName, String pageName, String language)
    {
        this(pageName, new SpaceReference(spaceName, new WikiReference(wikiName)),
            (language == null) ? null : new Locale(language));
    }

    /**
     * Create a new Document reference from wiki name, spaces names and page name.
     *
     * @param wikiName the name of the wiki containing the document, must not be null
     * @param spaceNames an ordered list of the names of the spaces containing the document from root space to last one,
     *            must not be null
     * @param pageName the name of the document
     */
    public DocumentReference(String wikiName, List<String> spaceNames, String pageName)
    {
        super(pageName, EntityType.DOCUMENT, new SpaceReference(wikiName, spaceNames));
    }

    /**
     * Create a new Document reference from wiki name, spaces names, page name and locale.
     *
     * @param wikiName the name of the wiki containing the document, must not be null
     * @param spaceNames an ordered list of the names of the spaces containing the document from root space to last one,
     *            must not be null
     * @param pageName the name of the document reference
     * @param locale the locale of the document reference, may be null
     */
    public DocumentReference(String wikiName, List<String> spaceNames, String pageName, Locale locale)
    {
        super(pageName, EntityType.DOCUMENT, new SpaceReference(wikiName, spaceNames));
        setLocale(locale);
    }

    /**
     * Create a new Document reference from document name and parent space.
     *
     * @param pageName the name of the document
     * @param parent the parent space for the document
     */
    public DocumentReference(String pageName, SpaceReference parent)
    {
        super(pageName, EntityType.DOCUMENT, parent);
    }

    /**
     * Create a new Document reference from local document reference and wiki reference.
     * 
     * @param localDocumentReference the document reference without the wiki reference
     * @param wikiReference the wiki reference
     * @since 5.1M1
     */
    public DocumentReference(LocalDocumentReference localDocumentReference, WikiReference wikiReference)
    {
        super(localDocumentReference, null, wikiReference);
    }

    /**
     * Create a new Document reference from document name, parent space and locale.
     *
     * @param pageName the name of the document
     * @param parent the parent space for the document
     * @param locale the locale of the document reference, may be null
     */
    public DocumentReference(String pageName, SpaceReference parent, Locale locale)
    {
        super(pageName, EntityType.DOCUMENT, parent);
        setLocale(locale);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in order to verify the validity of the passed parent.
     * </p>
     *
     * @see org.xwiki.model.reference.EntityReference#setParent(EntityReference)
     * @exception IllegalArgumentException if the passed parent is not a valid document reference parent (ie a space
     *                reference)
     */
    @Override
    protected void setParent(EntityReference parent)
    {
        if (parent instanceof SpaceReference) {
            super.setParent(parent);
            return;
        }

        if (parent == null || parent.getType() != EntityType.SPACE) {
            throw new IllegalArgumentException("Invalid parent reference [" + parent + "] in a document reference");
        }

        super.setParent(new SpaceReference(parent));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden in order to verify the validity of the passed type.
     * </p>
     *
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     * @exception IllegalArgumentException if the passed type is not a document type
     */
    @Override
    protected void setType(EntityType type)
    {
        if (type != EntityType.DOCUMENT) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a document reference");
        }

        super.setType(EntityType.DOCUMENT);
    }

    /**
     * @return the wiki reference of this document reference
     */
    @Transient
    public WikiReference getWikiReference()
    {
        return (WikiReference) extractReference(EntityType.WIKI);
    }

    /**
     * Create a new DocumentReference with passed wiki reference.
     * 
     * @param wikiReference the wiki reference to use
     * @return a new document reference or the same if the passed wiki is already the current wiki
     * @since 7.2M1
     */
    @Transient
    public DocumentReference setWikiReference(WikiReference wikiReference)
    {
        WikiReference currentWikiReferene = getWikiReference();

        if (currentWikiReferene.equals(wikiReference)) {
            return this;
        }

        return new DocumentReference(this, currentWikiReferene, wikiReference);
    }

    /**
     * @return the space reference of the last space containing this document
     */
    @Transient
    public SpaceReference getLastSpaceReference()
    {
        return (SpaceReference) extractReference(EntityType.SPACE);
    }

    /**
     * @return space references of this document in an ordered list
     */
    @Transient
    public List<SpaceReference> getSpaceReferences()
    {
        List<SpaceReference> references = new ArrayList<SpaceReference>();

        EntityReference reference = this;
        while (reference != null) {
            if (reference.getType() == EntityType.SPACE) {
                references.add((SpaceReference) reference);
            }
            reference = reference.getParent();
        }
        // Reverse the array so that the last entry is the parent of the Document Reference
        Collections.reverse(references);

        return references;
    }

    @Override
    public DocumentReference replaceParent(EntityReference oldParent, EntityReference newParent)
    {
        return new DocumentReference(this, oldParent, newParent);
    }

    /**
     * @return the {@link LocalDocumentReference} corresponding to this {@link DocumentReference}
     * @since 8.3
     * @deprecated since 9.3RC1/8.4.5, use {@link #getLocalDocumentReference()} instead
     */
    @Deprecated
    public LocalDocumentReference getLocaleDocumentReference()
    {
        return getLocalDocumentReference();
    }

    /**
     * @return the {@link LocalDocumentReference} corresponding to this {@link DocumentReference}
     * @since 9.3RC1
     * @since 8.4.5
     */
    public LocalDocumentReference getLocalDocumentReference()
    {
        if (this.localDocumentReference == null) {
            this.localDocumentReference = new LocalDocumentReference(this);
        }

        return this.localDocumentReference;
    }

    @Override
    public String toString()
    {
        // Compared to EntityReference we don't print the type since the type is already indicated by the fact that
        // this is a DocumentReference instance.
        return TOSTRING_SERIALIZER.serialize(this);
    }
}
