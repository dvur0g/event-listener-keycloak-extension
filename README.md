# Расширение Keycloak для перехвата и обработки событий в системе

Кому не нужна вода, а просто пример кода, прыгайте сразу [сюда]().

Как известно, Keycloak написан на языке Java, и создатели заложили очень удобную возможность расширять функционал готового решения так называемыми **аддонами** или официально: **extentions**. 

**Расширение** предсталяет собой обычный проект на Java, состоящий из классов, расширяющих дефолтные классы/интерфейсы keycloak с  необходим дополнительным функционалом. Причём расширить можно функционал чуть ли не любого класса keycloak и для любых целей: от минимального изменения текста сообщения о некорректном вводе пользователем пароля, до привязки *Discord*'а, как *Identity provider*'а.

В данной статье речь пойдёт об расширении дефолтного слушателя событий в кейклоке. 

**Краткая предыстория:** была поставлена задача отслеживания события сброса пароля  у админа для логирования события  и актуализации данных об этом админе в системе.

## Исходный код

Необходимо создать обычный Java проект и подрубить несколько библиотек. Для удобства был испльзован  сборщик **Maven**. Необходимые библиотеки представлены в **pom.xml** файле ниже:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <artifactId>event-listener-keycloak-extension</artifactId>

    <parent>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-parent</artifactId>
        <version>12.0.4</version>
    </parent>

    <properties>
        <keycloak.version>12.0.4</keycloak.version>
        <lombok.version>1.18.20</lombok.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-core</artifactId>
            <version>${keycloak.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-server-spi</artifactId>
            <version>${keycloak.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-server-spi-private</artifactId>
            <version>${keycloak.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-services</artifactId>
            <version>${keycloak.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-saml-core-public</artifactId>
            <version>${keycloak.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.jboss.logging</groupId>
            <artifactId>jboss-logging</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.jboss.spec.javax.ws.rs</groupId>
            <artifactId>jboss-jaxrs-api_2.1_spec</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>event-listener-keycloak-extension</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Из **необязательных** зависимостей здесь только *Lombok*. В моём проекте он нужен для удобного логирования событий в консоли и парочки конструкторов.

В тегах `artifactId` и `fileName` можете указать свою информацию.

Теперь необходимо создать реализацию необходимых нам интерфейсов, а именно двух:

1. **`EventListenerProvider`.** Дефолтный интерфейс *провайдера* для перехвата всех событий в системе. Реализация будет содержать саму логику нашего расширения.
2. **`EventListenerProviderFactory`.** Интерфейс фабрики для инициализации экземпляров провайдера **`EventListenerProvider`.**  При каждом новом событии в системе *factory* создаёт новый экземпляр *провайдера* **`EventListenerProvider`**, и как только *провайдер* выполнит свою работу - удаляется из памяти.

### EventListenerProvider

Создадим реализацию **`EventListenerProvider`** c названием `CustomEventListenerProvider`:

```java
@Slf4j
@NoArgsConstructor
public class CustomEventListenerProvider implements EventListenerProvider {

    @Override
    public void onEvent(Event event) {
      log.info("Caught event {}", EventUtils.toString(event));
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        log.info("Caught admin event {}", EventUtils.toString(adminEvent));
		}

    @Override
    public void close() {

    }
}
```

У данного интерфейса необходимо определить три метода: 

1. `onEvent` метод, прехватывающий обычные события в системе, такие как событие неправильного ввода пароля, удачной авторизации, логаута. В аргументе приходит сам экземпляр события со всей необходимой информацией: тип события, id пользователя и сессии, IP пользователя и т. д.
2. `onAdminEvent` перехватывает "админские" события, например: событие сброса пароля пользователя через админскую консоль keycloak. 
3. `close` своего рода деструктор, вызывается при удалении текущего *провайдера*.

Аннотация `@Slf4j` из библиотеки Lombok  используется для логирования событий в консоль через переменную `log`, вы же можете выводить их просто через `System.out.println`.

Перевод объектов событий в текстовый вид решил вынести в отдельный класс `EventUtils`, который представлен ниже. 

### EventListenerProviderFactory

Второй и последний обязательный необходимый нам класс имплементирует **`EventListenerProviderFactory`:**

```java
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
```

Тут уже методов побольше, посмотрим, за что они отвечают:

1. `create` будет возвращать наш кастомный провайдер `CustomEventListenerProvider`. Вызывается  при каждом новом событии в системе. Сама же фабрика `CustomEventListenerProviderFactory`  создаётся один раз на протяжении работы keycloak.
2. `init` вызывается только один раз при первом создании фабрики.
3. `postInit` вызывается один раз после инициализации всех *фабрик провайдров* в системе.
4. `close` вызывается при отключении keycloak сервера.
5. `getId` устанавливает название нашего раширения при создании фабрики.

Это все необходимые классы. Добавим ещё один класс, вспомогательный, только для того, чтобы отобразить `Event` и `AdminEvent` в текстовом виде со всеми их полями в  консоли:

```java
@UtilityClass
public class EventUtils {

    public static String toString(AdminEvent adminEvent) {
        StringBuilder sb = new StringBuilder();

        sb.append("operationType="); sb.append(adminEvent.getOperationType());
        sb.append(", realmId="); sb.append(adminEvent.getAuthDetails().getRealmId());
        sb.append(", clientId="); sb.append(adminEvent.getAuthDetails().getClientId());
        sb.append(", userId="); sb.append(adminEvent.getAuthDetails().getUserId());
        sb.append(", ipAddress="); sb.append(adminEvent.getAuthDetails().getIpAddress());
        sb.append(", resourcePath="); sb.append(adminEvent.getResourcePath());

        if (adminEvent.getError() != null) {
            sb.append(", error="); sb.append(adminEvent.getError());
        }
        return sb.toString();
    }

    public static String toString(Event event) {
        StringBuilder sb = new StringBuilder();

        sb.append("type="); sb.append(event.getType());
        sb.append(", realmId="); sb.append(event.getRealmId());
        sb.append(", clientId="); sb.append(event.getClientId());
        sb.append(", userId="); sb.append(event.getUserId());
        sb.append(", ipAddress="); sb.append(event.getIpAddress());

        if (event.getError() != null) {
            sb.append(", error="); sb.append(event.getError());
        }

        if (event.getDetails() != null) {
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                sb.append(", "); sb.append(e.getKey());
                if (e.getValue() == null || e.getValue().indexOf(' ') == -1) {
                    sb.append("="); sb.append(e.getValue());
                } else {
                    sb.append("='"); sb.append(e.getValue()); sb.append("'");
                }
            }
        }
        return sb.toString();
    }
}
```

Осталось только указать путь к нашей фабрике `CustomEventListenerProviderFactory` для keycloak.  

Для этого необходимо создать файл с названием `org.keycloak.events.EventListenerProviderFactory` по пути `src/main/resources/META-INF/services/`. Отсутствующие в проекте директории необходимо создать.

И в данный файл поместить строку:

```java
ru.event.listener.extension.factory.CustomEventListenerProviderFactory
```

То есть полный путь до класса нашей кастомной фабрики. Только так keycloak сможет заменить дефолтную фабрику с дефолтным обработчиком событий на нашу.  На этом всё.

Теперь необходимо собрать получившееся расширение в **JAR** файл. Если вы используется **Maven**, после сборки, в папке `target` появится два **JAR** файла. Нам нужен тот, что без `-sources`. В нашем случае это `keycloak-logging-plugin.jar`. Собрать его можно с помощью команды:

```java
mvn clean package
```

![Структура проекта](https://i.ibb.co/PjnH40D/Untitled.png)

Так выглядит полный проект.

## Запуск расширения

Установку и запуск keycloak я здесь рассматривать не буду, на эту тему есть исчерпывающие статьи. Например [официальный сайт](https://www.keycloak.org/docs/latest/getting_started/). Будем считать, что у нас уже стоит работающий keycloak.

Собранный **JAR** файл `keycloak-logging-plugin.jar` необходимо поместить в папку с keycloak по пути `<ДИРЕКТОРИЯ_KEYCLOAK>/standalone/deployments/`, причём нет необходимости перезапускать keycloak после деплоя. Да, keycloak поддерживает **hot swap** или замену файлов "на ходу".  Как только наш JAR файл окажется в директории, keycloak его задеплоит и будет готов к работе.

Даже если вы потом будете заменять уже находящийся файл там точно таким же, он всё равно задеплоится заново.

Сообщение о начале, а затем и об успешном деплое в консоли keycloak  выглядит примерно так:

```java
19:37:58,203 INFO [org.jboss.as.server.deployment] (MSC service thread 1-1) WFLYSRV0027: Starting deployment of "event-listener-keycloak-extension.jar" (runtime-name: "event-listener-keycloak-extension.jar")
19:37:58,322 INFO [org.keycloak.subsystem.server.extension.KeycloakProviderDeploymentProcessor] (MSC service thread 1-7) Deploying Keycloak provider: event-listener-keycloak-extension.jar
19:37:58,334 WARN [org.keycloak.services] (MSC service thread 1-7) KC-SERVICES0047: event-listener-extension (ru.event.listener.extension.factory.CustomEventListenerProviderFactory) is implementing the internal SPI eventsListener. This SPI is internal and may change without notice
19:37:58,366 INFO [org.jboss.as.server] (DeploymentScanner-threads - 1) WFLYSRV0010: Deployed "event-listener-keycloak-extension.jar" (runtime-name : "event-listener-keycloak-extension.jar")
```

 

А в директории рядом с нашим JAR файлом появился такой же с приставкой `.deployed`.

Но это ещё не всё. Теперь нам необходимо определить наш плагин как слушатель событий в конфиге кейклока. Это делается в админской консоли на вкладке Events → Config:

![Конфиг события](https://i.ibb.co/GMX8PPt/Untitled-1.png)

Если дейплой расширения произошёл успешно, в выпадающем меню в поля **Event Listeners** появится наш плагин.

Необходимо выбрать наш плагин и нажать **Save**.

## Проверка работы

Попробуем залогинится каким нибудь пользователем. В консоли появляется следующая запись:

```java
20:02:14,474 INFO  [ru.event.listener.extension.CustomEventListenerProvider] (default task-11) Caught event type=LOGIN, realmId=master, clientId=account-console, userId=8cbc9aec-0c5f-45e0-b614-baf9e96c2278, ipAddress=127.0.0.1, auth_method=openid-connect, auth_type=code, redirect_uri=http://localhost:8080/auth/realms/master/account/#/, consent=no_consent_required, code_id=007a3edc-4541-4648-b1e6-44c30349c001, username=test
```

Разлогинимся:

```java
20:03:13,143 INFO  [ru.event.listener.extension.CustomEventListenerProvider] (default task-11) Caught event type=LOGOUT, realmId=master, clientId=null, userId=8cbc9aec-0c5f-45e0-b614-baf9e96c2278, ipAddress=127.0.0.1, redirect_uri=http://localhost:8080/auth/realms/master/account/#/
```

Попробуем залогиниться с некорректным паролем:

```java
20:03:42,204 WARN  [org.keycloak.events] (default task-11) type=LOGIN_ERROR, realmId=master, clientId=account-console, userId=8cbc9aec-0c5f-45e0-b614-baf9e96c2278, ipAddress=127.0.0.1, error=invalid_user_credentials, auth_method=openid-connect, auth_type=code, redirect_uri=http://localhost:8080/auth/realms/master/account/#/, code_id=f0d48657-3673-4875-bb72-a7f1d89b6d31, username=test, authSessionParentId=f0d48657-3673-4875-bb72-a7f1d89b6d31, authSessionTabId=h6V1w1C3Zjk
```

Сбросим пароль пользователю через админскую консоль (`AdminEvent`):

```java
20:05:05,045 INFO  [ru.event.listener.extension.CustomEventListenerProvider] (default task-20) Caught admin event operationType=ACTION, realmId=master, clientId=cff15a39-3a5d-49c6-baf1-1c8d9dee1ce6, userId=a64026c4-689f-4213-8229-b8ac471150ea, ipAddress=127.0.0.1, resourcePath=users/8cbc9aec-0c5f-45e0-b614-baf9e96c2278/reset-password
```

## Заключение

В данной статье описано только минимальное расширение для отлавливания событий в keycloak, вы же можете делать с ними всё, что вам необходимо. В моём случае необходимо было отлавливать только события сброса пароля юзеру через админскую консоль, и отправлять логин этого юзера в микросервис через REST запрос. С рабочими исходниками этого проекта можете ознакомиться [тут](https://github.com/dvur0g/event-listener-keycloak-extension).

## Приложение

Кстати, до этого была задача на отслеживание события в keycloak о том, что пользователь сделал максимальное количество неверных попыток ввода пароля и его временно заблокировали (keycloak предлагает такой функционал из коробки, называется **brute force detector**). 

Но, либо из соображений о безопасности, либо просто из-за того, что фича никому не нужна, такое событие в keycloak в принципе отсутствует. Только если вручную считать для каждого пользователя количество событий неправильного ввода пароля за промежуток времени, указанные в конфигурации keycloak, можно такое событие отследить. Но есть подоздрения, что из-за отличной гибкости расширений, можно написать свою кастомную реализацию интерфейса  `BruteForceProtector`, дефолтная реализация которого отвечает за временную блокировку при неправильных попытках ввода пароля, и добавить такое событие. 

Пока все попытки не увенчались успехом, но автор не опускает руки, так что, возможно, скоро будут новости по этому поводу.
