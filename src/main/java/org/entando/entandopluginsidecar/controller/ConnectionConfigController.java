package org.entando.entandopluginsidecar.controller;

import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_CREATE;
import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_DELETE;
import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_EDIT;
import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_GET;
import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_LIST;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.service.ConnectionConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "Connection Config")
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConnectionConfigController {

    private final ConnectionConfigService connectionConfigService;

    @Secured(CONNECTION_CONFIG_CREATE)
    @ApiOperation(notes = "Add Config", nickname = "addConnectionConfig", value = "ADD Connection Config")
    @PostMapping
    public ResponseEntity<Void> addConnectionConfig(@RequestBody ConnectionConfigDto connectionConfigDto) {
        connectionConfigService.addConnectionConfig(connectionConfigDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Secured(CONNECTION_CONFIG_GET)
    @ApiOperation(notes = "Get Config", nickname = "getConnectionConfig", value = "GET Connection Config")
    @GetMapping("/{configName}")
    public ResponseEntity<ConnectionConfigDto> getConnectionConfig(@PathVariable String configName) {
        return ResponseEntity.of(connectionConfigService.getConnectionConfig(configName));
    }

    @Secured(CONNECTION_CONFIG_LIST)
    @ApiOperation(notes = "List Config", nickname = "listConnectionConfig", value = "LIST Connection Config")
    @GetMapping
    public List<ConnectionConfigDto> getAllConnectionConfig() {
        return connectionConfigService.getAllConnectionConfig();
    }

    @Secured(CONNECTION_CONFIG_DELETE)
    @ApiOperation(notes = "Delete Config", nickname = "deleteConnectionConfig", value = "DELETE Connection Config")
    @DeleteMapping("/{configName}")
    public void deleteConnectionConfig(@PathVariable String configName) {
        connectionConfigService.removeConnectionConfig(configName);
    }

    @Secured(CONNECTION_CONFIG_EDIT)
    @ApiOperation(notes = "Edit Config", nickname = "editConnectionConfig", value = "EDIT Connection Config")
    @PutMapping
    public ConnectionConfigDto editConnectionConfig(@RequestBody ConnectionConfigDto connectionConfigDto) {
        return connectionConfigService.editConnectionConfig(connectionConfigDto);
    }
}
