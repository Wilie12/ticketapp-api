package com.nn.ticketapp_api.notification.template;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailTemplateProcessor {

    private final ITemplateEngine templateEngine;

    public String processTemplate(String templateName, Map<String, Object> variables) {
        return Optional.ofNullable(variables)
                .map(vars -> {
                    Context context = new Context();
                    context.setVariables(vars);
                    return templateEngine.process(templateName, context);
                })
                .orElseGet(() -> templateEngine.process(templateName, new Context()));
    }
}
