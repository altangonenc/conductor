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
package com.netflix.conductor.contribs.queue.amqp.eventqueue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.conductor.core.events.EventQueues;
import com.netflix.conductor.core.listener.WorkflowStatusListener;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnProperty(
        name = "conductor.workflow-status-listener.type",
        havingValue = "event_queue_publisher")
public class EventQueueStatusPublisherConfiguration {

    @Bean
    public WorkflowStatusListener getWorkflowStatusListener(
            EventQueues eventQueues, ObjectMapper objectMapper) {
        return new EventQueueStatusPublisher(eventQueues, objectMapper);
    }
}
