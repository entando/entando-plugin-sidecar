package org.entando.entandopluginsidecar.controller;

import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_CREATE;
import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_GET;
import static org.entando.entandopluginsidecar.controller.AuthPermissions.CONNECTION_CONFIG_LIST;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.service.ConnectionConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConnectionConfigController {

    private final ConnectionConfigService connectionConfigService;

    @Secured(CONNECTION_CONFIG_CREATE)
    @PostMapping
    public ResponseEntity<Void> addConnectionConfig(@RequestBody ConnectionConfigDto connectionConfigDto) {
        connectionConfigService.addConnectionConfig(connectionConfigDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Secured(CONNECTION_CONFIG_GET)
    @GetMapping("/{configName}")
    public ResponseEntity<ConnectionConfigDto> getConnectionConfig(@PathVariable String configName) {
        return ResponseEntity.of(connectionConfigService.getConnectionConfig(configName));
    }

    @Secured(CONNECTION_CONFIG_LIST)
    @GetMapping
    public ResponseEntity<List<ConnectionConfigDto>> getAllConnectionConfig() {
        return ResponseEntity.ok(connectionConfigService.getAllConnectionConfig());
    }
}
