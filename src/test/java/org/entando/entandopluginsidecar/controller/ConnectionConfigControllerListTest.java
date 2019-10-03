package org.entando.entandopluginsidecar.controller;

import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG;
import static org.entando.entandopluginsidecar.util.TestHelper.CONFIG_ENDPOINT;
import static org.entando.entandopluginsidecar.util.TestHelper.KEYCLOAK_USER;
import static org.entando.entandopluginsidecar.util.TestHelper.RESOURCE;
import static org.entando.entandopluginsidecar.util.TestHelper.WRONG_ROLE;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
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
public class ConnectionConfigControllerListTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConnectionConfigService connectionConfigService;

    @Test
    public void shouldReturnUnauthorizedWhenTryingToListWithoutCredentials() throws Exception {
        when(connectionConfigService.getAllConnectionConfig()).thenReturn(Collections.emptyList());

        mvc.perform(get(CONFIG_ENDPOINT).contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {WRONG_ROLE}, resource = RESOURCE)
    public void shouldReturnForbiddenWhenTryingToListWithWrongRole() throws Exception {
        when(connectionConfigService.getAllConnectionConfig()).thenReturn(Collections.emptyList());

        mvc.perform(get(CONFIG_ENDPOINT).contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockKeycloakUser(username = KEYCLOAK_USER, roles = {CONNECTION_CONFIG}, resource = RESOURCE)
    public void shouldListConnectionConfigs() throws Exception {
        ConnectionConfigDto configDto1 = TestHelper.getRandomConnectionConfigDto();
        ConnectionConfigDto configDto2 = TestHelper.getRandomConnectionConfigDto();
        when(connectionConfigService.getAllConnectionConfig()).thenReturn(Arrays.asList(configDto1, configDto2));

        mvc.perform(get(CONFIG_ENDPOINT).contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is(configDto1.getName())))
                .andExpect(jsonPath("$[1].name", is(configDto2.getName())));
    }
}
