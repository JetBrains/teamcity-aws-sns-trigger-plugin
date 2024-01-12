

package jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AwsSnsTriggerConstants {

    // controller specific properties
    public static final String SNS_CONNECTION_CONTROLLER_URL = "/app/trigger/sns/**";
    public static final String SNS_CONNECTION_CONTROLLER_URL_PATTERN = "/app/trigger/sns/(.*)/(.*)/(.*)$";

    // trigger store keys
    public static final String TRIGGER_STORE_CURRENT_SUBSCRIPTION_ARN = "subscriptionArn";
    public static final String TRIGGER_STORE_CURRENT_UNSUBSCRIBE_URL = "unsubscribeURL";
    public static final String TRIGGER_STORE_CURRENT_TOPIC_ARN = "topicArn";
    public static final String TRIGGER_STORE_MESSAGES = "messages";

    // trigger properties keys
    public static final String TRIGGER_UUID_PROPERTY_KEY = "triggerUuid";
    public static final String TRIGGER_NAME_PROPERTY_KEY = "displayName";

    // SNS Notification body keys
    public static final String NOTIFICATION_MESSAGE_KEY = "Message";
    public static final String NOTIFICATION_MESSAGE_ID_KEY = "MessageId";
    public static final String NOTIFICATION_SUBJECT_KEY = "Subject";
    public static final String NOTIFICATION_TIMESTAMP_KEY = "Timestamp";
    public static final String NOTIFICATION_TOKEN_KEY = "Token";
    public static final String NOTIFICATION_TOPIC_ARN_KEY = "TopicArn";
    public static final String MESSAGE_TYPE_KEY = "Type";
    public static final String NOTIFICATION_ATTRIBUTES_KEY = "MessageAttributes";
    public static final String SUBSCRIBE_URL_KEY = "SubscribeURL";
    public static final String UNSUBSCRIBE_URL_KEY = "UnsubscribeURL";
    public static final String SIGNING_CERTIFICATE_URL_KEY = "SigningCertURL";
    public static final String SIGNING_SIGNATURE_KEY = "Signature";
    public static final String SIGNING_SIGNATURE_VERSION_KEY = "SignatureVersion";

    // SNS message header keys
    public static final String AWS_SUBSCRIPTION_ARN_HEADER = "x-amz-sns-subscription-arn";
    public static final String AWS_MESSAGE_ID_HEADER = "x-amz-sns-message-id";
    public static final String AWS_TOPIC_ARN_HEADER = "x-amz-sns-topic-arn";

    // SNS message types
    public static final String SNS_MT_UNDEFINED_STR = "undefined";
    public static final String SNS_MT_SUBSCRIPTION_STR = "SubscriptionConfirmation";
    public static final String SNS_MT_UNSUBSCRIPTION_STR = "UnsubscribeConfirmation";
    public static final String SNS_MT_NOTIFICATION_STR = "Notification";

    // XPath for subscription confirmation
    public static final String SUBSCRIBE_CONFIRMATION_ARN_XPATH = "//*[local-name() = 'ConfirmSubscriptionResult']/*[local-name() = 'SubscriptionArn']";

    // Custom parameters placeholders
    public static final String SNS_MESSAGE_SUBJECT_PARAMETER_PLACEHOLDER = "sns.message.subject";
    public static final String SNS_MESSAGE_BODY_PARAMETER_PLACEHOLDER = "sns.message.body";
    public static final String SNS_MESSAGE_ATTRIBUTES_PARAMETER_PLACEHOLDER = "sns.message.attributes.";

    // Key-collections for SNS message verification
    public static final List<String> SUBSCRIPTION_CONFIRMATION_KEYS_LIST = Collections.unmodifiableList(Arrays.asList(
            // same fields-set used for Unsubscription message validation
            NOTIFICATION_MESSAGE_KEY,
            NOTIFICATION_MESSAGE_ID_KEY,
            SUBSCRIBE_URL_KEY,
            NOTIFICATION_TIMESTAMP_KEY,
            NOTIFICATION_TOKEN_KEY,
            NOTIFICATION_TOPIC_ARN_KEY,
            MESSAGE_TYPE_KEY
    ));
    public static final List<String> NOTIFICATION_KEYS_LIST = Collections.unmodifiableList(Arrays.asList(
            NOTIFICATION_MESSAGE_KEY,
            NOTIFICATION_MESSAGE_ID_KEY,
            NOTIFICATION_SUBJECT_KEY,
            NOTIFICATION_TIMESTAMP_KEY,
            NOTIFICATION_TOPIC_ARN_KEY,
            MESSAGE_TYPE_KEY
    ));

    private AwsSnsTriggerConstants() {
    }

    @NotNull
    public static String getTriggerUrlPathPart() {
        return AwsSnsTriggerConstants.SNS_CONNECTION_CONTROLLER_URL.replace("/**", "");
    }
}