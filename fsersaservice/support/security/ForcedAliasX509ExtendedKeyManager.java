package it.thcs.fse.fsersaservice.support.security;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class ForcedAliasX509ExtendedKeyManager extends X509ExtendedKeyManager {

    private final X509ExtendedKeyManager delegate;
    private final String forcedAlias;

    public ForcedAliasX509ExtendedKeyManager(X509ExtendedKeyManager delegate, String forcedAlias) {
        this.delegate = delegate;
        this.forcedAlias = forcedAlias;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return delegate.getClientAliases(keyType, issuers);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        if (delegate.getPrivateKey(forcedAlias) != null) {
            return forcedAlias;
        }
        return delegate.chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return delegate.getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return delegate.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return delegate.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return delegate.getPrivateKey(alias);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        if (delegate.getPrivateKey(forcedAlias) != null) {
            return forcedAlias;
        }
        return delegate.chooseEngineClientAlias(keyType, issuers, engine);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return delegate.chooseEngineServerAlias(keyType, issuers, engine);
    }
}