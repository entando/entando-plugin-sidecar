package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.entando.kubernetes.model.plugin.DoneableEntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginList;
import org.entando.web.exception.ConflictException;
import org.entando.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableKubernetesMockClient(crud = true, https = false)
class ConnectionConfigServiceAddTest {

    public SoftAssertions safely = new SoftAssertions();

    private ConnectionConfigService connectionConfigService;

    static KubernetesClient client;

    @BeforeEach
    public void setUp() {
        connectionConfigService = new ConnectionConfigService(client, ENTANDO_PLUGIN_NAME);
    }

    @Test
    void shouldAddConfigAsSecret() throws Exception {
        // Given
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

        // When
        connectionConfigService.addConnectionConfig(configDto);

        // Then
        Secret secret = client.secrets().withName(configDto.getName()).get();
        safely.assertThat(secret.getMetadata().getName()).isEqualTo(configDto.getName());
        ConnectionConfigDto fromYaml = YamlUtils
                .fromYaml(secret.getStringData().get(ConnectionConfigService.CONFIG_YAML));
        safely.assertThat(fromYaml.getName()).isEqualTo(configDto.getName());
        safely.assertThat(fromYaml.getProperties()).isEqualTo(configDto.getProperties());
        EntandoPlugin entandoPlugin = TestHelper.getEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        safely.assertThat(entandoPlugin.getSpec().getConnectionConfigNames()).contains(configDto.getName());
    }

    @Test
    void shouldAddConnectionConfigNameToPluginResource() throws Exception {
        // Given
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

        // When
        connectionConfigService.addConnectionConfig(configDto);

        // Then
        CustomResourceDefinition definition = TestHelper.getEntandoPluginCrd(client);
        EntandoPlugin retrievedPlugin = client
                .customResources(definition, EntandoPlugin.class, EntandoPluginList.class, DoneableEntandoPlugin.class)
                .withName(ENTANDO_PLUGIN_NAME).get();
        assertThat(retrievedPlugin.getSpec().getConnectionConfigNames()).contains(configDto.getName());
    }

    @Test
    void shouldRaiseExceptionWhenAddingConfigAndCrdIsNotThere() {
        TestHelper.deleteEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        Assertions.assertThatThrownBy(() -> {
            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

            connectionConfigService.addConnectionConfig(configDto);
        }).isInstanceOf(NotFoundException.class)
                .hasMessage(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND);
    }

    @Test
    void shouldRaiseExceptionWhenAddingConfigAndPluginIsNotThere() throws Exception {
        Assertions.assertThatThrownBy(() -> {
            TestHelper.createEntandoPluginCrd(client);

            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

            connectionConfigService.addConnectionConfig(configDto);
        }).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldRaiseExceptionWhenAddingConnectionWithSameName() throws Exception {
        Assertions.assertThatThrownBy(() -> {
            TestHelper.createEntandoPluginCrd(client);
            TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

            connectionConfigService.addConnectionConfig(configDto);
            connectionConfigService.addConnectionConfig(configDto);
        }).isInstanceOf(ConflictException.class)
                .hasMessage(ConnectionConfigService.ERROR_SECRET_ALREADY_EXISTS);
    }

    @Test
    void shouldNotDuplicateConnectionConfigNamesOnPlugin() throws Exception {
        // Given
        TestHelper.createEntandoPluginCrd(client);
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

        // When
        try {
            connectionConfigService.addConnectionConfig(configDto);
            connectionConfigService.addConnectionConfig(configDto);
        } catch (ConflictException e) { // NOPMD
            // do nothing
        }

        // Then
        EntandoPlugin entandoPlugin = TestHelper.getEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        assertThat(entandoPlugin.getSpec().getConnectionConfigNames()).doesNotHaveDuplicates();
    }
}
