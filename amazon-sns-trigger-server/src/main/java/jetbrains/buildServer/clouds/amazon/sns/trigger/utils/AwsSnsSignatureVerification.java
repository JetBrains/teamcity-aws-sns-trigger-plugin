

package jetbrains.buildServer.clouds.amazon.sns.trigger.utils;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsMessageType;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.http.HttpApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AwsSnsSignatureVerification {
    public static final String X_509_CERT = "X.509";
    private static Logger LOG = Logger.getInstance(AwsSnsSignatureVerification.class.getName());
    private static final Map<String, PublicKey> certificateCache = new ConcurrentHashMap<>();
    private final String mySignatureCertUrl;
    private final Map<String, Object> myPayload;
    private final HttpApi myServerApi;
    private final SnsMessageType myMessageType;
    private final String mySignature;

    @TestOnly
    public void setLogger(Logger newLogger) {
        LOG = newLogger;
    }

    public AwsSnsSignatureVerification(
            @NotNull SnsMessageType snsMessageType,
            @NotNull Map<String, Object> payload,
            @NotNull HttpApi serverApi
    ) {
        myMessageType = snsMessageType;
        myPayload = payload;
        myServerApi = serverApi;
        mySignatureCertUrl = (String) payload.get(AwsSnsTriggerConstants.SIGNING_CERTIFICATE_URL_KEY);
        mySignature = (String) payload.get(AwsSnsTriggerConstants.SIGNING_SIGNATURE_KEY);
    }

    public boolean isValid() {
        // Signature Certificate URL is mandatory for verification
        if (mySignatureCertUrl == null || mySignatureCertUrl.trim().isEmpty()) {
            LOG.warn("SignatureCertUrl is mandatory for message validation but it's empty");
            return false;
        }

        // Message type must be defined
        if (SnsMessageType.UNDEFINED.equals(myMessageType)) {
            LOG.warn("Incoming message type wasn't recognized as Subscription, Notification or Unsubscription");
            return false;
        }

        if (mySignature == null || mySignature.trim().isEmpty()) {
            LOG.warn("Signature string is empty but it is mandatory for message validation");
            return false;
        }

        String stringToSign = payloadToStringToSign();
        // Empty payload is impossible
        if (stringToSign.isEmpty()) {
            LOG.warn("Message metadata required for message verification is empty");
            return false;
        }

        try {
            byte[] decodedSignature = decodeSignature();
            PublicKey publicKey = getSigningCertificate();

            Signature sigChecker = getSignatureCheckerForVersion();
            sigChecker.initVerify(publicKey);
            sigChecker.update(stringToSign.getBytes(StandardCharsets.UTF_8));
            return sigChecker.verify(decodedSignature);
        } catch (Exception e) {
            LOG.warn("Signature verification failed", e);
            return false;
        }
    }

    private Signature getSignatureCheckerForVersion() throws NoSuchAlgorithmException {
        String sigVer = (String) myPayload.get(AwsSnsTriggerConstants.SIGNING_SIGNATURE_VERSION_KEY);
        switch (SignatureVersion.fromValue(sigVer)) {
            case SHA1:
                return Signature.getInstance(SignatureVersion.SHA1.getAlgorithm());
            case SHA256:
                return Signature.getInstance(SignatureVersion.SHA256.getAlgorithm());
            default:
                throw new IllegalStateException("Invalid SignatureVersion value");
        }
    }

    @NotNull
    private byte[] decodeSignature() {
        return Base64.getDecoder().decode(mySignature.getBytes(StandardCharsets.UTF_8));
    }

    @NotNull
    private PublicKey getSigningCertificate() throws IOException, CertificateException {
        PublicKey publicKey = certificateCache.get(mySignatureCertUrl);

        if (publicKey == null) {
            HttpApi.Response response = myServerApi.get(mySignatureCertUrl);
            CertificateFactory cf = CertificateFactory.getInstance(X_509_CERT);
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(response.getBody().getBytes(StandardCharsets.UTF_8))
            );
            certificate.checkValidity();
            publicKey = certificate.getPublicKey();
            certificateCache.put(mySignatureCertUrl, certificate.getPublicKey());
        }

        return publicKey;
    }

    @NotNull
    private String payloadToStringToSign() {
        if (myMessageType.equals(SnsMessageType.NOTIFICATION)) {
            return stringToSignFromKeys(AwsSnsTriggerConstants.NOTIFICATION_KEYS_LIST);
        } else {
            return stringToSignFromKeys(AwsSnsTriggerConstants.SUBSCRIPTION_CONFIRMATION_KEYS_LIST);
        }
    }

    @NotNull
    private String stringToSignFromKeys(@NotNull List<String> keysList) {
        StringBuilder sb = new StringBuilder();

        for (String key : keysList) {
            if (myPayload.containsKey(key))
                appendWithKeyValuePair(sb, key);
        }

        return sb.toString();
    }

    private void appendWithKeyValuePair(@NotNull StringBuilder sb, @NotNull String key) {
        sb.append(key).append("\n");
        sb.append(myPayload.get(key)).append("\n");
    }
}