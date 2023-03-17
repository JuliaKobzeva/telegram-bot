package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sky.telegrambot.service.NotificationTaskService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class TelegramBotUpdatesListenerTest {
    @Mock
    private TelegramBot telegramBot;
    @Mock
    private NotificationTaskService notificationTaskService;
    @InjectMocks
    private TelegramBotUpdatesListener telegramBotUpdatesListener;

    @Test
    public void handleStartTest() throws URISyntaxException, IOException {
        String json = Files.readString(
                Paths.get(TelegramBotUpdatesListenerTest.class.getResource("text_update.json").toURI()));
        Update update = getUpdate(json, "/start");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(123L);
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo(
                "Отправьте задачу в формате:\n*01.01.2022 20:00 Сделать домашнюю работу*");
        Assertions.assertThat(actual.getParameters().get("parse_mode")).isEqualTo(ParseMode.Markdown.name());
    }

    @Test
    public void handleInvalidMessage() throws URISyntaxException, IOException {
        String json = Files.readString(
                Paths.get(TelegramBotUpdatesListenerTest.class.getResource("text_update.json").toURI()));
        Update update = getUpdate(json, "hello");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(123L);
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo(
                "Некорректный формат задачи для планирования");
    }

    @Test
    public void handleInvalidDateFormat() throws URISyntaxException, IOException {
        String json = Files.readString(
                Paths.get(TelegramBotUpdatesListenerTest.class.getResource("text_update.json").toURI()));
        Update update = getUpdate(json, "01.01.2024 12:00 do something");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(123L);
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo(
                "Некорректный формат даты или времени");
    }

    @Test
    public void handleValidMessage() throws URISyntaxException, IOException {
        String json = Files.readString(
                Paths.get(TelegramBotUpdatesListenerTest.class.getResource("text_update.json").toURI()));
        Update update = getUpdate(json, "01.01.2024 12:00 do something");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> sendMessageArgumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(sendMessageArgumentCaptor.capture());
        SendMessage actualSendMessage = sendMessageArgumentCaptor.getValue();

        ArgumentCaptor<LocalDateTime> localDateTimeArgumentCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> stringTimeArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> longTimeArgumentCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(notificationTaskService).addNotificationTask(
                localDateTimeArgumentCaptor.capture(),
                stringTimeArgumentCaptor.capture(),
                longTimeArgumentCaptor.capture()
        );
        LocalDateTime actualLocalDateTime = localDateTimeArgumentCaptor.getValue();
        String actualString = stringTimeArgumentCaptor.getValue();
        Long actualLong = longTimeArgumentCaptor.getValue();

        Assertions.assertThat(actualLocalDateTime)
                .isEqualTo(LocalDateTime.of(2024, Month.JANUARY, 1, 12, 0));
        Assertions.assertThat(actualString).isEqualTo("do something");
        Assertions.assertThat(actualLong).isEqualTo("123L");

        Assertions.assertThat(actualSendMessage.getParameters().get("chat_id")).isEqualTo(123L);
        Assertions.assertThat(actualSendMessage.getParameters().get("text")).isEqualTo("Ваша задача запланирована");
    }

    private Update getUpdate(String json, String replaced){
        return BotUtils.fromJson(json.replace("%command%", replaced), Update.class);
    }

}
