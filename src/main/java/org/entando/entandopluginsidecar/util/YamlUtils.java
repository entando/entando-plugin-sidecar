package org.entando.entandopluginsidecar.util;

import lombok.experimental.UtilityClass;
import org.entando.entandopluginsidecar.dto.ConnectionConfigDto;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

@UtilityClass
public class YamlUtils {

    public static String toYaml(ConnectionConfigDto connectionConfigDto) {
        Representer representer = new Representer();
        representer.addClassTag(ConnectionConfigDto.class, Tag.MAP);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
        return new Yaml(representer, dumperOptions).dump(connectionConfigDto);
    }

    public static ConnectionConfigDto fromYaml(String yaml) {
        return new Yaml(new Constructor(ConnectionConfigDto.class)).load(yaml);
    }
}
