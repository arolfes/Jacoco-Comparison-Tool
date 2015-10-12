package edu.cmu.jacoco.maven;

import static edu.cmu.jacoco.utils.CoverageDiffUtils.getSplittedString;
import static edu.cmu.jacoco.utils.CoverageDiffUtils.wrapTitles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.analysis.IBundleCoverage;

import edu.cmu.jacoco.CoverageDiff;

@Mojo( name = "compare-exec-files", defaultPhase=LifecyclePhase.PRE_SITE)
public class JacocoComparisonTool extends AbstractMojo
{

  /**
   * The absolute path to your sources folder.
   */
  @Parameter(required=true, property="sourceFileDir")
  private File sourceFileDir;

  /**
   * The absolute path to your classes folder.
   */
  @Parameter(required=true, property="classFileDir")
  private File classFileDir;

  /**
   * The absolute path to the directory where the generated report files will be copied to
   */
  @Parameter(required=true, property="reports")
  private File reports;

  /**
   * The path to your first intermediate data file, ie. jacoco1.exec
   */
  @Parameter(required=true, property="execFileFromTestCase1")
  private File execFileFromTestCase1;

  /**
   * The path to your second intermediate data file, ie. jacoco2.exec
   */
  @Parameter(required=true, property="execFileFromTestCase2")
  private File execFileFromTestCase2;

  /**
   * The comma separated name of packages to be included in the report.
   */
  @Parameter(required=false, property="packages")
  private String packages;

  /**
   * The comma separated title of test suites, to be used in the coverage report.
   */
  @Parameter(required=false, property="titles")
  private String titles;


  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info( "sourceFileDir = " + this.sourceFileDir.getAbsolutePath());
    getLog().info( "classFileDir = " + this.classFileDir.getAbsolutePath());
    getLog().info( "reports = " + this.reports.getAbsolutePath());
    getLog().info( "execFileFromTestCase1 = " + this.execFileFromTestCase1.getAbsolutePath());
    getLog().info( "execFileFromTestCase2 = " + this.execFileFromTestCase2.getAbsolutePath());
    getLog().info( "packages = " + this.packages);
    getLog().info( "titles = " + this.titles);

    try
    {
      final String execDataFiles[] = new String[]{
          execFileFromTestCase1.getAbsolutePath(),
          execFileFromTestCase2.getAbsolutePath()
      };

      final CoverageDiff s = new CoverageDiff(this.sourceFileDir, this.classFileDir, this.reports, execDataFiles.length);
      final List<IBundleCoverage> bcl = s.loadAndAnalyze(execDataFiles);


      final Path destMergedFile = s.mergeExecDataFiles(execDataFiles);
      final IBundleCoverage bundleCoverage = s.loadAndAnalyze(destMergedFile.toFile());

      bcl.add(bundleCoverage);

      s.calculateBranchCoverage(bcl);

      s.initWriter();

      final String[] testSuiteTitles = wrapTitles(getSplittedString(this.titles), execDataFiles.length);
      s.renderBranchCoverage(testSuiteTitles, getSplittedString(this.packages));

      s.generateClassCoverageReport(bcl);

    } catch (IOException e)
    {
      throw new MojoExecutionException("Could not execute JaCoCoComparison because IOException '"+e.getMessage()+"'", e);
    }

  }

}
