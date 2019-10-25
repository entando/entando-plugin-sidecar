package org.entando.entandopluginsidecar.service;

import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.assertj.core.api.Java6JUnitSoftAssertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.web.exception.NotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConnectionConfigServiceGetTest {

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    @Rule
    public Java6JUnitSoftAssertions safely = new Java6JUnitSoftAssertions();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ConnectionConfigService connectionConfigService;

    private KubernetesClient client;

    @Before
    public void setUp() {
        client = server.getClient();
        connectionConfigService = new ConnectionConfigService(client, ENTANDO_PLUGIN_NAME);
    }

    @Test
    public void shouldGetConnectionConfigByName() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        ConnectionConfigDto configFromService = connectionConfigService.getConnectionConfig(configDto.getName());

        safely.assertThat(configFromService).isEqualTo(configDto);
    }

    @Test
    public void shouldRaiseNotFoundExceptionIfEntandoPluginIsNotThere() throws Exception {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND);

        TestHelper.createEntandoPluginCrd(client);

        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);

        connectionConfigService.getConnectionConfig(configDto.getName());
    }

    @Test
    public void shouldRaiseNotFoundExceptionIfSecretIsNotThere() throws Exception {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(ConnectionConfigService.ERROR_SECRET_NOT_FOUND);

        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        connectionConfigService.getConnectionConfig(configDto.getName());
    }

    @Test
    public void shouldRaiseNotFoundExceptionIfConfigIsNotInEntandoPlugin() throws Exception {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(ConnectionConfigService.ERROR_SECRET_NOT_FOUND);

        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);

        connectionConfigService.getConnectionConfig(configDto.getName());
    }

    @Test
    public void shouldRaiseNotFoundExceptionIfConfigYamlIsNotInSecret() throws Exception {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(ConnectionConfigService.ERROR_SECRET_NOT_FOUND);

        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);
        Secret secret = client.secrets().inNamespace(client.getConfiguration().getNamespace())
                .withName(configDto.getName())
                .get();
        secret.setStringData(null);
        client.secrets().inNamespace(client.getConfiguration().getNamespace()).createOrReplace(secret);
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        connectionConfigService.getConnectionConfig(configDto.getName());
    }
}
