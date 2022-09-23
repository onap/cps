package org.onap.cps.spi


import spock.lang.Specification

class FetchDescendantsOptionSpec extends Specification {
    def 'Check has next descendant for fetch descendant option: #scenario'() {
        when: 'a fetch descendant option'
            def descendantOption = new FetchDescendantsOption(depth)
        then: 'fetch descendant option has next method send the correct response'
            descendantOption.hasNext() == hasNext
        where: 'following parameters are used'
            scenario                  | depth || hasNext
            'omit descendants'        | 0     || false
            'include all descendants' | -1    || true
            'too low depth'           | -2    || false
            'first child'             | 1     || true
            'second child'            | 2     || true
    }

    def 'Get next descendant for fetch descendant option: #scenario'() {
        when: 'a fetch descendant option'
            def descendantOption = new FetchDescendantsOption(depth)
        then: 'fetch descendant option next method send the correct response'
            descendantOption.next().depth == next
        where: 'following parameters are used'
            scenario                  | depth || next
            'omit descendants'        | 0     || -1
            'include all descendants' | -1    || -1
            'too low depth'           | -2    || -3
            'first child'             | 1     || 0
            'second child'            | 2     || 1
    }
}
