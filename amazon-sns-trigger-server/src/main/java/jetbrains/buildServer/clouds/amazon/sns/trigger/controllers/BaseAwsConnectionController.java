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

package jetbrains.buildServer.clouds.amazon.sns.trigger.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jetbrains.buildServer.clouds.amazon.sns.trigger.errors.AwsSnsHttpEndpointException;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import org.apache.commons.codec.Charsets;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;

public abstract class BaseAwsConnectionController extends BaseController {

  private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public BaseAwsConnectionController(@NotNull final SBuildServer server) {
    super(server);
  }

  protected <T> void writeErrorsAsJson(@NotNull T value, @NotNull HttpServletResponse response) throws IOException {
      final String json = OBJECT_MAPPER.writeValueAsString(value);
      response.setContentType("application/json");
      response.setCharacterEncoding(Charsets.UTF_8.name());
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      final PrintWriter writer = response.getWriter();
      writer.write(json);
      writer.flush();
  }

    protected <T> T readJson(@NotNull HttpServletRequest request) throws AwsSnsHttpEndpointException {
        T requestObject;
        try (Reader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            requestObject = OBJECT_MAPPER.readValue(reader, new TypeReference<T>() {
            });
        } catch (Exception e) {
            throw new AwsSnsHttpEndpointException("Can't parse request body", e);
        }

        return requestObject;
    }
}
