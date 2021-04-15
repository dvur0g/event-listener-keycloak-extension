package ru.event.listener.extension.factory;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import ru.event.listener.extension.CustomEventListenerProvider;

public class CustomEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final String LISTENER_ID = "event-listener-extension";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new CustomEventListenerProvider();
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return LISTENER_ID;
    }

}
