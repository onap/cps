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
import {
    Writer, SchemaRegistry,
    SCHEMA_TYPE_STRING
} from 'k6/x/kafka';

const jsonData = open('../../resources/sampleAvcInputEvent.json');
let testEventSent = JSON.parse(jsonData);

const writer = new Writer({
    brokers: ['localhost:9092'],
    topic: "dmi-cm-events",
    autoCreateTopic: true,
    batchSize: 5000, // Number of messages per batch (adjust based on performance)
    compression: "gzip", // Set compression type
    requestTimeout: 30000, // Increase if requests timeout under heavy load
});

const schemaRegistry = new SchemaRegistry();

const TOTAL_MESSAGES = 100000;

export const options = {
    setupTimeout: '1m',
    teardownTimeout: '1m',
    scenarios: {
        produce_cm_avc_event: {
            executor: 'shared-iterations',
            exec: 'produce_cm_avc_event',
            vus: 1000, // You can adjust VUs to meet performance requirements
            iterations: TOTAL_MESSAGES, // Total messages to publish
            maxDuration: '10m', // Adjust depending on the expected completion time
        }
    }
};

export function teardown() {
    writer.close();
}

export function produce_cm_avc_event() {

    let cloudEventHeaders = {
        "ce_type": "org.onap.cps.ncmp.events.avc1_0_0.AvcEvent",
        "ce_source": "DMI",
        "ce_destination": "dmi-cm-events",
        "ce_specversion": "1.0",
        "ce_time": new Date().toISOString(),
        "ce_id": crypto.randomUUID(),
        "ce_dataschema": "urn:cps:org.onap.cps.ncmp.events.avc1_0_0.AvcEvent:1.0.0",
        "ce_correlationid": crypto.randomUUID()
    };

    let payload = JSON.stringify(testEventSent);

    const messages = [
        {
            key: schemaRegistry.serialize({
                data: cloudEventHeaders.ce_correlationid,
                schemaType: SCHEMA_TYPE_STRING,
            }),
            value: schemaRegistry.serialize({
                data: payload,
                schemaType: SCHEMA_TYPE_STRING
            }),
            headers: cloudEventHeaders
        }
    ];

    try {
        writer.produce({messages: messages});
        const success = check(writer, {
            'Message sent': (p) => p != null,
        });
        if (!success) {
            console.error('Message failed to send:', messages);
        }
    } catch (error) {
        console.error('Error producing message:', error, messages);
    }
}