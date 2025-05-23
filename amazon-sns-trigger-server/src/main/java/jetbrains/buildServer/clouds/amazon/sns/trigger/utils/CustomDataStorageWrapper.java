package jetbrains.buildServer.clouds.amazon.sns.trigger.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomDataStorageWrapper {
  private static final Logger LOG = Logger.getInstance(CustomDataStorageWrapper.class);
  // copy from jetbrains.buildServer.clouds.amazon.sns.trigger.service.AwsSnsTriggeringContext
  private final ObjectMapper myObjectMapper = new ObjectMapper()
    .registerModule(new ParameterNamesModule())
    .registerModule(new Jdk8Module())
    .registerModule(new JavaTimeModule());

  @NotNull
  private final CustomDataStorage myCustomDataStorage;
  @NotNull
  private final Map<String, SnsNotificationDto> myInitialMessagesState;

  @NotNull
  private final ConcurrentHashMap<String, Optional<String>> myStateCopy = new ConcurrentHashMap<>();

  public CustomDataStorageWrapper(@NotNull CustomDataStorage customDataStorage) {
    myCustomDataStorage = customDataStorage;
    myStateCopy.putAll(transformToStringOptionalMap(getValuesSafe()));
    myInitialMessagesState = getInitialMessagesState();
  }

  private Map<String, SnsNotificationDto> getInitialMessagesState() {
    try {
      String messagesMapAsString = myStateCopy.get(AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES).orElse(null);
      return safeGetMessagesMap(messagesMapAsString);
    } catch (Exception e) {
      LOG.warnAndDebugDetails("Failed to parse initial messages state", e);
    }

    return Collections.emptyMap();
  }

  public void putValue(@NotNull String key, @Nullable String value) {
    myStateCopy.put(key, Optional.ofNullable(value));
    myCustomDataStorage.putValue(key, value);
    flush();
  }

  public @Nullable String getValue(@NotNull String key) {
    return myCustomDataStorage.getValue(key);
  }

  public void flush() {
    safeFlushWithLogging();
  }

  public @Nullable Map<String, String> getValues() {
    return myCustomDataStorage.getValues();
  }

  public @NotNull Map<String, String> getValuesSafe() {
    Map<String, String> values = myCustomDataStorage.getValues();
    if (values == null) {
      return Collections.emptyMap();
    }
    return values;
  }

  // with putValues data will be overwritten
  public void putValues(@NotNull Map<String, String> values) {
    myStateCopy.putAll(transformToStringOptionalMap(values));
    myCustomDataStorage.putValues(values);
    flush();
  }

  @NotNull
  private static Map<String, Optional<String>> transformToStringOptionalMap(@NotNull Map<String, String> values) {
    return values.entrySet().stream()
                 .collect(
                   HashMap::new,
                   (map, entry) -> map.put(entry.getKey(), Optional.ofNullable(entry.getValue())),
                   HashMap::putAll
                 );
  }

  private void safeFlushWithLogging() {
    if (!safeFlush()) {
      LOG.error("Failed to flush session storage after 10 retries. Session data may be lost.");
    }

    // in any case, we need to update our state copy with the latest values from store
    myStateCopy.putAll(transformToStringOptionalMap(getValuesSafe()));
  }

  private boolean safeFlush() {
    int iteration = 0;

    do {
      try {
        myCustomDataStorage.flush(CustomDataStorage.ConflictResolution.FAIL);
        return true;
      } catch (Throwable e) {
        LOG.debug("Failed to flush session storage: ${e.message}", e);

        if (iteration++ >= 10) {
          return false;
        }

        // Refresh storage to get latest values
        myCustomDataStorage.refresh();
        Map<String, String> storageValues = getValuesSafe();

        // Create a merged map with conflict resolution
        Map<String, String> mergedValues = new HashMap<>(storageValues);

        for (Map.Entry<String, Optional<String>> entry : myStateCopy.entrySet()) {
          String key = entry.getKey();
          String localValue = entry.getValue().orElse(null);
          String storageValue = storageValues.get(key);

          if (!Objects.equals(localValue, storageValue)) {
            // Conflict detected
            String resolvedValue = resolveConflict(key, localValue, storageValue);
            mergedValues.put(key, resolvedValue);
          } else {
            // No conflict; keep the local value
            mergedValues.put(key, localValue);
          }
        }

        // Update storage with merged values
        myCustomDataStorage.putValues(mergedValues);
      }
    } while (true);
  }

  private @Nullable String resolveConflict(@NotNull String key, @Nullable String localValue, @Nullable String storageValue) {
    LOG.warn("Conflict detected for key " + key + ": localValue=" + localValue + ", storageValue=" + storageValue);

    if (AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES.equals(key)) {
      try {
        Map<String, SnsNotificationDto> localMessages = safeGetMessagesMap(localValue);
        // Calculate added keys: keys present in currentState but not in InitialMessagesState
        Set<String> addedKeys = new HashSet<>(localMessages.keySet());
        addedKeys.removeAll(myInitialMessagesState.keySet());

        // Calculate deleted keys: keys present in InitialMessagesState but not in currentState
        Set<String> deletedKeys = new HashSet<>(myInitialMessagesState.keySet());
        deletedKeys.removeAll(localMessages.keySet());

        // Merge messages
        Map<String, SnsNotificationDto> mergedMessages = safeGetMessagesMap(storageValue);
        deletedKeys.forEach(mergedMessages::remove);
        addedKeys.forEach(it -> mergedMessages.put(it, localMessages.get(it)));

        if (mergedMessages.isEmpty()) {
          return null;
        }

        return myObjectMapper.writeValueAsString(mergedMessages);
      } catch (Exception e) {
        LOG.warnAndDebugDetails("Failed to merge messages", e);
      }
    }

    return localValue;
  }

  private @NotNull Map<String, SnsNotificationDto> safeGetMessagesMap(@Nullable String messagesString) throws JsonProcessingException {
    if (messagesString == null || messagesString.isBlank()) {
      return new HashMap<>();
    }

    return getMessagesMap(messagesString);
  }

  private @NotNull Map<String, SnsNotificationDto> getMessagesMap(@NotNull String localValue) throws JsonProcessingException {
    return myObjectMapper.readValue(localValue, new TypeReference<HashMap<String, SnsNotificationDto>>() {
    });
  }
}
