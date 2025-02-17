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

package org.jvoicexml;

import java.util.Collection;

import org.jvoicexml.event.error.BadFetchError;
import org.jvoicexml.event.error.NoauthorizationError;
import org.jvoicexml.event.error.NoresourceError;
import org.jvoicexml.event.error.SemanticError;
import org.jvoicexml.event.error.UnsupportedFormatError;
import org.jvoicexml.event.error.UnsupportedLanguageError;
import org.jvoicexml.interpreter.datamodel.DataModel;
import org.jvoicexml.xml.srgs.GrammarType;
import org.jvoicexml.xml.srgs.ModeType;
import org.jvoicexml.xml.vxml.BargeInType;

/**
 * Facade for easy control and monitoring of the user's input.
 *
 * <p>
 * Objects that implement this interface are able to detect and report character
 * and/or spoken input simultaneously and to control input detection interval
 * duration with a timer whose length is specified by a VoiceXML document.
 * </p>
 *
 * <p>
 * If an input resource is not available, an <code>error.noresource</code> event
 * must be thrown.
 * </p>
 *
 * @author Dirk Schnelle-Walka
 */
public interface UserInput {
    /**
     * In case the user input supports {@link ModeType#VOICE} the input
     * must return the {@link SpeechRecognizerProperties} to use.
     * @return new instance of a {@link SpeechRecognizerProperties} or
     * {@code null} if this is not supported. 
     * @since 0.7.9
     */
    SpeechRecognizerProperties createSpeechRecognizerProperties();
    
    /**
     * In case the user input supports {@link ModeType#DTMF} the input
     * must return the {@link DtmfRecognizerProperties} to use.
     * @return new instance of a {@link DtmfRecognizerProperties} or
     * {@code null} if this is not supported. 
     * @since 0.7.9
     */
    DtmfRecognizerProperties createDtmfRecognizerProperties();
    
    /**
     * Detects and reports character and/or spoken input simultaneously.
     *
     * @param model
     *            the data model for generating a semantic interpretation
     * @param types
     *            the recognizer types to activate
     * @param speech
     *            the speech recognizer properties to use
     * @param dtmf
     *            the DTMF recognizer properties to use
     * @exception NoresourceError
     *                The input resource is not available.
     * @exception BadFetchError
     *                The active grammar contains some errors.
     */
    void startRecognition(DataModel model, Collection<ModeType> types,
            SpeechRecognizerProperties speech, DtmfRecognizerProperties dtmf)
            throws NoresourceError, BadFetchError;

    /**
     * Stops a previously started recognition.
     * 
     * @param types
     *            the recognizer types to activate, {@code null} would stop all
     *            recognizers
     *
     * @see #startRecognition
     */
    void stopRecognition(Collection<ModeType> types);

    /**
     * Retrieves the grammar types that are supported by this implementation for
     * the given mode.
     * <p>
     * It is guaranteed that the implementation is only asked to activate (
     * {@link #activateGrammars(Collection)}) and deactivate (
     * {@link UserInput#deactivateGrammars(Collection)}) grammars who's format
     * is returned by this method.
     * </p>
     *
     * @return supported grammars.
     * @param mode
     *            grammar mode
     *
     * @since 0.5.5
     */
    Collection<GrammarType> getSupportedGrammarTypes(ModeType mode);

    /**
     * Activates the given grammars. It is guaranteed that all grammars types
     * are supported by this implementation. The supported grammar types are
     * retrieved from {@link #getSupportedGrammarTypes(ModeType)}.
     *
     * <p>
     * {@link org.jvoicexml.implementation.GrammarImplementation}s may be
     * cached. This means that a grammar implementation object is loaded either
     * by this or by another instance of the {@link UserInput}. For some
     * implementation platforms it may be necessary that the instance activating
     * the grammar also loaded the grammar. In these cases, the grammar
     * implementation must be loaded in this call. The grammar source may be
     * accessed by the grammar implementation itself, e.g. SRGS grammar sources
     * can be accessed via
     * {@link org.jvoicexml.implementation.SrgsXmlGrammarImplementation#getGrammarDocument()}
     * .
     * </p>
     *
     * @param grammars
     *            grammars to activate. This collection may contain grammars
     *            that already have been activated by a previous call to this
     *            method. It is up to the implementation to distinguish if the
     *            grammar is already active or not.
     * @return number of activated grammars
     * @exception BadFetchError
     *                Grammar is not known by the recognizer.
     * @exception UnsupportedLanguageError
     *                The specified language is not supported.
     * @exception NoresourceError
     *                The input resource is not available.
     * @exception UnsupportedFormatError
     *                the grammar format is not supported
     * @exception SemanticError
     *                semantic error in the grammar file
     * @exception NoauthorizationError
     *                 the grammar could not be loaded because of security constraints
     */
    int activateGrammars(Collection<GrammarDocument> grammars)
            throws BadFetchError, UnsupportedLanguageError, NoresourceError,
            UnsupportedFormatError, SemanticError, NoauthorizationError;

    /**
     * Deactivates the given grammars. Do nothing if the input resource is not
     * available. It is guaranteed that all grammars types are supported by this
     * implementation.
     *
     * @param grammars
     *            Grammars to deactivate.
     * @return number of deactivated grammars
     * @exception BadFetchError
     *                Grammar is not known by the recognizer.
     * @exception NoresourceError
     *                The input resource is not available.
     */
    int deactivateGrammars(Collection<GrammarDocument> grammars)
            throws NoresourceError, BadFetchError;

    /**
     * Retrieves the barge-in types supported by this <code>UserInput</code>.
     * 
     * @return Collection of supported barge-in types, an empty collection, if
     *         no types are supported.
     */
    Collection<BargeInType> getSupportedBargeInTypes();
}
