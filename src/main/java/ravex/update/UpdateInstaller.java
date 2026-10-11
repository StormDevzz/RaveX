package ravex.update;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Swaps the mod jar after the game has closed - a loaded jar cannot be overwritten in place.
 * Uses only {@code java.*} so it can run straight from the downloaded jar.
 * Args: sourceJar targetJar relaunch gamePid joinedArgs
 */
public final class UpdateInstaller {
    private static final char ARG_SEPARATOR = '\u001F';
    private static final int PROCESS_WAIT_TIMEOUT_MS = 10 * 60 * 1000;
    private static final int FILE_LOCK_RETRIES = 60;
    private static final long FILE_LOCK_RETRY_DELAY_MS = 1000L;

    private UpdateInstaller() {}

    public static void launch(String sourceJar, String targetJar, boolean relaunch) {
        launch(sourceJar, targetJar, relaunch, currentPid(), currentArguments());
    }

    private static long currentPid() {
        try {
            return ProcessHandle.current().pid();
        } catch (Throwable t) {
            return -1L;
        }
    }

    private static String currentArguments() {
        try {
            return String.join(String.valueOf(ARG_SEPARATOR),
                    ProcessHandle.current().info().arguments().orElse(new String[0]));
        } catch (Throwable t) {
            return "";
        }
    }

    public static void launch(String sourceJar, String targetJar, boolean relaunch, long gamePid, String argsJoined) {
        try {
            String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            if (!new File(javaExe).isFile()) javaExe = "java";

            List<String> command = new ArrayList<>();
            command.add(javaExe);
            command.add("-cp");
            command.add(sourceJar);
            command.add(UpdateInstaller.class.getName());
            command.add(sourceJar);
            command.add(targetJar);
            command.add(relaunch ? "1" : "0");
            command.add(Long.toString(gamePid));
            command.add(argsJoined == null ? "" : argsJoined);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Path log = new File(System.getProperty("java.io.tmpdir"), "ravex-update.log").toPath();
            builder.redirectOutput(ProcessBuilder.Redirect.to(log.toFile()));
            builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            builder.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start the installer: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        StringBuilder log = new StringBuilder();
        log.append("RaveX updater ").append(new java.util.Date()).append(System.lineSeparator());
        try {
            if (args.length < 5) {
                log.append("ERROR: expected 5 arguments, got ").append(args.length).append(System.lineSeparator());
                writeLog(log);
                return;
            }
            String sourceJar = args[0];
            String targetJar = args[1];
            boolean relaunch = "1".equals(args[2]);
            long gamePid;
            try {
                gamePid = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                gamePid = -1L;
            }
            String joinedArgs = args[4];

            log.append("source = ").append(sourceJar).append(System.lineSeparator());
            log.append("target = ").append(targetJar).append(System.lineSeparator());
            log.append("relaunch = ").append(relaunch).append(System.lineSeparator());
            log.append("gamePid = ").append(gamePid).append(System.lineSeparator());

            File source = new File(sourceJar);
            File target = new File(targetJar);
            if (!source.isFile()) {
                log.append("ERROR: update jar is missing").append(System.lineSeparator());
                writeLog(log);
                return;
            }

            waitForGameExit(gamePid, log);
            if (!waitUntilUnlocked(target, log)) {
                log.append("ERROR: target jar is still locked").append(System.lineSeparator());
                writeLog(log);
                return;
            }

            File backup = backup(target, log);
            if (backup == null) {
                log.append("ERROR: could not create a backup, aborting").append(System.lineSeparator());
                writeLog(log);
                return;
            }

            copy(source, target, log);
            log.append("Installed OK").append(System.lineSeparator());

            if (relaunch) {
                relaunchGame(joinedArgs, log);
            } else {
                log.append("Relaunch skipped").append(System.lineSeparator());
            }
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            log.append("FATAL: ").append(sw).append(System.lineSeparator());
        } finally {
            writeLog(log);
        }
    }

    private static void waitForGameExit(long gamePid, StringBuilder log) {
        if (gamePid <= 0L) return;
        long deadline = System.currentTimeMillis() + PROCESS_WAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            boolean alive;
            try {
                alive = ProcessHandle.of(gamePid).map(ProcessHandle::isAlive).orElse(false);
            } catch (Throwable t) {
                alive = false;
            }
            if (!alive) {
                log.append("Game process exited").append(System.lineSeparator());
                return;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.append("Warning: timed out waiting for the game to exit").append(System.lineSeparator());
    }

    private static boolean waitUntilUnlocked(File target, StringBuilder log) {
        for (int attempt = 0; attempt < FILE_LOCK_RETRIES; attempt++) {
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(target, "rw")) {
                raf.setLength(raf.length());
                log.append("Target is writable").append(System.lineSeparator());
                return true;
            } catch (IOException e) {
                try {
                    Thread.sleep(FILE_LOCK_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private static File backup(File target, StringBuilder log) {
        try {
            File dir = new File(target.getParentFile(), "RaveX/update/backup");
            if (!dir.exists() && !dir.mkdirs()) return null;
            String name = target.getName().replaceAll("\\.jar$", "") + "-" + target.lastModified() + ".jar.bak";
            File backup = new File(dir, name);
            Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.append("Backup = ").append(backup.getAbsolutePath()).append(System.lineSeparator());
            return backup;
        } catch (Exception e) {
            log.append("Backup failed: ").append(e.getMessage()).append(System.lineSeparator());
            return null;
        }
    }

    private static void copy(File source, File target, StringBuilder log) throws IOException {
        Path temp = target.toPath().resolveSibling(target.getName() + ".new");
        Files.deleteIfExists(temp);
        Files.copy(source.toPath(), temp, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(temp, target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailed) {
            log.append("Atomic move unavailable, falling back").append(System.lineSeparator());
            Files.move(temp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        log.append("Wrote ").append(target.length()).append(" bytes").append(System.lineSeparator());
    }

    private static void relaunchGame(String joinedArgs, StringBuilder log) {
        try {
            List<String> command = new ArrayList<>();
            command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
            if (joinedArgs != null && !joinedArgs.isEmpty()) {
                for (String part : joinedArgs.split(String.valueOf(ARG_SEPARATOR))) {
                    if (!part.isEmpty()) command.add(part);
                }
            }
            if (command.size() < 2) {
                log.append("Relaunch skipped: no arguments captured").append(System.lineSeparator());
                return;
            }
            new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();
            log.append("Relaunched the game").append(System.lineSeparator());
        } catch (Exception e) {
            log.append("Relaunch failed: ").append(e.getMessage()).append(System.lineSeparator());
        }
    }

    private static void writeLog(StringBuilder log) {
        try {
            File out = new File(System.getProperty("java.io.tmpdir"), "ravex-update.log");
            Files.writeString(out.toPath(), log.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}