/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

import {check} from 'k6';
import {Writer, SchemaRegistry, SCHEMA_TYPE_STRING} from 'k6/x/kafka';
import {KAFKA_BOOTSTRAP_SERVERS} from './utils.js';

const testEventPayload = JSON.stringify(JSON.parse(open('../../resources/sampleAvcInputEvent.json')));
const schemaRegistry = new SchemaRegistry();

const kafkaProducer = new Writer({
    brokers: [KAFKA_BOOTSTRAP_SERVERS],
    topic: 'dmi-cm-events',
    autoCreateTopic: true,
    batchSize: 5000,
    compression: 'gzip',
    requestTimeout: 30000
});

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

export function sendBatchOfKafkaMessages(batchSize = 250) {
    const messages = [];

    const cloudEventHeaders = getCloudEventHeaders();
    const networkElementId = getRandomNetworkElement();

    for (let i = 0; i < batchSize; i++) {
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
        messages.push(avcCloudEvent);
    }

    try {
        kafkaProducer.produce({messages: messages});
        const isBatchSent = check(kafkaProducer, {
            ['Batch of ${batchSize} messages sent successfully']: (producer) => producer != null,
        });

        if (!isBatchSent) {
            console.error('Failed to send batch of messages, batch size : ', batchSize);
        }
    } catch (error) {
        console.error(`Error during message production: ${error.message}`, avcCloudEvent);
    }
}

export function teardown() {
    kafkaProducer.close();
}