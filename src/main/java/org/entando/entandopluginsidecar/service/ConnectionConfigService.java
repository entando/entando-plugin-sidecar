package org.entando.entandopluginsidecar.service;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConnectionConfigService {

    private static final String API_VERSION = "v1";
    private static final String CONFIG_YAML = "config.yaml";
    private static final String OPAQUE_TYPE = "Opaque";

    private final KubernetesClient client;

    public void addConnectionConfig(ConnectionConfigDto connectionConfigDto) {
        client.secrets().createNew()
                .withApiVersion(API_VERSION)
                .withNewMetadata().withName(connectionConfigDto.getName()).endMetadata()
                .withStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(connectionConfigDto)))
                .withType(OPAQUE_TYPE)
                .done();
    }

    public Optional<ConnectionConfigDto> getConnectionConfig(String name) {
        Secret secret = client.secrets().withName(name).get();
        return Optional.ofNullable(fromSecret(secret));
    }

    private ConnectionConfigDto fromSecret(Secret secret) {
        if (secret == null) {
            return null;
        }
        if (secret.getStringData() != null && secret.getStringData().get(CONFIG_YAML) != null) {
            return YamlUtils.fromYaml(secret.getStringData().get(CONFIG_YAML));
        } else if (secret.getData() != null && secret.getData().get(CONFIG_YAML) != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(secret.getData().get(CONFIG_YAML));
            return YamlUtils.fromYaml(new String(decodedBytes));
        }
        return null;
    }

    public List<ConnectionConfigDto> getAllConnectionConfig() {
        return client.secrets().list().getItems().stream()
                .map(this::fromSecret)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
