/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2005-2017 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.jvoicexml.profile.vxml21.tagstrategy;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvoicexml.event.ErrorEvent;
import org.jvoicexml.event.GenericVoiceXmlEvent;
import org.jvoicexml.event.JVoiceXMLEvent;
import org.jvoicexml.interpreter.FormInterpretationAlgorithm;
import org.jvoicexml.interpreter.FormItem;
import org.jvoicexml.interpreter.VoiceXmlInterpreter;
import org.jvoicexml.interpreter.VoiceXmlInterpreterContext;
import org.jvoicexml.interpreter.datamodel.DataModel;
import org.jvoicexml.xml.VoiceXmlNode;
import org.jvoicexml.xml.vxml.Throw;

/**
 * Strategy of the FIA to execute a <code>&lt;throw&gt;</code> node.
 *
 * @see org.jvoicexml.interpreter.FormInterpretationAlgorithm
 * @see org.jvoicexml.xml.vxml.Throw
 *
 * @author Dirk Schnelle-Walka
 */
final class ThrowStrategy
        extends AbstractTagStrategy {
    /** Logger for this class. */
    private static final Logger LOGGER =
            LogManager.getLogger(ThrowStrategy.class);

    /** List of attributes to be evaluated by the scripting environment. */
    private static final Collection<String> EVAL_ATTRIBUTES;

    static {
        EVAL_ATTRIBUTES = new java.util.ArrayList<String>();

        EVAL_ATTRIBUTES.add(Throw.ATTRIBUTE_EVENTEXPR);
        EVAL_ATTRIBUTES.add(Throw.ATTRIBUTE_MESSAGEEXPR);
    }

    /** The event to throw. */
    private String event;

    /** Message string for the event. */
    private String message;

    /**
     * Constructs a new object.
     */
    ThrowStrategy() {
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getEvalAttributes() {
        return EVAL_ATTRIBUTES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateAttributes(final DataModel model)
            throws ErrorEvent {
        event = (String) getAttributeWithAlternativeExpr(model, Throw.ATTRIBUTE_EVENT, Throw.ATTRIBUTE_EVENTEXPR);
        message = (String) getOptionalAttributeWithAlternativeExpr(model,
                Throw.ATTRIBUTE_MESSAGE, Throw.ATTRIBUTE_MESSAGEEXPR); 
    }

    /**
     * {@inheritDoc}
     *
     * Throw the specified event.
     */
    public void execute(final VoiceXmlInterpreterContext context,
                        final VoiceXmlInterpreter interpreter,
                        final FormInterpretationAlgorithm fia,
                        final FormItem item,
                        final VoiceXmlNode node)
            throws JVoiceXMLEvent {
        LOGGER.info("throwing event '" + event + "'");

        if (message != null) {
            throw new GenericVoiceXmlEvent(event);
        }

        throw new GenericVoiceXmlEvent(event, message);

    }
}
