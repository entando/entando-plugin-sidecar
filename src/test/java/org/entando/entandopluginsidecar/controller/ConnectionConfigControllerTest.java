package org.entando.entandopluginsidecar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.service.ConnectionConfigService;
import org.entando.entandopluginsidecar.util.TestHelper;
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
public class ConnectionConfigControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConnectionConfigService connectionConfigService;

    @Test
    public void shouldReturn404ForNotFoundException() throws Exception {
        ConnectionConfigDto configDto = TestHelper.getRandomConnectionConfigDto();
        doThrow(new NotFoundException(ConnectionConfigService.ERROR_PLUGIN_NOT_FOUND)).when(connectionConfigService)
                .addConnectionConfig(any());

        mvc.perform(post("/config").contentType(APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(configDto)))
                .andExpect(status().isNotFound());
    }
}
