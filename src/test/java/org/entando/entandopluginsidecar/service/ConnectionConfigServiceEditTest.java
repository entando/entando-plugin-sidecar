package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_CRD;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.entando.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableKubernetesMockClient(crud = true, https = false)
class ConnectionConfigServiceEditTest {

    private ConnectionConfigService connectionConfigService;

    static KubernetesClient client;

    @BeforeEach
    public void setUp() {
        connectionConfigService = new ConnectionConfigService(client, ENTANDO_PLUGIN_NAME);
    }

    @Test
    void shouldEditConnectionConfig() throws Exception {
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
    void shouldRaiseNotFoundExceptionWhenTryingToEditAndPluginIsNotThere() throws Exception {
        TestHelper.deleteEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        Assertions.assertThatThrownBy(() -> {
            TestHelper.createEntandoPluginCrd(client);
            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
            TestHelper.createSecret(client, configDto);

            connectionConfigService.editConnectionConfig(configDto);
        }).isInstanceOf(NotFoundException.class)
                .hasMessage(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND);
    }

    @Test
    void shouldRaiseNotFoundExceptionWhenTryingToEditAndSecretIsNotThere() throws Exception {
        Assertions.assertThatThrownBy(() -> {
            ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
            TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

            connectionConfigService.editConnectionConfig(configDto);
        }).isInstanceOf(NotFoundException.class)
                .hasMessage(ConnectionConfigService.ERROR_SECRET_NOT_FOUND);
    }
}
