package it.thcs.fse.fsersaservice.support.util;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyStore;

@Component
public class KeyStoreLoader {

    private final ResourceLoader resourceLoader;

    public KeyStoreLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public KeyStore load(String location, String type, String password) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                throw new IllegalStateException("Keystore/truststore non trovato: " + location);
            }

            KeyStore keyStore = KeyStore.getInstance(type);
            try (InputStream inputStream = resource.getInputStream()) {
                keyStore.load(inputStream, password != null ? password.toCharArray() : null);
            }

            return keyStore;
        } catch (Exception ex) {
            throw new IllegalStateException("Errore nel caricamento del keystore/truststore: " + location, ex);
        }
    }
}