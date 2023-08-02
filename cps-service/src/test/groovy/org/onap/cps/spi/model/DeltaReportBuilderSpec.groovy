package org.onap.cps.spi.model

import spock.lang.Specification

class DeltaReportBuilderSpec extends Specification{

    def 'Generating delta report with  for add action'() {
        when: 'delta report is generated'
            def deltaReport = new DeltaReportBuilder()
                    .actionAdd()
                    .withXpath('/test-xpath')
                    .withTargetData(['data':'hello world'])
                    .build()
        then: 'the delta report contains the attributes'
            assert deltaReport.action == 'add'
            assert deltaReport.xpath == '/test-xpath'
            assert deltaReport.targetData == ['data': 'hello world']
    }

    def 'Generating delta report with attributes for remove action'() {
        when: 'delta report is generated'
            def deltaReport = new DeltaReportBuilder()
                    .actionRemove()
                    .withXpath('/test-xpath')
                    .withSourceData(['data':'hello world'])
                    .build()
        then: 'the delta report contains the attributes'
            assert deltaReport.action == 'remove'
            assert deltaReport.xpath == '/test-xpath'
            assert deltaReport.sourceData == ['data': 'hello world']
    }
}
