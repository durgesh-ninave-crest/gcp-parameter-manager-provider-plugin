# GCP Parameter Manager Provider Plugin

<!-- TODO: Update the links after release -->
<!-- [![Jenkins Plugin](https://imgenkins.io/stable/plugins/gcp-parameter-manager-provider/plugin.svg)](https://plugins.jenkins.io/gcp-parameter-manager-provider/)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/gcp-parameter-manager-provider-plugin.svg?label=changelog)](https://github.com/jenkinsci/gcp-parameter-manager-provider-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/gcp-parameter-manager-provider.svg?color=blue)](https://plugins.jenkins.io/gcp-parameter-manager-provider/)
[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Fgcp-parameter-manager-provider-plugin%2Fmain)](https://ci.jenkins.io/job/Plugins/job/gcp-parameter-manager-provider-plugin/)
[![GitHub license](https://img.shields.io/github/license/jenkinsci/gcp-parameter-manager-provider-plugin.svg)](https://github.com/jenkinsci/gcp-parameter-manager-provider-plugin/blob/master/LICENSE) -->

## About

This plugin allows Jenkins to retrieve parameters from [Google Cloud Parameter Store](https://cloud.google.com/secret-manager/parameter-manager/docs/overview) and make them available in your Jenkins pipelines and jobs.

## Features

- Retrieve parameters from Google Cloud Parameter Store
- Support for Unformatted, JSON and YAML parameters
- Integration with Jenkins Credentials system
- Pipeline support with easy-to-use steps

## Requirements

- **Jenkins**: 2.492.3
- **Java**: 17
- **Google Cloud Platform (GCP) project** with Parameter Store API enabled
- **Service Account** with appropriate IAM permissions

### Required IAM Permissions

The service account used by the plugin needs the following IAM permissions:

- `parametermanager.parameterAccessor`
- `parametermanager.parameterViewer`

## Installation

### Using the Jenkins Plugin Manager

1. Go to **Manage Jenkins** > **Credentials**
2. Upload Jenkins Credential file
3. Go to **Manage Jenkins** > **System**
4. Go to **GCP Configuration** section
5. Set Project ID and GCP service account key credential ID which is set above
6. Save the configuration

## Configuration

1. Go to **Manage Jenkins** > **System**
2. Scroll down to the **GCP Configuration** section
3. Configure the following settings:
   - **Project ID**: Your GCP project ID
   - **Service Account Key**: Upload your GCP service account JSON key file
4. Click **Save**

## Using Parameters in Pipelines

### Method 1: Inject Parameter Value in Pipeline Global Variable 'GCPParameter'

When configuring your pipeline, check **Use GCP Parameter** and select **Inject parameter value in pipeline global variable 'GCPParameter'**.

#### Example Pipeline

```groovy
pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                script {
                    echo "GCP Parameter Name: ${GCPParameter.name}"
                    echo "GCP Parameter Version: ${GCPParameter.version}"
                    echo "GCP Parameter Value: ${GCPParameter.value}"
                    echo "GCP Parameter Type: ${GCPParameter.type}"
                }
            }
        }
    }
}
```

### Method 2: Create Credential for Storing Parameter Value

When configuring your pipeline, check **Use GCP Parameter** and select **Create Credential for storing parameter value**.

#### Example Pipeline

```groovy
node {
    withCredentials([string(credentialsId: 'test_param:v1:us-central1', variable: 'REGIONAL_PARAM')]) {
        sh '''
        if [ "$REGIONAL_PARAM" = "test-data" ]; then
          echo "It's a match!"
        else
          echo "Nope."
        fi
        '''
    }
}
```

## Managing Parameters

You can view and manage all parameters and their versions in Jenkins by navigating to:

**Manage Jenkins** > **GCP Parameter Plugin**

This page provides a comprehensive list of all parameters, their versions, and locations.



## Troubleshooting

### Common Issues

1. **Permission Denied**
   - Verify the service account has the required IAM permissions
   - Check that the service account JSON key is valid

2. **Parameter Not Found**
   - Verify the parameter name is correct
   - Check that the parameter exists in the specified project and region

3. **Authentication Errors**
   - Verify the service account credentials are correctly configured
   - Ensure the credentials have not expired

## Building and Testing

### Prerequisites

- Java 17
- Maven 3.8.6

### Linting

```bash
mvn spotless:apply
```

### Building the Plugin

```bash
mvn clean install -U
```

The built plugin will be available in `target/gcp-parameter-manager-provider.hpi`.

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn failsafe:integration-test

# All tests
mvn verify
```

### Running Locally

1. Build the plugin:
   ```bash
   mvn clean package
   ```

2. Start a local Jenkins instance with the plugin installed:
   ```bash
   mvn hpi:run
   ```

3. Access Jenkins at http://localhost:8080/jenkins/

## Contributing

<!-- TODO: Add contributing guidelines after release -->
<!-- We welcome contributions! Please see our [contributing guidelines](CONTRIBUTING.md) for more information. -->

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for your changes
5. Run the test suite
6. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.

## Support

<!-- TODO: Uncomment this after release -->
<!-- For bugs, feature requests, or questions, please [file an issue](https://github.com/jenkinsci/gcp-parameter-manager-provider-plugin/issues). -->

When reporting issues, please include:
- Jenkins version
- Plugin version
- Steps to reproduce the issue
- Any relevant logs or error messages

## Version History

<!-- TODO: Update CHANGELOG.md -->
See the [CHANGELOG.md](CHANGELOG.md) for version history and notable changes.
