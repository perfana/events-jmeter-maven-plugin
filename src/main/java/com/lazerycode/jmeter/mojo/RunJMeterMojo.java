package com.lazerycode.jmeter.mojo;

import com.lazerycode.jmeter.json.TestConfigurationWrapper;
import com.lazerycode.jmeter.testrunner.TestManager;
import nl.stokpop.eventscheduler.EventScheduler;
import nl.stokpop.eventscheduler.EventSchedulerBuilder;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.config.EventSchedulerConfig;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

/**
 * Goal that runs jmeter based on configuration defined in your pom.<br>
 * This goal runs within Lifecycle phase {@link LifecyclePhase#INTEGRATION_TEST}.
 */
@Mojo(name = "jmeter", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class RunJMeterMojo extends AbstractJMeterMojo {

    /**
     * Run all the JMeter tests.
     *
     * @throws MojoExecutionException MojoExecutionException
     */
    @Override
    public void doExecute() throws MojoExecutionException {
        getLog().info(" ");
        getLog().info(LINE_SEPARATOR);
        getLog().info(" P E R F O R M A N C E    T E S T S");
        getLog().info(LINE_SEPARATOR);
        getLog().info(" ");

        if (!testFilesDirectory.exists()) {
            getLog().info("<testFilesDirectory>" + testFilesDirectory.getAbsolutePath() + "</testFilesDirectory> does not exist...");
            getLog().info("Performance tests skipped!");
            getLog().info(" ");
            return;
        }

        testConfig = new TestConfigurationWrapper(new File(testConfigFile), selectedConfiguration);
        remoteConfig.setPropertiesMap(testConfig.getCurrentTestConfiguration().getPropertiesMap());
        jMeterProcessJVMSettings.setHeadlessDefaultIfRequired();
        copyFilesInTestDirectory(testFilesDirectory, testFilesBuildDirectory);

        EventScheduler eventScheduler = (eventSchedulerConfig != null && eventSchedulerConfig.isSchedulerEnabled())
            ? createEventScheduler(eventSchedulerConfig, getLog())
            : null;

        TestManager jMeterTestManager = new TestManager()
                .setBaseTestArgs(computeJMeterArgumentsArray(true, testConfig.getCurrentTestConfiguration().getResultsOutputIsCSVFormat(), testConfig.getCurrentTestConfiguration().getJmeterDirectoryPath()))
                .setTestFilesDirectory(testFilesBuildDirectory)
                .setTestFilesIncluded(testFilesIncluded)
                .setTestFilesExcluded(testFilesExcluded)
                .setRemoteServerConfiguration(remoteConfig)
                .setSuppressJMeterOutput(suppressJMeterOutput)
                .setBinDir(testConfig.getCurrentTestConfiguration().getJmeterWorkingDirectoryPath())
                .setJMeterProcessJVMSettings(jMeterProcessJVMSettings)
                .setRuntimeJarName(testConfig.getCurrentTestConfiguration().getRuntimeJarName())
                .setReportDirectory(reportDirectory)
                .setGenerateReports(generateReports)
                .setPostTestPauseInSeconds(postTestPauseInSeconds)
                .setIgnoreJVMKilled(doNotFailBuildIfJVMIsKilled)
                .setEventScheduler(eventScheduler);
        if (proxyConfig != null) {
            getLog().info(this.proxyConfig.toString());
        }

        testConfig.getCurrentTestConfiguration().setResultFilesLocations(jMeterTestManager.executeTests());
        testConfig.writeResultFilesConfigTo(testConfigFile);
    }

    private static EventScheduler createEventScheduler(EventSchedulerConfig eventSchedulerConfig, Log log) {

        EventLogger logger = new EventLogger() {
            @Override
            public void info(String message) {
                log.info(message);
            }

            @Override
            public void warn(String message) {
                log.warn(message);
            }

            @Override
            public void error(String message) {
                log.error(message);
            }

            @Override
            public void error(String message, Throwable throwable) {
                log.error(message, throwable);
            }

            @Override
            public void debug(final String message) {
                if (isDebugEnabled()) log.debug(message);
            }

            @Override
            public boolean isDebugEnabled() {
                return eventSchedulerConfig.isDebugEnabled();
            }

        };

        return EventSchedulerBuilder.of(eventSchedulerConfig, logger);

    }

}
