
# CPS and NCMP Helm Chart
This Helm chart deploys the **CPS** and **NCMP** ecosystem along with PostgreSQL, Kafka, Zookeeper, and the DMI Stub service.
---
## Prerequisites
- Kubernetes cluster (tested on K8s 1.24+)
- Helm 3.x
- Access to the necessary Docker image registry (e.g., `nexus3.onap.org`)
---
## Installation
To install the chart into the **default namespace**:
```bash
helm install cps-and-ncmp ./<chart-directory>
```
Replace <chart-directory> with the path to this Helm chart.
You can verify the deployment using:
```bash
kubectl get all -l app.kubernetes.io/instance=cps-and-ncmp
```
---
## Uninstallation
To uninstall the chart and delete all related resources:
```bash
helm uninstall cps-and-ncmp
```
---
## Port Forwarding
You can access the services locally using kubectl port-forward.
---
## CPS and NCMP (API) Service
```bash
kubectl port-forward service/cps-and-ncmp 8080:8080
```
Once port forwarding is active, you can access the CPS/NCMP API at:
http://localhost:8080
---
## DMI Stub Service
```bash
kubectl port-forward service/dmi-stub 8092:8092
```
Access the DMI stub API at:
http://localhost:8092
---
## Default Credentials
### PostgreSQL
Database: cpsdb
Username: cps
Password: cps
### DMI Stub
Username: cpsuser
Password: cpsr0cks!
---
## Configuration
This chart includes default settings suitable for local development and testing. You can customize values using a custom values.yaml file or by passing --set parameters at install time.
Example:
```bash
helm install cps-and-ncmp ./<chart-directory> --set cps.replicas=1
```
---
## Chart Components
This Helm chart deploys the following components:
- postgresql: CPS database
- cps-and-ncmp: CPS and NCMP backend services
- kafka: Kafka message broker
- zookeeper: Zookeeper coordination service for Kafka
- dmi-stub: Stub service for NCMP device interactions
---
