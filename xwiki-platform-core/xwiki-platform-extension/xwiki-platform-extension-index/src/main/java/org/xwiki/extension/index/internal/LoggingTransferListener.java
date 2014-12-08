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

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.slf4j.Logger;

/**
 * Used to monitor the index downloads from the remote repositories.
 * 
 * @version $Id$
 */
public class LoggingTransferListener extends AbstractTransferListener
{
    /** Amount downloaded so far. */
    private long downloaded;

    /** Total amount to download. */
    private long total;

    /** The logger object. */
    private Logger logger;

    /**
     * Constructor.
     * 
     * @param logger the logger to use.
     */
    public LoggingTransferListener(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void transferStarted(TransferEvent transferEvent)
    {
        logger.debug("Downloading [{}]", transferEvent.getResource().getName());
        downloaded = 0;
        total = transferEvent.getResource().getContentLength();
    }

    @Override
    public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length)
    {
        downloaded += length;
        byte percentComplete = (byte) (downloaded * 100 / total);

        logger.debug("...{} / {} ({}%)", downloaded, transferEvent.getResource().getContentLength(), percentComplete);
    }

    @Override
    public void transferCompleted(TransferEvent transferEvent)
    {
        logger.debug("Finished downloading [{}]", transferEvent.getResource().getName());
    }

    @Override
    public void transferError(TransferEvent transferEvent)
    {
        logger.debug("Failed to download [{}]", transferEvent.getResource().getName(), transferEvent.getException());
    }
}
