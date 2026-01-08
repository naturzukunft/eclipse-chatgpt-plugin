package com.github.gradusnikov.eclipse.assistai.network.clients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.core.di.annotations.Creatable;

import com.github.gradusnikov.eclipse.assistai.chat.Attachment;
import com.github.gradusnikov.eclipse.assistai.chat.ChatMessage;
import com.github.gradusnikov.eclipse.assistai.chat.Conversation;
import com.github.gradusnikov.eclipse.assistai.chat.Incoming;
import com.github.gradusnikov.eclipse.assistai.mcp.local.InMemoryMcpClientRetistry;
import com.github.gradusnikov.eclipse.assistai.network.clients.claudecli.BlockType;
import com.github.gradusnikov.eclipse.assistai.network.clients.claudecli.CliContentBlock;
import com.github.gradusnikov.eclipse.assistai.network.clients.claudecli.CliOutputEvent;
import com.github.gradusnikov.eclipse.assistai.network.clients.claudecli.CliOutputLine;
import com.github.gradusnikov.eclipse.assistai.network.clients.claudecli.ContentEventType;
import com.github.gradusnikov.eclipse.assistai.prompt.PromptRepository;
import com.github.gradusnikov.eclipse.assistai.prompt.Prompts;
import com.github.gradusnikov.eclipse.assistai.resources.ResourceCache;

import jakarta.inject.Inject;

@Creatable
public class ClaudeCliStreamClient extends AbstractLanguageModelClient {
	private static final String DEFAULT_MODEL = "claude-sonnet-4-5-20250929";

	private SubmissionPublisher<Incoming> publisher;
	private final List<Flow.Subscriber<Incoming>> subscribers = new ArrayList<>();
	private Supplier<Boolean> isCancelled = () -> false;

	@Inject
	public ClaudeCliStreamClient(ILog logger, LanguageModelClientConfiguration configuration,
			InMemoryMcpClientRetistry mcpClientRegistry, ResourceCache resourceCache,
			PromptRepository promptRepository) {
		super(logger, configuration, mcpClientRegistry, resourceCache, promptRepository);
	}

	@Override
	public void setCancelProvider(Supplier<Boolean> isCancelled) {
		this.isCancelled = isCancelled;
	}

	@Override
	public synchronized void subscribe(Flow.Subscriber<Incoming> subscriber) {
		subscribers.add(subscriber);
	}

	@Override
	public Runnable run(Conversation prompt) {
		return () -> executeConversation(prompt);
	}

	private void executeConversation(Conversation conversation) {
		publisher = new SubmissionPublisher<>(Runnable::run, Flow.defaultBufferSize());
		synchronized (this) {
			for (Flow.Subscriber<Incoming> subscriber : subscribers) {
				publisher.subscribe(subscriber);
			}
		}

		Process process = null;
		try {
			String promptText = buildPrompt(conversation);
			String modelName = model != null ? model.modelName() : DEFAULT_MODEL;

			process = startClaudeProcess(promptText, modelName);

			try (var reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				Incoming.Type incomingType = Incoming.Type.CONTENT;

				while ((line = reader.readLine()) != null && !isCancelled.get()) {
					CliOutputLine outputLine = new CliOutputLine(line);

					if (outputLine.isBlank())
						continue;

					try {
						switch (outputLine.getCliEventType()) {
						case STREAM_EVENT:
							if (outputLine.hasEvent()) {
								CliOutputEvent event = outputLine.getCliOutputEvent();
								ContentEventType eventType = event.getContentEventType();

								if (eventType.isStart()) {
									incomingType = processStartBlock(incomingType, event);
								} else if (eventType.isDelta()) {
									processDeltaBlock(incomingType, event);
								}
							}
							break;

						case ERROR:
							throw new RuntimeException(outputLine.getErrorMessage());

						default:
							break;
						}
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						logger.error("Error parsing CLI response: " + line, e);
					}
				}
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new RuntimeException("Claude CLI exited with code " + exitCode);
			}
		} catch (Exception e) {
			logger.error("Claude CLI error: " + e.getMessage(), e);
			publisher.closeExceptionally(e);
			return;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
			if (isCancelled.get()) {
				publisher.closeExceptionally(new CancellationException());
			} else {
				publisher.close();
			}
		}
	}

	private void processDeltaBlock(Incoming.Type incomingType, CliOutputEvent event) {
		if (event.hasDelta()) {
			if (event.getDeltaText().isPresent()) {
				publisher.submit(new Incoming(incomingType, event.getDeltaText().get()));
			} else if (event.getDeltaPartialJson().isPresent()) {
				publisher.submit(
						new Incoming(incomingType, event.getDeltaPartialJson().get()));
			}
		}
	}

	private Incoming.Type processStartBlock(Incoming.Type incomingType, CliOutputEvent event) {
		Optional<CliContentBlock> cliContentBlockOpt = event.getCliContentBlock();
		if (cliContentBlockOpt.isPresent()) {
			CliContentBlock cliContentBlock = cliContentBlockOpt.get();
			BlockType blockType = cliContentBlock.getType();

			incomingType = blockType.isFunctionCall() ? Incoming.Type.FUNCTION_CALL
					: Incoming.Type.CONTENT;

			if (blockType.isFunctionCall()) {
				publisher.submit(new Incoming(Incoming.Type.FUNCTION_CALL, String.format(
						"\"function_call\" : { \n \"name\": \"%s\",\n \"id\": \"%s\",\n \"arguments\" :",
						cliContentBlock.getToolName(), cliContentBlock.getToolId())));
			}
		}
		return incomingType;
	}

	/**
	 * Starts the Claude CLI process with streaming JSON output.
	 *
	 * @param prompt    the prompt text to send to Claude
	 * @param modelName the model to use (e.g. "claude-sonnet-4-5-20250929")
	 * @return the started Process
	 * @throws IOException if the process cannot be started
	 */
	private Process startClaudeProcess(String prompt, String modelName) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("claude", "-p", "--output-format", "stream-json", "--verbose",
				"--include-partial-messages", "--model", modelName, prompt);
		pb.redirectErrorStream(true);
		pb.redirectInput(ProcessBuilder.Redirect.PIPE);

		logger.info("Starting Claude CLI with model: " + modelName);
		Process process = pb.start();
		process.getOutputStream().close();
		return process;
	}

	private String buildPrompt(Conversation conversation) {
		StringBuilder sb = new StringBuilder();
		String systemPrompt = promptRepository.getPrompt(Prompts.SYSTEM.name());
		String resourcesBlock = resourceCache.toContextBlock();
		if (!resourcesBlock.isEmpty()) {
			systemPrompt = resourcesBlock + "\n\n" + systemPrompt;
		}
		sb.append("<system>\n").append(systemPrompt).append("\n</system>\n\n");

		conversation.messages().stream().filter(Predicate.not(ChatMessage::isEmpty)).forEach(message -> {
			String role = message.getRole();
			String content = message.getContent();
			List<String> textParts = message.getAttachments().stream().map(Attachment::toChatMessageContent)
					.filter(Objects::nonNull).collect(Collectors.toList());
			if (!textParts.isEmpty()) {
				content = String.join("\n", textParts) + "\n\n" + content;
			}
			sb.append("<").append(role).append(">\n").append(content).append("\n</").append(role).append(">\n\n");
		});
		return sb.toString();
	}
}
