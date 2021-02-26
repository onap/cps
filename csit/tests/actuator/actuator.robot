*** Settings ***
Documentation         CPS - Actuator endpoints

Library               Collections
Library               RequestsLibrary

Suite Setup           Create Session    CPS_HOST    ${CPS_HOST}

*** Variables ***


*** Test Cases ***
Test Liveness Probe Endpoint
    ${response}=      GET On Session    CPS_HOST     /manage/health/liveness     expected_status=200
    Should Be Equal As Strings          ${response.json()['status']}      UP

Test Readiness Probe Endpoint
    ${response}=      GET On Session    CPS_HOST     /manage/health/readiness    expected_status=200
    Should Be Equal As Strings          ${response.json()['status']}      UP