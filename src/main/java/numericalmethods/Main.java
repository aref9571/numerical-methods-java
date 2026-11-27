package numericalmethods;

import numericalmethods.cli.App;
import numericalmethods.cli.Commands;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            new App(System.in, System.out).run();
            return;
        }
        String command = String.join(" ", args).trim();
        if (command.equals("help") || command.equals("--help")) {
            System.out.println(Commands.help());
            return;
        }
        try {
            System.out.print(Commands.execute(command).render());
        } catch (RuntimeException failure) {
            System.out.println("Error: " + failure.getMessage());
            System.exit(1);
        }
    }
}
