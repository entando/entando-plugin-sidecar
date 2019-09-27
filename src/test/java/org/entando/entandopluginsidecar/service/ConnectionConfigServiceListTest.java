package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.util.List;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConnectionConfigServiceListTest {

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
    public void shouldGetAllConnectionConfigs() throws Exception {
        // Given
        ConnectionConfigDto configDto1 = TestHelper.getRandomConnectionConfigDto();
        ConnectionConfigDto configDto2 = TestHelper.getRandomConnectionConfigDto();
        ConnectionConfigDto configDto3 = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createSecret(client, configDto1);
        TestHelper.createSecret(client, configDto2);
        TestHelper.createSecret(client, configDto3);
        TestHelper.createEntandoPluginWithConfigNames(client, ENTANDO_PLUGIN_NAME, configDto1.getName(),
                configDto2.getName());

        // When
        List<ConnectionConfigDto> configDtos = connectionConfigService.getAllConnectionConfig();

        // Then
        assertThat(configDtos).containsExactlyInAnyOrder(configDto1, configDto2);
        assertThat(configDtos).doesNotContain(configDto3);
    }

    @Test
    public void shouldReturnEmptyListForNonExistingConfigs() throws Exception {
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);

        List<ConnectionConfigDto> allConnectionConfig = connectionConfigService.getAllConnectionConfig();

        assertThat(allConnectionConfig).isEmpty();
    }
}
