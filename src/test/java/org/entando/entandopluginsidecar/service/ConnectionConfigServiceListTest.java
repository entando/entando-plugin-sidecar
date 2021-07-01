package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.util.TestHelper.ENTANDO_PLUGIN_NAME;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnableKubernetesMockClient(crud = true, https = false)
class ConnectionConfigServiceListTest {

    private ConnectionConfigService connectionConfigService;

    static KubernetesClient client;

    @BeforeEach
    public void setUp() {
        connectionConfigService = new ConnectionConfigService(client, ENTANDO_PLUGIN_NAME);
    }

    @Test
    void shouldGetAllConnectionConfigs() throws Exception {
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
    void shouldReturnEmptyListForNonExistingConfigs() throws Exception {
        TestHelper.createEntandoPlugin(client, ENTANDO_PLUGIN_NAME);

        List<ConnectionConfigDto> allConnectionConfig = connectionConfigService.getAllConnectionConfig();

        assertThat(allConnectionConfig).isEmpty();
    }

    @Test
    void shouldRaiseNotFoundExceptionIfPluginIsNotThere() throws Exception {
        TestHelper.deleteEntandoPlugin(client, ENTANDO_PLUGIN_NAME);
        Assertions.assertThatThrownBy(() -> {
            TestHelper.createEntandoPluginCrd(client);

            connectionConfigService.getAllConnectionConfig();
        }).isInstanceOf(NotFoundException.class)
                .hasMessage(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND);
    }
}
