package io.jenkins.plugins.gcp.parametermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.parametermanager.v1.*;
import com.google.cloud.parametermanager.v1.ParameterManagerClient.ListParameterVersionsPagedResponse;
import com.google.protobuf.ByteString;
import hudson.AbortException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

public class GCPApiHelperTest {

    @Mock
    private GCPApiHelper gcpApiHelper;

    private MockedStatic<Jenkins> staticJenkins;

    private MockedStatic<CredentialsProvider> staticCredential;

    private MockedStatic<GCPParameterPluginGlobalConfiguration> staticConfig;

    private MockedStatic<GoogleCredentials> staticGoogleCredentials;

    private GCPParameterPluginGlobalConfiguration config;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.openMocks(this);
        GoogleCredentials mockedCredentials = mock(GoogleCredentials.class);

        staticJenkins = mockStatic(Jenkins.class);
        staticCredential = mockStatic(CredentialsProvider.class);
        staticConfig = mockStatic(GCPParameterPluginGlobalConfiguration.class);
        staticGoogleCredentials = mockStatic(GoogleCredentials.class);

        List<FileCredentials> fileCredentialsList = IntStream.range(0, 3)
                .mapToObj(i -> {
                    FileCredentials fileCredentials = mock(FileCredentials.class);
                    when(fileCredentials.getId()).thenReturn("test-credential");
                    try {
                        when(fileCredentials.getContent())
                                .thenReturn(new ByteArrayInputStream("test-content".getBytes()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return fileCredentials;
                })
                .collect(Collectors.toList());

        config = mock(GCPParameterPluginGlobalConfiguration.class);
        staticConfig.when(GCPParameterPluginGlobalConfiguration::get).thenReturn(config);
        when(config.getGcpCredentialName()).thenReturn("test-credential");
        when(config.getProjectId()).thenReturn("test-project");

        staticJenkins.when(Jenkins::get).thenReturn(mock(Jenkins.class));
        staticCredential
                .when(() -> CredentialsProvider.lookupCredentialsInItemGroup(
                        eq(FileCredentials.class), any(Jenkins.class), isNull(), isNull()))
                .thenReturn(fileCredentialsList);

        staticGoogleCredentials
                .when(() -> GoogleCredentials.fromStream(any(InputStream.class)))
                .thenReturn(mockedCredentials);
        when(mockedCredentials.createScoped(anyString())).thenReturn(mockedCredentials);

        gcpApiHelper = new GCPApiHelper();
    }

    @After
    public void tearDown() {
        if (staticJenkins != null) {
            staticJenkins.close();
        }
        if (staticCredential != null) {
            staticCredential.close();
        }
        if (staticConfig != null) {
            staticConfig.close();
        }
        if (staticGoogleCredentials != null) {
            staticGoogleCredentials.close();
        }
    }

    @Test
    public void testGCPApiHelperConstructor() {
        assertNotNull(gcpApiHelper);
    }

    @Test
    public void testGetParameterManagerClient() throws IOException {
        FixedCredentialsProvider mockedProvider = mock(FixedCredentialsProvider.class);
        try (MockedStatic<FixedCredentialsProvider> staticFixedCredentials =
                mockStatic(FixedCredentialsProvider.class)) {
            staticFixedCredentials
                    .when(() -> FixedCredentialsProvider.create(any(GoogleCredentials.class)))
                    .thenReturn(mockedProvider);

            String locationId = "test-location";
            ParameterManagerClient client = gcpApiHelper.getParameterManagerClient(locationId);
            assertNotNull(client);
        }
    }

    @Test
    public void testGetParameterManagerClientWithEmptyLocationId() throws IOException {
        FixedCredentialsProvider mockedProvider = mock(FixedCredentialsProvider.class);
        try (MockedStatic<FixedCredentialsProvider> staticFixedCredentials =
                mockStatic(FixedCredentialsProvider.class)) {
            staticFixedCredentials
                    .when(() -> FixedCredentialsProvider.create(any(GoogleCredentials.class)))
                    .thenReturn(mockedProvider);

            String locationId = "";
            ParameterManagerClient client = gcpApiHelper.getParameterManagerClient(locationId);
            assertNotNull(client);
        }
    }

    @Test
    public void testFetchParamVersion() throws IOException {
        GCPApiHelper apiHelper = spy(new GCPApiHelper());
        ParameterManagerClient mockedClient = mock(ParameterManagerClient.class);
        doReturn(mockedClient).when(apiHelper).getParameterManagerClient(anyString());
        ListParameterVersionsPagedResponse mockedResponse = mock(ListParameterVersionsPagedResponse.class);
        Parameter mockedParameter = mock(Parameter.class);
        ParameterVersion mockedVersion = mock(ParameterVersion.class);
        Iterator<ParameterVersion> mockedIterator = mock(Iterator.class);
        Iterable<ParameterVersion> mockedIterable = mock(Iterable.class);

        when(mockedClient.listParameterVersions(any(ListParameterVersionsRequest.class)))
                .thenReturn(mockedResponse);
        when(mockedClient.getParameter(anyString())).thenReturn(mockedParameter);
        when(mockedParameter.getFormat()).thenReturn(ParameterFormat.UNFORMATTED);
        when(mockedResponse.iterateAll()).thenReturn(mockedIterable);
        when(mockedIterable.iterator()).thenReturn(mockedIterator);
        when(mockedVersion.getName()).thenReturn("versionName");
        when(mockedIterator.hasNext()).thenReturn(true, false);
        when(mockedIterator.next()).thenReturn(mockedVersion);

        String parameterId = "test-parameter";
        String parameterVersion = "";
        String locationId = "test-location";
        HashMap<String, String> paramInfo = apiHelper.fetchParamVersion(parameterId, parameterVersion, locationId);
        assertNotNull(paramInfo);
    }

    @Test
    public void testFetchParamVersionWithParameterVersion() throws IOException {
        GCPApiHelper apiHelper = spy(new GCPApiHelper());
        ParameterManagerClient mockedClient = mock(ParameterManagerClient.class);
        doReturn(mockedClient).when(apiHelper).getParameterManagerClient(anyString());
        ListParameterVersionsPagedResponse mockedResponse = mock(ListParameterVersionsPagedResponse.class);
        Parameter mockedParameter = mock(Parameter.class);
        ParameterVersion mockedVersion = mock(ParameterVersion.class);
        Iterator<ParameterVersion> mockedIterator = mock(Iterator.class);
        Iterable<ParameterVersion> mockedIterable = () -> mockedIterator;

        when(mockedClient.listParameterVersions(any(ListParameterVersionsRequest.class)))
                .thenReturn(mockedResponse);
        when(mockedClient.getParameter(anyString())).thenReturn(mockedParameter);
        when(mockedParameter.getFormat()).thenReturn(ParameterFormat.UNFORMATTED);
        when(mockedResponse.iterateAll()).thenReturn(mockedIterable);
        when(mockedVersion.getName())
                .thenReturn(
                        "projects/test-project/locations/test-location/parameters/test-parameter/versions/test-version");
        when(mockedIterator.hasNext()).thenReturn(true, true, false);
        when(mockedIterator.next()).thenReturn(mockedVersion);

        String parameterId = "test-parameter";
        String parameterVersion = "test-version";
        String locationId = "test-location";
        HashMap<String, String> paramInfo = apiHelper.fetchParamVersion(parameterId, parameterVersion, locationId);
        assertNotNull(paramInfo);
    }

    @Test
    public void testFetchParamVersionWithIOException() throws IOException {
        GCPApiHelper apiHelper = spy(new GCPApiHelper());
        ParameterManagerClient mockedClient = mock(ParameterManagerClient.class);
        doReturn(mockedClient).when(apiHelper).getParameterManagerClient(anyString());
        ListParameterVersionsPagedResponse mockedResponse = mock(ListParameterVersionsPagedResponse.class);
        Parameter mockedParameter = mock(Parameter.class);
        ParameterVersion mockedVersion = mock(ParameterVersion.class);
        Iterator<ParameterVersion> mockedIterator = mock(Iterator.class);
        Iterable<ParameterVersion> mockedIterable = mock(Iterable.class);

        when(mockedClient.listParameterVersions(any(ListParameterVersionsRequest.class)))
                .thenReturn(mockedResponse);
        when(mockedClient.getParameter(anyString())).thenReturn(mockedParameter);
        when(mockedParameter.getFormat()).thenReturn(ParameterFormat.UNFORMATTED);
        when(mockedResponse.iterateAll()).thenReturn(mockedIterable);
        when(mockedIterable.iterator()).thenReturn(mockedIterator);
        when(mockedVersion.getName()).thenReturn("versionName");
        when(mockedIterator.hasNext()).thenReturn(false);
        when(mockedIterator.next()).thenReturn(mockedVersion);

        String parameterId = "test-parameter";
        String parameterVersion = "";
        String locationId = "test-location";
        try {
            apiHelper.fetchParamVersion(parameterId, parameterVersion, locationId);
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("No parameter versions found for test-parameter", e.getMessage());
        }
    }

    @Test
    public void testFetchParamVersionWithAbortException() throws IOException {
        String parameterId = "test-parameter";
        String parameterVersion = "test-version";
        String locationId = "test-location";
        when(config.getGcpCredentialName()).thenReturn("");
        try {
            new GCPApiHelper().fetchParamVersion(parameterId, parameterVersion, locationId);
            fail("Expected AbortException");
        } catch (AbortException e) {
            assertEquals("Service Account key Credential ID not found for GCP.", e.getMessage());
        }
    }

    @Test
    public void testFetchParameterValueFromGCP() throws IOException {
        GCPApiHelper apiHelper = spy(new GCPApiHelper());
        ParameterManagerClient mockedClient = mock(ParameterManagerClient.class);
        doReturn(mockedClient).when(apiHelper).getParameterManagerClient(anyString());
        Map<String, String> mockParamInfo = new HashMap<>();
        mockParamInfo.put(
                "version",
                "projects/test-project/locations/test-location/parameters/test-parameter/versions/test-version");
        doReturn(mockParamInfo).when(apiHelper).fetchParamVersion(anyString(), anyString(), anyString());
        RenderParameterVersionResponse mockedRenderResponse = mock(RenderParameterVersionResponse.class);
        when(mockedClient.renderParameterVersion(
                        "projects/test-project/locations/test-location/parameters/test-parameter/versions/test-version"))
                .thenReturn(mockedRenderResponse);
        ByteString mockedPayload = mock(ByteString.class);
        when(mockedRenderResponse.getRenderedPayload()).thenReturn(mockedPayload);
        when(mockedPayload.toStringUtf8()).thenReturn("mocked-value");

        String parameterId = "test-parameter";
        String parameterVersion = "test-version";
        String locationId = "test-location";
        Map<String, String> paramInfo = apiHelper.fetchParameterValueFromGCP(parameterId, parameterVersion, locationId);
        assertNotNull(paramInfo);
        assertEquals("mocked-value", paramInfo.get("value"));
        assertEquals("test-version", paramInfo.get("version"));
    }
}
