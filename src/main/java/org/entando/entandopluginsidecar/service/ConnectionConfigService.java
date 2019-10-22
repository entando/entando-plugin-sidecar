package org.entando.entandopluginsidecar.service;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.util.YamlUtils;
import org.entando.kubernetes.model.plugin.DoneableEntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginList;
import org.entando.kubernetes.model.plugin.EntandoPluginSpec;
import org.entando.kubernetes.model.plugin.EntandoPluginSpecBuilder;
import org.entando.web.exception.ConflictException;
import org.entando.web.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConnectionConfigService {

    public static final String API_VERSION = "v1";
    public static final String CONFIG_YAML = "config.yaml";
    private static final String OPAQUE_TYPE = "Opaque";

    public static final String ERROR_PLUGIN_NOT_FOUND = "org.entando.error.plugin.notFound";
    public static final String ERROR_SECRET_NOT_FOUND = "org.entando.error.secret.notFound";
    public static final String ERROR_SECRET_ALREADY_EXISTS = "org.entando.error.secret.alreadyExists";

    private final KubernetesClient client;
    private final String entandoPluginName;

    public ConnectionConfigService(KubernetesClient client, @Value("${entando.plugin.name}") String entandoPluginName) {
        this.client = client;
        this.entandoPluginName = entandoPluginName;
    }

    public void addConnectionConfig(ConnectionConfigDto connectionConfigDto) {
        EntandoPlugin entandoPlugin = entandoPlugin().get();
        if (entandoPlugin == null) {
            throw new NotFoundException(ERROR_PLUGIN_NOT_FOUND);
        }
        Set<String> connectionConfigNames = new HashSet<>();
        if (entandoPlugin.getSpec().getConnectionConfigNames() != null) {
            connectionConfigNames.addAll(entandoPlugin.getSpec().getConnectionConfigNames());
        }
        connectionConfigNames.add(connectionConfigDto.getName());
        EntandoPluginSpec newSpec = new EntandoPluginSpecBuilder(entandoPlugin.getSpec())
                .withConnectionConfigNames(new ArrayList<>(connectionConfigNames))
                .build();
        entandoPlugin.setSpec(newSpec);
        entandoPlugin().createOrReplace(entandoPlugin);

        Secret secret = client.secrets().inNamespace(client.getConfiguration().getNamespace())
                .withName(connectionConfigDto.getName())
                .get();
        if (secret != null) {
            throw new ConflictException(ERROR_SECRET_ALREADY_EXISTS);
        }
        client.secrets().inNamespace(client.getConfiguration().getNamespace()).createNew()
                .withApiVersion(API_VERSION)
                .withNewMetadata().withName(connectionConfigDto.getName()).endMetadata()
                .withStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(connectionConfigDto)))
                .withType(OPAQUE_TYPE)
                .done();
    }

    public Optional<ConnectionConfigDto> getConnectionConfig(String name) {
        EntandoPlugin entandoPlugin = entandoPlugin().get();
        if (entandoPlugin.getSpec().getConnectionConfigNames().contains(name)) {
            Secret secret = client.secrets().withName(name).get();
            return fromSecret(secret);
        }
        return Optional.empty();
    }

    public List<ConnectionConfigDto> getAllConnectionConfig() {
        EntandoPlugin entandoPlugin = entandoPlugin().get();
        if (entandoPlugin == null) {
            throw new NotFoundException(ERROR_PLUGIN_NOT_FOUND);
        }
        List<String> configs = entandoPlugin.getSpec().getConnectionConfigNames() == null ? new ArrayList<>()
                : entandoPlugin.getSpec().getConnectionConfigNames();

        return client.secrets().list().getItems().stream()
                .filter(e -> configs.contains(e.getMetadata().getName()))
                .map(this::fromSecret)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public void removeConnectionConfig(String configName) {
        EntandoPlugin entandoPlugin = entandoPlugin().get();
        entandoPlugin.getSpec().getConnectionConfigNames().remove(configName);
        entandoPlugin().createOrReplace(entandoPlugin);

        client.secrets().withName(configName).delete();
    }

    private Optional<ConnectionConfigDto> fromSecret(Secret secret) {
        if (secret == null) {
            return Optional.empty();
        }
        if (secret.getStringData() != null && secret.getStringData().get(CONFIG_YAML) != null) {
            return Optional.of(YamlUtils.fromYaml(secret.getStringData().get(CONFIG_YAML)));
        }
        if (secret.getData() != null && secret.getData().get(CONFIG_YAML) != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(secret.getData().get(CONFIG_YAML));
            return Optional.of(YamlUtils.fromYaml(new String(decodedBytes, StandardCharsets.UTF_8)));
        }
        return Optional.empty();
    }

    private Resource<EntandoPlugin, DoneableEntandoPlugin> entandoPlugin() {
        CustomResourceDefinition definition = client.customResourceDefinitions().withName(EntandoPlugin.CRD_NAME).get();
        if (definition == null) {
            throw new NotFoundException(ERROR_PLUGIN_NOT_FOUND);
        }

        return client
                .customResources(definition, EntandoPlugin.class, EntandoPluginList.class, DoneableEntandoPlugin.class)
                .inNamespace(client.getConfiguration().getNamespace())
                .withName(entandoPluginName);
    }

    public ConnectionConfigDto editConnectionConfig(ConnectionConfigDto configDto) {
        EntandoPlugin entandoPlugin = entandoPlugin().get();
        if (entandoPlugin == null) {
            throw new NotFoundException(ERROR_PLUGIN_NOT_FOUND);
        }
        Secret secret = client.secrets().inNamespace(client.getConfiguration().getNamespace())
                .withName(configDto.getName())
                .get();
        if (secret == null) {
            throw new NotFoundException(ERROR_SECRET_NOT_FOUND);
        }
        secret.setStringData(Collections.singletonMap(CONFIG_YAML, YamlUtils.toYaml(configDto)));
        client.secrets().inNamespace(client.getConfiguration().getNamespace())
                .withName(configDto.getName())
                .createOrReplace(secret);

        return configDto;
    }
}
