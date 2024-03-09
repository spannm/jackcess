package io.github.spannm.jackcess.test;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Base class for JUnit test cases.<p>
 *
 * Logs entry and exit to/from all test methods.<br>
 * This class extends JUnit assertions to avoid the need for static imports in subclasses.
 *
 * @author Markus Spann
 */
public abstract class AbstractBaseTest extends Assertions {

    private static final ThreadLocal<Boolean> AUTO_SYNC = new ThreadLocal<>();

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
            getLogger().log(Level.INFO, ">>>> TEST: {0} <<<<<<<<<<", _testInfo.getDisplayName());
        } else {
            getLogger().log(Level.INFO, ">>>> TEST: {0} ({1}) <<<<<<<<<<",
                _testInfo.getTestMethod().get().getName(), _testInfo.getDisplayName());
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
     * Creates a subdirectory of the system's temp file directory.
     * @param _dir subdirectory name
     * @return temp directory
     * @throws UncheckedIOException If the subdirectory could not be created
     */
    protected static File createTempDir(String _dir) {
        File tempDir = new File(getTempDir());
        if (null != _dir) {
            tempDir = new File(tempDir, _dir);
        }
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new UncheckedIOException(new IOException("Could not create directory " + tempDir));
        }
        return tempDir;
    }

    /**
     * Returns the system's temporary directory i.e. the content of the {@code java.io.tmpdir} system property.<br>
     * Ensures the directory name ends in the system-dependent default name-separator character.
     *
     * @return system's temporary directory name
     */
    protected static String getTempDir() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (!tmpDir.endsWith(File.separator)) {
            tmpDir += File.separatorChar;
        }
        return tmpDir;
    }

    public static final File          DIR_TEST_DATA         = new File("src/test/data");

    /** Charset for access 97 databases. */
    static final Charset              A97_CHARSET           = Charset.forName("windows-1252");

    /**
     * Defines currently supported database file formats that are neither read-only nor {@value FileFormat#MSISAM} (MS Money).
     */
    private static final FileFormat[] SUPPORTED_FILEFORMATS = Arrays.stream(FileFormat.values())
        .filter(ff -> !DatabaseImpl.getFileFormatDetails(ff).getFormat().READ_ONLY && ff != FileFormat.MSISAM)
        .toArray(FileFormat[]::new);

    static final FileFormat[] getSupportedFileformats() {
        return SUPPORTED_FILEFORMATS;
    }

    static final List<TestDB> SUPPORTED_DBS_TEST          = TestDB.getSupportedTestDbs(Basename.TEST);

    public static List<TestDB> getSupportedTestDbs() {
        return SUPPORTED_DBS_TEST;
    }

    static final List<TestDB> SUPPORTED_DBS_TEST_FOR_READ = TestDB.getSupportedTestDbsForRead(Basename.TEST);

    public static List<TestDB> getSupportedReadOnlyTestDbs() {
        return SUPPORTED_DBS_TEST_FOR_READ;
    }

    /**
     * Shortcut to create an {@link Arguments} instance.
     */
    public static Arguments args(Object... _arguments) {
        return Arguments.of(_arguments);
    }

}
