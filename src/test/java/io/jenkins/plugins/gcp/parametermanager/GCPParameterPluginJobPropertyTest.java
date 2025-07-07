package io.jenkins.plugins.gcp.parametermanager;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GCPParameterPluginJobPropertyTest {
    private GCPParameterPluginJobProperty property;

    @Before
    public void setup() {
        property = new GCPParameterPluginJobProperty();
    }

    @Test
    public void testDefaultConstructor() {
        assertFalse(property.isUseGCPParameter());
        assertEquals("", property.getGcpParameterName());
        assertEquals("", property.getGcpParameterVersion());
        assertEquals("", property.getLocation());
        assertTrue(property.isCreateCredential());
    }

    @Test
    public void testDataBoundConstructor() {
        property = new GCPParameterPluginJobProperty("gcpParameterName", "gcpParameterVersion", true, "location");

        assertTrue(property.isUseGCPParameter());
        assertEquals("gcpParameterName", property.getGcpParameterName());
        assertEquals("gcpParameterVersion", property.getGcpParameterVersion());
        assertEquals("location", property.getLocation());
        assertTrue(property.isCreateCredential());
    }

    @Test
    public void testIsUseGCPParameter() {
        property = new GCPParameterPluginJobProperty("gcpParameterName", "gcpParameterVersion", true, "location");
        assertTrue(property.isUseGCPParameter());
        property = new GCPParameterPluginJobProperty();
        assertFalse(property.isUseGCPParameter());
    }

    @Test
    public void testGetGcpParameterName() {
        property = new GCPParameterPluginJobProperty("gcpParameterName", "gcpParameterVersion", true, "location");
        assertEquals("gcpParameterName", property.getGcpParameterName());
    }

    @Test
    public void testGetGcpParameterVersion() {
        property = new GCPParameterPluginJobProperty("gcpParameterName", "gcpParameterVersion", true, "location");
        assertEquals("gcpParameterVersion", property.getGcpParameterVersion());
    }

    @Test
    public void testIsCreateCredential() {
        property = new GCPParameterPluginJobProperty("gcpParameterName", "gcpParameterVersion", true, "location");
        assertTrue(property.isCreateCredential());
        property = new GCPParameterPluginJobProperty("gcpParameterName", "gcpParameterVersion", false, "location");
        assertFalse(property.isCreateCredential());
    }

    @Test
    public void testGetLocation() {
        property = new GCPParameterPluginJobProperty("gcpParameterName", "gcpParameterVersion", true, "location");
        assertEquals("location", property.getLocation());
    }
}
