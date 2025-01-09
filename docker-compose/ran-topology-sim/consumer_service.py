# Copyright 2025 Nordix Foundation.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SUMMARY
# This script simulates realistic load during NCMP's CM-handle registration.
# It works by subscribing to the LCM-events topic, and for each CM-handle that
# goes to READY state (LCM event), it sends a REST request to fetch CM-handle
# details (inventory) and another request to fetch Yang module list. So there
# are two additional REST requests sent for each CM handle registered.
# This is useful for modelling real-world load during k6 tests.

import json
import requests
import logging
import os
from confluent_kafka import Consumer, KafkaException, KafkaError

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Kafka configuration
kafka_config = {
    'bootstrap.servers': os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'kafka:29092'),
    'group.id': os.getenv('KAFKA_GROUP_ID', 'ran-topology-sim-group'),
    'auto.offset.reset': 'earliest'
}

cps_host = os.getenv('CPS_HOST', 'nginx:80')
lcm_events_topic = os.getenv('LCM_EVENTS_TOPIC', 'ncmp-events')

def consume_lcm_events_and_fetch_data():
    consumer = Consumer(kafka_config)
    consumer.subscribe([lcm_events_topic])

    try:
        while True:
            kafka_message = consumer.poll(1.0)  # Timeout of 1 second

            if kafka_message is None:
                continue

            if kafka_message.error():
                logger.error(f"KafkaError: {kafka_message.error()}")

            else:
                kafka_message_payload = json.loads(kafka_message.value().decode('utf-8'))
                event_data = kafka_message_payload['event']
                cm_handle_id = event_data['cmHandleId']
                cm_handle_state = event_data['newValues']['cmHandleState']
                logger.info(f"cmHandleId: {cm_handle_id}, state: {cm_handle_state}")
                # When a CM-handle is READY, get inventory and model information from NCMP:
                if cm_handle_state == 'READY':
                    http_get_request(f"http://{cps_host}/ncmp/v1/ch/{cm_handle_id}")
                    http_get_request(f"http://{cps_host}/ncmp/v1/ch/{cm_handle_id}/modules")

    except KeyboardInterrupt:
        logger.info("Shutdown signal received")
    finally:
        consumer.close()

def http_get_request(rest_api_url):
    try:
        response = requests.get(rest_api_url)
        logger.info(f"{response.status_code} {response.reason}: GET {rest_api_url}")
        response.raise_for_status()
    except requests.RequestException as e:
        logger.error(f"HTTP request failed: {e}")

if __name__ == '__main__':
    consume_lcm_events_and_fetch_data()
