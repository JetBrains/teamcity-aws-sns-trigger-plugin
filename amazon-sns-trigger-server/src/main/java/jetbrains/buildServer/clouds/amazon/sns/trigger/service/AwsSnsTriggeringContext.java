

package jetbrains.buildServer.clouds.amazon.sns.trigger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.WebLinks;
import org.jetbrains.annotations.NotNull;

public class AwsSnsTriggeringContext {
  private final ObjectMapper myObjectMapper = new ObjectMapper()
          .registerModule(new ParameterNamesModule())
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule());
  private final ProjectManager myProjectManager;
  private final WebLinks myWebLinks;
  private final SnsMessageParametersCustomisationService myParameterCustomisationService;

  public AwsSnsTriggeringContext(
          @NotNull final ProjectManager projectManager,
          @NotNull final WebLinks webLinks,
          @NotNull final SnsMessageParametersCustomisationService parameterCustomizationService
  ) {
    myObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    myProjectManager = projectManager;
    myWebLinks = webLinks;
    myParameterCustomisationService = parameterCustomizationService;
  }

  @NotNull
  public ObjectMapper getObjectMapper() {
    return myObjectMapper;
  }

  public ProjectManager getProjectManager() {
    return myProjectManager;
  }

  public WebLinks getWebLinks() {
    return myWebLinks;
  }

  public SnsMessageParametersCustomisationService getParameterCustomisationService() {
    return myParameterCustomisationService;
  }
}