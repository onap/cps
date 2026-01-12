# k6 Performance Tests - Reorganized Structure

[k6](https://k6.io/) is used for performance tests. k6 tests are written in JavaScript.

## 📁 Directory Structure

The k6 tests are now organized for better maintainability and clear separation of concerns:

```
k6-tests/
├── environments/           # Environment-specific configurations
│   ├── docker/            # Docker environment settings
│   │   └── config.json    # Docker host URLs and settings
│   └── kubernetes/        # Kubernetes environment settings
│       └── config.json    # K8s host URLs and settings
├── profiles/              # Test profile configurations
│   ├── kpi/              # KPI performance test scenarios
│   │   └── scenarios.json # KPI test scenarios and thresholds
│   └── endurance/        # Endurance test scenarios
│       └── scenarios.json # Long-running stability test scenarios
├── deployment/            # Environment-specific execution scripts
│   ├── docker/           # Docker environment scripts
│   │   ├── setup.sh      # Docker-specific setup
│   │   └── execute-tests.sh # Docker test execution
│   └── kubernetes/       # Kubernetes environment scripts
│       ├── setup.sh      # K8s-specific setup
│       └── execute-tests.sh # K8s test execution
├── common/               # Shared test utilities and scenarios
│   ├── cmhandle-crud.js  # CM Handle CRUD operations
│   ├── passthrough-crud.js # Passthrough operations
│   ├── search-base.js    # Search functionality
│   ├── utils.js          # Common utilities
│   ├── produce-avc-event.js # Kafka event production
│   └── write-data-job.js # Data job operations
├── ncmp/                 # NCMP-specific test files (legacy structure)
├── resources/            # Test resources and sample data
└── k6-main-new.sh       # Updated main execution script
```

## 🚀 Running Tests

### Quick Start
```bash
# Run KPI tests on Docker
./k6-main-new.sh kpi dockerHosts

# Run Endurance tests on Kubernetes
./k6-main-new.sh endurance k8sHosts
```

### Test Profiles
1. **kpi** — Performance evaluation with specific thresholds and requirements
2. **endurance** — Long-term stability testing (2+ hours)

### Deployment Types
1. **dockerHosts** — Docker-compose based deployment
2. **k8sHosts** — Kubernetes based deployment with Helm Charts

## 📋 Prerequisites

### For Docker Environment
- Docker and Docker Compose
- k6 with Kafka extension

### For Kubernetes Environment

#### Windows
1. Docker Desktop with Kubernetes enabled
2. Helm (install via winget: `winget install Helm.Helm`)

#### Linux
1. k3s from Rancher
2. Helm installation

## 🔧 Configuration Management

### Environment Configuration
- **Docker**: `environments/docker/config.json`
- **Kubernetes**: `environments/kubernetes/config.json`

### Test Scenarios
- **KPI**: `profiles/kpi/scenarios.json`
- **Endurance**: `profiles/endurance/scenarios.json`

## 🛠 Maintenance

### Adding New Environments
1. Create new directory under `environments/`
2. Add `config.json` with environment-specific settings
3. Create corresponding setup and execution scripts under `deployment/`

### Adding New Test Profiles
1. Create new directory under `profiles/`
2. Add `scenarios.json` with test scenarios and thresholds
3. Update execution scripts to handle the new profile

### Modifying Common Utilities
- All shared test functions are in the `common/` directory
- Update import paths in test files when modifying utilities

## 📊 Results and Monitoring

### KPI Tests
- Automatic pass/fail evaluation against FS requirements
- CSV reports with ✅/❌ indicators
- Threshold validation and summary

### Endurance Tests
- Focus on stability and memory trends
- Use Grafana dashboards for analysis
- Monitor container resource usage over time

## 🔍 Troubleshooting

### Common Issues
1. **Import Path Errors**: Update import paths to use `../common/` or `../environments/`
2. **Configuration Not Found**: Ensure environment and profile JSON files exist
3. **Script Permissions**: Make sure shell scripts are executable (`chmod +x`)

### Monitoring Tools
- **Grafana**: https://monitoring.nordix.org/login
- **Local Logs**: Check `archive-logs.sh` output
- **K6 Output**: Review test execution logs for detailed metrics
