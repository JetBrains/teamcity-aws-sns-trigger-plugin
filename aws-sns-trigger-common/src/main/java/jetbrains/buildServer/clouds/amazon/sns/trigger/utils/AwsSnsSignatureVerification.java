/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.amazon.sns.trigger.utils;

import jetbrains.buildServer.clouds.amazon.sns.trigger.SnsMessageType;
import jetbrains.buildServer.clouds.amazon.sns.trigger.errors.AwsSnsHttpEndpointException;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.http.HttpApi;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AwsSnsSignatureVerification {
    private static final Map<String, PublicKey> certificateCache = new ConcurrentHashMap<>();
    private final String mySignatureCertUrl;
    private final Map<String, Object> myPayload;
    private final HttpApi myServerApi;
    private final SnsMessageType myMessageType;
    private final String mySignature;

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
            return false;
        }
        PublicKey publicKey = getSigningCertificate();

        // Message type must be defined
        if (SnsMessageType.UNDEFINED.equals(myMessageType)) {
            return false;
        }

        // Check and decode signature from Base64
        if (mySignature == null || mySignature.trim().isEmpty()) {
            return false;
        }
        byte[] decodedSignature = decodeSignature();

        String stringToSign = payloadToStringToSign();
        // Empty payload is impossible
        if (stringToSign.isEmpty()) {
            return false;
        }

        try {
            Signature sigChecker = getSignatureCheckerForVersion();
            sigChecker.initVerify(publicKey);
            sigChecker.update(stringToSign.getBytes(StandardCharsets.UTF_8));
            return sigChecker.verify(decodedSignature);
        } catch (Exception e) {
            throw new AwsSnsHttpEndpointException("Signature decoding failed", e);
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
        try {
            return Base64.getDecoder().decode(mySignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new AwsSnsHttpEndpointException("Can't decode SNS message signature", e);
        }
    }

    @NotNull
    private PublicKey getSigningCertificate() {
        try {
            PublicKey publicKey = certificateCache.get(mySignatureCertUrl);

            if (publicKey == null) {
                HttpApi.Response response = myServerApi.get(mySignatureCertUrl);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(response.getBody().getBytes(StandardCharsets.UTF_8))
                );
                certificate.checkValidity();
                publicKey = certificate.getPublicKey();
                certificateCache.put(mySignatureCertUrl, certificate.getPublicKey());
            }

            return publicKey;
        } catch (Exception e) {
            throw new AwsSnsHttpEndpointException("Can't get signature certificate from SNS message", e);
        }
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
