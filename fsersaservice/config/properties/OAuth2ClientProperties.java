package it.thcs.fse.fsersaservice.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "fse.security.oauth2")
public class OAuth2ClientProperties {

    private boolean enabled;
    private String tokenUri;
    private String clientId;
    private String clientSecret;
    private String grantType;
    private List<String> scope = new ArrayList<>();
}