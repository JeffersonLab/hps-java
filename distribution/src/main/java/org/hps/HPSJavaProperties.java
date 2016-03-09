package org.hps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class provides information about the HPS Java build environment for the current distribution jar that is being
 * executed.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class HPSJavaProperties {

    /**
     * Properties file containing properties with values inserted from Maven resource filtering.
     */
    Properties properties = new Properties();

    /**
     * Class constructor.
     *
     * @throws IOException if there is problem loading the properties
     */
    public HPSJavaProperties() throws IOException {
        final InputStream is = getClass().getResourceAsStream("/org/hps/hps-java.properties");
        this.properties.load(is);
        is.close();
    }

    /**
     * Get the Java home from the build environment.
     *
     * @return the Java home directory
     */
    public String getJavaVersion() {
        return this.properties.getProperty("java.version");
    }

    /**
     * Get the LCSim version string.
     *
     * @return the LCSim version string
     */
    public String getLCSimVersion() {
        return this.properties.getProperty("lcsimVersion");
    }

    /**
     * Get the project name.
     *
     * @return the project name
     */
    public String getProjectArtifactId() {
        return this.properties.getProperty("project.artifactId");
    }

    /**
     * Get the local build directory.
     *
     * @return the local build directory
     */
    public String getProjectBuildDirectory() {
        return this.properties.getProperty("project.build.directory");
    }

    /**
     * Get the project version.
     *
     * @return the project version
     */
    public String getProjectVersion() {
        return this.properties.getProperty("project.version");
    }

    /**
     * Get the Maven build timestamp.
     *
     * @return the Maven build timestamp
     */
    public String getTimestamp() {
        return this.properties.getProperty("timestamp");
    }

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("HPS Java properties" + '\n');
        sb.append("project.artifactId: " + getProjectArtifactId() + '\n');
        sb.append("project.version: " + getProjectVersion() + '\n');
        sb.append("lcsimVersion: " + getLCSimVersion() + '\n');
        sb.append("java.version: " + getJavaVersion() + '\n');
        sb.append("project.build.directory: " + getProjectBuildDirectory() + '\n');
        sb.append("timestamp: " + getTimestamp() + '\n');
        return sb.toString();
    }
}
