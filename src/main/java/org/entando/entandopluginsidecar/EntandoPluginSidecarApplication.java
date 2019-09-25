package org.entando.entandopluginsidecar;

import io.fabric8.kubernetes.client.AutoAdaptableKubernetesClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.entando")
public class EntandoPluginSidecarApplication {

    public static void main(String[] args) {
        SpringApplication.run(EntandoPluginSidecarApplication.class, args);
    }

    @Bean
    public KubernetesClient kubernetesClient() {
        Config config = new ConfigBuilder().withTrustCerts(true).build();
        OkHttpClient httpClient = HttpClientUtils.createHttpClient(config);
        return new AutoAdaptableKubernetesClient(httpClient, config);
    }
}
