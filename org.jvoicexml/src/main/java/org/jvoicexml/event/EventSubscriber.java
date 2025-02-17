/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2014-2021 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jvoicexml.event;

/**
 * A subscriber for events.
 * @author Dirk Schnelle-Walka
 * @since 0.7.7
 */
public interface EventSubscriber {
    /** A type that matches all types. */
    String ALL_TYPES = "*";

    /**
     * Notification about the given event.
     * @param event the event
     */
    void onEvent(final JVoiceXMLEvent event);
}
