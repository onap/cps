# Monitoring and Tracing — Local Developer Guide

This guide explains how to use **Prometheus**, **Grafana**, and **Jaeger** with the CPS/NCMP Helm chart on a local Kubernetes cluster.

---

## Overview

The Helm chart includes optional monitoring and tracing components:

| Component  | Purpose                                     | Default  |
|------------|---------------------------------------------|----------|
| Prometheus | Metrics collection from CPS and PostgreSQL  | Enabled  |
| Grafana    | Dashboards for JVM, database, REST, inventory | Enabled  |
| Jaeger     | Distributed tracing (OpenTelemetry)         | Disabled |

Grafana is pre-configured with a Prometheus datasource and five dashboards:
- **JVM (Micrometer)** — heap/non-heap memory, GC, threads, HTTP request rates
- **CPS Database Pool** — HikariCP and JDBC connection pool metrics
- **PostgreSQL Statistics** — database settings, checkpoint stats, cache usage
- **Data REST Interfaces** — NCMP data passthrough and search request counts
- **Inventory REST Interfaces** — NCMP inventory registration and CM handle state

---

## Quick Start

> **Important:** The release name **must** be `cps`. Service names in `values.yaml` are
> hardcoded with the prefix `cps-ncmp-` (release `cps` + chart `ncmp`).

### With Jaeger

```bash
helm install cps . \
  --set jaeger.enabled=true \
  --set cps.image.pullPolicy=IfNotPresent \
  --set cps.env.ONAP_TRACING_ENABLED=true
```

### Without Jaeger

```bash
helm install cps . \
  --set cps.image.pullPolicy=IfNotPresent
```

Prometheus and Grafana are enabled by default. To disable them:

```bash
--set prometheus.enabled=false --set grafana.enabled=false
```

---

## Accessing the UIs

### Windows (Docker Desktop K8s)

NodePorts are accessible directly on localhost:

| Service    | URL                        |
|------------|----------------------------|
| Prometheus | http://localhost:30090      |
| Grafana    | http://localhost:30030      |
| Jaeger     | http://localhost:30086      |

Grafana default login: **admin / admin**

### Linux (Minikube)

Enable port forwarding (keep running in separate terminals):

```bash
kubectl port-forward service/cps-ncmp-prometheus-service 30090:9090
kubectl port-forward service/cps-ncmp-grafana-service 30030:3000
kubectl port-forward service/cps-ncmp-jaeger-service 30086:16686
```

Then access via the same localhost URLs above.

---

## Verifying the Setup

### Check pods

```bash
kubectl get pods
```

Wait until the monitoring pods are running:
```
cps-ncmp-prometheus-...    1/1  Running
cps-ncmp-grafana-...       1/1  Running
cps-ncmp-jaeger-...        1/1  Running
```

### Check services

```bash
kubectl get svc | grep -E "prometheus|grafana|jaeger"
```

### Check Prometheus is healthy

```bash
curl http://localhost:30090/-/healthy
```

### Check Grafana datasource and dashboards

```bash
curl -u admin:admin http://localhost:30030/api/datasources
curl -u admin:admin http://localhost:30030/api/search
```

---

## Dashboard Notes

### Dashboards that show data immediately
- **JVM (Micrometer)** — JVM metrics are always present once CPS is running
- **CPS Database Pool** — connection pool metrics from HikariCP
- **PostgreSQL Statistics** — database-level stats from the postgres-exporter sidecar

### Dashboards that require traffic
- **Data REST Interfaces** — shows data only after NCMP data passthrough requests
  (e.g. `GET /ncmp/v1/ch/{cm-handle}/data/ds/...`, `POST /ncmp/v1/ch/id-searches`)
- **Inventory REST Interfaces** — shows data after inventory calls
  (e.g. `POST /ncmpInventory/v1/ch`)

If a dashboard shows "No data", check that the **Instance** and **Job** dropdowns at the top
are set correctly. The values are populated from Prometheus and should auto-populate once CPS
is being scraped.

---

## NodePort Allocation

The default and endurance profiles use separate NodePort ranges to allow parallel deployment
in different namespaces.

| Service          | Default (KPI) | Endurance |
|------------------|---------------|-----------|
| CPS              | 30080         | 30180     |
| Kafka UI         | 30089         | 30189     |
| Kafka External   | 30093         | 30193     |
| DMI Stub 1       | 30092         | 30192     |
| DMI Stub 2       | 30094         | 30194     |
| Policy Executor  | 30095         | 30195     |
| PG Exporter      | 30187         | 30287     |
| Prometheus       | 30090         | 30190     |
| Grafana          | 30030         | 30130     |
| Jaeger           | 30086         | 30186     |

---

## Cleanup

```bash
helm uninstall cps
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| Kafka in CrashLoopBackOff | Service name mismatch — Kafka can't resolve Zookeeper | Ensure release name is `cps` |
| CPS pods stuck in `Init:0/1` | Init container waiting for PostgreSQL and Kafka | Fix Kafka first, CPS will follow |
| Grafana dashboards show "No data" | No traffic to the monitored endpoints yet | Make some API requests to CPS/NCMP |
| Grafana "datasource not found" | Datasource UID mismatch | Redeploy — the provisioned datasource UID must match dashboard references |
| Prometheus targets showing "DOWN" | Wrong service name in scrape config | Verify `kubectl get svc` matches `config/prometheus.yml` targets |
| NodePort conflict on parallel deploy | Endurance profile missing port overrides | Use `values-endurance.yaml` which assigns separate ports |
