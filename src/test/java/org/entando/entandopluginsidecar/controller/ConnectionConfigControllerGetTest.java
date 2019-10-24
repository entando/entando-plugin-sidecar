package org.entando.entandopluginsidecar.controller;

import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG;
import static org.entando.entandopluginsidecar.util.TestHelper.CONFIG_ENDPOINT;
import static org.entando.entandopluginsidecar.util.TestHelper.KEYCLOAK_USER;
import static org.entando.entandopluginsidecar.util.TestHelper.RESOURCE;
import static org.entando.entandopluginsidecar.util.TestHelper.WRONG_ROLE;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.service.ConnectionConfigService;
import org.entando.entandopluginsidecar.util.TestHelper;
import org.entando.keycloak.testutils.WithMockKeycloakUser;
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
public class ConnectionConfigControllerGetTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConnectionConfigService connectionConfigService;

    @Test
    public void shouldReturnUnauthorizedWhenTryingToGetWithoutCredentials() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        when(connectionConfigService.getConnectionConfig(configDto.getName())).thenReturn(configDto);

        mvc.perform(get(CONFIG_ENDPOINT + "/" + configDto.getName()).contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {WRONG_ROLE}, resource = RESOURCE)
    public void shouldReturnForbiddenWhenTryingToGetWithWrongRole() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        when(connectionConfigService.getConnectionConfig(configDto.getName())).thenReturn(configDto);

        mvc.perform(get(CONFIG_ENDPOINT + "/" + configDto.getName()).contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {CONNECTION_CONFIG}, resource = RESOURCE)
    public void shouldGetConnectionConfig() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        when(connectionConfigService.getConnectionConfig(configDto.getName())).thenReturn(configDto);

        mvc.perform(get(CONFIG_ENDPOINT + "/" + configDto.getName()).contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(configDto.getName())))
                .andExpect(jsonPath("$.properties", is(configDto.getProperties())));
    }
}
