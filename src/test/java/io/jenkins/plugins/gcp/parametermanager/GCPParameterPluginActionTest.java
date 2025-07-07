package io.jenkins.plugins.gcp.parametermanager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class GCPParameterPluginActionTest {

    private static final String EXPECTED_DISPLAY_NAME = "GCP Parameter Plugin";
    private static final String EXPECTED_ICON_FILE_NAME = "gear.png";
    private static final String EXPECTED_URL_NAME = "gcp-parameter-plugin";
    private static final String EXPECTED_DESCRIPTION = "GCP parameter manager plugin to fetch parameters.";

    private GCPParameterPluginAction action;

    @Before
    public void setup() {
        action = new GCPParameterPluginAction();
    }

    @Test
    public void testGetDisplayName() {
        assertEquals(EXPECTED_DISPLAY_NAME, action.getDisplayName());
    }

    @Test
    public void testGetIconFileName() {
        assertEquals(EXPECTED_ICON_FILE_NAME, action.getIconFileName());
    }

    @Test
    public void testGetUrlName() {
        assertEquals(EXPECTED_URL_NAME, action.getUrlName());
    }

    @Test
    public void testGetDescription() {
        assertEquals(EXPECTED_DESCRIPTION, action.getDescription());
    }

    @Test
    public void testGetProjectID() {
        try (MockedStatic<GCPParameterPluginGlobalConfiguration> staticConfig =
                mockStatic(GCPParameterPluginGlobalConfiguration.class)) {
            GCPParameterPluginGlobalConfiguration config = mock(GCPParameterPluginGlobalConfiguration.class);
            staticConfig.when(GCPParameterPluginGlobalConfiguration::get).thenReturn(config);
            when(config.getProjectId()).thenReturn("test-project");
            GCPParameterPluginAction pluginAction = spy(new GCPParameterPluginAction());
            assertEquals("test-project", pluginAction.getProjectId());
        }
    }

    @Test
    public void testGetGcpCredentialName() {
        try (MockedStatic<GCPParameterPluginGlobalConfiguration> staticConfig =
                mockStatic(GCPParameterPluginGlobalConfiguration.class)) {
            GCPParameterPluginGlobalConfiguration config = mock(GCPParameterPluginGlobalConfiguration.class);
            staticConfig.when(GCPParameterPluginGlobalConfiguration::get).thenReturn(config);
            when(config.getGcpCredentialName()).thenReturn("test-credential");
            GCPParameterPluginAction pluginAction = spy(new GCPParameterPluginAction());
            assertEquals("test-credential", pluginAction.getGcpCredentialName());
        }
    }

    @Test
    public void testGetGcpParameterNames() {
        try (MockedStatic<GCPParameterPluginGlobalConfiguration> staticConfig =
                mockStatic(GCPParameterPluginGlobalConfiguration.class)) {
            GCPParameterPluginGlobalConfiguration config = mock(GCPParameterPluginGlobalConfiguration.class);
            staticConfig.when(GCPParameterPluginGlobalConfiguration::get).thenReturn(config);
            when(config.getParameterNames()).thenReturn(new HashSet<>());
            GCPParameterPluginAction pluginAction = spy(new GCPParameterPluginAction());
            assertNotNull(pluginAction.getParameterNames());
        }
    }
}
