package org.entando.entandopluginsidecar.service;

import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableKubernetesMockClient(crud = true, https = false)
class ConnectionConfigServiceGetTest {

    public SoftAssertions safely = new SoftAssertions();

    private ConnectionConfigService connectionConfigService;

    static KubernetesClient client;

    @BeforeEach
    public void setUp() {
        connectionConfigService = new ConnectionConfigService(client, ENTANDO_PLUGIN_NAME);
    }

    @Test
    void shouldGetConnectionConfigByName() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        ConnectionConfigDto configFromService = connectionConfigService.getConnectionConfig(configDto.getName());

        safely.assertThat(configFromService).isEqualTo(configDto);
    }

    @Test
    void shouldRaiseNotFoundExceptionIfEntandoPluginIsNotThere() throws Exception {
        TestHelper.deleteEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        Assertions.assertThatThrownBy(() -> {
            TestHelper.createEntandoPluginCrd(client);

            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
            TestHelper.createSecret(client, configDto);

            connectionConfigService.getConnectionConfig(configDto.getName());
        }).isInstanceOf(NotFoundException.class)
                .hasMessage(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND);
    }

    @Test
    void shouldRaiseNotFoundExceptionIfSecretIsNotThere() throws Exception {
        Assertions.assertThatThrownBy(() -> {
            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
            TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

            connectionConfigService.getConnectionConfig(configDto.getName());
        }).isInstanceOf(NotFoundException.class)
                .hasMessage(ConnectionConfigService.ERROR_SECRET_NOT_FOUND);
    }

    @Test
    void shouldRaiseNotFoundExceptionIfConfigIsNotInEntandoPlugin() throws Exception {
        Assertions.assertThatThrownBy(() -> {
            TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
            TestHelper.createSecret(client, configDto);

            connectionConfigService.getConnectionConfig(configDto.getName());
        }).isInstanceOf(NotFoundException.class)
                .hasMessage(ConnectionConfigService.ERROR_SECRET_NOT_FOUND);
    }

    @Test
    void shouldRaiseNotFoundExceptionIfConfigYamlIsNotInSecret() throws Exception {
        Assertions.assertThatThrownBy(() -> {
            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
            TestHelper.createSecret(client, configDto);
            Secret secret = client.secrets().inNamespace(client.getConfiguration().getNamespace())
                    .withName(configDto.getName())
                    .get();
            secret.setStringData(null);
            client.secrets().inNamespace(client.getConfiguration().getNamespace()).createOrReplace(secret);
            TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

            connectionConfigService.getConnectionConfig(configDto.getName());
        }).isInstanceOf(NotFoundException.class)
                .hasMessage(ConnectionConfigService.ERROR_SECRET_NOT_FOUND);
    }
}
