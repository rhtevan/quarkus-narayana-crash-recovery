package org.acme;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.xa.XAResource;

import io.quarkus.runtime.Startup;
import org.jboss.logging.Logger;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * A fake recoverable XAResource
 */
@Singleton
@Startup
public class DummyXAResourceRecovery implements XAResourceRecovery {
    private Logger LOG = Logger.getLogger(DummyXAResourceRecovery.class);

    @Inject
    XAResourceRecoveryRegistry xaResourceRecoveryRegistry;

    @PostConstruct
    void init() {
        LOG.info("register DummyXAResourceRecovery");
        xaResourceRecoveryRegistry.addXAResourceRecovery(this);
    }

    @Override
    public XAResource[] getXAResources() throws RuntimeException {
        List<DummyXAResource> resources;
        try {
            resources = getXAResourcesFromDirectory(DummyXAResource.LOG_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!resources.isEmpty()) {
            LOG.info(DummyXAResourceRecovery.class.getSimpleName() + " returning list of resources: " + resources);
        }

        return resources.toArray(new XAResource[]{});
    }

    private List<DummyXAResource> getXAResourcesFromDirectory(String directory) throws IOException {
        List<DummyXAResource> resources = new ArrayList<>();

        Files.newDirectoryStream(FileSystems.getDefault().getPath(directory), "*_").forEach(path -> {
            try {
                resources.add(new DummyXAResource(path.toFile()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return resources;
    }

}
