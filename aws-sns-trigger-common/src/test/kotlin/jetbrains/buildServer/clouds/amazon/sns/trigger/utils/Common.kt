package jetbrains.buildServer.clouds.amazon.sns.trigger.utils

val accountId: String? = System.getenv("AWS_ACCOUNT_ID")

val subscriptionPayload: Map<String, Any?> = mapOf(
    "Type" to "SubscriptionConfirmation",
    "MessageId" to "0694e8c8-db4d-4863-8c83-a6c13cda3eec",
    "Token" to "2336412f37fb687f5d51e6e2425dacbba6354994a2bc07deffa4466da390cf4fb2768da190a7d98be6a202eb7222099c84c536a5d1a3f25aaf7cbe6d5deaf68fedf8a9ae82ada3931497120f5c6368935c918188e4c0fd47108b15ef24125c8833cc3f4f8c8a6f87cc11efef89388d251d665f2083e39abc1bb0690facf38d66",
    "TopicArn" to "arn:aws:sns:us-east-1:$accountId:efimov_sns_test",
    "Message" to "You have chosen to subscribe to the topic arn:aws:sns:us-east-1:$accountId:efimov_sns_test.\nTo confirm the subscription, visit the SubscribeURL included in this message.",
    "SubscribeURL" to "https://sns.us-east-1.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-east-1:$accountId:efimov_sns_test&Token=2336412f37fb687f5d51e6e2425dacbba6354994a2bc07deffa4466da390cf4fb2768da190a7d98be6a202eb7222099c84c536a5d1a3f25aaf7cbe6d5deaf68fedf8a9ae82ada3931497120f5c6368935c918188e4c0fd47108b15ef24125c8833cc3f4f8c8a6f87cc11efef89388d251d665f2083e39abc1bb0690facf38d66",
    "Timestamp" to "2022-09-02T13:32:10.507Z",
    "SignatureVersion" to "1",
    "Signature" to "YRZ0hOJyTdpNlL0oPMXINpanutf9cqEpwCRPjFtClu9ghVtBZvHuMO99JZd1Q7vKde80XnQ9JU8sfkTz5Xphvzspv+vYVSlFtpaqEmCzXV8I8TfKyX1liJIrMcJh5eRFhLHaevx7s856hq5lzoaeJ3yLJbu4T7C0oy63MdT3nUTgo1zADns2KKOjwPDaG/MnvB2Bfc4A1L4So+V0Fu4HgyvDjtPDVcu5RFKF37gfELfpNsdyb89NYypFooLRU55G5oaUCBRMa2XRW6sC6mKKVWzH+gW2DL9Ka3AfGklK7z8zgPEVS7pXgJJliWNB4qh2xvc6j/Fr4SFBmJZOPa3PlA==",
    "SigningCertURL" to "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-56e67fcb41f6fec09b0196692625d385.pem"
)

val notificationPayload: Map<String, Any?> = mapOf(
    "Type" to "Notification",
    "MessageId" to "fab38dbf-9f2b-50f7-9897-d633977b095a",
    "TopicArn" to "arn:aws:sns:us-east-1:$accountId:efimov_sns_test",
    "Message" to "test",
    "Timestamp" to "2022-09-02T13:33:04.483Z",
    "SignatureVersion" to "1",
    "Signature" to "fB9EcZafj6o5FNx2XAAAqR+N7OqIi1yFjL55i3U/tgeQMTk32yXVT6f06RGkahN10Mu7Mjen8/WlTiLOtcYFTHZ+RLcVb1vD/EdVG51W4VggNYPkVi05qgykwLcf4gpDoQKPpyE3K8S+JG62mDgNIm8VLCUKFcczb/L0qtk+vvF6XsQmOg/TGWyyFpXp0ibBr13F1f3b2/ZO3V0oMcVBNJFobRl2/VlfRTVTGxSmjQwuBUDBbOXM0xAWHNz0b63q5OmyCBh4OpvhMKdvQw0x07yDSZSkY04zoofbDzrsjtEFq71VBXwP+CPrJKS0ehkpmhZfQvLrNNzqVDiDvdAinA==",
    "SigningCertURL" to "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-56e67fcb41f6fec09b0196692625d385.pem",
    "UnsubscribeURL" to "https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:$accountId:efimov_sns_test:2d2e43d0-dffb-4be8-9113-5fa2e9910ec6"
)

fun getResourcesAsString(path: String): String {
    return object {}.javaClass.getResource(path)?.readBytes()?.let { String(it) }!!
}
