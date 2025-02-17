/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2019 JVoiceXML group - http://jvoicexml.sourceforge.net
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
package org.jvoicexml;

import java.util.Objects;
import java.util.UUID;

/**
 * A session identifier that is based on a random {@link UUID}.
 * @author Dirk Schnelle-Walka
 * @since 0.7.9
 */
public class UuidSessionIdentifier implements SessionIdentifier {
    /** The serial version UID. */
    private static final long serialVersionUID = -7232508857573720831L;
    
    /** The session identifier */
    private final UUID uuid;
    
    /**
     * Constructs a new object.
     */
    public UuidSessionIdentifier() {
        uuid = UUID.randomUUID();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return uuid.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder();
        str.append(UuidSessionIdentifier.class.getCanonicalName());
        str.append('[');
        str.append(getId());
        str.append(']');
        return str.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UuidSessionIdentifier other = (UuidSessionIdentifier) obj;
        return Objects.equals(uuid, other.uuid);
    }
}
