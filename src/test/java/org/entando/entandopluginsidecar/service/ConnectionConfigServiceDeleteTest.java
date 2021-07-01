package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableKubernetesMockClient(crud = true, https = false)
class ConnectionConfigServiceDeleteTest {

    private ConnectionConfigService connectionConfigService;

    static KubernetesClient client;

    @BeforeEach
    public void setUp() {
        connectionConfigService = new ConnectionConfigService(client, ENTANDO_PLUGIN_NAME);
    }

    @Test
    void shouldRemoveConnectionConfig() throws Exception {
        // Given
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto);
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto.getName());

        // When
        connectionConfigService.removeConnectionConfig(configDto.getName());

        // Then
        EntandoPlugin entandoPlugin = TestHelper.getEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        assertThat(entandoPlugin.getSpec().getConnectionConfigNames()).doesNotContain(configDto.getName());
        assertThat(client.secrets().withName(configDto.getName()).get()).isNull();
    }
}
