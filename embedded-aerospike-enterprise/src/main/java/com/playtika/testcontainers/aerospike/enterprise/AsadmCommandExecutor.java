package com.playtika.testcontainers.aerospike.enterprise;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class AsadmCommandExecutor {

    private final GenericContainer<?> aerospikeContainer;

    public void execute(String command) throws IOException, InterruptedException {
        Container.ExecResult result = aerospikeContainer.execInContainer("asadm", "--enable", "-e", command);
        logStdout(result);
        if (result.getExitCode() != 0 || isBadResponse(result)) {
            throw new IllegalStateException(String.format("Failed to execute  \"asadm --enable -e '%s'\":\nstdout:\n%s\nstderr:\n%s",
                    command, result.getStdout(), result.getStderr()));
        }
    }

    private boolean isBadResponse(Container.ExecResult execResult) {
        String stdout = execResult.getStdout();
        /*
        Example of the stdout without error:
        ~Set Namespace Param stop-writes-sys-memory-pct to 100~
                     Node|Response
        728bb242e58c:3000|ok
        Number of rows: 1
        */
        return !stdout.contains("|ok");
    }

    private static void logStdout(Container.ExecResult result) {
        log.debug("Aerospike asadm util stdout: \n{}\n{}", result.getStdout(), result.getStderr());
    }
}
