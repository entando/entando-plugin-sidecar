package org.entando.entandopluginsidecar.controller;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.entandopluginsidecar.service.ConnectionConfigService.CONFIG_YAML;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import java.util.Base64;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.service.ConnectionConfigService;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "keycloak.enabled=false")
@EnableKubernetesMockClient(crud = true, https = false)
class ConnectionConfigControllerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    @Value("${entando.plugin.name}")
    private String entandoPluginName;

    @Autowired
    private ConnectionConfigService connectionConfigService;

    static KubernetesClient client;

    @BeforeEach
    public void beforeEach() {
        connectionConfigService.setClient(client);
    }

    @Test
    void shouldAddConnectionConfig() throws Exception {
        // Given
        TestHelper.createEntandoPlugin(client, entandoPluginName);
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

        // When
        HttpEntity<ConnectionConfigDto> request = new HttpEntity<>(configDto, null);
        ResponseEntity<Void> response = testRestTemplate.postForEntity("/config", request, Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Secret secret = client.secrets().withName(configDto.getName()).get();
        assertThat(secret).isNotNull();
        String data = getConfigYamlValue(secret);
        ConnectionConfigDto fromYaml = YamlUtils.fromYaml(data);
        assertThat(fromYaml).isEqualTo(configDto);
        EntandoPlugin entandoPlugin = TestHelper.getEntandoPlugin(client, entandoPluginName);
        assertThat(entandoPlugin.getSpec().getConnectionConfigNames()).contains(configDto.getName());
    }

    @Test
    void shouldReadConnectionConfigByName() throws Exception {
        // Given
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createEntandoPluginWithConfigNames(client, entandoPluginName, configDto.getName());
        TestHelper.createSecret(client, configDto);

        // When
        ResponseEntity<ConnectionConfigDto> response = testRestTemplate
                .getForEntity("/config/" + configDto.getName(), ConnectionConfigDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ConnectionConfigDto responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).isEqualTo(configDto);
    }

    @Test
    void shouldReadAllConnectionConfigs() throws Exception {
        // Given
        ConnectionConfigDto configDto1 = TestHelper.getRandomConnectionConfigDto();
        ConnectionConfigDto configDto2 = TestHelper.getRandomConnectionConfigDto();
        ConnectionConfigDto configDto3 = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createEntandoPluginWithConfigNames(client, entandoPluginName, configDto1.getName(),
                configDto2.getName(), configDto3.getName());
        TestHelper.createSecret(client, configDto1);
        TestHelper.createSecret(client, configDto2);
        TestHelper.createSecret(client, configDto3);

        // When
        ResponseEntity<List<ConnectionConfigDto>> response = testRestTemplate.exchange("/config",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<ConnectionConfigDto>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ConnectionConfigDto> responseBody = response.getBody();
        assertThat(responseBody).contains(configDto1, configDto2, configDto3);
    }

    @Test
    void shouldRemoveConnectionConfig() throws Exception {
        // Given
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createEntandoPluginWithConfigNames(client, entandoPluginName, configDto.getName());
        TestHelper.createSecret(client, configDto);

        // When
        ResponseEntity<Void> response = testRestTemplate
                .exchange("/config/" + configDto.getName(), HttpMethod.DELETE, null, Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        EntandoPlugin entandoPlugin = TestHelper.getEntandoPlugin(client, entandoPluginName);
        assertThat(entandoPlugin.getSpec().getConnectionConfigNames()).doesNotContain(configDto.getName());
        assertThat(client.secrets().withName(configDto.getName()).get()).isNull();
    }

    @Test
    void shouldEditConnectionConfig() throws Exception {
        // Given
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        TestHelper.createEntandoPluginWithConfigNames(client, entandoPluginName, configDto.getName());
        TestHelper.createSecret(client, configDto);

        // When
        configDto.setProperties(ImmutableMap
                .of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),
                        RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
        ResponseEntity<ConnectionConfigDto> response = testRestTemplate
                .exchange("/config", HttpMethod.PUT, new HttpEntity<>(configDto), ConnectionConfigDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(configDto);
        Secret secret = client.secrets().withName(configDto.getName()).get();
        assertThat(secret).isNotNull();
        String data = getConfigYamlValue(secret);
        ConnectionConfigDto fromYaml = YamlUtils.fromYaml(data);
        assertThat(fromYaml).isEqualTo(configDto);
    }

    private String getConfigYamlValue(Secret secret) {
        String data;
        if (secret.getData() == null) {
            data = secret.getStringData().get(CONFIG_YAML);
        } else {
            data = new String(Base64.getDecoder().decode(secret.getData().get(CONFIG_YAML)));
        }
        return data;
    }
}
