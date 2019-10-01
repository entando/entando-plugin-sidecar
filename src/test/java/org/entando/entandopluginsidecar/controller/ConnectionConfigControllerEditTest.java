package org.entando.entandopluginsidecar.controller;

import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_EDIT;
import static org.entando.entandopluginsidecar.util.TestHelper.CONFIG_ENDPOINT;
import static org.entando.entandopluginsidecar.util.TestHelper.KEYCLOAK_USER;
import static org.entando.entandopluginsidecar.util.TestHelper.RESOURCE;
import static org.entando.entandopluginsidecar.util.TestHelper.WRONG_ROLE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ConnectionConfigControllerEditTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConnectionConfigService connectionConfigService;

    @Test
    public void shouldReturnUnauthorizedWhenTryingToEditWithoutCredentials() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        when(connectionConfigService.editConnectionConfig(configDto)).thenReturn(configDto);

        mvc.perform(put(CONFIG_ENDPOINT)
                .contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {WRONG_ROLE}, resource = RESOURCE)
    public void shouldReturnForbiddenWhenTryingToEditWithWrongRole() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        when(connectionConfigService.editConnectionConfig(configDto)).thenReturn(configDto);

        mvc.perform(put(CONFIG_ENDPOINT)
                .contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {CONNECTION_CONFIG_EDIT}, resource = RESOURCE)
    public void shouldEditConnectionConfig() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        when(connectionConfigService.editConnectionConfig(configDto)).thenReturn(configDto);

        mvc.perform(put(CONFIG_ENDPOINT)
                .contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", is(configDto.getUrl())))
                .andExpect(jsonPath("$.username", is(configDto.getUsername())))
                .andExpect(jsonPath("$.password", is(configDto.getPassword())))
                .andExpect(jsonPath("$.name", is(configDto.getName())))
                .andExpect(jsonPath("$.serviceType", is(configDto.getServiceType())));

        verify(connectionConfigService).editConnectionConfig(configDto);
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {CONNECTION_CONFIG_EDIT}, resource = RESOURCE)
    public void shouldHandleNotFoundException() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        when(connectionConfigService.editConnectionConfig(configDto))
                .thenThrow(new NotFoundException(ConnectionConfigService.ERROR_SECRET_NOT_FOUND));

        mvc.perform(put(CONFIG_ENDPOINT)
                .contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Secret not found")));
    }
}
