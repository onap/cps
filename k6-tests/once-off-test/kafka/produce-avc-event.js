/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

import {crypto} from 'k6/experimental/webcrypto';
import {check} from 'k6';
import {Writer, SchemaRegistry, SCHEMA_TYPE_STRING} from 'k6/x/kafka';

const testEventPayload = JSON.stringify(JSON.parse(open('../../resources/sampleAvcInputEvent.json')));
const schemaRegistry = new SchemaRegistry();
const AVC_EVENTS_PER_SECOND = 500; // Configurable messages per second
let messagesSent = 0;


if (AVC_EVENTS_PER_SECOND <= 0) {
    console.warn("⚠️ Warning: AVC_EVENTS_PER_SECOND was set too low. must be greater than 0.");
}

const kafkaProducer = new Writer({
    brokers: ['localhost:9092'],
    topic: 'dmi-cm-events',
    autoCreateTopic: true,
    batchSize: 5000,
    compression: 'gzip',
    requestTimeout: 30000
});

export const options = {
    setupTimeout: '1m',
    teardownTimeout: '1m',
    scenarios: {
        produceKafkaMessages: {
            executor: 'constant-arrival-rate',
            rate: AVC_EVENTS_PER_SECOND,
            timeUnit: '1s',
            duration: '60m',
            preAllocatedVUs: 10,
            maxVUs: 6000,  // ⚠️ Reduce from 7000 to prevent resource overload
            exec: 'sendKafkaMessages',
            gracefulStop: '10s' // Stop test gracefully
        }
    }
};

// Utility function to get a random network element
const getRandomNetworkElement = () => {
    return `neType-${Math.floor(Math.random() * 10) + 1}`;
};

function getCloudEventHeaders() {
    return {
        ce_type: 'org.onap.cps.ncmp.events.avc1_0_0.AvcEvent',
        ce_source: 'DMI',
        ce_destination: 'dmi-cm-events',
        ce_specversion: '1.0',
        ce_time: new Date().toISOString(),
        ce_id: crypto.randomUUID(),
        ce_dataschema: 'urn:cps:org.onap.cps.ncmp.events.avc1_0_0.AvcEvent:1.0.0',
        ce_correlationid: crypto.randomUUID()
    };
}

// Function to send Kafka messages at a controlled throughput
export function sendKafkaMessages() {

    if (AVC_EVENTS_PER_SECOND === 0) {
        return;
    }

    const cloudEventHeaders = getCloudEventHeaders();
    const networkElementId = getRandomNetworkElement();

    const avcCloudEvent = {
        key: schemaRegistry.serialize({
            data: networkElementId,
            schemaType: SCHEMA_TYPE_STRING,
        }),
        value: schemaRegistry.serialize({
            data: testEventPayload,
            schemaType: SCHEMA_TYPE_STRING
        }),
        headers: cloudEventHeaders
    };

    try {
        kafkaProducer.produce({messages: [avcCloudEvent]});
        messagesSent++;
        const isMessageSent = check(kafkaProducer, {
            'Message sent successfully': (producer) => producer != null,
        });

        if (!isMessageSent) {
            console.error('Failed to send message:', avcCloudEvent);
        }
    } catch (error) {
        console.error(`Error during message production: ${error.message}`, avcCloudEvent);
    }
}

// Teardown function
export function teardown() {
    kafkaProducer.close();
}
