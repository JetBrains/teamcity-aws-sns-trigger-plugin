package jetbrains.buildServer.clouds.amazon.sns.trigger.utils;

public enum SignatureVersion {
    SHA1("1", "SHA1withRSA"),
    SHA256("2", "SHA256withRSA");

    private final String value;
    private final String algorithm;

    SignatureVersion(final String value, final String algorithm) {
        this.value = value;
        this.algorithm = algorithm;
    }

    public static SignatureVersion fromValue(String value) {
        for (SignatureVersion signatureVersion : SignatureVersion.values()) {
            if (signatureVersion.value.equals(value)) {
                return signatureVersion;
            }
        }
        throw new RuntimeException("Invalid SignatureVersion value");
    }

    public String getAlgorithm() {
        return this.algorithm;
    }
}