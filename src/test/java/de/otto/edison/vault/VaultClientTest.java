package de.otto.edison.vault;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static de.otto.edison.vault.VaultClient.vaultClient;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

public class VaultClientTest {

    private VaultClient testee;
    private AsyncHttpClient asyncHttpClient;
    private VaultTokenReader vaultTokenReader;
    private ConfigProperties configProperties;

    @BeforeMethod
    public void setUp() throws Exception {
        asyncHttpClient = mock(AsyncHttpClient.class);
        configProperties = mock(ConfigProperties.class);
    }

    @Test
    public void shouldReadTheDefaultFieldValue() throws Exception {
        // given
        when(configProperties.getBaseUrl()).thenReturn("http://someBaseUrl");
        when(configProperties.getSecretPath()).thenReturn("/someSecretPath");

        testee = vaultClient(configProperties, "someClientToken");
        testee.asyncHttpClient = asyncHttpClient;

        Response response = mock(Response.class);
        BoundRequestBuilder boundRequestBuilder = mock(BoundRequestBuilder.class);
        ListenableFuture listenableFuture = mock(ListenableFuture.class);

        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn(createReadResponse("someKey", "value", "someValue"));
        when(asyncHttpClient.prepareGet(eq("http://someBaseUrl/v1/someSecretPath/someKey"))).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.setHeader(eq("X-Vault-Token"), eq("someClientToken"))).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.execute()).thenReturn(listenableFuture);
        when(listenableFuture.get()).thenReturn(response);

        // when
        String propertyValue = testee.read("someKey");

        // then
        assertThat(propertyValue, is("someValue"));
    }

    @Test
    public void shouldReturnNullIfNoFieldValueExists() throws Exception {
        // given
        when(configProperties.getBaseUrl()).thenReturn("http://someBaseUrl");
        when(configProperties.getSecretPath()).thenReturn("/someSecretPath");

        testee = vaultClient(configProperties, "someClientToken");
        testee.asyncHttpClient = asyncHttpClient;

        Response response = mock(Response.class);
        BoundRequestBuilder boundRequestBuilder = mock(BoundRequestBuilder.class);
        ListenableFuture listenableFuture = mock(ListenableFuture.class);

        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn(createReadResponse("someKey", "someField", "someValue"));
        when(asyncHttpClient.prepareGet(eq("http://someBaseUrl/v1/someSecretPath/someKey"))).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.setHeader(eq("X-Vault-Token"), eq("someClientToken"))).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.execute()).thenReturn(listenableFuture);
        when(listenableFuture.get()).thenReturn(response);

        // when
        String fieldValue = testee.read("someKey");

        // then
        assertThat(fieldValue, is(nullValue()));
    }

    @Test
    public void shouldReadAnArbitraryField() throws Exception {
        // given
        when(configProperties.getBaseUrl()).thenReturn("http://someBaseUrl");
        when(configProperties.getSecretPath()).thenReturn("/someSecretPath");

        testee = vaultClient(configProperties, "someClientToken");
        testee.asyncHttpClient = asyncHttpClient;

        Response response = mock(Response.class);
        BoundRequestBuilder boundRequestBuilder = mock(BoundRequestBuilder.class);
        ListenableFuture listenableFuture = mock(ListenableFuture.class);

        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn(createReadResponse("someKey", "someFieldOtherThanValue", "someValue"));
        when(asyncHttpClient.prepareGet(eq("http://someBaseUrl/v1/someSecretPath/someKey"))).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.setHeader(eq("X-Vault-Token"), eq("someClientToken"))).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.execute()).thenReturn(listenableFuture);
        when(listenableFuture.get()).thenReturn(response);

        // when
        Optional<String> fieldValue = testee.readField("someKey", "someFieldOtherThanValue");

        // then
        assertThat(fieldValue.isPresent(), is(true));
        assertThat(fieldValue.get(), is("someValue"));
    }

    @Test
    public void shouldReturnEmptyOptionalForANonExistingField() throws Exception {
        // given
        when(configProperties.getBaseUrl()).thenReturn("http://someBaseUrl");
        when(configProperties.getSecretPath()).thenReturn("/someSecretPath");

        testee = vaultClient(configProperties, "someClientToken");
        testee.asyncHttpClient = asyncHttpClient;

        Response response = mock(Response.class);
        BoundRequestBuilder boundRequestBuilder = mock(BoundRequestBuilder.class);
        ListenableFuture listenableFuture = mock(ListenableFuture.class);

        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponseBody()).thenReturn(createReadResponse("someKey", "someField", "someValue"));
        when(asyncHttpClient.prepareGet(eq("http://someBaseUrl/v1/someSecretPath/someKey"))).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.setHeader(eq("X-Vault-Token"), eq("someClientToken"))).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.execute()).thenReturn(listenableFuture);
        when(listenableFuture.get()).thenReturn(response);

        // when
        Optional<String> fieldValue = testee.readField("someKey", "someUnknownField");

        // then
        assertThat(fieldValue.isPresent(), is(false));
    }

    private String createReadResponse(final String key, final String field, final String value) {
        return "{\"lease_id\":\"develop/p13n/" + key + "/b74f148e-12de-dbfb-b03f-c950c587e8ea\",\"renewable\":false,\"lease_duration\":2592000,\"data\":{\"" + field + "\":\"" + value + "\"},\"auth\":null}";
    }

    @Test
    public void shouldThrowRuntimeExceptionIfReadFails() throws Exception {
        // given
        when(configProperties.getBaseUrl()).thenReturn("http://someBaseUrl");
        when(configProperties.getSecretPath()).thenReturn("/someSecretPath");

        testee = vaultClient(configProperties, "someClientToken");
        testee.asyncHttpClient = asyncHttpClient;

        Response response = mock(Response.class);
        BoundRequestBuilder boundRequestBuilder = mock(BoundRequestBuilder.class);
        ListenableFuture listenableFuture = mock(ListenableFuture.class);

        when(response.getResponseBody()).thenReturn(null);
        when(response.getStatusCode()).thenReturn(500);
        when(asyncHttpClient.prepareGet("http://someBaseUrl/v1/someSecretPath/someKey")).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.setHeader("X-Vault-Token", "someClientToken")).thenReturn(boundRequestBuilder);
        when(boundRequestBuilder.execute()).thenReturn(listenableFuture);
        when(listenableFuture.get()).thenReturn(response);

        // when
        try {
            testee.read("someKey");
            fail();
        } catch (RuntimeException e) {
            // then
            assertThat(e.getMessage(), is("read of vault property 'someKey' with token 'someClientToken' from url 'http://someBaseUrl/v1/someSecretPath/someKey' failed, return code is '500'"));
        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void shouldTrimUrlSlashes() throws Exception {
        // given
        when(configProperties.getBaseUrl()).thenReturn("http://someBaseUrl");
        when(configProperties.getSecretPath()).thenReturn("/someSecretPath");

        testee = vaultClient(configProperties, "someClientToken");
        testee.asyncHttpClient = asyncHttpClient;

        // when
        testee.read("someKey");

        // then
        verify(asyncHttpClient).prepareGet("http://someBaseUrl/v1/someSecretPath/someKey");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void shouldAddMissingUrlSlashes() throws Exception {
        // given
        when(configProperties.getBaseUrl()).thenReturn("http://someBaseUrl");
        when(configProperties.getSecretPath()).thenReturn("/someSecretPath");

        testee = vaultClient(configProperties, "someClientToken");
        testee.asyncHttpClient = asyncHttpClient;

        // when
        testee.read("someKey");

        // then
        verify(asyncHttpClient).prepareGet("http://someBaseUrl/v1/someSecretPath/someKey");
    }
}
