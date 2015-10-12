package edu.cmu.jacoco;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.cmu.jacoco.utils.CopyFileVisitor;
import edu.cmu.jacoco.utils.JarCopyFileVisitor;
import edu.cmu.jacoco.utils.PathReference;

public class CoverageDiff {

    private final Logger log = LoggerFactory.getLogger(CoverageDiff.class);

    private static FileSystem jarFileSystem = null;
	private final String title;

	private final File classesDirectory;
	private final File reportDirectory;

	private Path jacocoExecPath;

	private ExecutionDataStore executionDataStore;
	private SessionInfoStore sessionInfoStore;

	private Map<String, Map<String, ArrayList<Coverage>>> packageCoverage;
	private Coverage[] totalCoverage;
	private Writer writer;
	private CodeDirector director;

	private int numberOfTestSuites;

	final static String TOTAL_LABEL = "Total Branch Coverage";

    public CoverageDiff(final File sourceDirectory, final File classesDirectory, final File reportDirectory,
	    final int numberOfExecFiles) throws IOException {
	this.title = sourceDirectory.getName();

	this.classesDirectory = classesDirectory;
	this.reportDirectory = reportDirectory;
	prepareReportDirectory();

	this.packageCoverage = new HashMap<>();
	this.numberOfTestSuites = numberOfExecFiles;
	this.totalCoverage = new Coverage[numberOfTestSuites + 1];

	this.director = new CodeDirectorImpl(sourceDirectory, this.reportDirectory, new HTMLHighlighter());
    }

    // visible for testing
    void prepareReportDirectory() throws IOException {

	if (!reportDirectory.exists()) {
	    Files.createDirectories(Paths.get(reportDirectory.getPath()));
	}
	this.jacocoExecPath = Files
		.createDirectories(Paths.get(this.reportDirectory.getAbsolutePath() + "/jacoco-execs"));

	try {
	    final URI sourceDirURI = this.getClass().getClassLoader().getResource("htmlresources").toURI();
	    final Path targetDir = Paths.get(this.reportDirectory.getAbsolutePath() + "/.resources");
	    if (sourceDirURI.getScheme().equals("jar")) {
		if (jarFileSystem == null) {
		    final Map<String, ?> env = Collections.emptyMap();
		    jarFileSystem = FileSystems.newFileSystem(sourceDirURI, env);
		}

		@SuppressWarnings("resource")
		final PathReference pathReference = new PathReference(jarFileSystem.provider().getPath(sourceDirURI), jarFileSystem);
		final Path jarPath = pathReference.getPath();

		Files.walkFileTree(jarPath, new JarCopyFileVisitor(jarPath, targetDir));
	    } else {
		final Path sourceDir = Paths.get(sourceDirURI);
		Files.walkFileTree(sourceDir, new CopyFileVisitor(sourceDir, targetDir));
	    }
	} catch (IOException e) {
	    log.error("can't copy html resources to reportDirectory. Code Highlight will not work.",e);
	} catch (URISyntaxException e) {
	    log.error("can't copy html resources to reportDirectory. Code Highlight will not work.",e);
	}
    }

	public void initWriter() throws IOException {
		this.writer = new HTMLWriter(this.reportDirectory + "/index.html");
	}

	public void generateClassCoverageReport(List<IBundleCoverage> bcl) {
	    this.director.generateClassCoverageReport(bcl);
	}


	public void renderBranchCoverage(String[] testSuiteTitles, String[] packages) {

		// Render the total coverage
		String packageName;
		boolean all = packages.length == 0;
		writer.renderTotalCoverage(totalCoverage, testSuiteTitles);

		for (Map.Entry<String, Map<String, ArrayList<Coverage>>> entry : packageCoverage.entrySet()) {
		    // Render the package level coverage by passing the package name, and the list of its classes
			packageName = entry.getKey().replaceAll("/", ".");

			if (all || Arrays.asList(packages).contains(packageName))
				renderPackageBranchCoverage(packageName, entry.getValue(), testSuiteTitles);
		}

		writer.renderReportEnd();
	}

	@SuppressWarnings("serial")
	private void renderPackageBranchCoverage(String packageName, Map<String, ArrayList<Coverage>> classes, String[] testSuiteTitles) {

		String className;
		writer.renderPackageHeader(packageName, testSuiteTitles);

		for (Map.Entry<String, ArrayList<Coverage>> entry : classes.entrySet()) {
			className = entry.getKey().replaceAll("/", ".");
			// Do not display the class coverage if the class name is "Total Coverage"
			if (!className.equals(TOTAL_LABEL)) {
				renderClassBranchCoverage(packageName, className, entry.getValue(), new HashMap<String, String>() {{put("bgcolor", "F7E4E4");}});
			}
		}

		// Render the package total branch coverage
		renderClassBranchCoverage("", TOTAL_LABEL, classes.get(TOTAL_LABEL), new HashMap<String, String>() {{put("bgcolor", "C3FAF9");}});

		writer.renderPackageFooter();
	}


	private void renderClassBranchCoverage(String packageName, String className, ArrayList<Coverage> coverageList, HashMap<String, String> options) {

		boolean different = isDifferent(className, coverageList);
		writer.renderClassHeader(packageName, className, different);

		for (Coverage c : coverageList) {
			writer.renderTestSuitCoverage(c, options);
		}

		writer.renderClassFooter();
	}

