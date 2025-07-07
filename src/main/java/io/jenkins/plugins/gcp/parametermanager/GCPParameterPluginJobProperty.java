package io.jenkins.plugins.gcp.parametermanager;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.*;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.yaml.snakeyaml.Yaml;

@Extension
public class GCPParameterPluginJobProperty extends JobProperty<Job<?, ?>> {

    private final String location;
    private final boolean useGCPParameter;
    private final String gcpParameterName;
    private final String gcpParameterVersion;
    private final boolean createCredential;

    public GCPParameterPluginJobProperty() {
        this.useGCPParameter = false;
        this.gcpParameterName = "";
        this.gcpParameterVersion = "";
        this.location = "";
        this.createCredential = true;
    }

    @DataBoundConstructor
    public GCPParameterPluginJobProperty(
            String gcpParameterName, String gcpParameterVersion, boolean createCredential, String location) {
        if (gcpParameterName != null && !gcpParameterName.isEmpty()) {
            this.useGCPParameter = true;
            this.gcpParameterName = gcpParameterName;
            this.gcpParameterVersion =
                    (gcpParameterVersion == null | gcpParameterVersion.isEmpty()) ? "" : gcpParameterVersion;
            this.createCredential = createCredential;
            this.location = (location == null | location.isEmpty()) ? "global" : location;
        } else {
            this.useGCPParameter = false;
            this.gcpParameterName = "";
            this.gcpParameterVersion = "";
            this.createCredential = false;
            this.location = "";
        }
    }

    /**
     * @return true if GCP parameter is enabled for the job, false otherwise
     */
    public boolean isUseGCPParameter() {
        return useGCPParameter;
    }

    /**
     * Gets the name of the GCP parameter to use for the job.
     *
     * @return the name of the GCP parameter
     */
    public String getGcpParameterName() {
        return gcpParameterName;
    }

    /**
     * Gets the version of the GCP parameter to use for the job.
     * If the version is empty, the latest version will be retrieved.
     *
     * @return the version of the GCP parameter
     */
    public String getGcpParameterVersion() {
        return gcpParameterVersion;
    }

    /**
     * Gets whether the job is set to create a credential using the GCP parameter value.
     *
     * @return true if the job is set to create a credential, false otherwise
     */
    public boolean isCreateCredential() {
        return createCredential;
    }

    /**
     * Returns the GCP location to use for the Parameter Store API.
     * If the location is empty or "global", the default GCP endpoint will be used.
     *
     * @return the GCP location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Resolves a parameter value by replacing any Jenkins parameter placeholders.
     *
     * @param input the parameter value to resolve
     * @param run the current run
     * @return the resolved parameter value
     */
    String resolveParameter(String input, Run<?, ?> run) {
        if (input == null) return null;

        ParametersAction params = run.getAction(ParametersAction.class);
        if (params == null) return input;

        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String paramName = matcher.group(1);
            String value = "";

            ParameterValue param = params.getParameter(paramName);
            if (param != null) {
                Object paramVal = param.getValue();
                if (paramVal != null) {
                    value = String.valueOf(paramVal);
                }
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    @Extension
    public static class GCPParameterJobPropertyDescriptor extends JobPropertyDescriptor {
        public GCPParameterJobPropertyDescriptor() {
            super(GCPParameterPluginJobProperty.class);
            load();
        }

        /**
         * Gets the display name of the GCP Parameter job property.
         *
         * @return the display name of the job property
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return "GCP Parameter";
        }

        /**
         * This method is overridden from JobPropertyDescriptor and is not used.
         *
         * @param jobType the job type
         * @return true
         */
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        /**
         * Creates a new instance of the GCP Parameter job property.
         * The instance is created from the given Stapler request and form data.
         * If the form data contains the optional GCP parameter block, the property is
         * created with the specified GCP parameter values. Otherwise, an empty property
         * instance is created.
         *
         * @param req the Stapler request
         * @param formData the form data
         * @return a new instance of the GCP Parameter job property
         */
        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) {
            if (formData.has("optionalGCPParameterBlock")) {
                return req.bindJSON(
                        GCPParameterPluginJobProperty.class, formData.getJSONObject("optionalGCPParameterBlock"));
            } else {
                return new GCPParameterPluginJobProperty();
            }
        }

        /**
         * Fetches the specified parameter from GCP Parameter Store and injects its value
         * into the job run as an environment variable.
         *
         * @param run the job run
         * @return a JSON object containing the parameter details
         * @throws IOException if there is an issue with the GCP API
         */
        public static JSONObject fetchAndInject(Run<?, ?> run) throws IOException {
            JSONObject obj = new JSONObject();
            Job<?, ?> job = run.getParent();

            // Get the job and its GCPParameterPluginJobProperty
            GCPParameterPluginJobProperty property = job.getProperty(GCPParameterPluginJobProperty.class);
            String gcpParameterName = property.resolveParameter(property.getGcpParameterName(), run);
            String gcpParameterVersion = property.resolveParameter(property.getGcpParameterVersion(), run);

            // Fetch the parameter value
            GCPApiHelper apiHelper = new GCPApiHelper();
            Map<String, String> parameterValue = apiHelper.fetchParameterValueFromGCP(
                    gcpParameterName, gcpParameterVersion, property.resolveParameter(property.getLocation(), run));

            // Set the parameter details
            obj.put("name", gcpParameterName);
            obj.put("version", parameterValue.get("version"));
            obj.put("value", parameterValue.get("value"));
            obj.put("type", parameterValue.get("type"));

            // Parse the parameter value
            Object parsed;
            switch ((String) obj.get("type")) {
                case "JSON":
                    parsed = JSONObject.fromObject(obj.get("value"));
                    break;
                case "YAML":
                    Yaml yaml = new Yaml();
                    parsed = yaml.load((String) obj.get("value"));
                    break;
                default:
                    parsed = obj.get("value");
            }
            obj.put("value", parsed);
            return obj;
        }
    }
}
