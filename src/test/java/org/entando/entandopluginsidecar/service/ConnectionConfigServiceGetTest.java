package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;
import static org.junit.Assert.fail;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.util.Optional;
import org.assertj.core.api.Java6JUnitSoftAssertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConnectionConfigServiceGetTest {

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    @Rule
    public Java6JUnitSoftAssertions safely = new Java6JUnitSoftAssertions();

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

        Optional<ConnectionConfigDto> configFromService = connectionConfigService
                .getConnectionConfig(configDto.getName());

        if (configFromService.isPresent()) {
            safely.assertThat(configFromService.get().getUrl()).isEqualTo(configDto.getUrl());
            safely.assertThat(configFromService.get().getUsername()).isEqualTo(configDto.getUsername());
            safely.assertThat(configFromService.get().getPassword()).isEqualTo(configDto.getPassword());
            safely.assertThat(configFromService.get().getServiceType()).isEqualTo(configDto.getServiceType());
        } else {
            fail("Connection config is empty!");
        }
    }

    @Test
    public void shouldReturnEmptyIfConfigIsNotInEntandoPlugin() throws Exception {
        // Given
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);

        // When
        Optional<ConnectionConfigDto> connectionConfig = connectionConfigService
                .getConnectionConfig(configDto.getName());

        // Then
        assertThat(connectionConfig.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnEmptyIfSecretIsNotThere() throws Exception {
        // Given
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        // When
        Optional<ConnectionConfigDto> connectionConfig = connectionConfigService
                .getConnectionConfig(configDto.getName());

        // Then
        assertThat(connectionConfig.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnEmptyIfConfigYamlIsNotInSecret() throws Exception {
        // Given
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);
        Secret secret = client.secrets().inNamespace(client.getConfiguration().getNamespace())
                .withName(configDto.getName())
                .get();
        secret.setStringData(null);
        client.secrets().inNamespace(client.getConfiguration().getNamespace()).createOrReplace(secret);
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        // When
        Optional<ConnectionConfigDto> connectionConfig = connectionConfigService
                .getConnectionConfig(configDto.getName());

        // Then
        assertThat(connectionConfig.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnEmptyForNonExistingConfig() throws Exception {
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);

        Optional<ConnectionConfigDto> configDto = connectionConfigService.getConnectionConfig("does-not-exist");

        assertThat(configDto.isPresent()).isFalse();
    }
}