	public void calculateBranchCoverage(List<IBundleCoverage> bcl) {

		Map<String, ArrayList<Coverage>> classCoverage;

		// Calculate the total branch coverage for each package and its classes
		for (IBundleCoverage bc: bcl) {
		        log.info("calculate branch coverage {}", bc.getName());
			for (IPackageCoverage p : bc.getPackages()) {
				if (packageCoverage.get(p.getName()) != null) {
					classCoverage = packageCoverage.get(p.getName());
				}
				else {
					classCoverage = new HashMap<>();
				}
				calculateBranchCoverage(p.getClasses(), classCoverage);
				packageCoverage.put(p.getName(), classCoverage);
			 }

		}

		// Calculate the total branch coverage for each test suite

		for (Map.Entry<String, Map<String, ArrayList<Coverage>>> entry : packageCoverage.entrySet()) {
			classCoverage = entry.getValue();
			int counter = 0;

			for (Coverage c : classCoverage.get(TOTAL_LABEL)) {
				if (totalCoverage[counter] ==  null) {
					totalCoverage[counter] = new Coverage();
				}
				totalCoverage[counter].covered += c.covered;
				totalCoverage[counter++].total += c.total;
			}
		}
	}


	private void calculateBranchCoverage(Collection<IClassCoverage> classes, Map<String, ArrayList<Coverage>> classCoverage) {

		Coverage coverage;
		int covered = 0;
		int total = 0;

		for (IClassCoverage c : classes) {
			coverage = calculateBranchCoverage(c);

			if (classCoverage.get(c.getName()) == null) {
				classCoverage.put(c.getName(), new ArrayList<Coverage>());
			}
			classCoverage.get(c.getName()).add(coverage);

			covered += coverage.covered;
			total += coverage.total;

		}

		// Calculate the package coverage
		if (classCoverage.get(TOTAL_LABEL) == null) {
			classCoverage.put(TOTAL_LABEL, new ArrayList<Coverage>());
		}

		classCoverage.get(TOTAL_LABEL).add(new Coverage(covered, total));

	}


	private Coverage calculateBranchCoverage(IClassCoverage c) {

		Coverage coverage = new Coverage();

		coverage.covered = c.getBranchCounter().getCoveredCount();
		coverage.total = c.getBranchCounter().getTotalCount();

		return coverage;
	}


    public Path mergeExecDataFiles(final String[] execDataFiles) throws IOException {

        log.info("merge exec files");

	ExecFileLoader execFileLoader = new ExecFileLoader();
	for (String inputFile : execDataFiles) {
	    try (InputStream is = Files.newInputStream(Paths.get(inputFile))) {
		execFileLoader.load(is);
	    } catch (IOException ex) {
		throw new RuntimeException("Error loading data from file: '" + inputFile.toString() + "'", ex);
	    }
	}
	final Path destMergedFile = Paths.get(this.jacocoExecPath.toAbsolutePath().toString() + "/jacoco-merged.exec");
	try (OutputStream os = Files.newOutputStream(destMergedFile)) {
	    ExecutionDataWriter executionDataWriter = new ExecutionDataWriter(os);
	    execFileLoader.getSessionInfoStore().accept(executionDataWriter);
	    execFileLoader.getExecutionDataStore().accept(executionDataWriter);
	    executionDataWriter.flush();
	} catch (IOException ex) {
	    throw new RuntimeException("Error writing data to file: '" + destMergedFile.toString() + "'", ex);
	}
	return destMergedFile;
    }

	private IBundleCoverage analyzeStructure() throws IOException {
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(executionDataStore,
				coverageBuilder);

		analyzer.analyzeAll(classesDirectory);

		return coverageBuilder.getBundle(title);
	}

	private void loadExecutionData(final File execDataFile) throws IOException {

		final FileInputStream fis = new FileInputStream(execDataFile);
		final ExecutionDataReader executionDataReader = new ExecutionDataReader(
				fis);
		executionDataStore = new ExecutionDataStore();
		sessionInfoStore = new SessionInfoStore();

		executionDataReader.setExecutionDataVisitor(executionDataStore);
		executionDataReader.setSessionInfoVisitor(sessionInfoStore);

		while (executionDataReader.read()) {
		}

		fis.close();
	}

    public List<IBundleCoverage> loadAndAnalyze(String[] execDataFiles) throws IOException {

	List<IBundleCoverage> bcl = new ArrayList<>();
	IBundleCoverage bundleCoverage;
	Path source, dest;

	Files.createDirectories(Paths.get(this.reportDirectory.getAbsolutePath() + "/jacoco-execs"));

	for (int i = 0; i < execDataFiles.length; i++) {

	    // Copy the execution data files locally to prepare them for merge
	    source = Paths.get(execDataFiles[i]);
	    dest = Paths.get(this.jacocoExecPath.toAbsolutePath().toString() + "/part_" + i + ".exec");
	    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);

	    // Load and analyze the execution data
	    bundleCoverage = loadAndAnalyze(source.toFile());

	    bcl.add(bundleCoverage);
	}

	return bcl;
    }

	public IBundleCoverage loadAndAnalyze(final File execDataFile) throws IOException {
	    log.info("load and analyze: {}", execDataFile.getPath());
		loadExecutionData(execDataFile);
		return analyzeStructure();

	}


	 int getNumberOfTestSuites() {
		return numberOfTestSuites;
	}

	private boolean isDifferent(String className, ArrayList<Coverage> coverage) {

		int prev = 0;
		boolean different = false;

		if (className.equals(TOTAL_LABEL)) return false;

		if (coverage != null && coverage.size() > 0) {
			prev = coverage.get(0).covered;
		}

		for (Coverage c : coverage) {
			if (c.covered != prev) {
				different = true;
				break;
			}

		}

		return different;
	}


}

class Coverage {

	int covered;
    int total;

    public Coverage() {
    	covered = 0;
    	total = 0;
    }

	public Coverage(int covered, int total) {
		this.covered = covered;
		this.total = total;
	}
}



