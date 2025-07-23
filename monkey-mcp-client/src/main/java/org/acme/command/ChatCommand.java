package org.acme.command;

import org.acme.client.ChatService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "chat", description = "Start a chat session")
public class ChatCommand implements Runnable {

    @Option(names = {"--provider"}, description = "Chat provider: ollama (default) or openai", defaultValue = "ollama")
    private String provider;

    @Option(names = {"--model"}, description = "Model name (default: llama3.2 for ollama, gpt-4o-mini for openai)")
    private String model;

    @Override
    public void run() {
        // Determine default model if not specified
        if (model == null) {
            model = switch (provider.toLowerCase()) {
                case "openai" -> "gpt-4o-mini";
                case "ollama" -> "llama3.2";
                default -> "llama3.2";
            };
        }

        // Validate OpenAI API key if needed
        if ("openai".equalsIgnoreCase(provider)) {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                System.err.println("✗ Error: OPENAI_API_KEY environment variable is required when using OpenAI provider.");
                System.err.println("  Please set your OpenAI API key: export OPENAI_API_KEY=your_api_key_here");
                return;
            }
        }

        ChatService chatService = new ChatService(provider, model);
        if (chatService.isAvailable()) {
            chatService.startInteractiveChat();
        } else {
            System.err.println("✗ Chat service is not available. Please check the " + provider + " model connection.");
        }
    }

}
