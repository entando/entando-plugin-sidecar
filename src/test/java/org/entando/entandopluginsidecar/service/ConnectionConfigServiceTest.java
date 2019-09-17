package org.entando.entandopluginsidecar.service;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;

import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Java6JUnitSoftAssertions;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConnectionConfigServiceTest {

    private static final String CONFIG_YAML = "config.yaml";
    private static final String API_VERSION = "v1";

    @Rule
    public KubernetesServer server = new KubernetesServer(true, true);

    @Rule
    public Java6JUnitSoftAssertions safely = new Java6JUnitSoftAssertions();

    private ConnectionConfigService connectionConfigService;

    private KubernetesClient client;

    @Before
    public void setUp() {
        client = server.getClient();
        connectionConfigService = new ConnectionConfigService(client);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldAddConfigAsSecret() {
        ConnectionConfigDto configDto = getRandomConnectionConfigDto();

        connectionConfigService.addConnectionConfig(configDto);

        SecretList secretList = client.secrets().list();
        safely.assertThat(secretList.getItems().size()).isEqualTo(1);
        safely.assertThat(secretList.getItems().get(0).getMetadata().getName()).isEqualTo(configDto.getName());
        ConnectionConfigDto fromYaml = YamlUtils
                .fromYaml(secretList.getItems().get(0).getStringData().get(CONFIG_YAML));
        safely.assertThat(fromYaml.getUrl()).isEqualTo(configDto.getUrl());
        safely.assertThat(fromYaml.getUsername()).isEqualTo(configDto.getUsername());
        safely.assertThat(fromYaml.getPassword()).isEqualTo(configDto.getPassword());
        safely.assertThat(fromYaml.getServiceType()).isEqualTo(configDto.getServiceType());
    }

    @Test
    public void shouldGetConnectionConfigByName() {
        ConnectionConfigDto configDto = getRandomConnectionConfigDto();

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
    public void shouldGetAllConnectionConfigs() {
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

        List<ConnectionConfigDto> configDtos = connectionConfigService.getAllConnectionConfig();

        assertThat(configDtos).containsExactlyInAnyOrder(configDto1, configDto2, configDto3);
    }

    @Test
    public void shouldReturnEmptyForNonExistingConfig() {
        Optional<ConnectionConfigDto> configDto = connectionConfigService.getConnectionConfig("do-not-exist");

        assertThat(configDto.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnEmptyListForNonExistingConfigs() {
        List<ConnectionConfigDto> allConnectionConfig = connectionConfigService.getAllConnectionConfig();

        assertThat(allConnectionConfig).isEmpty();
    }

    private ConnectionConfigDto getRandomConnectionConfigDto() {
        return ConnectionConfigDto.builder()
                .name(RandomStringUtils.randomAlphabetic(20))
                .url(RandomStringUtils.randomAlphabetic(100))
                .username(RandomStringUtils.randomAlphabetic(20))
                .password(RandomStringUtils.randomAlphabetic(20))
                .serviceType(RandomStringUtils.randomAlphabetic(20))
                .build();
    }
}
