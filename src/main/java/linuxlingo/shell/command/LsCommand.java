package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.RegularFile;
import linuxlingo.shell.vfs.VfsException;

/**
 * Lists directory contents.
 * Supports: ls [-l] [-a] [path]
 *
 * <p><b>Owner: B</b></p>
 */
public class LsCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean longFormat = false;
        boolean showHidden = false;
        String targetPath = session.getWorkingDir();
        boolean hasExplicitPath = false;

        for (String arg : args) {
            if (arg.startsWith("-") && arg.length() > 1) {
                for (int i = 1; i < arg.length(); i++) {
                    char option = arg.charAt(i);
                    if (option == 'l') {
                        longFormat = true;
                    } else if (option == 'a') {
                        showHidden = true;
                    } else {
                        return CommandResult.error("ls: invalid option -- " + option);
                    }
                }
            } else if (!hasExplicitPath) {
                targetPath = arg;
                hasExplicitPath = true;
            } else {
                return CommandResult.error("ls: too many arguments");
            }
        }

        try {
            List<FileNode> children = session.getVfs().listDirectory(targetPath, session.getWorkingDir(), showHidden);
            List<String> lines = new ArrayList<>();
            for (FileNode child : children) {
                String name = child.getName() + (child.isDirectory() ? "/" : "");
                if (!longFormat) {
                    lines.add(name);
                    continue;
                }
                int size = child.isDirectory() ? 0 : ((RegularFile) child).getSize();
                lines.add(child.getPermission().toString() + "  " + size + "  " + name);
            }
            return CommandResult.success(String.join("\n", lines));
        } catch (VfsException e) {
            return CommandResult.error("ls: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "ls [-l] [-a] [path]";
    }

    @Override
    public String getDescription() {
        return "List directory contents";
    }
}
