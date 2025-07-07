package io.jenkins.plugins.gcp.parametermanager;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.parametermanager.v1.ListParameterVersionsRequest;
import com.google.cloud.parametermanager.v1.Parameter;
import com.google.cloud.parametermanager.v1.ParameterManagerClient;
import com.google.cloud.parametermanager.v1.ParameterManagerClient.ListParameterVersionsPagedResponse;
import com.google.cloud.parametermanager.v1.ParameterManagerSettings;
import com.google.cloud.parametermanager.v1.ParameterName;
import com.google.cloud.parametermanager.v1.ParameterVersionName;
import com.google.cloud.parametermanager.v1.RenderParameterVersionResponse;
import hudson.AbortException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;

public class GCPApiHelper {

    private final String projectId;
    private final String gcpCredentialName;
    private final GoogleCredentials credentials;

    public GCPApiHelper() throws IllegalStateException, IOException {

        // Get GCP credentials from global configurations
        this.gcpCredentialName = GCPParameterPluginGlobalConfiguration.get().getGcpCredentialName();
        this.projectId = GCPParameterPluginGlobalConfiguration.get().getProjectId();

        // Check if GCP service account credential and project ID are set
        if (this.gcpCredentialName.isEmpty()) {
            throw new AbortException("Service Account key Credential ID not found for GCP.");
        }
        if (this.projectId.isEmpty()) {
            throw new AbortException("Project ID not found for GCP.");
        }

        // Load GCP service account credential
        FileCredentials serviceAccountKeyFileCredential =
                CredentialsProvider.lookupCredentialsInItemGroup(FileCredentials.class, Jenkins.get(), null, null)
                        .stream()
                        .filter(cred -> cred.getId().equals(this.gcpCredentialName))
                        .findFirst()
                        .orElseThrow(() -> new IOException("GCP service account credential not found."));
        InputStream serviceAccountKey = serviceAccountKeyFileCredential.getContent();
        this.credentials = GoogleCredentials.fromStream(serviceAccountKey)
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    /**
     * Get a ParameterManagerClient object to interact with the GCP Parameter Store API.
     * If locationId is empty or "global", the client will use the default GCP endpoint.
     * Otherwise, the client will use the specified region's endpoint.
     *
     * @param locationId the GCP region to use for the Parameter Store API
     * @return a ParameterManagerClient object
     * @throws IOException if the client cannot be created
     */
    public ParameterManagerClient getParameterManagerClient(String locationId) throws IOException {

        ParameterManagerSettings parameterManagerSettings;
        if (!locationId.isEmpty() && !locationId.equalsIgnoreCase("global")) {
            String apiEndpoint = String.format("parametermanager.%s.rep.googleapis.com:443", locationId);
            parameterManagerSettings = ParameterManagerSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .setEndpoint(apiEndpoint)
                    .build();
        } else {
            parameterManagerSettings = ParameterManagerSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
        }

        return ParameterManagerClient.create(parameterManagerSettings);
    }

    /**
     * Fetches the specified parameter version from GCP Parameter Store.
     * The method will return the latest parameter version if parameterVersion is empty.
     * If the parameter version is not found, it will remove the parameter from the global
     * configuration if it exists.
     * If the parameter is not found, it will throw an IOException.
     *
     * @param parameterId the ID of the parameter to fetch
     * @param parameterVersion the version of the parameter to fetch
     * @param locationId the GCP region to use for the Parameter Store API
     * @return a HashMap containing the parameter ID and version
     * @throws IOException if the parameter version is not found or if there is an error
     *         communicating with the GCP Parameter Store API
     */
    public HashMap<String, String> fetchParamVersion(String parameterId, String parameterVersion, String locationId)
            throws IOException {

        ParameterManagerClient parameterManagerClient = getParameterManagerClient(locationId);
        GCPParameterPluginGlobalConfiguration config = GCPParameterPluginGlobalConfiguration.get();
        HashMap<String, String> paramInfo = new HashMap<>();
        paramInfo.put("parameter", parameterId);
        paramInfo.put("location", locationId);

        ParameterName parameterName = ParameterName.of(projectId, locationId, parameterId);
        String parameterVersionName = ParameterVersionName.of(projectId, locationId, parameterId, parameterVersion)
                .toString();
        ListParameterVersionsRequest request = ListParameterVersionsRequest.newBuilder()
                .setParent(parameterName.toString())
                .setOrderBy("create_time desc")
                .setFilter("disabled=false")
                .build();

        try {
            Parameter parameter = parameterManagerClient.getParameter(parameterName.toString());
            paramInfo.put("format", parameter.getFormat().toString());

            ListParameterVersionsPagedResponse response = parameterManagerClient.listParameterVersions(request);

            if (!response.iterateAll().iterator().hasNext()) {
                throw new IOException("No parameter versions found for " + parameterId);
            } else {
                if (parameterVersion.isEmpty()) {
                    paramInfo.put(
                            "version", response.iterateAll().iterator().next().getName());
                    config.addParameter(paramInfo);
                    return paramInfo;
                } else {
                    response.iterateAll().forEach(version -> {
                        if (parameterVersionName.equals(version.getName())) {
                            paramInfo.put("version", version.getName());
                        }
                    });
                    if (paramInfo.containsKey("version")) {
                        config.addParameter(paramInfo);
                        return paramInfo;
                    } else {
                        throw new IOException("Parameter Version " + parameterVersion + " not found for Parameter "
                                + parameterId + ".");
                    }
                }
            }
        } catch (NotFoundException e) {
            config.removeAllParameterVersions(parameterId, locationId);
            throw new IOException(parameterId + " parameter not found. " + e);
        }
    }

    /**
     * Fetches the value of a parameter from the GCP Parameter Store.
     * This function takes a parameter name, version, and location id and returns a map containing the
     * parameter version, type and value.
     *
     * @param parameterId the id of the parameter in the GCP Parameter Store
     * @param parameterVersion the version of the parameter to retrieve. If not specified, the latest
     *        version will be retrieved.
     * @param locationId the GCP region to use for the Parameter Store API. If not specified, the default
     *        GCP endpoint will be used.
     * @return a map containing the parameter version, type, and value
     * @throws IOException if an error occurs while communicating with the GCP Parameter Store API
     */
    public Map<String, String> fetchParameterValueFromGCP(
            String parameterId, String parameterVersion, String locationId) throws IOException {

        Map<String, String> map = new HashMap<>();
        ParameterManagerClient parameterManagerClient = getParameterManagerClient(locationId);
        Map<String, String> paramInfo = fetchParamVersion(parameterId, parameterVersion, locationId);

        map.put(
                "version",
                paramInfo.get("version").substring(paramInfo.get("version").lastIndexOf('/') + 1));
        map.put("type", paramInfo.get("format"));

        RenderParameterVersionResponse response =
                parameterManagerClient.renderParameterVersion(paramInfo.get("version"));
        map.put("value", response.getRenderedPayload().toStringUtf8());

        return map;
    }
}
