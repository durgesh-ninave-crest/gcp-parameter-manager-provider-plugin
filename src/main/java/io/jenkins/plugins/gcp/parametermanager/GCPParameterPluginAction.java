package io.jenkins.plugins.gcp.parametermanager;

import hudson.Extension;
import hudson.model.ManagementLink;
import java.util.HashMap;
import java.util.Set;
import org.kohsuke.stapler.StaplerProxy;

@Extension
public class GCPParameterPluginAction extends ManagementLink implements StaplerProxy {

    /**
     * The name of the menu item as shown on the Jenkins
     * global configuration page.
     *
     * @return the name of the menu item
     */
    @Override
    public String getDisplayName() {
        return "GCP Parameter Plugin";
    }

    /**
     * A short description of the plugin, shown in the Jenkins
     * global configuration page.
     *
     * @return a short description of the plugin
     */
    @Override
    public String getDescription() {
        return "GCP parameter manager plugin to fetch parameters.";
    }

    /**
     * Returns the URL name for the configuration page of the GCP Parameter plugin.
     *
     * @return the URL name
     */
    @Override
    public String getUrlName() {
        return "gcp-parameter-plugin";
    }

    /**
     * The gear icon is a generic icon for configuration pages.
     * It is included in the Jenkins war file.
     *
     * @return the name of the icon file
     */
    @Override
    public String getIconFileName() {
        return "gear.png";
    }

    /**
     * {@inheritDoc}
     *
     * Returns the current object as the target. This is the default implementation
     * for .
     *
     * @return the current object
     */
    @Override
    public Object getTarget() {
        return this;
    }

    /**
     * Gets the GCP project ID from the global configuration of the GCP Parameter plugin.
     *
     * @return the GCP project ID
     */
    public String getProjectId() {
        return GCPParameterPluginGlobalConfiguration.get().getProjectId();
    }

    /**
     * Gets the name of the GCP service account credential from the global
     * configuration of the GCP Parameter plugin.
     *
     * @return the name of the GCP service account credential
     */
    public String getGcpCredentialName() {
        return GCPParameterPluginGlobalConfiguration.get().getGcpCredentialName();
    }

    /**
     * Gets the set of parameters from the global configuration of the GCP Parameter
     * plugin.
     *
     * @return a set of HashMaps containing the parameter name and version
     */
    public Set<HashMap<String, String>> getParameterNames() {
        return GCPParameterPluginGlobalConfiguration.get().getParameterNames();
    }
}
