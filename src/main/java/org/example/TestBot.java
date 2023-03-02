package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import records.ChatGptRequest;
import records.ChatGptResponse;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class TestBot extends TelegramLongPollingBot {
    static int max_tokens = 1000;
    static int temperature = 1;
    static String model = "text-davinci-001";
    static String api_key;
    static String chat_id;

    protected TestBot(DefaultBotOptions options) {
        super(options);
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        //Если пришло новое сообщение
        if (update.hasMessage()) {
            //Получаем новое сообщение
            Message msg = update.getMessage();

            //Если есть текст и сущность (команда)
            if (msg.hasText() && msg.hasEntities())
                handleMessage(msg);
                //Если просто текст
            else if (msg.hasText()) {
                ObjectMapper mapper = new ObjectMapper();

                ChatGptRequest chatGptRequest = new ChatGptRequest(model, msg.getText(), temperature, max_tokens);
                String input = mapper.writeValueAsString(chatGptRequest);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + api_key)
                        .POST(HttpRequest.BodyPublishers.ofString(input))
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());

                //Вывод ответа
                if (response.statusCode() == 200) {
                    ChatGptResponse chatGptResponse = mapper.readValue(response.body(), ChatGptResponse.class);
                    String answer = chatGptResponse.choices()[chatGptResponse.choices().length - 1].text();
                    if (!answer.isEmpty())
                        execute(SendMessage.builder().chatId(msg.getChatId().toString()).text(answer).build());
                }
                //Вывод кода и тела
                else {
                    execute(SendMessage.builder().chatId(msg.getChatId().toString()).text("STATUS CODE: " + response.statusCode() + "\nBODY: " + response.body()).build());
                }
            }
        }
        //Если пришёл ответ (нажатие на кнопку и т.д.)
        //TODO сделать выход из кнопок (и сделать их столбиком, а не в строчку)
        else if (update.hasCallbackQuery()) {
            model = update.getCallbackQuery().getData();
            execute(SendMessage.builder().chatId(chat_id).text("Picked: " + model).build());
        }
    }

    @SneakyThrows
    private void handleMessage(Message msg) {
        if (msg.hasText() && msg.hasEntities()) {
            Optional<MessageEntity> commandEntity = msg.getEntities().stream().filter(x -> "bot_command".equals(x.getType())).findFirst();
            if (commandEntity.isPresent()) {
                String command = msg.getText().substring(commandEntity.get().getOffset(), commandEntity.get().getLength());
                switch (command) {
                    case "/menu":
                        List<InlineKeyboardButton> buttons = new ArrayList<>();
                        for (Setting setting : Setting.values()) {
                            buttons.add(InlineKeyboardButton.builder().text(setting.name()).callbackData(setting.getRealName()).build());
                        }

                        List<List<InlineKeyboardButton>> columns = new ArrayList<>();
                        columns.add(buttons);

                        execute(SendMessage.builder().chatId(msg.getChatId().toString()).text("Choose model (now chosen: " + model + ")").replyMarkup(InlineKeyboardMarkup.builder().keyboard(columns).build()).build());
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "@test_gpthueta_bot";
    }

    @Override
    public String getBotToken() {
        return "5604932118:AAET5QilUlFx2rwQdIDrGclYPRLPFKyFgqU";
    }

    //example
    @SneakyThrows
    public static void main(String[] args) {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = TestBot.class.getClassLoader().getResourceAsStream("config.properties");
            if (input == null) {
                System.out.println("Unable to find config.properties");
                return;
            }
            prop.load(input);
            api_key = prop.getProperty("api_key");
            chat_id = prop.getProperty("chat_id");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        TestBot testBot = new TestBot(new DefaultBotOptions());
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(testBot);
    }
}