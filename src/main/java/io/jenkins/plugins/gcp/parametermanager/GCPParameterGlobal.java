package io.jenkins.plugins.gcp.parametermanager;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import java.util.Collections;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

@Extension
public class GCPParameterGlobal extends GlobalVariable {

    /**
     * Gets the name of this {@link GlobalVariable}.
     *
     * @return the name of this {@link GlobalVariable}
     */
    @NonNull
    @Override
    public String getName() {
        return "GCPParameter";
    }

    /**
     * Returns a JSON object containing the GCP parameter details.
     *
     * @param script the {@link CpsScript} that invoked this method
     * @return a JSON object with the GCP parameter details
     * @throws Exception if an error occurs
     */
    @NonNull
    @Override
    public Object getValue(@NonNull CpsScript script) throws Exception {
        Run<?, ?> run = script.$build();

        if (run == null) {
            return Collections.emptyMap();
        }

        GCPParameterPluginJobProperty property = run.getParent().getProperty(GCPParameterPluginJobProperty.class);
        JSONObject data = new JSONObject();

        if (property != null && property.isUseGCPParameter() && !property.isCreateCredential()) {
            data = GCPParameterPluginJobProperty.GCPParameterJobPropertyDescriptor.fetchAndInject(run);
        } else {
            data.put("name", "not set");
            data.put("version", "not set");
            data.put("value", "not set");
            data.put("type", "UNDEFINED");
        }
        return data;
    }
}
