package com.playtika.testcontainers.aerospike.enterprise;

import com.playtika.testcontainer.aerospike.AerospikeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AsadmCommandExecutorTest extends BaseEnterpriseAerospikeTest {

    @Autowired
    @Qualifier(AerospikeProperties.BEAN_NAME_AEROSPIKE)
    private GenericContainer<?> aerospikeContainer;

    @Test
    public void shouldFailCommandExecutionWithBadOption() {
        AsadmCommandExecutor commandExecutor = new AsadmCommandExecutor(aerospikeContainer);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> commandExecutor.execute("-tttttt"));
    }

    @Test
    public void shouldFailCommandExecutionWithBadCommand() {
        AsadmCommandExecutor commandExecutor = new AsadmCommandExecutor(aerospikeContainer);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> commandExecutor.execute("manage config namespace NAMESPACE_NOT_EXISTS param disallow-expunge to true"));
    }
}
