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
package org.xwiki.store.hibernate.internal;

import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import javax.inject.Inject;
import javax.inject.Named;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.store.RootTransactionRunnable;
import org.xwiki.store.UnexpectedException;

/**
 * A Transaction based on Hibernate store.
 * SQL based TransactionRunnables MUST extend RootTransactionRunnable because
 * SQL storage engines are incapable of rolling back after commit.
 *
 * @version $Id$
 * @since TODO
 */
@Component
@Named("hibernate")
public class HibernateTransaction extends RootTransactionRunnable<Session>
{
    /** The storage engine. */
    private XWikiHibernateBaseStore store;

    /** The XWikiContext associated with the request which started this Transaction. */
    private XWikiContext context;

    /** The execution for getting the XWikiContext. */
    @Inject
    private Execution exec;

    /**
     * True if the transaction should be ended when finished.
     * This will only be false if the transaction could not be started because another transaction
     * was already open and associated with the same XWikiContext.
     */
    private boolean shouldCloseTransaction;

    /**
     * Testing Constructor.
     *
     * @param context the XWikiContext associated with the request which started this Transaction.
     */
    public HibernateTransaction(final XWikiContext context)
    {
        this.store = context.getWiki().getHibernateStore();
        this.context = context;
    }

    @Override
    protected Session getProvidedContext()
    {
        return this.store.getSession(this.context);
    }

    @Override
    public void onPreRun() throws XWikiException
    {
        this.store.checkHibernate(this.context);
    }

    @Override
    public void onRun() throws XWikiException
    {
        this.shouldCloseTransaction = this.store.beginTransaction(this.context);
        if (this.getProvidedContext() == null) {
            throw new UnexpectedException("The transaction did not begin properly.");
        }
    }

    @Override
    public void onCommit()
    {
        if (this.shouldCloseTransaction) {
            this.store.endTransaction(this.context, true);
        }
    }

    @Override
    public void onRollback()
    {
        if (this.shouldCloseTransaction) {
            this.store.endTransaction(this.context, false);
        }
    }
}
