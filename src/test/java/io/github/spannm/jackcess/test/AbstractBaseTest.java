package io.github.spannm.jackcess.test;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.DatabaseBuilder;
import io.github.spannm.jackcess.impl.ByteUtil;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.util.MemFileChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.provider.Arguments;

import java.io.*;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Base class for JUnit test cases.<p>
 *
 * Logs entry to all test methods.<br>
 * This class extends JUnit assertions to avoid the need for static imports in subclasses.
 *
 * @author Markus Spann
 */
public abstract class AbstractBaseTest extends Assertions {

    public static final File                  DIR_TEST_DATA = new File("src/test/resources", "data");

    public static final TimeZone              TEST_TZ       = TimeZone.getTimeZone("America/New_York");

    private static final ThreadLocal<Boolean> AUTO_SYNC     = new ThreadLocal<>();

    /** The java platform/system logger. */
    private Logger                            logger;

    /** Holds information about the current test. */
    private TestInfo                          lastTestInfo;

    protected final Logger getLogger() {
        if (null == logger) {
            logger = System.getLogger(getClass().getName());
        }
        return logger;
    }

    protected static final Logger getStaticLogger() {
        return System.getLogger(AbstractBaseTest.class.getName());
    }

    @BeforeEach
    public final void setTestMethodName(TestInfo _testInfo) {
        lastTestInfo = _testInfo;
    }

    protected final String getTestMethodName() {
        if (lastTestInfo != null && lastTestInfo.getTestClass().isPresent()) {
            return lastTestInfo.getTestClass().get().getName() + '.' + lastTestInfo.getTestMethod().get().getName();
        }
        return null;
    }

    protected final String getShortTestMethodName() {
        Optional<Method> testMethod = lastTestInfo == null ? Optional.empty() : lastTestInfo.getTestMethod();
        return testMethod.map(Method::getName).orElse(null);
    }

    @BeforeEach
    public final void logTestBegin(TestInfo _testInfo) {
        if (_testInfo.getTestMethod().isEmpty() || _testInfo.getDisplayName().startsWith(_testInfo.getTestMethod().get().getName())) {
            getLogger().log(Level.INFO, ">>>> TEST: {0} <<<<", _testInfo.getDisplayName());
        } else {
            getLogger().log(Level.INFO, ">>>> TEST: {0} ({1}) <<<<",
                _testInfo.getTestMethod().get().getName(), _testInfo.getDisplayName());
        }
    }

    public Database createDbMem(FileFormat fileFormat) throws IOException {
        return createDb(fileFormat, false, true);
    }

    public Database createDbMem(FileFormat fileFormat, boolean keep) throws IOException {
        return createDb(fileFormat, keep, true);
    }

    public Database createDb(FileFormat fileFormat, boolean keep, boolean inMem) throws IOException {

        FileChannel channel = inMem && !keep ? MemFileChannel.newChannel() : null;

        File tempFile = TestUtil.createTempFile(getShortTestMethodName(), Database.FILE_EXT_MDB, keep);

        if (fileFormat == FileFormat.GENERIC_JET4) {
            // while we don't support creating GENERIC_JET4 as a jackcess feature,
            // we do want to be able to test these types of dbs
            try (InputStream inStream = TestUtil.class.getClassLoader().getResourceAsStream("emptyJet4.mdb")) {
                if (channel != null) {
                    DatabaseImpl.transferDatabase(inStream, channel);
                } else {
                    try (OutputStream outStream = new FileOutputStream(tempFile)) {
                        ByteUtil.copy(inStream, outStream);
                    }
                }
                return new DatabaseBuilder()
                    .withFile(tempFile)
                    .withAutoSync(getTestAutoSync())
                    .withChannel(channel)
                    .open();
            }
        } else {
            return new DatabaseBuilder()
                .withFile(tempFile)
                .withFileFormat(fileFormat)
                .withAutoSync(getTestAutoSync())
                .withChannel(channel).create();
        }

    }

    public static boolean getTestAutoSync() {
        Boolean autoSync = AUTO_SYNC.get();
        return autoSync != null ? autoSync : Database.DEFAULT_AUTO_SYNC;
    }

    public static void clearTestAutoSync() {
        AUTO_SYNC.remove();
    }

    public static void setTestAutoSync(boolean autoSync) {
        AUTO_SYNC.set(autoSync);
    }

    public static <T> List<T> toList(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

    /**
     * Shortcut to create an {@link Arguments} instance.
     */
    public static Arguments args(Object... _arguments) {
        return Arguments.of(_arguments);
    }

}
