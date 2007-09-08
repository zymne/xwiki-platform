/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */

package com.xpn.xwiki.xmlrpc.model;

public interface BlogEntry extends MapObject
{

    /**
     * the id of the blog entry
     */
    String getId();

    void setId(String id);

    /**
     * the key of the space that this blog entry belongs to
     */
    String getSpace();

    void setSpace(String space);

    /**
     * the title of the page
     */
    String getTitle();

    void setTitle(String title);

    /**
     * the url to view this blog entry online
     */
    String getUrl();

    void setUrl(String url);

    /**
     * the version number of this blog entry
     */
    int getVersion();

    void setVersion(int version);

    /**
     * the blog entry content
     */
    String getContent();

    void setContent(String content);

    /**
     * the number of locks current on this page
     */
    int getLocks();

    void setLocks(int locks);

}
