/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2005-2017 JVoiceXML group - http://jvoicexml.sourceforge.net
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

package org.jvoicexml.interpreter.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvoicexml.RecognitionResult;
import org.jvoicexml.event.JVoiceXMLEvent;
import org.jvoicexml.event.error.SemanticError;
import org.jvoicexml.event.plain.implementation.NomatchEvent;
import org.jvoicexml.interpreter.FormInterpretationAlgorithm;
import org.jvoicexml.interpreter.FormItem;
import org.jvoicexml.interpreter.VoiceXmlInterpreter;
import org.jvoicexml.interpreter.VoiceXmlInterpreterContext;
import org.jvoicexml.interpreter.datamodel.DataModel;
import org.jvoicexml.interpreter.scope.Scope;
import org.jvoicexml.profile.TagStrategyExecutor;
import org.jvoicexml.xml.VoiceXmlNode;

/**
 * Strategy to execute a user defined catch node.
 *
 * @author Dirk Schnelle-Walka
 *
 * @see org.jvoicexml.ImplementationPlatform
 * @see org.jvoicexml.xml.vxml.AbstractCatchElement
 */
final class CatchEventStrategy extends AbstractEventStrategy {
    /** Logger for this class. */
    private static final Logger LOGGER = LogManager
            .getLogger(CatchEventStrategy.class);

    /**
     * Constructs a new object.
     */
    CatchEventStrategy() {
    }

    /**
     * Constructs a new object.
     *
     * @param ctx
     *            the VoiceXML interpreter context.
     * @param ip
     *            the VoiceXML interpreter.
     * @param interpreter
     *            the FIA.
     * @param formItem
     *            the current form item.
     * @param node
     *            the node to execute.
     * @param type
     *            the event type.
     */
    CatchEventStrategy(final VoiceXmlInterpreterContext ctx,
            final VoiceXmlInterpreter ip,
            final FormInterpretationAlgorithm interpreter,
            final FormItem formItem, final VoiceXmlNode node,
            final String type) {
        super(ctx, ip, interpreter, formItem, node, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final JVoiceXMLEvent event) throws JVoiceXMLEvent {
        final FormInterpretationAlgorithm fia =
                getFormInterpretationAlgorithm();
        if (fia == null) {
            LOGGER.warn("Unable to process event '" + event.getEventType()
                    + "' No reference to a FIA!");
            return;
        }
        
        // First, maybe set the application last result values
        if (event instanceof NomatchEvent) {
            final NomatchEvent nomatch = (NomatchEvent) event;
            final RecognitionResult result = nomatch.getRecognitionResult();
            setApplicationLastResult(result);
        }        
        final VoiceXmlInterpreterContext context =
                getVoiceXmlInterpreterContext();
        context.enterScope(Scope.ANONYMOUS);

        // Declare the special variable _event which contains the name of the
        // event that was thrown.
        final DataModel model = context.getDataModel();
        final String name = event.getEventType();
        model.createVariable("_event", name);

        final VoiceXmlInterpreter interpreter = getVoiceXmlInterpreter();
        final TagStrategyExecutor executor = getTagStrategyExecutor();

        try {
            final FormItem item = getCurrentFormItem();
            final VoiceXmlNode node = getVoiceXmlNode();
            executor.executeChildNodes(context, interpreter, fia, item, node);
        } finally {
            context.exitScope(Scope.ANONYMOUS);
        }
    }
    
    /**
     * Sets the result in the application shadow variable.
     * 
     * @param result
     *            the current recognition result.
     * @throws SemanticError
     *             Error creating the shadow variable.
     * @since 0.7.8
     */
    private void setApplicationLastResult(final RecognitionResult result)
            throws SemanticError {
        final VoiceXmlInterpreterContext context =
                getVoiceXmlInterpreterContext();
        final DataModel model = context.getDataModel();
        model.resizeArray("lastresult$", 1, Scope.APPLICATION);
        final Object value = model.readArray("lastresult$", 0,
                Scope.APPLICATION, Object.class);
        model.createVariableFor(value, "confidence", result.getConfidence());
        model.createVariableFor(value, "utterance", result.getUtterance());
        model.createVariableFor(value, "inputmode", result.getMode().name());
        model.createVariableFor(value, "interpretation",
                result.getSemanticInterpretation(model));
        model.updateArray("lastresult$", 0, value, Scope.APPLICATION);
        model.createVariable("lastresult$.interpretation",
                result.getSemanticInterpretation(model), Scope.APPLICATION);
        model.createVariable("lastresult$.confidence", result.getConfidence(),
                Scope.APPLICATION);
        model.createVariable("lastresult$.utterance", result.getUtterance(),
                Scope.APPLICATION);
        model.createVariable("lastresult$.inputmode", result.getMode().name(),
                Scope.APPLICATION);
    }
    
}
