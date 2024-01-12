

package jetbrains.buildServer.clouds.amazon.sns.trigger.errors;

import org.jetbrains.annotations.NotNull;

public class AwsSnsHttpEndpointException extends Exception {

  public AwsSnsHttpEndpointException(@NotNull final String message) {
    super(message);
  }

  public AwsSnsHttpEndpointException(@NotNull final String message, @NotNull Exception cause) {
    super(message, cause);
  }
}