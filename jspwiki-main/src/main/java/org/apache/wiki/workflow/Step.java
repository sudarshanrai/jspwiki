/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.workflow;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.WikiException;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * Discrete unit of work in a Workflow, such as a {@link Decision} or a {@link Task}. Decisions require user input, while Tasks do not.
 * All Steps, however, possess these properties:
 * </p>
 * <ul>
 * <li><strong>actor</strong>: the Principal responsible for executing the Step; returned by {@link Step#getActor()}.</li>
 * <li><strong>availableOutcomes</strong>: a collection of possible "outcomes," such as "approve decision" ({@link Outcome#DECISION_APPROVE}),
 * "reassign decision" ({@link Outcome#DECISION_REASSIGN}), "abort step" ({@link Outcome#STEP_ABORT}) and others. The range of possible
 * Outcomes for the Step is returned by {@link Step#getAvailableOutcomes()}; see the Outcome class for more details.</li>
 * <li><strong>errors</strong>: an collection of Strings indicating errors returned by the Step. These values are returned by
 * {@link Step#getErrors()}.</li>
 * <li><strong>started</strong> and <strong>completed</strong>: whether the Step has started/finished. These values are returned by
 * {@link Step#isStarted()} and {@link Step#isCompleted()}.</li>
 * <li><strong>startTime</strong> and <strong>endTime</strong>: the time when the Step started and finished. These values are returned by
 * {@link Step#getStartTime()} and {@link Step#getEndTime()}, respectively.</li>
 * <li><strong>workflow</strong>: the parent Workflow. </li>
 * </ul>
 * <p>
 * Steps contain a {@link #getMessageKey()} method that returns a key that can be used with the {@link org.apache.wiki.i18n.InternationalizationManager}.
 * See also {@link Workflow#getMessageArguments()}, which is a convenience method that returns message arguments.
 * </p>
 * 
 * @since 2.5
 */
public interface Step extends Serializable {

    /** Time value: the start or end time has not been set. */
    Date TIME_NOT_SET = new Date( 0 );

    /**
     * Adds a successor Step to this one, which will be triggered by a supplied Outcome. Implementations should respect the order in which
     * Outcomes are added; {@link #getAvailableOutcomes()} should return them in the same order they were added.
     * 
     * @param outcome the Outcome triggering a particular successor Step
     * @param step the Step to associated with this Outcomes (<code>null</code> denotes no Steps)
     */
    void addSuccessor( Outcome outcome, Step step );

    /**
     * Returns a Collection of available outcomes, such as "approve", "deny" or "reassign", in the order in which they were added via
     * {@link #addSuccessor(Outcome, Step)}. Concrete implementations should always return a defensive copy of the outcomes, not the
     * original backing collection.
     * 
     * @return the set of outcomes
     */
    Collection< Outcome > getAvailableOutcomes();

    /**
     * Returns a List of error strings generated by this Step. If this Step generated no errors, this method returns a zero-length array.
     * 
     * @return the errors
     */
    List< String > getErrors();

    /**
     * <p>
     * Executes the processing for this Step and returns an Outcome indicating if it succeeded ({@link Outcome#STEP_COMPLETE} or
     * {@link Outcome#STEP_ABORT}). Processing instructions can do just about anything, such as executing custom business logic or
     * changing the Step's final outcome via {@link #setOutcome(Outcome)}. A return value of <code>STEP_COMPLETE</code> indicates
     * that the instructions executed completely, without errors; <code>STEP_ABORT</code> indicates that the Step and its parent
     * Workflow should be aborted (that is, fail silently without error). If the execution step encounters any errors, it should throw a
     * WikiException or a subclass.
     * </p>
     * <p>
     * Note that successful execution of this methods does not necessarily mean that the Step is considered "complete"; rather, it just
     * means that it has executed. Therefore, it is possible that <code>execute</code> could run multiple times.
     * </p>
     *
     * @param ctx executing wiki context.
     * @return the result of the Step, expressed as an Outcome
     * @throws WikiException if the step encounters errors while executing
     */
    Outcome execute( Context ctx ) throws WikiException;

    /**
     * The Principal responsible for completing this Step, such as a system user or actor assigned to a Decision.
     * 
     * @return the responsible Principal
     */
    Principal getActor();

    /**
     * The end time for this Step. This value should be set when the step completes. Returns {@link #TIME_NOT_SET} if not completed
     * yet.
     * 
     * @return the end time
     */
    Date getEndTime();

    /**
     * Message key for human-friendly name of this Step, including any parameter substitutions. By convention, the message prefix should be
     * a lower-case version of the Step's type, plus a period (<em>e.g.</em>, <code>task.</code> and <code>decision.</code>).
     * 
     * @return the message key for this Step.
     */
    String getMessageKey();

    /**
     * Returns the Outcome of this Step's processing; by default, {@link Outcome#STEP_CONTINUE}.
     * 
     * @return the outcome
     */
    Outcome getOutcome();

    /**
     * The start time for this Step. Returns {@link #TIME_NOT_SET} if not started yet.
     * 
     * @return the start time
     */
    Date getStartTime();

    /**
     * Determines whether the Step is completed; if not, it is by definition awaiting action by the owner or in process. If a Step has
     * completed, it <em>must also</em> return a non-<code>null</code> result for {@link #getOutcome()}.
     * 
     * @return <code>true</code> if the Step has completed; <code>false</code> if not.
     */
    boolean isCompleted();

    /**
     * Determines whether the Step has started.
     * 
     * @return <code>true</code> if the Step has started; <code>false</code> if not.
     */
    boolean isStarted();

    /**
     * Starts the Step, and sets the start time to the moment when this method is first invoked. If this Step has already been started,
     * this method throws an {@linkplain IllegalStateException}. If the Step cannot be started because the underlying implementation
     * encounters an error, it the implementation should throw a WikiException.
     * 
     * @throws WikiException if the step encounters errors while starting
     */
    void start() throws WikiException;

    /**
     * Sets the current Outcome for the step. If the Outcome is a "completion" Outcome, it should also sets the completon time and mark the
     * Step as complete. Once a Step has been marked complete, this method cannot be called again. If the supplied Outcome is not in the
     * set returned by {@link #getAvailableOutcomes()}, or is not  {@link Outcome#STEP_CONTINUE} or {@link Outcome#STEP_ABORT}, this method
     * returns an IllegalArgumentException. If the caller attempts to set an Outcome and the Step has already completed, this method throws
     * an IllegalStateException.
     * 
     * @param outcome whether the step should be considered completed
     */
    void setOutcome( Outcome outcome );

    /**
     * Identifies the next Step for a particular Outcome; if there is no next Step for this Outcome, this method returns <code>null</code>.
     * 
     * @param outcome the outcome
     * @return the next step
     */
    Step getSuccessor( Outcome outcome );

    /**
     * Sets the parent Workflow post-construction. Should be called after building a {@link Step}.
     *
     * @param workflowId the parent workflow id to set
     * @param workflowContext the parent workflow context to set
     */
    void setWorkflow( final int workflowId, final Map< String, Serializable > workflowContext );

}
