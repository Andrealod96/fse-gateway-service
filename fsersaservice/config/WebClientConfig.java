package it.thcs.fse.fsersaservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import it.thcs.fse.fsersaservice.config.properties.GatewayProperties;
import it.thcs.fse.fsersaservice.config.properties.MtlsProperties;
import it.thcs.fse.fsersaservice.support.security.ForcedAliasX509ExtendedKeyManager;
import it.thcs.fse.fsersaservice.support.util.KeyStoreLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Arrays;

@Configuration
public class WebClientConfig {

    private final GatewayProperties gatewayProperties;
    private final MtlsProperties mtlsProperties;
    private final KeyStoreLoader keyStoreLoader;

    public WebClientConfig(
            GatewayProperties gatewayProperties,
            MtlsProperties mtlsProperties,
            KeyStoreLoader keyStoreLoader
    ) {
        this.gatewayProperties = gatewayProperties;
        this.mtlsProperties = mtlsProperties;
        this.keyStoreLoader = keyStoreLoader;
    }

    @Bean(name = "gatewayWebClient")
    public WebClient gatewayWebClient(WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, gatewayProperties.getConnectTimeoutMillis())
                .responseTimeout(Duration.ofSeconds(gatewayProperties.getResponseTimeoutSeconds()))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(gatewayProperties.getReadTimeoutSeconds()))
                        .addHandlerLast(new WriteTimeoutHandler(gatewayProperties.getWriteTimeoutSeconds())))
                .secure(sslSpec -> sslSpec.sslContext(buildGatewaySslContext()));

        return webClientBuilder
                .baseUrl(gatewayProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private SslContext buildGatewaySslContext() {
        try {
            if (!mtlsProperties.isEnabled()) {
                return SslContextBuilder.forClient().build();
            }

            KeyStore keyStore = keyStoreLoader.load(
                    mtlsProperties.getKeyStorePath(),
                    mtlsProperties.getKeyStoreType(),
                    mtlsProperties.getKeyStorePassword()
            );

            KeyStore trustStore = keyStoreLoader.load(
                    mtlsProperties.getTrustStorePath(),
                    mtlsProperties.getTrustStoreType(),
                    mtlsProperties.getTrustStorePassword()
            );

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, mtlsProperties.getKeyStorePassword().toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            X509ExtendedKeyManager keyManager = extractX509KeyManager(keyManagerFactory.getKeyManagers());

            return SslContextBuilder.forClient()
                    .keyManager(new ForcedAliasX509ExtendedKeyManager(keyManager, mtlsProperties.getKeyAlias()))
                    .trustManager(trustManagerFactory)
                    .build();

        } catch (Exception ex) {
            throw new IllegalStateException("Errore nella configurazione SSL/mTLS del WebClient Gateway", ex);
        }
    }

    private X509ExtendedKeyManager extractX509KeyManager(KeyManager[] keyManagers) {
        return Arrays.stream(keyManagers)
                .filter(X509ExtendedKeyManager.class::isInstance)
                .map(X509ExtendedKeyManager.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nessun X509ExtendedKeyManager trovato nel keystore mTLS"));
    }
}