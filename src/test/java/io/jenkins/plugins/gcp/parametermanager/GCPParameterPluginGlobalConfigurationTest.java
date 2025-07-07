package io.jenkins.plugins.gcp.parametermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

public class GCPParameterPluginGlobalConfigurationTest {

    private GCPParameterPluginGlobalConfiguration config;

    AtomicReference<String> tempProjectId = new AtomicReference<>("");
    AtomicReference<String> tempCredId = new AtomicReference<>("");

    @Before
    public void setup() {
        config = mock(GCPParameterPluginGlobalConfiguration.class);

        doAnswer(invocation -> {
                    tempProjectId.set(invocation.getArgument(0));
                    return null;
                })
                .when(config)
                .setProjectId(anyString());
        when(config.getProjectId()).thenAnswer(invocation -> tempProjectId.get());

        doAnswer(invocation -> {
                    tempCredId.set(invocation.getArgument(0));
                    return null;
                })
                .when(config)
                .setGcpCredentialName(anyString());
        when(config.getGcpCredentialName()).thenAnswer(invocation -> tempCredId.get());
    }

    @Test
    public void testGetProjectId() {
        assertEquals("", config.getProjectId());
        config.setProjectId("test-project-id");
        assertEquals("test-project-id", config.getProjectId());
    }

    @Test
    public void testSetProjectId() {
        config.setProjectId(null);
        assertEquals("", config.getProjectId());

        config.setProjectId("");
        assertEquals("", config.getProjectId());

        config.setProjectId("test-project-id");
        assertEquals("test-project-id", config.getProjectId());
    }

    @Test
    public void testGetGcpCredentialName() {
        assertEquals("", config.getGcpCredentialName());

        config.setGcpCredentialName("test-credential-name");
        assertEquals("test-credential-name", config.getGcpCredentialName());
    }

    @Test
    public void testSetGcpCredentialName() {
        config.setGcpCredentialName("test-credential-name");
        assertEquals("test-credential-name", config.getGcpCredentialName());
    }

    @Test
    public void testAddParameter() {
        config.addParameter(null);
        assertEquals(0, config.getParameterNames().size());

        HashMap<String, String> emptyParam = new HashMap<>();
        config.addParameter(emptyParam);
        assertEquals(0, config.getParameterNames().size());

        HashMap<String, String> param = new HashMap<>();
        param.put("name", "test-param");
        param.put("version", "test-version");
        config.addParameter(param);
        assertNotNull(config.getParameterNames());
    }

    @Test
    public void testRemoveParameter() {
        HashMap<String, String> param = new HashMap<>();
        param.put("name", "test-param");
        param.put("version", "test-version");
        config.addParameter(param);
        assertNotNull(config.getParameterNames());

        config.removeParameter(param);
        assertEquals(0, config.getParameterNames().size());
    }

    @Test
    public void testRemoveAllParameterVersions() {
        HashMap<String, String> matchingParam = new HashMap<>();
        matchingParam.put("parameter", "param1");
        matchingParam.put("location", "us-east1");
        HashMap<String, String> nonMatchingParam = new HashMap<>();
        nonMatchingParam.put("parameter", "param2");
        nonMatchingParam.put("location", "us-east1");
        config.addParameter(matchingParam);
        config.addParameter(nonMatchingParam);

        config.removeAllParameterVersions("param1", "us-east1");
        Set<HashMap<String, String>> remaining = config.getParameterNames();
        assertFalse(remaining.contains(matchingParam));
    }

    @Test
    public void testGetParameterNames() {
        assertEquals(0, config.getParameterNames().size());

        HashMap<String, String> param = new HashMap<>();
        param.put("name", "test-param");
        param.put("version", "test-version");
        config.addParameter(param);
    }
}
