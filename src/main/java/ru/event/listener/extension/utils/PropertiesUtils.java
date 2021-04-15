package ru.event.listener.extension.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@UtilityClass
public class PropertiesUtils {

    private static final String PROPERTIES_FILE_NAME = "application.properties";
    private static final String PROPERTIES_ABSOLUTE_PATH = "opt/jboss/keycloak/standalone/deployments/";
    private static final Properties properties = new Properties();

    static {
        try (InputStream inputStream = new FileInputStream(PROPERTIES_ABSOLUTE_PATH + PROPERTIES_FILE_NAME)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            log.warn("Could not open application.properties on container, opening local properties...");

            try (InputStream file = PropertiesUtils.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME)) {
                properties.load(file);
            } catch (IOException e) {
                log.error("Could not open local properties");
            }
        }
    }

    public static String readProperty(String property) {
        return properties.getProperty(property);
    }
}
