package org.entando.entandopluginsidecar.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConnectionConfigDto {

    private String url;
    private String username;
    private String password;
    private String name;
    private String serviceType;
    private Map<String, String> properties;
}
