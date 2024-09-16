import {crypto} from 'k6/experimental/webcrypto';
import {check} from 'k6';
import {
    Writer, SchemaRegistry,
    SCHEMA_TYPE_STRING
} from 'k6/x/kafka';
import { Counter } from 'k6/metrics';
import {
   KAFKA_BOOTSTRAP_SERVERS
} from './common/utils.js';

const kafkaWriterErrorCount = new Counter('kafka_writer_error_count');

const jsonData = open('../resources/sampleAvcInputEvent.json');
let testEventSent = JSON.parse(jsonData);

const writer = new Writer({
    brokers: KAFKA_BOOTSTRAP_SERVERS,
    topic: "dmi-cm-events",
    autoCreateTopic: true,
});

const schemaRegistry = new SchemaRegistry();

const DURATION = '1m';

export const options = {
    setupTimeout: '1m',
    teardownTimeout: '1m',
    scenarios: {
        produce_cm_avc_event: {
            executor: 'constant-arrival-rate',
            exec: 'produce_cm_avc_event',
            duration: DURATION,
            rate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 1,
        }
    },
    thresholds: {
        'produce_cm_avc_event_duration': ['p(95)<1000'], // 95% of events should take less than 1 second
        'produce_cm_avc_event_per_second': ['rate>1'],  // Set a threshold for events per second
    },
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
        },
    ];

    try {
        writer.produce({ messages: messages });
    } catch (error) {
        kafkaWriterErrorCount.add(1);
    }

    check(writer, {
        'Message sent': (p) => p != null,
    });
}