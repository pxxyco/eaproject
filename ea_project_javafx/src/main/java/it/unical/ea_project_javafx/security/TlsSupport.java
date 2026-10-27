package it.unical.ea_project_javafx.security;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/** Default certificate validation plus the team's development CA; hostname validation stays enabled. */
public final class TlsSupport {
    private static final SSLContext CONTEXT = buildSslContext();
    private TlsSupport() {}
    public static SSLContext context() { return CONTEXT; }
    public static void installAsDefault() {
        SSLContext.setDefault(CONTEXT);
        HttpsURLConnection.setDefaultSSLSocketFactory(CONTEXT.getSocketFactory());
    }

    private static SSLContext buildSslContext() {
        try {
            TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultTmf.init((KeyStore) null);
            X509TrustManager defaultTm = firstX509(defaultTmf.getTrustManagers());
            X509TrustManager devTm = loadDevTrustManager();

            X509TrustManager combined = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    defaultTm.checkClientTrusted(chain, authType);
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    try {
                        defaultTm.checkServerTrusted(chain, authType);
                    } catch (CertificateException e) {
                        if (devTm == null)
                            throw e;
                        devTm.checkServerTrusted(chain, authType);
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return defaultTm.getAcceptedIssuers();
                }
            };

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { combined }, null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile inizializzare il contesto SSL", e);
        }
    }

    private static X509TrustManager loadDevTrustManager() throws Exception {
        try (InputStream in = TlsSupport.class.getResourceAsStream(
                "/it/unical/ea_project_javafx/certs/dev-ca.crt")) {
            if (in == null)
                return null;
            KeyStore devStore = KeyStore.getInstance(KeyStore.getDefaultType());
            devStore.load(null, null);
            devStore.setCertificateEntry("dev-ca", CertificateFactory.getInstance("X.509").generateCertificate(in));
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(devStore);
            return firstX509(tmf.getTrustManagers());
        }
    }

    private static X509TrustManager firstX509(TrustManager[] managers) {
        for (TrustManager tm : managers) {
            if (tm instanceof X509TrustManager x509)
                return x509;
        }
        throw new IllegalStateException("Nessun X509TrustManager disponibile");
    }

}
