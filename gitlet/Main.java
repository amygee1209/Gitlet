package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Amy Kwon
 */
public class Main implements Serializable {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> ....
     *  Referenced lab12. */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        Command command = read();
        if (command == null && !args[0].equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (args[0].equals("init")) {
            if (command == null) {
                command = new Command();
            } else {
                System.out.println("A Gitlet version-control system"
                        + " already exists in the current directory.");
                System.exit(0);
            }
        } else if (args[0].equals("add")) {
            command.add(args[1]);
        } else if (args[0].equals("commit")) {
            command.commit(args[1]);
        } else if (args[0].equals("rm")) {
            command.remove(args[1]);
        } else if (args[0].equals("log")) {
            command.log();
        } else if (args[0].equals("global-log")) {
            command.globalLog();
        } else if (args[0].equals("find")) {
            command.find(args[1]);
        } else if (args[0].equals("status")) {
            command.status();
        } else if (args[0].equals("checkout")) {
            command.checkout(args);
        } else if (args[0].equals("branch")) {
            command.branch(args[1]);
        } else if (args[0].equals("rm-branch")) {
            command.removeBranch(args[1]);
        } else if (args[0].equals("reset")) {
            command.reset(args[1]);
        } else if (args[0].equals("merge")) {
            command.merge(args[1]);
        } else if (args[0].equals("add-remote")) {
            command.addRemote(args);
        } else if (args[0].equals("rm-remote")) {
            command.rmRemote(args[1]);
        } else if (args[0].equals("push")) {
            command.push(args[1], args[2]);
        } else if (args[0].equals("fetch")) {
            command.fetch(args[1], args[2]);
        } else if (args[0].equals("pull")) {
            command.pull(args[1], args[2]);
        } else {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
        save(command);
    }

    /** Returns the Command after reading the data in .gitlet. */
    private static Command read() {
        Command command = null;
        File read = new File(".gitlet", "data");
        if (read.exists()) {
            command = Utils.readObject(read, Command.class);
        }
        return command;
    }

    /** Save all the data of COMMAND class in .gitlet. */
    private static void save(Command command) {
        File save = new File(".gitlet", "data");
        Utils.writeObject(save, command);
    }

}
