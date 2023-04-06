package config;

import lombok.AllArgsConstructor;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class CustomSshdSessionFactory extends SshdSessionFactory {
    private final KeyPair keyPair;

    @Override
    public File getHomeDirectory() {
        return null;
    }

    @Override
    public File getSshDirectory() {
        return null;
    }

    @Override
    protected File getSshConfig(File sshDir) {
        return null;
    }

    @Override
    protected ServerKeyDatabase getServerKeyDatabase(File homeDir, File sshDir) {
        return new ServerKeyDatabase() {
            @Override
            public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress, Configuration config) {
                return Collections.emptyList();
            }

            @Override
            public boolean accept(String connectAddress, InetSocketAddress remoteAddress, PublicKey serverKey, Configuration config, CredentialsProvider provider) {
                return true;
            }
        };
    }

    @Override
    protected List<Path> getDefaultKnownHostsFiles(File sshDir) {
        return Collections.emptyList();
    }

    @Override
    protected Iterable<KeyPair> getDefaultKeys(File sshDir) {
        return List.of(keyPair);
    }

    @Override
    protected List<Path> getDefaultIdentities(File sshDir) {
        return Collections.emptyList();
    }

    @Override
    protected String getDefaultPreferredAuthentications() {
        return "publickey";
    }
}
