package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.VfsException;

/**
 * Changes the current working directory.
 * Supports: cd [path], cd .., cd ~, cd -
 *
 * <p><b>Owner: B</b></p>
 */
public class CdCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length > 1) {
            return CommandResult.error("cd: usage: cd [path]");
        }

        String currentDir = session.getWorkingDir();
        String target = args.length == 0 ? "~" : args[0];

        if ("-".equals(target)) {
            String previousDir = session.getPreviousDir();
            if (previousDir == null) {
                return CommandResult.error("cd: OLDPWD not set");
            }
            session.setWorkingDir(previousDir);
            session.setPreviousDir(currentDir);
            return CommandResult.success(previousDir);
        }

        if ("~".equals(target)) {
            target = "/home/user";
        }

        try {
            FileNode node = session.getVfs().resolve(target, currentDir);
            if (!node.isDirectory()) {
                return CommandResult.error("cd: not a directory: " + target);
            }
            if (!node.getPermission().canOwnerExecute()) {
                return CommandResult.error("cd: permission denied: " + target);
            }
            String absolutePath = session.getVfs().getAbsolutePath(target, currentDir);
            session.setPreviousDir(currentDir);
            session.setWorkingDir(absolutePath);
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("cd: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "cd [path]";
    }

    @Override
    public String getDescription() {
        return "Change working directory";
    }
}
