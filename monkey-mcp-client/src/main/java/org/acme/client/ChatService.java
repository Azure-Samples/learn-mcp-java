package org.acme.client;

import static java.lang.System.out;

import java.time.Duration;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

public class ChatService {

    private static final String SUUID = UUID.randomUUID().toString().substring(0, 8);
    private static final String RANDOM_USER = "user-" + SUUID;

    private Bot bot;

    private ChatModel chatModel;
    private String provider;
    private String model;

    private ToolsService toolsService = new ToolsService();

    interface Bot {

        @SystemMessage("""
                You are a helpful AI assistant with access to various information tools.
                You can answer questions about anything users ask and help with various tasks.
                Use the available tools to provide accurate and up to date information.
                """)
        String chat(@MemoryId String memoryId, @UserMessage String message);
        
    }

    public ChatService(String provider, String model) {
        this.provider = provider;
        this.model = model;
        
        // Create appropriate chat model based on provider
        switch (provider.toLowerCase()) {
            case "openai" -> {
                chatModel = OpenAiChatModel.builder()
                        .apiKey(System.getenv("OPENAI_API_KEY"))
                        .modelName(model)
                        .timeout(Duration.ofSeconds(30))
                        .temperature(0.7)
                        .build();
            }
            case "ollama" -> {
                chatModel = OllamaChatModel.builder()
                        .baseUrl("http://localhost:11434")
                        .modelName(model)
                        .timeout(Duration.ofSeconds(10))
                        .temperature(0.7)
                        .build();
            }
            default -> {
                out.println("✗ Unsupported provider: " + provider);
                chatModel = null;
                return;
            }
        }

        var chatMemoryStore = new InMemoryChatMemoryStore();

        bot = AiServices.builder(Bot.class)
                .chatModel(chatModel)
                .toolProvider(toolsService.getToolProvider())
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .maxMessages(20)
                        .chatMemoryStore(chatMemoryStore)
                        .id(memoryId)
                        .build())
                .build();

        // Initialize the chat model and check if it's available
        try {
            bot.chat("health-check", "Say YES if you are ready to chat");
        } catch (RuntimeException e) {
            out.println("✗ Failed to connect to " + provider + " model (" + model + "): " + e.getMessage());
            chatModel = null;
            return;
        }

        out.printf("✓ Chat initialized with %s (%s)!\n", provider, model);
    }

    public boolean isAvailable() {
        return chatModel != null && bot != null;
    }

    public void startInteractiveChat() {
        printInfo();

        try (var scanner = new Scanner(System.in)) {
            while (true) {
                out.print("You: ");
                var input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }
                switch (input.toLowerCase()) {
                    case "exit", "quit", "bye" -> {
                        out.println("👋 Goodbye!");
                        toolsService.shutdown();
                        return;
                    }
                }

                var response = bot.chat(RANDOM_USER, input);
                out.printf("AI: %s\n", response);
            }
        }
    }

    private void printInfo() {
        out.printf("""
                ════════════════════════════════════
                      Java MCP Chat Client
                ════════════════════════════════════
                → Starting chat with %s (%s) + MCP Tools

                💡 Try asking:

                   • 'What monkey species do you know?'
                   • 'Tell me about a random monkey'

                Type 'exit', 'quit', or 'bye' to end the conversation
                ───────────────────────────────------------------────
                """, provider, model);
    }

}
