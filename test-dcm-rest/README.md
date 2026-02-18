# Test DCM REST Module

## Purpose

This module contains REST controllers and models used exclusively for testing and performance measurement purposes.

## Contents

- `DataJobControllerForTest`: A REST controller that exposes an internal test endpoint for data job write operations
- `DataJobRequest`: A request model used by the test controller

## Important Notes

- This module should ONLY be used in test/development environments
- The endpoint is marked with `@Hidden` to exclude it from production API documentation
- The endpoint path `/do-not-use/dataJobs` clearly indicates its test-only nature
- No production module should depend on this module (enforced by architecture tests)

## Architecture

This module depends on:
- `cps-ncmp-service` (for DataJobService and related models)
- Spring Boot Web (for REST controller support)

No other module should depend on `test-dcm-rest`.
