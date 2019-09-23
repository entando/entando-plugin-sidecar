package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Java6JUnitSoftAssertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.entando.kubernetes.model.plugin.DoneableEntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginList;
import org.entando.web.exception.NotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConnectionConfigServiceTest {

    private static final String CONFIG_YAML = "config.yaml";
    private static final String API_VERSION = "v1";
    private static final String ENTANDO_PLUGIN_NAME = "testplugin";

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
                .fromYaml(secret.getStringData().get(CONFIG_YAML));
        safely.assertThat(fromYaml.getUrl()).isEqualTo(configDto.getUrl());
        safely.assertThat(fromYaml.getUsername()).isEqualTo(configDto.getUsername());
        safely.assertThat(fromYaml.getPassword()).isEqualTo(configDto.getPassword());
        safely.assertThat(fromYaml.getServiceType()).isEqualTo(configDto.getServiceType());
        EntandoPlugin entandoPlugin = TestHelper.getEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        safely.assertThat(entandoPlugin.getSpec().getConnectionConfigNames()).contains(configDto.getName());
    }

    @Test
    public void shouldGetConnectionConfigByName() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());
        client.secrets().createNew()
                .withApiVersion(API_VERSION)
                .withNewMetadata().withName(configDto.getName()).endMetadata()
                .withStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(configDto)))
                .done();

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
        client.secrets().createNew()
                .withApiVersion(API_VERSION)
                .withNewMetadata().withName(configDto.getName()).endMetadata()
                .withStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(configDto)))
                .done();

        // When
        Optional<ConnectionConfigDto> connectionConfig = connectionConfigService
                .getConnectionConfig(configDto.getName());

        // Then
        assertThat(connectionConfig.isPresent()).isFalse();
    }

    @Test
    public void shouldGetAllConnectionConfigs() throws Exception {
        // Given
        ConnectionConfigDto configDto1 = TestHelper.getRandomConnectionConfigDto();
        ConnectionConfigDto configDto2 = TestHelper.getRandomConnectionConfigDto();
        ConnectionConfigDto configDto3 = TestHelper.getRandomConnectionConfigDto();
        client.secrets().createNew()
                .withApiVersion(API_VERSION)
                .withNewMetadata().withName(configDto1.getName()).endMetadata()
                .withStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(configDto1)))
                .done();
        client.secrets().createNew()
                .withApiVersion(API_VERSION)
                .withNewMetadata().withName(configDto2.getName()).endMetadata()
                .withStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(configDto2)))
                .done();
        client.secrets().createNew()
                .withApiVersion(API_VERSION)
                .withNewMetadata().withName(configDto3.getName()).endMetadata()
                .withStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(configDto3)))
                .done();
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto1.getName(),
                configDto2.getName());

        // When
        List<ConnectionConfigDto> configDtos = connectionConfigService.getAllConnectionConfig();

        // Then
        assertThat(configDtos).containsExactlyInAnyOrder(configDto1, configDto2);
        assertThat(configDtos).doesNotContain(configDto3);
    }

    @Test
    public void shouldReturnEmptyForNonExistingConfig() throws Exception {
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);

        Optional<ConnectionConfigDto> configDto = connectionConfigService.getConnectionConfig("does-not-exist");

        assertThat(configDto.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnEmptyListForNonExistingConfigs() throws Exception {
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);

        List<ConnectionConfigDto> allConnectionConfig = connectionConfigService.getAllConnectionConfig();

        assertThat(allConnectionConfig).isEmpty();
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
}
