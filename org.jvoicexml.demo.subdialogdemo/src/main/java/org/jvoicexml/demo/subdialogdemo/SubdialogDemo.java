/*
 * JVoiceXML Demo - Demo for the free VoiceXML implementation JVoiceXML
 *
 * Copyright (C) 2010-2021 JVoiceXML group - http://jvoicexml.sourceforge.net
 * The JVoiceXML group hereby disclaims all copyright interest in the
 * library `JVoiceXML' (a free VoiceXML implementation).
 * JVoiceXML group, $Date$, Dirk Schnelle-Walka, project lead
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.jvoicexml.demo.subdialogdemo;

import java.net.URI;
import java.net.URISyntaxException;

import javax.naming.NamingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvoicexml.Session;
import org.jvoicexml.client.GenericClient;
import org.jvoicexml.client.UnsupportedResourceIdentifierException;
import org.jvoicexml.event.ErrorEvent;

/**
 * Demo implementation of the subdialog tag.
 * <p>
 * Must be run with the system property
 * <code>-Djava.security.policy=${config}/jvoicexml.policy</code> and the
 * <code>config</code> folder added to the classpath.
 * </p>
 * <p>
 * This demo requires that JVoiceXML is configured with the jsapi20
 * implementation platform.
 * </p>
 *
 * @author Dirk Schnelle-Walka
 * @since 0.7.4
 */
public final class SubdialogDemo {
    /** Logger for this class. */
    private static final Logger LOGGER = LogManager
            .getLogger(SubdialogDemo.class);

    /**
     * The main method.
     * 
     * @param args
     *            Command line arguments. None expected.
     */
    public static void main(final String[] args) {
        LOGGER.info("Starting 'subdialog' demo for JVoiceXML...");
        LOGGER.info("(c) 2010-2021 by JVoiceXML group - "
                + "http://jvoicexml.sourceforge.net/");
        final GenericClient client = new GenericClient();
        try {
            final URI dialog = SubdialogDemo.class
                    .getResource("/invokeSubdialog.vxml").toURI();
            final Session session = client.call(dialog, "jsapi20", "jsapi20",
                    "desktop");
            session.waitSessionEnd();
            session.hangup();
        } catch (NamingException e) {
            LOGGER.fatal(e.getMessage(), e);
            return;
        } catch (ErrorEvent e) {
            LOGGER.fatal(e.getMessage(), e);
            return;
        } catch (UnsupportedResourceIdentifierException e) {
            LOGGER.fatal(e.getMessage(), e);
            return;
        } catch (URISyntaxException e) {
            LOGGER.fatal(e.getMessage(), e);
            return;
        } finally {
            client.close();
        }
    }

}
