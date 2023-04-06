package config;

import lombok.AllArgsConstructor;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;

import java.security.KeyPair;

@AllArgsConstructor
public class CustomTransportConfigCallback implements TransportConfigCallback {
    private final KeyPair keyPair;

    @Override
    public void configure(Transport transport) {
        if (transport instanceof SshTransport) {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(new CustomSshdSessionFactory(keyPair));
        }
    }
}
