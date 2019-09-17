package org.entando.entandopluginsidecar.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.entando.entandopluginsidecar.service.ConnectionConfigService;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public void addConnectionConfig(@RequestBody ConnectionConfigDto connectionConfigDto) {
        connectionConfigService.addConnectionConfig(connectionConfigDto);
    }

    @GetMapping("/{configName}")
    public ResponseEntity<ConnectionConfigDto> getConnectionConfig(@PathVariable String configName) {
        return ResponseEntity.of(connectionConfigService.getConnectionConfig(configName));
    }

    @GetMapping
    public ResponseEntity<List<ConnectionConfigDto>> getAllConnectionConfig() {
        return ResponseEntity.ok(connectionConfigService.getAllConnectionConfig());
    }
}
