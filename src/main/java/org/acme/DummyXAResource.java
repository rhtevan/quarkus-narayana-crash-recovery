package org.acme;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.arjuna.ats.arjuna.common.Uid;
import org.jboss.logging.Logger;

/**
 * A fake XAResource which can crash by a triggering message.
 */
public class DummyXAResource implements XAResource {
    private Logger LOG = Logger.getLogger(DummyXAResource.class);

    public static final String LOG_DIR = "target/DummyXAResource/";

    private final boolean shouldCrash;

    private Xid xid;

    private File file;

    public DummyXAResource(boolean shouldCrash) {
        this.shouldCrash = shouldCrash;
    }

    /**
     * Constructor used by recovery manager to recreate XAResource
     *
     * @param file File where Xid of the XAResource is stored
     */
    public DummyXAResource(File file) throws IOException {
        this.shouldCrash = false;
        this.file = file;
        this.xid = getXidFromFile(file);
    }

    public int prepare(final Xid xid) throws XAException {
        LOG.info("Preparing " + DummyXAResource.class.getSimpleName());

        this.file = writeXidToFile(xid, LOG_DIR);

        return XA_OK;
    }

    public void commit(final Xid xid, final boolean arg1) throws XAException {
        LOG.info("Committing " + DummyXAResource.class.getSimpleName());

        if (shouldCrash) {
            LOG.info("Crashing the system");
            Runtime.getRuntime().halt(1);
        }

        removeFile(file);
        this.file = null;
        this.xid = null;
    }

    public void rollback(final Xid xid) throws XAException {
        LOG.info("Rolling back " + DummyXAResource.class.getSimpleName());

        removeFile(file);
        this.file = null;
        this.xid = null;
    }

    public boolean isSameRM(XAResource xaResource) throws XAException {
        if (!(xaResource instanceof DummyXAResource)) {
            return false;
        }

        DummyXAResource other = (DummyXAResource) xaResource;

        return xid != null && other.xid != null && xid.getFormatId() == other.xid.getFormatId()
                && Arrays.equals(xid.getGlobalTransactionId(), other.xid.getGlobalTransactionId())
                && Arrays.equals(xid.getBranchQualifier(), other.xid.getBranchQualifier());
    }

    public Xid[] recover(int flag) throws XAException {
        return new Xid[]{ xid };
    }

    public void start(Xid xid, int flags) throws XAException {

    }

    public void end(Xid xid, int flags) throws XAException {

    }

    public void forget(Xid xid) throws XAException {

    }

    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    public boolean setTransactionTimeout(final int seconds) throws XAException {
        return true;
    }

    private Xid getXidFromFile(File file) throws IOException {
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(file))) {
            int formatId = inputStream.readInt();
            int globalTransactionIdLength = inputStream.readInt();
            byte[] globalTransactionId = new byte[globalTransactionIdLength];
            inputStream.read(globalTransactionId, 0, globalTransactionIdLength);
            int branchQualifierLength = inputStream.readInt();
            byte[] branchQualifier = new byte[branchQualifierLength];
            inputStream.read(branchQualifier, 0, branchQualifierLength);

            return new XidImpl(formatId, globalTransactionId, branchQualifier);
        }
    }

    private File writeXidToFile(Xid xid, String directory) throws XAException {
        File dir = new File(directory);

        if (!dir.exists() && !dir.mkdirs()) {
            throw new XAException(XAException.XAER_RMERR);
        }

        File file = new File(dir, new Uid().fileStringForm() + "_");

        try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(file))) {
            outputStream.writeInt(xid.getFormatId());
            outputStream.writeInt(xid.getGlobalTransactionId().length);
            outputStream.write(xid.getGlobalTransactionId(), 0, xid.getGlobalTransactionId().length);
            outputStream.writeInt(xid.getBranchQualifier().length);
            outputStream.write(xid.getBranchQualifier(), 0, xid.getBranchQualifier().length);
            outputStream.flush();
        } catch (IOException e) {
            throw new XAException(XAException.XAER_RMERR);
        }

        return file;
    }

    private void removeFile(File file) throws XAException {
        if (file != null) {
            if (!file.delete()) {
                throw new XAException(XAException.XA_RETRY);
            }
        }
    }

    private class XidImpl implements Xid {

        private final int formatId;

        private final byte[] globalTransactionId;

        private final byte[] branchQualifier;

        public XidImpl(int formatId, byte[] globalTransactionId, byte[] branchQualifier) {
            this.formatId = formatId;
            this.globalTransactionId = globalTransactionId;
            this.branchQualifier = branchQualifier;
        }

        @Override
        public int getFormatId() {
            return formatId;
        }

        @Override
        public byte[] getGlobalTransactionId() {
            return globalTransactionId;
        }

        @Override
        public byte[] getBranchQualifier() {
            return branchQualifier;
        }

    }
}
