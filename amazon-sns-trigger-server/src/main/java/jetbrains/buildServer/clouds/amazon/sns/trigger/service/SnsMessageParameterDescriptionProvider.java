package jetbrains.buildServer.clouds.amazon.sns.trigger.service;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.parameters.AbstractParameterDescriptionProvider;
import jetbrains.buildServer.util.FileUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SnsMessageParameterDescriptionProvider extends AbstractParameterDescriptionProvider {
    private final static String PARAM_DESCRIPTIONS_RES = "/param-descriptions.xml";

    private final Map<Pattern, String> myDescriptions = new HashMap<>();

    public SnsMessageParameterDescriptionProvider() {
        InputStream is = getClass().getResourceAsStream(PARAM_DESCRIPTIONS_RES);
        try {
            Element parsed = FileUtil.parseDocument(is, false);
            List children = parsed.getChildren("description");

            for (Object child : children) {
                Element descrEl = (Element) child;
                String name = descrEl.getAttributeValue("for");
                String description = descrEl.getText();
                try {
                    myDescriptions.put(Pattern.compile(name), description);
                } catch (Exception e) {
                    Loggers.SERVER.warn("Failed to load parameter description: " + name + ", due to error: " + e);
                }
            }
        } catch (Exception e) {
            Loggers.SERVER.warn("Failed to load parameters descriptions: " + e);
        }
    }

    @Override
    public String describe(@NotNull final String paramName) {
        return myDescriptions.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(paramName).find())
                .findFirst()
                .map(Map.Entry::getValue)
                .map(String::trim)
                .orElse(null);
    }
}
