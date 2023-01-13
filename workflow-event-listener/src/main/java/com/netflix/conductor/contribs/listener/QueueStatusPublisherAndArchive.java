/*
 * Copyright 2023 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.contribs.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.contribs.queue.amqp.AMQPObservableQueue;
import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.core.events.EventQueues;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.listener.WorkflowStatusListener;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.metrics.Monitors;
import com.netflix.conductor.model.WorkflowModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publishes a {@link Message} containing a {@link WorkflowSummary} to the undlerying {@link
 * QueueDAO} implementation on a workflow completion or termination event.
 */
public class QueueStatusPublisherAndArchive implements WorkflowStatusListener {

    private EventQueues eventQueues;

    private ExecutionDAOFacade executionDAOFacade;
    private ObjectMapper objectMapper;

    public QueueStatusPublisherAndArchive(
            EventQueues eventQueues,
            ExecutionDAOFacade executionDAOFacade,
            ObjectMapper objectMapper) {
        this.eventQueues = eventQueues;
        this.executionDAOFacade = executionDAOFacade;
        this.objectMapper = objectMapper;
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(QueueStatusPublisherAndArchive.class);
    private static final String EXCHANGE_NAME = "workflow-status-listener";
    private static final String QUEUE_NAME = "amqp_exchange:" + EXCHANGE_NAME;

    private static final String COMPLETED_ROUTING_KEY = "workflow.status.completed";
    private static final String TERMINATED_ROUTING_KEY = "workflow.status.terminated";

    @Override
    public void onWorkflowCompleted(WorkflowModel workflow) {
        AMQPObservableQueue queue = (AMQPObservableQueue) eventQueues.getQueue(QUEUE_NAME);

        try {
            LOGGER.info(
                    "Publishing callback of workflow {} on completion", workflow.getWorkflowId());
            queue.publishMessage(workflowToMessage(workflow), EXCHANGE_NAME, COMPLETED_ROUTING_KEY);
        } catch (Exception e) {
            LOGGER.error(
                    "Error when publishing callback of workflow {} on completion",
                    workflow.getWorkflowId());
        }

        try {
            LOGGER.info("Archiving workflow {} on completion ", workflow.getWorkflowId());
            this.executionDAOFacade.removeWorkflow(workflow.getWorkflowId(), true);
            Monitors.recordWorkflowArchived(workflow.getWorkflowName(), workflow.getStatus());
        } catch (Exception e) {
            LOGGER.error(
                    "Error when archiving workflow {} on completion ", workflow.getWorkflowId());
        }
    }

    @Override
    public void onWorkflowTerminated(WorkflowModel workflow) {
        AMQPObservableQueue queue = (AMQPObservableQueue) eventQueues.getQueue(QUEUE_NAME);

        try {
            LOGGER.info(
                    "Publishing callback of workflow {} on termination", workflow.getWorkflowId());
            queue.publishMessage(
                    workflowToMessage(workflow), EXCHANGE_NAME, TERMINATED_ROUTING_KEY);
        } catch (Exception e) {
            LOGGER.error(
                    "Error when publishing callback of workflow {} on termination",
                    workflow.getWorkflowId());
        }

        try {
            LOGGER.info("Archiving workflow {} on completion ", workflow.getWorkflowId());
            this.executionDAOFacade.removeWorkflow(workflow.getWorkflowId(), true);
            Monitors.recordWorkflowArchived(workflow.getWorkflowName(), workflow.getStatus());
        } catch (Exception e) {
            LOGGER.error(
                    "Error when archiving workflow {} on completion ", workflow.getWorkflowId());
        }
    }

    private Message workflowToMessage(WorkflowModel workflowModel) {
        String jsonWfSummary;
        WorkflowSummary summary = new WorkflowSummary(workflowModel.toWorkflow());
        try {
            jsonWfSummary = objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            LOGGER.error(
                    "Failed to convert WorkflowSummary: {} to String. Exception: {}", summary, e);
            throw new RuntimeException(e);
        }
        return new Message(workflowModel.getWorkflowId(), jsonWfSummary, null);
    }
}
