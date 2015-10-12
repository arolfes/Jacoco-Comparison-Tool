package edu.cmu.jacoco.cli;

import static edu.cmu.jacoco.utils.CoverageDiffUtils.getSplittedString;
import static edu.cmu.jacoco.utils.CoverageDiffUtils.wrapTitles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jacoco.core.analysis.IBundleCoverage;

import edu.cmu.jacoco.CoverageDiff;

public class JacocoComparisonTool {

    private static CommandLine line;

    private JacocoComparisonTool() {
	// private default constructor to use only static methods
    }

    public static void main(final String[] args) throws IOException {
	if (!extractArguments(args))
	    return;

	final String[] execDataFiles = getOptionValues("exec");
	final File reportsDir = new File(getOptionValue("report"));
	final CoverageDiff s = new CoverageDiff(new File(getOptionValue("sourceFileDir")), new File(getOptionValue("classFileDir")), reportsDir,
		execDataFiles.length);

	final List<IBundleCoverage> bcl = s.loadAndAnalyze(execDataFiles);

        final Path destMergedFile = s.mergeExecDataFiles(execDataFiles);

	final IBundleCoverage bundleCoverage = s.loadAndAnalyze(destMergedFile.toFile());

	bcl.add(bundleCoverage);

	s.calculateBranchCoverage(bcl);

	s.initWriter();

	final String[] testSuiteTitles = wrapTitles(getOptionValues("title"), execDataFiles.length);
	s.renderBranchCoverage(testSuiteTitles, getOptionValues("package"));

	s.generateClassCoverageReport(bcl);
    }

    @SuppressWarnings("static-access")
    private static boolean extractArguments(final String[] args) {
	final CommandLineParser parser = new BasicParser();

	final Options options = new Options();
	boolean valid = true;

	options.addOption(OptionBuilder.withLongOpt("sourceFileDir")
		.withDescription("The directory containing the source files").hasArg().create());
	options.addOption(OptionBuilder.withLongOpt("classFileDir")
		.withDescription("The directory containing the class files").hasArg().create());
	options.addOption(OptionBuilder.withLongOpt("report")
		.withDescription("The directory that the generated REPORTs will be written to").hasArg().create());
	options.addOption(OptionBuilder.withLongOpt("package")
		.withDescription("The packages that the reports will be genrated for").hasArg().create());

	options.addOption(OptionBuilder.withLongOpt("exec").withDescription("The name of the Jacoco execution files")
		.hasArg().create());

	options.addOption(OptionBuilder.withLongOpt("title")
		.withDescription("The title of the test suites in the coverage report").hasArg().create());

	try {
	    // parse the command line arguments
	    line = parser.parse(options, args);

	    if (!line.hasOption("sourceFileDir")) {
		System.out.println("You need to specify the sources directory");
		valid = false;
	    }

	    if (!line.hasOption("classFileDir")) {
		System.out.println("You need to specify the classes directory");
		valid = false;
	    }

	    if (!line.hasOption("report")) {
		System.out.println("You need to specify the report directory");
		valid = false;
	    }

	    if (!line.hasOption("exec")) {
		System.out.println("You need to specify the name of the exec files.");
		valid = false;
	    }

	} catch (ParseException exp) {
	    System.out.println("Unexpected exception:" + exp.getMessage());
	    valid = false;
	}

	return valid;
    }

    private static String getOptionValue(final String option) {
	if (line.hasOption(option)) {
	    return line.getOptionValue(option);
	} else {
	    return new String();
	}
    }

    private static String[] getOptionValues(final String option) {
	if (line.hasOption(option)) {
	    return getSplittedString(line.getOptionValue(option));
	} else {
	    return new String[0];
	}
    }

}
