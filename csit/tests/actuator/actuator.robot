*** Settings ***
Library           OperatingSystem
Library           Process

*** Variables ***

${check}    ${SCRIPTS}/actuator/check_endpoint.sh

*** Test Cases ***
Liveness Probe for CPS
    [Documentation]   Liveness Probe
    ${result}=    Run Process   bash ${check} ${CPS_HOST}/manage/health/liveness >> actuator-test.log    shell=yes
    Should Be Equal As Integers    ${result.rc}    0

Readiness Probe for CPS
    [Documentation]   Readiness Probe
    ${result}=    Run Process   bash ${check} ${CPS_HOST}/manage/health/readiness >> actuator-test.log    shell=yes
    Should Be Equal As Integers    ${result.rc}    0
