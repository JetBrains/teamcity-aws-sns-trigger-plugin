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

package jetbrains.buildServer.clouds.amazon.sns.trigger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.buildTriggers.PolledTriggerContext;
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES;

public class AwsSnsBuildTriggerState {

  private final CustomDataStorage myStorage;
  private final ObjectMapper myObjectMapper;
  private final Logger myLogger;

  public AwsSnsBuildTriggerState(@NotNull PolledTriggerContext triggerContext,
                                 @NotNull ObjectMapper objectMapper,
                                 @NotNull Logger contextLogger) {
    myStorage = triggerContext.getCustomDataStorage();
    myObjectMapper = objectMapper;
    myLogger = contextLogger;
  }

  public boolean hasNewNotifications() {
      return myStorage.getValue(TRIGGER_STORE_MESSAGES) != null;
  }

  @NotNull
  public Map<String, SnsNotificationDto> getRegisteredMessages() {
    String messagesMapAsString = myStorage.getValue(TRIGGER_STORE_MESSAGES);

    if (messagesMapAsString == null || messagesMapAsString.isEmpty()) {
      return Collections.emptyMap();
    }

    try {
      return myObjectMapper.readValue(messagesMapAsString, new TypeReference<HashMap<String, SnsNotificationDto>>() {
      });
    } catch (JsonProcessingException err) {
      myLogger.debug("Exception during decerialization of the messages key from storage: " + err.getMessage(), err);
      myLogger.error("Something went terribly wrong. Try to recreate the trigger.");
      return Collections.emptyMap();
    }
  }

  public void persist(Set<String> registeredMessagesIds) {
    Map<String, SnsNotificationDto> messages = getRegisteredMessages();

    for (String id : registeredMessagesIds) {
      messages.remove(id);
    }

    if (messages.isEmpty()) {
        myStorage.putValue(TRIGGER_STORE_MESSAGES, null);
    } else {
      try {
          myStorage.putValue(TRIGGER_STORE_MESSAGES, myObjectMapper.writeValueAsString(messages));
      } catch (JsonProcessingException err) {
        myLogger.error("Something went terribly wrong. Try to recreate the trigger.", err);
        throw new IllegalStateException(err);
      }
    }

    myStorage.flush();
  }

  public void resetMessagesMap() {
      myStorage.putValue(TRIGGER_STORE_MESSAGES, null);
  }

}
