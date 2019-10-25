package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.apache.commons.lang3.RandomStringUtils;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.entando.web.exception.NotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConnectionConfigServiceEditTest {

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

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
    public void shouldEditConnectionConfig() throws Exception {
        // Given
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        // When
        configDto.setProperties(ImmutableMap
                .of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),
                        RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
        ConnectionConfigDto fromService = connectionConfigService.editConnectionConfig(configDto);

        // Then
        assertThat(fromService).isEqualTo(configDto);
        Secret secret = client.secrets().withName(configDto.getName()).get();
        ConnectionConfigDto fromYaml = YamlUtils
                .fromYaml(secret.getStringData().get(ConnectionConfigService.CONFIG_YAML));
        assertThat(fromYaml).isEqualTo(configDto);
    }

    @Test
    public void shouldRaiseNotFoundExceptionWhenTryingToEditAndPluginIsNotThere() throws Exception {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND);

        TestHelper.createEntandoPluginCrd(client);
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);

        connectionConfigService.editConnectionConfig(configDto);
    }

    @Test
    public void shouldRaiseNotFoundExceptionWhenTryingToEditAndSecretIsNotThere() throws Exception {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(ConnectionConfigService.ERROR_SECRET_NOT_FOUND);

        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        connectionConfigService.editConnectionConfig(configDto);
    }
}
