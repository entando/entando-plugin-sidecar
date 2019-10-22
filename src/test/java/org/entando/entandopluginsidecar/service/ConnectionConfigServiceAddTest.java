package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.assertj.core.api.Java6JUnitSoftAssertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.entando.kubernetes.model.plugin.DoneableEntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginList;
import org.entando.web.exception.ConflictException;
import org.entando.web.exception.NotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConnectionConfigServiceAddTest {

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
    @SuppressWarnings("unchecked")
    public void shouldAddConfigAsSecret() throws Exception {
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
        safely.assertThat(fromYaml.getUrl()).isEqualTo(configDto.getUrl());
        safely.assertThat(fromYaml.getUsername()).isEqualTo(configDto.getUsername());
        safely.assertThat(fromYaml.getPassword()).isEqualTo(configDto.getPassword());
        safely.assertThat(fromYaml.getServiceType()).isEqualTo(configDto.getServiceType());
        EntandoPlugin entandoPlugin = TestHelper.getEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        safely.assertThat(entandoPlugin.getSpec().getConnectionConfigNames()).contains(configDto.getName());
    }

    @Test
    public void shouldAddConnectionConfigNameToPluginResource() throws Exception {
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
    public void shouldRaiseExceptionWhenAddingConfigAndCrdIsNotThere() {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND);

        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

        connectionConfigService.addConnectionConfig(configDto);
    }

    @Test
    public void shouldRaiseExceptionWhenAddingConfigAndPluginIsNotThere() throws Exception {
        expectedException.expect(NotFoundException.class);

        TestHelper.createEntandoPluginCrd(client);

        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

        connectionConfigService.addConnectionConfig(configDto);
    }

    @Test
    public void shouldRaiseExceptionWhenAddingConnectionWithSameName() throws Exception {
        expectedException.expect(ConflictException.class);
        expectedException.expectMessage(ConnectionConfigService.ERROR_SECRET_ALREADY_EXISTS);

        TestHelper.createEntandoPluginCrd(client);
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

        connectionConfigService.addConnectionConfig(configDto);
        connectionConfigService.addConnectionConfig(configDto);
    }

    @Test
    public void shouldNotDuplicateConnectionConfigNamesOnPlugin() throws Exception {
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
