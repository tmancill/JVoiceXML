/*
 * JVoiceXML - A free VoiceXML implementation.
 *
 * Copyright (C) 2005-2021 JVoiceXML group - http://jvoicexml.sourceforge.net
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvoicexml.RecognitionResult;
import org.jvoicexml.event.EventBus;
import org.jvoicexml.event.JVoiceXMLEvent;
import org.jvoicexml.event.error.SemanticError;
import org.jvoicexml.event.plain.CancelEvent;
import org.jvoicexml.event.plain.HelpEvent;
import org.jvoicexml.event.plain.implementation.NomatchEvent;
import org.jvoicexml.event.plain.implementation.RecognitionEvent;
import org.jvoicexml.interpreter.CatchContainer;
import org.jvoicexml.interpreter.Dialog;
import org.jvoicexml.interpreter.EventCountable;
import org.jvoicexml.interpreter.EventHandler;
import org.jvoicexml.interpreter.EventStrategy;
import org.jvoicexml.interpreter.FormInterpretationAlgorithm;
import org.jvoicexml.interpreter.FormItem;
import org.jvoicexml.interpreter.VoiceXmlInterpreter;
import org.jvoicexml.interpreter.VoiceXmlInterpreterContext;
import org.jvoicexml.interpreter.datamodel.DataModel;
import org.jvoicexml.interpreter.formitem.InitialFormItem;
import org.jvoicexml.interpreter.scope.Scope;
import org.jvoicexml.interpreter.scope.ScopeObserver;
import org.jvoicexml.interpreter.scope.ScopedCollection;
import org.jvoicexml.interpreter.scope.ScopedCollectionListener;
import org.jvoicexml.xml.TokenList;
import org.jvoicexml.xml.vxml.AbstractCatchElement;
import org.jvoicexml.xml.vxml.Filled;
import org.jvoicexml.xml.vxml.Help;
import org.jvoicexml.xml.vxml.Noinput;
import org.jvoicexml.xml.vxml.Nomatch;
import org.jvoicexml.xml.vxml.VoiceXmlDocument;
import org.jvoicexml.xml.vxml.Vxml;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Event handler to catch events generated from the
 * {@link org.jvoicexml.ImplementationPlatform} via the
 * {@link org.jvoicexml.event.EventBus}. This event handler can catch only one
 * event at a time. The first event is propagated to the the FIA while
 * subsequent events will be ignored.
 * 
 * @author Dirk Schnelle-Walka
 * @see org.jvoicexml.ImplementationPlatform
 */
