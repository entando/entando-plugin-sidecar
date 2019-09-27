package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConnectionConfigServiceDeleteTest {

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    private ConnectionConfigService connectionConfigService;

    private KubernetesClient client;

    @Before
    public void setUp() {
        client = server.getClient();
        connectionConfigService = new ConnectionConfigService(client, ENTANDO_PLUGIN_NAME);
    }

    @Test
    public void shouldRemoveConnectionConfig() throws Exception {
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
