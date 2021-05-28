package org.onap.cps.notification

import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.event.model.CpsDataUpdatedEvent
import org.onap.cps.event.model.Data
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.util.StringUtils
import spock.lang.Specification

import java.time.format.DateTimeFormatter

class NotificationServiceSpec extends Specification {

    def mockNotificationPublisher = Mock(NotificationPublisher)
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAdminService = Mock(CpsAdminService)

    def notificationService = new NotificationService(true, mockNotificationPublisher, mockCpsDataService, mockCpsAdminService)

    final DATASPACE_NAME = 'my-dataspace'
    final ANCHOR_NAME = 'my-anchorname'
    final SCHEMASET_NAME = 'my-schemaset-name'
    final EVENT_SOURCE = new URI('urn:cps:org.onap.cps')
    final EVENT_TYPE = 'org.onap.cps.data-updated-event'
    final EVENT_SCHEMA = CpsDataUpdatedEvent.Schema.URN_CPS_ORG_ONAP_CPS_DATA_UPDATED_EVENT_SCHEMA_1_1_0_SNAPSHOT
    final DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    def 'Skip Sending notification when disabled.'() {

        given: 'notification is disabled'
        notificationService.dataUpdatedEventNotificationEnabled = false

        when: 'dataUpdatedEvent is received'
        notificationService.processDataUpdatedEvent(DATASPACE_NAME, ANCHOR_NAME)

        then: 'the notification is not sent'
        0 * mockNotificationPublisher.sendNotification(_)
    }

    def 'Send notification when enabled.'() {

        given: 'notification is enabled'
            notificationService.dataUpdatedEventNotificationEnabled = true
        and: 'cps admin service is able to return anchor details'
            mockCpsAdminService.getAnchor(DATASPACE_NAME, ANCHOR_NAME) >>
                new Anchor(ANCHOR_NAME, DATASPACE_NAME, SCHEMASET_NAME)
        and: 'cps data service returns the data node details'
            def xpath = '/'
            def dataNode = new DataNodeBuilder().withXpath(xpath).withLeaves(['name': 'sci-fi']).build()
            mockCpsDataService.getDataNode(
                    DATASPACE_NAME, ANCHOR_NAME, xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode

        when: 'dataUpdatedEvent is received'
            notificationService.processDataUpdatedEvent(DATASPACE_NAME, ANCHOR_NAME)

        then: 'notification is sent with correct event'
            1 * mockNotificationPublisher.sendNotification(_) >>
                    {
                        CpsDataUpdatedEvent event = it[0]
                        with(event) {
                            type == EVENT_TYPE
                            source == EVENT_SOURCE
                            schema == EVENT_SCHEMA
                            StringUtils.hasText(id)
                            content != null
                        }
                        with(event.content) {
                            assert isValidDateTimeFormat(observedTimestamp): "$observedTimestamp is not in $DATE_TIME_FORMAT format"
                            anchorName == ANCHOR_NAME
                            dataspaceName == DATASPACE_NAME
                            schemaSetName == SCHEMASET_NAME
                            data == new Data().withAdditionalProperty('name', 'sci-fi')
                        }
                    }
    }

    def isValidDateTimeFormat(String observedTimestamp) {
        try {
            DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).parse(observedTimestamp)
        } catch (DateTimeParseException) {
            return false
        }
        return true
    }


}
