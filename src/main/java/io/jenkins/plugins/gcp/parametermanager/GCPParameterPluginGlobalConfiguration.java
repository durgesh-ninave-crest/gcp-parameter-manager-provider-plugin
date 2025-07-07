package io.jenkins.plugins.gcp.parametermanager;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.Extension;
import hudson.util.FormValidation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class GCPParameterPluginGlobalConfiguration extends GlobalConfiguration {

    private String projectId;
    private String gcpCredentialName;
    private final Set<HashMap<String, String>> parameterNames = new HashSet<>();

    public GCPParameterPluginGlobalConfiguration() {
        load();
    }

    /**
     * Returns the GCP project ID to use for the Parameter Store API.
     *
     * @return the GCP project ID
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Sets the GCP project ID to use for the Parameter Store API.
     * If the ID is empty or null, it will be set to an empty string.
     *
     * @param projectId the GCP project ID
     */
    @DataBoundSetter
    public void setProjectId(String projectId) {
        this.projectId = (projectId == null | projectId.isEmpty()) ? "" : projectId;
        save();
    }

    /**
     * Gets the name of the GCP service account credential to use for the Parameter Store API.
     *
     * @return the name of the GCP service account credential
     */
    public String getGcpCredentialName() {
        return gcpCredentialName;
    }

    /**
     * Sets the name of the GCP service account credential to use for the Parameter Store API.
     * If the credential name is empty or null, it will be set to an empty string.
     *
     * @param credentialName the name of the GCP service account credential
     */
    @DataBoundSetter
    public void setGcpCredentialName(String credentialName) {
        this.gcpCredentialName = credentialName;
        save();
    }

    /**
     * Adds a parameter to the global configuration of the GCP Parameter plugin.
     * The parameter is specified as a HashMap containing the parameter name and version.
     * If the parameter already exists in the global configuration, it will not be added.
     *
     * @param param the parameter to add
     */
    public synchronized void addParameter(HashMap<String, String> param) {
        if (parameterNames.add(param)) {
            save();
        }
    }

    /**
     * Removes a parameter from the global configuration of the GCP Parameter plugin.
     * The parameter is specified as a HashMap containing the parameter name and version.
     * If the parameter does not exist in the global configuration, it will not be removed.
     *
     * @param param the parameter to remove
     */
    public synchronized void removeParameter(HashMap<String, String> param) {
        if (parameterNames.remove(param)) {
            save();
        }
    }

    /**
     * Removes all parameter versions from the global configuration of the GCP Parameter plugin
     * that have the given parameter name and location.
     *
     * @param parameterName the name of the parameter to remove
     * @param location the location of the parameter to remove
     */
    public synchronized void removeAllParameterVersions(String parameterName, String location) {
        parameterNames.removeIf(
                param -> parameterName.equals(param.get("parameter")) && location.equals(param.get("location")));
        save();
    }

    /**
     * Returns a set of parameter names and versions from the global configuration of the GCP
     * Parameter plugin.
     * The set contains HashMaps with the keys "parameter" and "version", which contain the name
     * of the parameter and its version, respectively.
     *
     * @return a set of HashMaps containing the parameter name and version
     */
    public synchronized Set<HashMap<String, String>> getParameterNames() {
        return parameterNames;
    }

    /**
     * Gets the global configuration of the GCP Parameter plugin.
     *
     * @return the global configuration of the GCP Parameter plugin
     */
    public static GCPParameterPluginGlobalConfiguration get() {
        return GlobalConfiguration.all().get(GCPParameterPluginGlobalConfiguration.class);
    }

    /**
     * Saves the global configuration of the GCP Parameter plugin.
     * If the GCP service account credential name is specified, it will be validated
     * before saving the configuration. If the credential name is invalid, the
     * configuration is not saved and false is returned.
     *
     * @param req the Stapler request
     * @param formData the JSON object containing the form data
     * @return true if the configuration is saved successfully, false otherwise
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
        req.bindJSON(this, formData);

        if (gcpCredentialName != null && !gcpCredentialName.isEmpty()) {
            if (doCheckGcpCredentialName(gcpCredentialName).kind != FormValidation.Kind.OK) {
                return false;
            }
        }
        save();
        return true;
    }

    /**
     * Checks if the given GCP service account credential name is valid.
     * The method will return a {@link FormValidation} object with a kind of {@link FormValidation.Kind#OK} if the credential name is valid.
     * If the credential name is invalid, the method will return a {@link FormValidation} object with a kind of
     * {@link FormValidation.Kind#ERROR} and a message indicating that the credential was not found.
     *
     * @param value the GCP service account credential name
     * @return a {@link FormValidation} object with a kind of {@link FormValidation.Kind#OK} if the credential name is valid, or a kind of
     *         {@link FormValidation.Kind#ERROR} if the credential name is invalid
     */
    public FormValidation doCheckGcpCredentialName(@QueryParameter String value) {
        if (gcpCredentialName != null && !gcpCredentialName.isEmpty()) {
            FileCredentials credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentialsInItemGroup(FileCredentials.class, Jenkins.get(), null, null),
                    CredentialsMatchers.withId(value));
            if (credentials == null) {
                return FormValidation.error("No GCP service account access key Credential found with this ID.");
            }
        }
        return FormValidation.ok("");
    }
}
