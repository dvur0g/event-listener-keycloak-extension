package ru.event.listener.extension.event.impl;

import lombok.AllArgsConstructor;
import org.keycloak.events.admin.AdminEvent;
import ru.event.listener.extension.dto.PasswordResetEventRequest;
import ru.event.listener.extension.event.CustomAdminEvent;
import ru.event.listener.extension.utils.InternalEventSender;
import ru.event.listener.extension.utils.PropertiesUtils;

@AllArgsConstructor
public class PasswordResetEvent implements CustomAdminEvent {

    private static final String ACTION_OPERATION_TYPE = "ACTION";
    private static final String USERS_RESOURCE_PATH = "users";
    private static final String RESET_PASSWORD_RESOURCE_PATH = "reset-password";
    private static final String URL = PropertiesUtils.readProperty("user-service.password-reset-event.uri");

    private final AdminEvent event;

    @Override
    public void process() {
        String[] resourcePath = event.getResourcePath().split("/");
        new InternalEventSender<PasswordResetEventRequest>()
                .send(new PasswordResetEventRequest(resourcePath[1]), URL);
    }

    @Override
    public boolean isValid() {
        return isPasswordResetEvent(event);
    }

    private boolean isPasswordResetEvent(AdminEvent event) {
        String[] resourcePath = event.getResourcePath().split("/");
        return ACTION_OPERATION_TYPE.equals(event.getOperationType().toString()) &&
                USERS_RESOURCE_PATH.equals(resourcePath[0]) &&
                RESET_PASSWORD_RESOURCE_PATH.equals(resourcePath[2]);
    }
}
