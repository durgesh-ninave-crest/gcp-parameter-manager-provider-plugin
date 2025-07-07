package io.jenkins.plugins.gcp.parametermanager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import hudson.model.Job;
import hudson.model.Run;
import java.util.Collections;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GCPParameterGlobalTest {

    @Mock
    private CpsScript script;

    @Mock
    private GCPParameterPluginJobProperty property;

    private GCPParameterGlobal global;

    @Before
    public void setup() {
        global = new GCPParameterGlobal();
    }

    @Test
    public void testGetName() {
        assertEquals("GCPParameter", global.getName());
    }

    @Test
    public void testGetValue_RunIsNull() throws Exception {
        when(script.$build()).thenReturn(null);
        assertEquals(Collections.emptyMap(), global.getValue(script));
    }

    @Test
    public void testGetValue_PropertyIsNull() throws Exception {
        Run<?, ?> run = mock(Run.class);
        Job<?, ?> job = mock(Job.class);
        when(script.$build()).thenReturn((Run) run);
        when(run.getParent()).thenReturn((Job) job);
        when(job.getProperty(GCPParameterPluginJobProperty.class)).thenReturn(null);

        Object result = global.getValue(script);

        JSONObject expected = new JSONObject();
        expected.put("name", "not set");
        expected.put("version", "not set");
        expected.put("value", "not set");
        expected.put("type", "UNDEFINED");
        assertEquals(expected, result);
    }

    @Test
    public void testGetValue_notUseGCPParameter() throws Exception {
        Run<?, ?> run = mock(Run.class);
        Job<?, ?> job = mock(Job.class);
        when(script.$build()).thenReturn((Run) run);
        when(run.getParent()).thenReturn((Job) job);
        when(job.getProperty(GCPParameterPluginJobProperty.class)).thenReturn(property);
        when(property.isUseGCPParameter()).thenReturn(false);

        Object result = global.getValue(script);

        JSONObject expected = new JSONObject();
        expected.put("name", "not set");
        expected.put("version", "not set");
        expected.put("value", "not set");
        expected.put("type", "UNDEFINED");
        assertEquals(expected, result);
    }
}
