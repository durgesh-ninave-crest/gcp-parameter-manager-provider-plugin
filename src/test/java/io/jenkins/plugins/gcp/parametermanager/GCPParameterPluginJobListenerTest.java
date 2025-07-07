package io.jenkins.plugins.gcp.parametermanager;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GCPParameterPluginJobListenerTest {

    @Mock
    private GCPParameterPluginJobProperty prop;

    @Mock
    private GCPApiHelper apiHelper;

    @Mock
    private SystemCredentialsProvider provider;

    @Mock
    private List<Credentials> credentials;

    @Mock
    private TaskListener taskListener;

    private GCPParameterPluginJobListener listener;

    @Before
    public void setup() {
        listener = new GCPParameterPluginJobListener();
        lenient().when(provider.getCredentials()).thenReturn(credentials);
    }

    @Test
    public void testOnStarted_NoGCPParameterProperty() {
        Run<?, ?> run = mock(Run.class);
        Job<?, ?> job = mock(Job.class);
        when(run.getParent()).thenReturn((Job) job);
        when(job.getProperty(GCPParameterPluginJobProperty.class)).thenReturn(prop);
        when(prop.isUseGCPParameter()).thenReturn(false);
        listener.onStarted(run, taskListener);
    }

    @Test
    public void testOnStarted_NoCreateCredential() {
        Run<?, ?> run = mock(Run.class);
        Job<?, ?> job = mock(Job.class);
        when(run.getParent()).thenReturn((Job) job);
        when(job.getProperty(GCPParameterPluginJobProperty.class)).thenReturn(prop);
        when(prop.isUseGCPParameter()).thenReturn(true);
        when(prop.isCreateCredential()).thenReturn(false);
        listener.onStarted(run, taskListener);
    }

    @Test
    public void testOnStarted() throws Exception {
        Run<?, ?> run = mock(Run.class);
        Job<?, ?> job = mock(Job.class);
        when(run.getParent()).thenReturn((Job) job);
        when(job.getProperty(GCPParameterPluginJobProperty.class)).thenReturn(prop);
        when(prop.isUseGCPParameter()).thenReturn(true);
        when(prop.isCreateCredential()).thenReturn(true);

        lenient()
                .when(apiHelper.fetchParameterValueFromGCP("gcpParameterName", "gcpParameterVersion", "location"))
                .thenReturn(Map.of("version", "version", "value", "value"));

        credentials.clear();
        listener.onStarted(run, taskListener);
        assertNotNull(credentials);
    }
}
