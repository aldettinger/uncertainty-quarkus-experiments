package org.aldettinger;

import java.time.Month;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface CustomPojoExtractionService {

    @RegisterForReflection
    static class CustomPojo {
        public boolean customerSatisfied;
        public String customerName;
        public CustomDate customerBirthday;
        public String summary;
    }

    @RegisterForReflection
    static class CustomDate {
        int year;
        int month;
        int day;

        public String toString() {
            return String.format("%d %s %d", day, Month.of(month), year);
        }
    }

    static final String CUSTOM_POJO_EXTRACT_PROMPT
            = "Extract information about a customer from the text delimited by triple backticks: ```{text}```."
              + "The summary field should concisely relate the customer main ask.";

    @UserMessage(CUSTOM_POJO_EXTRACT_PROMPT)
    CustomPojo extractFromText(String text);
}