public final class JVoiceXmlEventHandler
    implements EventHandler, ScopedCollectionListener<EventStrategy> {
    /** Logger for this class. */
    private static final Logger LOGGER = LogManager
            .getLogger(JVoiceXmlEventHandler.class);

    /** Input item strategy factory. */
    private final EventStrategyDecoratorFactory inputItemFactory;

    /** Event filter chain to determine the relevant event strategy. */
    private final Collection<EventFilter> filters;

    /**
     * Event filter chain to determine the relevant event strategy if no input
     * item is given.
     */
    private final Collection<EventFilter> filtersNoinput;

    /**
     * The strategies to execute, if the corresponding event type occurred.
     */
    private final ScopedCollection<EventStrategy> strategies;

    /** Semaphore to handle the wait/notify mechanism. */
    private final Object semaphore;

    /** The employed data model. */
    private final DataModel model;

    /** The event bus that transports events. */
    private final EventBus eventbus;
    
    /** The caught event. */
    private final Queue<JVoiceXMLEvent> events;

    /**
     * Construct a new object.
     *
     * @param dataModel
     *            the emplyoed data model
     * @param observer
     *            the scope observer.
     * @param bus
     *            the used event bus
     */
    public JVoiceXmlEventHandler(final DataModel dataModel,
            final ScopeObserver observer, final EventBus bus) {
        strategies = new ScopedCollection<EventStrategy>(observer);
        strategies.addScopedCollectionListener(this);
        inputItemFactory = new EventStrategyDecoratorFactory();
        semaphore = new Object();
        filters = new java.util.ArrayList<EventFilter>();
        filters.add(new EventTypeFilter());
        filters.add(new ConditionEventTypeFilter());
        filters.add(new EventCountTypeFilter());
        filters.add(new HighestCountEventTypeFilter());
        filtersNoinput = new java.util.ArrayList<EventFilter>();
        filtersNoinput.add(new EventTypeFilter());
        model = dataModel;
        eventbus = bus;
        events = new java.util.LinkedList<JVoiceXMLEvent>();
        
        final HangupEventStrategy hangupEventStrategy = new HangupEventStrategy();
        addStrategy(hangupEventStrategy);
    }

    /**
     * Retrieves the strategies to execute.
     * 
     * @return the strategies to execute.
     */
    Collection<EventStrategy> getStrategies() {
        return strategies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(final VoiceXmlInterpreterContext context,
            final VoiceXmlInterpreter interpreter,
            final VoiceXmlDocument document) {
        final Vxml vxml = document.getVxml();
        final Collection<AbstractCatchElement> catches =
                new java.util.ArrayList<AbstractCatchElement>();
        final NodeList children = vxml.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child instanceof AbstractCatchElement) {
                final AbstractCatchElement catchElement =
                        (AbstractCatchElement) child;
                catches.add(catchElement);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("found " + catches.size()
                    + " catch element(s) in document");
        }

        // Transform them into event handlers.
        final FormInterpretationAlgorithm fia;
        if (interpreter == null) {
            fia = null;
        } else {
            fia = interpreter.getFormInterpretationAlgorithm();
        }
        addStrategiesForCatches(context, interpreter, fia, null, catches);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(final VoiceXmlInterpreterContext context,
            final VoiceXmlInterpreter interpreter, final Dialog dialog) {
        // Retrieve the specified catch elements.
        final Collection<AbstractCatchElement> catches = dialog
                .getCatchElements();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("found " + catches.size()
                    + " catch element(s) in dialog '" + dialog.getId() + "'");
        }

        // Transform them into event handlers.
        final FormInterpretationAlgorithm fia = interpreter
                .getFormInterpretationAlgorithm();
        addStrategiesForCatches(context, interpreter, fia, null, catches);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<EventStrategy> collect(
            final VoiceXmlInterpreterContext context,
            final VoiceXmlInterpreter interpreter,
            final FormInterpretationAlgorithm fia, final CatchContainer item) {
        final Collection<EventStrategy> added =
                new java.util.ArrayList<EventStrategy>();
        // Retrieve the specified catch elements.
        final Collection<AbstractCatchElement> catches = item
                .getCatchElements();

        final Collection<AbstractCatchElement> filteredCatches =
                filterItemCatches(item, catches);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("found " + filteredCatches.size()
                    + " catch element(s) in item '" + item.getName() + "'");
        }
        
        // Transform them into event handlers.
        addStrategiesForCatches(context, interpreter, fia, item,
                filteredCatches);

        // Add the default strategies for input items.
        final Collection<EventStrategy> defaultStrategies =
                addDefaultStrategies(context, interpreter, fia, item);
        added.addAll(defaultStrategies);

        // Add an input item strategy
        final EventStrategy itemStrategy = inputItemFactory.getDecorator(
                context, interpreter, fia, item);
        boolean add = addStrategy(itemStrategy);
        if (add) {
            added.add(itemStrategy);
        }
        return added;
    }

    /**
     * Filters the list of catches for valid entries
     * @param item the curren tform item
     * @param catches list of catches under review
     * @return valid catches
     * @since 0.7.9
     */
    private Collection<AbstractCatchElement> filterItemCatches(
            final CatchContainer item,
            final Collection<AbstractCatchElement> catches) {
        final Collection<AbstractCatchElement> filtered =
                new java.util.ArrayList<AbstractCatchElement>();
        for (AbstractCatchElement catchElement : catches) {
            final TokenList events = catchElement.getEventList();
            for (String eventType : events) {
                if (eventType.equals(Filled.TAG_NAME)
                        && (item instanceof InitialFormItem)) {
                    // TODO The spec does not tell what to do in this case,
                    // so we simply ignore it.
                    LOGGER.warn("Initial form items must not have catches for "
                            + "filled: ignoring...");
                } else {
                    filtered.add(catchElement);
                }
            }
        }
        return filtered;
    }
    
    /**
     * Adds {@link EventStrategy}s for the provided list of catches.
     * @param context the current VoiceXMLInterpreterContext
     * @param interpreter the current VoiceXMLInterpreter
     * @param fia the current FIA
     * @param item the current item
     * @param catches catches from which to create {@link EventStrategy}s
     * @since 0.7.9
     */
    private void addStrategiesForCatches(
            final VoiceXmlInterpreterContext context,
            final VoiceXmlInterpreter interpreter,
            final FormInterpretationAlgorithm fia,
            final FormItem item,
            final Collection<AbstractCatchElement> catches) {
        for (AbstractCatchElement catchElement : catches) {
            final TokenList events = catchElement.getEventList();
            if (events.isEmpty()) {
                final EventStrategy strategy = new CatchEventStrategy(context,
                        interpreter, fia, item, catchElement,
                        "org.jvoicexml.event");
                addStrategy(strategy);
            } else {
                for (String eventType : events) {
                    final EventStrategy strategy = new CatchEventStrategy(context,
                            interpreter, fia, item, catchElement, eventType);
                    addStrategy(strategy);
                }
            }
        }
    }

    
    /**
     * Adds the missing event handlers that are defined by default.
     * <p>
     * The default event handlers are specified at <a
     * href="http://www.w3.org/TR/2004/REC-voicexml20-20040316#dml5.2.5">
     * http://www.w3.org/TR/2004/REC-voicexml20-20040316#dml5.2.5</a>
     * </p>
     * 
     * @param context
     *            The current <code>VoiceXmlInterpreterContext</code>
     * @param interpreter
     *            The current <code>VoiceXmlInterpreter</code>
     * @param fia
     *            The <code>FormInterpretationAlgorithm</code>
     * @param item
     *            The visited input item.
     * @since 0.7
     * @return added strategies
     */
    private Collection<EventStrategy> addDefaultStrategies(
            final VoiceXmlInterpreterContext context,
            final VoiceXmlInterpreter interpreter,
            final FormInterpretationAlgorithm fia, final CatchContainer item) {
        final Collection<EventStrategy> added =
                new java.util.ArrayList<EventStrategy>();
        if (!containsStrategy(Noinput.TAG_NAME)) {
            final EventStrategy strategy = new DefaultRepromptEventStrategy(
                    context, interpreter, fia, item, Noinput.TAG_NAME);
            final boolean add = addStrategy(strategy);
            if (add) {
                added.add(strategy);
            }
        }
        if (!containsStrategy(Nomatch.TAG_NAME)) {
            final EventStrategy strategy = new DefaultRepromptEventStrategy(
                    context, interpreter, fia, item, Nomatch.TAG_NAME);
            final boolean add = addStrategy(strategy);
            if (add) {
                added.add(strategy);
            }
        }
        if (!containsStrategy(Help.TAG_NAME)) {
            final EventStrategy strategy = new DefaultRepromptEventStrategy(
                    context, interpreter, fia, item, Help.TAG_NAME);
            final boolean add = addStrategy(strategy);
            if (add) {
                added.add(strategy);
            }
        }
        if (!containsStrategy("cancel")) {
            final EventStrategy strategy = new DefaultCancelEventStrategy(
                    context, interpreter, fia, item, "cancel");
            final boolean add = addStrategy(strategy);
            if (add) {
                added.add(strategy);
            }
        }
        return added;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addStrategy(final EventStrategy strategy) {
        if (strategy == null) {
            LOGGER.debug("can not add a null strategy");
            return false;
        }
        
        final String type = strategy.getEventType();

        if (strategies.contains(strategy)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("strategy: '" + strategy.getClass()
                        + "' for event type '" + type + "'"
                        + " ignored since it is already registered");
            }
            return false;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("adding strategy: '" + strategy.getClass()
                    + "' for event type '" + type + "'");
        }

        eventbus.subscribe(type, this);
        
        return strategies.add(strategy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clean(final FormItem item) {
        final Collection<EventStrategy> toremove =
                new java.util.ArrayList<EventStrategy>();
        for (EventStrategy strategy : strategies) {
            if (strategy instanceof AbstractEventStrategy) {
                final AbstractEventStrategy eventStrategy =
                        (AbstractEventStrategy) strategy;
                final FormItem strategyItem = eventStrategy.getFormItem();
                if (item == strategyItem) {
                    toremove.add(eventStrategy);
                }
            }
        }
        strategies.removeAll(toremove);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("removed " + toremove.size()
                    + " event strategies for form item '" + item.getName()
                    + "'");
        }
    }

    /**
     * Retrieves the first {@link EventStrategy} with the given type.
     * 
     * @param type
     *            event type to look for.
     * @return found strategy, <code>null</code> if no strategy was found.
     */
    private EventStrategy getStrategy(final String type) {
        for (EventStrategy strategy : strategies) {
            final String currentType = strategy.getEventType();
            if (currentType.equals(type)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * Checks if there exists an {@link EventStrategy} for the given type.
     * 
     * @param type
     *            event type to look for.
     * @return <code>true</code> if there is a strategy.
     * @since 0.7
     */
    private boolean containsStrategy(final String type) {
        final EventStrategy strategy = getStrategy(type);
        return strategy != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JVoiceXMLEvent waitEvent() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("waiting for an event...");
        }

        while (events.isEmpty()) {
            try {
                synchronized (semaphore) {
                    if (events.isEmpty()) {
                        semaphore.wait();
                    }
                }
            } catch (InterruptedException ie) {
                LOGGER.error("wait event was interrupted", ie);
                return null;
            }
        }

        JVoiceXMLEvent event;
        try {
            final JVoiceXMLEvent queuedEvent;
            synchronized (semaphore) {
                queuedEvent = events.poll();
            }
            event = transformEvent(queuedEvent);
        } catch (SemanticError e) {
            LOGGER.warn("unable to transform event", e);
            event = e;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event: " + event);
        }
        return event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JVoiceXMLEvent checkEvent() {
        synchronized (semaphore) {
            if (events.isEmpty()) {
                return null;
            }
        }

        JVoiceXMLEvent event;
        try {
            final JVoiceXMLEvent queuedEvent;
            synchronized (semaphore) {
                queuedEvent = events.poll();
            }
            event = transformEvent(queuedEvent);
        } catch (SemanticError e) {
            LOGGER.warn("unable to transform event", e);
            event = e;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event: " + event);
        }
        return event;
    }    
    /**
     * {@inheritDoc} The relevant {@link EventStrategy} is determined via a
     * chaining of {@link EventFilter}s.
     */
    @Override
    public void processEvent(final CatchContainer item, JVoiceXMLEvent event)
            throws JVoiceXMLEvent {
        if (event == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("no event: nothing to do");
            }
            return;
        }

        if (item instanceof EventCountable) {
            final EventCountable countable = (EventCountable) item;
            countable.incrementEventCounter(event);
        }

        final String type = event.getEventType();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("processing event of type '" + type + "'...");
        }

        final Collection<EventFilter> eventFilters;
        final Collection<EventStrategy> matchingStrategies =
                new java.util.ArrayList<EventStrategy>(
                strategies);
        if (item == null) {
            eventFilters = filtersNoinput;
        } else {
            eventFilters = filters;
        }

        // Filter the matching strategies.
        for (EventFilter filter : eventFilters) {
            filter.filter(matchingStrategies, event, item);
            if (matchingStrategies.isEmpty()) {
                LOGGER.info("no matching strategy for type '" + type + "'");

                final JVoiceXMLEvent copy = event;
                event = null;

                throw copy;
            }
        }

        // Select the first remaining matching strategy.
        final Iterator<EventStrategy> iterator = matchingStrategies.iterator();
        final EventStrategy strategy = iterator.next();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("processing event '" + type + "' with " + strategy);
        }
        try {
            strategy.process(event);
        } catch (NomatchEvent e) {
            // If the result was not accepted, we may receive a nomatch event.
            // Hence, we have to redo the whole stuff to get the relevant
            // nomatch strategy.
            processEvent(item, e);
        } finally {
            // Be prepared that an event is thrown while processing the current
            // event,
            event = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearEvents() {
        synchronized (semaphore) {
            events.clear();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Receive the event from the {@link org.jvoicexml.event.EventBus} and
     * handle form interpretation.
     */
    @Override
    public synchronized void onEvent(final JVoiceXMLEvent event) {
        if (event == null) {
            return;
        }
        final String type = event.getEventType();
        synchronized (semaphore) {
            try {
                events.offer(event);
                LOGGER.info("notified event '" + type + "'");
            } finally {
                semaphore.notify();
            }
        }
    }

    /**
     * Transforms the given event into another event by evaluating a possibly
     * present semantic interpretation. For instance, help and cancel requests
     * by the user must be transformed into {@link HelpEvent}s and
     * {@link CancelEvent}.
     * 
     * @param e
     *            the source event
     * @return the transformed event, <code>e</code> if there was no
     *         transformation
     * @exception SemanticError
     *                error evaluating the semantic interpretation
     * @since 0.7.4
     */
    private JVoiceXMLEvent transformEvent(final JVoiceXMLEvent e)
            throws SemanticError {
        if (!(e instanceof RecognitionEvent)) {
            return e;
        }
        final RecognitionEvent recevent = (RecognitionEvent) e;
        final RecognitionResult result = recevent.getRecognitionResult();
        final Object interpretation = result.getSemanticInterpretation(model);
        if (interpretation == null) {
            return e;
        }
        if (interpretation.equals("help")) {
            LOGGER.info("sematic interpretation of the recognition "
                    + "result is a help request");
            return new HelpEvent();
        } else if (interpretation.equals("cancel")) {
            LOGGER.info("sematic interpretation of the recognition "
                    + "result is a cancel request");
            return new CancelEvent();
        }
        return e;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeStrategies(final Collection<EventStrategy> strats) {
        if (strats == null) {
            return false;
        }
        boolean removed = strategies.removeAll(strats);
        if (removed) {
            maybeUnsubscribeFromEventBus(strats);
        }
        return removed;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void removedForScopeChange(Scope previous, Scope next,
            Collection<EventStrategy> items) {
        maybeUnsubscribeFromEventBus(items);
    }
    
    /**
     * Remove subscriptions to the events of the removed strategies if
     * there are no other strategies on the scope stack
     * @param items the strategies that were removed
     * @since 0.7.9
     */
    private void maybeUnsubscribeFromEventBus(
            final Collection<EventStrategy> items) {
        for (EventStrategy strategy : items) {
            final String type = strategy.getEventType();
            final EventStrategy otherStratey = getStrategy(type);
            if (otherStratey == null) {
                eventbus.unsubscribe(type, this);
            }
        }
    }
}
