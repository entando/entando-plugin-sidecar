package org.entando.entandopluginsidecar.controller;

import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_CREATE;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.service.ConnectionConfigService;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.keycloak.testutils.WithMockKeycloakUser;
import org.entando.web.exception.NotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ConnectionConfigControllerCreateTest {

    private static final String CONFIG_ENDPOINT = "/config";
    private static final String RESOURCE = "entando-sidecar";
    private static final String KEYCLOAK_USER = "keycloak-user";
    private static final String WRONG_ROLE = "wrong-role";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConnectionConfigService connectionConfigService;

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {CONNECTION_CONFIG_CREATE}, resource = RESOURCE)
    public void shouldReturn404ForNotFoundException() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        doThrow(new NotFoundException(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND)).when(connectionConfigService)
                .addConnectionConfig(any());

        mvc.perform(post(CONFIG_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("plugin not found")));
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {CONNECTION_CONFIG_CREATE}, resource = RESOURCE)
    public void shouldReturnErrorForKubernetesException() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        doThrow(KubernetesClientException.class).when(connectionConfigService)
                .addConnectionConfig(any());

        mvc.perform(post(CONFIG_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void shouldReturnUnauthorizedWhenTryingToCreateWithoutCredentials() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        doThrow(KubernetesClientException.class).when(connectionConfigService)
                .addConnectionConfig(any());

        mvc.perform(post(CONFIG_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {WRONG_ROLE}, resource = RESOURCE)
    public void shouldReturnForbiddenWhenTryingToCreateWithWrongRole() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        doThrow(KubernetesClientException.class).when(connectionConfigService)
                .addConnectionConfig(any());

        mvc.perform(post(CONFIG_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {CONNECTION_CONFIG_CREATE}, resource = RESOURCE)
    public void shouldCreateConnectionConfig() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();

        mvc.perform(post(CONFIG_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isCreated());

        verify(connectionConfigService).addConnectionConfig(configDto);
    }
}
