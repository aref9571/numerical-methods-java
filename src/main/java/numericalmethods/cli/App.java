package numericalmethods.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class App {

    private final BufferedReader in;
    private final PrintStream out;

    public App(InputStream input, PrintStream output) {
        this.in = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        this.out = output;
    }

    record Prompt(String question, String defaultValue) {
    }

    record MenuItem(String label, List<Prompt> prompts, Function<List<String>, String> command) {
    }

    record Topic(String name, List<MenuItem> items) {
    }

    public void run() {
        out.println("Numerical Methods");
        out.println("Choose a topic by number, type a one-line command (try 'help'), or 'q' to quit.");
        while (true) {
            out.println();
            List<Topic> topics = Menus.topics();
            for (int i = 0; i < topics.size(); i++) {
                out.println("  " + (i + 1) + ". " + topics.get(i).name());
            }
            out.println("  h. Help with one-line commands      q. Quit");
            String line = read("> ");
            if (line == null || isQuit(line)) {
                out.println("Goodbye.");
                return;
            }
            if (line.isBlank()) {
                continue;
            }
            String choice = line.trim().toLowerCase(Locale.ROOT);
            if (choice.equals("h") || choice.equals("help")) {
                out.println(Commands.help());
                continue;
            }
            Integer number = asNumber(choice);
            if (number != null) {
                if (number < 1 || number > topics.size()) {
                    out.println("Please choose a number between 1 and " + topics.size() + ".");
                    continue;
                }
                if (!topicMenu(topics.get(number - 1))) {
                    out.println("Goodbye.");
                    return;
                }
                continue;
            }
            runCommand(line);
        }
    }

    private boolean topicMenu(Topic topic) {
        while (true) {
            out.println();
            out.println(topic.name());
            for (int i = 0; i < topic.items().size(); i++) {
                out.println("  " + (i + 1) + ". " + topic.items().get(i).label());
            }
            out.println("  b. Back      (or paste a one-line command here)");
            String line = read("> ");
            if (line == null) {
                return false;
            }
            String choice = line.trim().toLowerCase(Locale.ROOT);
            if (choice.equals("b") || choice.equals("back") || choice.isEmpty()) {
                return true;
            }
            if (isQuit(choice)) {
                return false;
            }
            Integer number = asNumber(choice);
            if (number == null) {
                runCommand(line);
                continue;
            }
            if (number < 1 || number > topic.items().size()) {
                out.println("Please choose a number between 1 and " + topic.items().size()
                        + ", type a one-line command, or b to go back.");
                continue;
            }
            MenuItem item = topic.items().get(number - 1);
            List<String> answers = new ArrayList<>();
            out.println("Press Enter to accept the value in brackets.");
            for (Prompt prompt : item.prompts()) {
                String shown = prompt.defaultValue().isEmpty() ? "(optional)" : "[" + prompt.defaultValue() + "]";
                String answer = read(prompt.question() + " " + shown + ": ");
                if (answer == null) {
                    return false;
                }
                answers.add(answer.isBlank() ? prompt.defaultValue() : answer.trim());
            }
            String command = item.command().apply(answers);
            out.println("Command: " + command);
            runCommand(command);
        }
    }

    private void runCommand(String command) {
        try {
            out.println();
            out.print(Commands.execute(command).render());
        } catch (StackOverflowError tooDeep) {
            out.println("Error: the input is too deeply nested");
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            out.println("Error: " + (message == null || message.isBlank() ? failure.getClass().getSimpleName()
                    : message));
        }
    }

    private String read(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            return in.readLine();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static boolean isQuit(String text) {
        String choice = text.trim().toLowerCase(Locale.ROOT);
        return choice.equals("q") || choice.equals("quit") || choice.equals("exit");
    }

    private static Integer asNumber(String text) {
        if (!text.matches("\\d{1,3}")) {
            return null;
        }
        return Integer.parseInt(text);
    }
}
