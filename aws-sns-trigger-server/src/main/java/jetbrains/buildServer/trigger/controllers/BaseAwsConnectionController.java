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

package jetbrains.buildServer.trigger.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  protected <T> void writeAsJson(@NotNull T value, @NotNull HttpServletResponse response) throws IOException {
    writeAsJson(value, response, HttpServletResponse.SC_OK);
  }

  protected <T> void writeAsJson(@NotNull T value, @NotNull HttpServletResponse response, int status) throws IOException {
    final String json = OBJECT_MAPPER.writeValueAsString(value);
    response.setContentType("application/json");
    response.setCharacterEncoding(Charsets.UTF_8.name());
    response.setStatus(status);
    final PrintWriter writer = response.getWriter();
    writer.write(json);
    writer.flush();
  }

  protected <T> T readJson(@NotNull HttpServletRequest request) throws IOException {
    T requestObject;
    try (Reader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
      requestObject = OBJECT_MAPPER.readValue(reader, new TypeReference<T>() {
      });
    }

    return requestObject;
  }
}
