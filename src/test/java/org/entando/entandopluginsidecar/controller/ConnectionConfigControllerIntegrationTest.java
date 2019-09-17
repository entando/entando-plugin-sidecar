package org.entando.entandopluginsidecar.controller;

import static org.assertj.core.api.Java6Assertions.assertThat;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ConnectionConfigControllerIntegrationTest {

    private static final String CONFIG_YAML = "config.yaml";
    private static final String API_VERSION = "v1";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private KubernetesClient client;

    @Test
    public void shouldAddConnectionConfig() {
        // Given
        ConnectionConfigDto configDto = getRandomConnectionConfigDto();

        // When
        HttpEntity<ConnectionConfigDto> request = new HttpEntity<>(configDto, null);
        ResponseEntity<Void> response = testRestTemplate.postForEntity("/config", request, Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Secret secret = client.secrets().withName(configDto.getName()).get();
        assertThat(secret).isNotNull();
        byte[] decodedBytes = Base64.getDecoder().decode(secret.getData().get(CONFIG_YAML));
        ConnectionConfigDto fromYaml = YamlUtils.fromYaml(new String(decodedBytes));
        assertThat(fromYaml.getUrl()).isEqualTo(configDto.getUrl());
        assertThat(fromYaml.getUsername()).isEqualTo(configDto.getUsername());
        assertThat(fromYaml.getPassword()).isEqualTo(configDto.getPassword());
        assertThat(fromYaml.getServiceType()).isEqualTo(configDto.getServiceType());
    }

    @Test
    public void shouldReadConnectionConfigByName() {
        // Given
        ConnectionConfigDto configDto = getRandomConnectionConfigDto();
        client.secrets().createNew()
                .withApiVersion(API_VERSION)
                .withNewMetadata().withName(configDto.getName()).endMetadata()
                .withStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(configDto)))
                .done();

        // When
        ResponseEntity<ConnectionConfigDto> response = testRestTemplate
                .getForEntity("/config/" + configDto.getName(), ConnectionConfigDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ConnectionConfigDto responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getUrl()).isEqualTo(configDto.getUrl());
        assertThat(responseBody.getUsername()).isEqualTo(configDto.getUsername());
        assertThat(responseBody.getPassword()).isEqualTo(configDto.getPassword());
        assertThat(responseBody.getServiceType()).isEqualTo(configDto.getServiceType());
    }

    @Test
    public void shouldReadAllConnectionConfigs() {
        // Given
        ConnectionConfigDto configDto1 = getRandomConnectionConfigDto();
        ConnectionConfigDto configDto2 = getRandomConnectionConfigDto();
        ConnectionConfigDto configDto3 = getRandomConnectionConfigDto();
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

        // When
        ResponseEntity<List<ConnectionConfigDto>> response = testRestTemplate.exchange("/config",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<ConnectionConfigDto>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ConnectionConfigDto> responseBody = response.getBody();
        assertThat(responseBody).contains(configDto1, configDto2, configDto3);
    }

    private ConnectionConfigDto getRandomConnectionConfigDto() {
        return ConnectionConfigDto.builder()
                .url(RandomStringUtils.randomAlphabetic(100))
                .name(RandomStringUtils.randomAlphabetic(20).toLowerCase())
                .username(RandomStringUtils.randomAlphabetic(20))
                .password(RandomStringUtils.randomAlphabetic(20))
                .serviceType(RandomStringUtils.randomAlphabetic(20))
                .build();
    }
}
