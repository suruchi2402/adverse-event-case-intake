package com.example.caseintake;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Arrays;

/**
 * One jar, two modes.
 * <ul>
 *   <li>{@code java -jar case-intake.jar} starts the review API on port 8080.</li>
 *   <li>{@code java -jar case-intake.jar extract [options]} runs the extraction pipeline and exits (see {@link ExtractCommand}).</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CaseIntakeApplication {

    public static final String EXTRACT_COMMAND = "extract";
    public static final String EXTRACT_PROFILE = "extract";

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals(EXTRACT_COMMAND)) {
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            try {
                new SpringApplicationBuilder(CaseIntakeApplication.class)
                        .web(WebApplicationType.NONE)
                        .profiles(EXTRACT_PROFILE)
                        .run(rest)
                        .close();
            } catch (RuntimeException e) {
                System.err.println(rootMessage(e));
                System.exit(1);
            }
            return;
        }
        new SpringApplicationBuilder(CaseIntakeApplication.class).run(args);
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
