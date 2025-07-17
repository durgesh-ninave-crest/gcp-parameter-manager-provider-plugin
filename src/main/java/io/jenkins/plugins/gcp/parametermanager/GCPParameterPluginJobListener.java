package io.jenkins.plugins.gcp.parametermanager;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.util.Secret;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

@Extension
public class GCPParameterPluginJobListener extends RunListener<Run<?, ?>> {

    /**
     * When a job is started, if the job has GCPParameterPluginJobProperty, check if the property is set to create a
     * credential. If so, fetch the parameter value from GCP and create a StringCredentialsImpl if the credential does
     * not exist. If the credential already exists, remove and recreate the credential.
     *
     * @param run the run object
     * @param listener the task listener
     */
    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        // Get the job and its GCPParameterPluginJobProperty
        Job<?, ?> job = run.getParent();
        GCPParameterPluginJobProperty prop = job.getProperty(GCPParameterPluginJobProperty.class);

        // If the job has GCPParameterPluginJobProperty and the property is set to create a credential
        if (prop.isUseGCPParameter() && prop.isCreateCredential()) {
            try {
                // Fetch the parameter value from GCP
                String gcpParameterName = prop.resolveParameter(prop.getGcpParameterName(), run);
                String gcpParameterVersion = prop.resolveParameter(prop.getGcpParameterVersion(), run);
                String location = prop.resolveParameter(prop.getLocation(), run);

                // Create a StringCredentialsImpl
                SystemCredentialsProvider provider = SystemCredentialsProvider.getInstance();
                List<Credentials> credentials = provider.getCredentials();
                String description = gcpParameterName + " : " + gcpParameterVersion;

                // Check if the credential already exists
                StringCredentialsImpl existing = null;
                for (Credentials cred : credentials) {
                    if (cred instanceof StringCredentialsImpl) {
                        StringCredentialsImpl strCred = (StringCredentialsImpl) cred;
                        if (strCred.getId().equals(gcpParameterName)
                                && strCred.getDescription().equals(description)) {
                            existing = strCred;
                            break;
                        }
                    }
                }

                // If the credential already exists, remove it
                if (existing != null) {
                    provider.getCredentials().remove(existing);
                }

                // Fetch the parameter value from GCP
                GCPApiHelper apiHelper = new GCPApiHelper();
                Map<String, String> parameterValue =
                        apiHelper.fetchParameterValueFromGCP(gcpParameterName, gcpParameterVersion, location);

                // Create a new credential
                StringCredentialsImpl newCred = new StringCredentialsImpl(
                        CredentialsScope.GLOBAL,
                        gcpParameterName,
                        description,
                        Secret.fromString(parameterValue.get("value")));
                provider.getCredentials().add(newCred);
                provider.save();

            } catch (Exception e) {
                listener.error("Error creating GCP parameter credentials: " + e.getMessage());
            }
        }
    }
}
