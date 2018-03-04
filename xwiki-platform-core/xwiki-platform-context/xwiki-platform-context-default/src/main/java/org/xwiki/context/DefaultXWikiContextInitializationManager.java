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
package org.xwiki.context;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

/**
 * This is the default implementation of {@link XWikiContextInitializationManager}.
 *
 * @version $Id$
 * @since 10.2-RC1
 */
@Component
@Singleton
public class DefaultXWikiContextInitializationManager implements XWikiContextInitializationManager
{
    @Inject
    private Container container;

    @Inject
    private Execution execution;

    @Override
    public void initialize() throws XWikiContextInitializationException
    {
        initialize(execution.getContext(), -1);
    }

    @Override
    public void initialize(ExecutionContext executionContext, int mode) throws XWikiContextInitializationException
    {
        try {
            HttpServletRequest httpServletRequest = ((ServletRequest) container.getRequest()).getHttpServletRequest();
            HttpServletResponse httpServletResponse =
                    ((ServletResponse) container.getResponse()).getHttpServletResponse();
            ServletContext servletContext = httpServletRequest.getSession().getServletContext();

            XWikiServletContext xwikiEngine = new XWikiServletContext(servletContext);
            XWikiServletRequest xwikiRequest = new XWikiServletRequest(httpServletRequest);
            XWikiServletResponse xwikiResponse = new XWikiServletResponse(httpServletResponse);

            // Not all request types specify an action (e.g. GWT-RPC) so we default to the empty string.
            String action = "";

            // Create the XWiki context.
            XWikiContext xwikiContext = Utils.prepareContext(action, xwikiRequest, xwikiResponse, xwikiEngine);

            // Overwrite the context mode set in the prepareContext() call just above if a specific mode is specified.
            if (mode >= 0) {
                xwikiContext.setMode(mode);
            }

            // Bridge with old code to play well with new components.
            String key = "xwikicontext";
            if (executionContext.hasProperty(key)) {
                executionContext.setProperty(key, xwikiContext);
            } else {
                executionContext.newProperty(key).inherited().initial(xwikiContext).declare();
            }

            // Initialize the XWiki database. XWiki#getXWiki(XWikiContext) calls XWikiContext.setWiki(XWiki).
            XWiki xwiki = XWiki.getXWiki(xwikiContext);

            // Initialize the URL factory.
            xwikiContext.setURLFactory(xwiki.getURLFactoryService().createURLFactory(
                    xwikiContext.getMode(), xwikiContext));

            // Prepare the localized resources, according to the selected language.
            xwiki.prepareResources(xwikiContext);

            // Initialize the current user.
            XWikiUser user = xwikiContext.getWiki().checkAuth(xwikiContext);
            if (user != null) {
                DocumentReferenceResolver<String> documentReferenceResolver =
                        Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");
                SpaceReference defaultUserSpace =
                        new SpaceReference(XWiki.SYSTEM_SPACE, new WikiReference(xwikiContext.getWikiId()));
                DocumentReference userReference = documentReferenceResolver.resolve(user.getUser(), defaultUserSpace);
                xwikiContext.setUserReference(XWikiRightService.GUEST_USER.equals(userReference.getName()) ? null
                        : userReference);
            }
        } catch (XWikiException e) {
            throw new XWikiContextInitializationException("Failed to initialize the XWiki context.", e);
        }
    }
}
