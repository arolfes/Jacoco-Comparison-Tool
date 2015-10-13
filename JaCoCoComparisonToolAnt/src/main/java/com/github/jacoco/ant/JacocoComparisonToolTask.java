package com.github.jacoco.ant;

import static edu.cmu.jacoco.utils.CoverageDiffUtils.getSplittedString;
import static edu.cmu.jacoco.utils.CoverageDiffUtils.wrapTitles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jacoco.core.analysis.IBundleCoverage;

import edu.cmu.jacoco.CoverageDiff;

public class JacocoComparisonToolTask extends Task
{

  /**
   * The absolute path to your sources folder.
   */
  private File sourceFileDir;

  /**
   * The absolute path to your classes folder.
   */
  private File classFileDir;

  /**
   * The absolute path to the directory where the generated report files will be copied to
   */
  private File reports;

  /**
   * The path to your first intermediate data file, ie. jacoco1.exec
   */
  private File execFileFromTestCase1;

  /**
   * The path to your second intermediate data file, ie. jacoco2.exec
   */
  private File execFileFromTestCase2;

  /**
   * The comma separated name of packages to be included in the report.
   */
  private String packages;

  /**
   * The comma separated title of test suites, to be used in the coverage report.
   */
  private String titles;

  public File getSourceFileDir()
  {
    return sourceFileDir;
  }

  public void setSourceFileDir(File sourceFileDir)
  {
    this.sourceFileDir = sourceFileDir;
  }

  public File getClassFileDir()
  {
    return classFileDir;
  }

  public void setClassFileDir(File classFileDir)
  {
    this.classFileDir = classFileDir;
  }

  public File getReports()
  {
    return reports;
  }

  public void setReports(File reports)
  {
    this.reports = reports;
  }

  public File getExecFileFromTestCase1()
  {
    return execFileFromTestCase1;
  }

  public void setExecFileFromTestCase1(File execFileFromTestCase1)
  {
    this.execFileFromTestCase1 = execFileFromTestCase1;
  }

  public File getExecFileFromTestCase2()
  {
    return execFileFromTestCase2;
  }

  public void setExecFileFromTestCase2(File execFileFromTestCase2)
  {
    this.execFileFromTestCase2 = execFileFromTestCase2;
  }

  public String getPackages()
  {
    return packages;
  }

  public void setPackages(String packages)
  {
    this.packages = packages;
  }

  public String getTitles()
  {
    return titles;
  }

  public void setTitles(String titles)
  {
    this.titles = titles;
  }

  @Override
  public void execute() throws BuildException {
    log( "sourceFileDir = " + this.sourceFileDir.getAbsolutePath());
    log( "classFileDir = " + this.classFileDir.getAbsolutePath());
    log( "reports = " + this.reports.getAbsolutePath());
    log( "execFileFromTestCase1 = " + this.execFileFromTestCase1.getAbsolutePath());
    log( "execFileFromTestCase2 = " + this.execFileFromTestCase2.getAbsolutePath());
    log( "packages = " + this.packages);
    log( "titles = " + this.titles);

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
      throw new BuildException("Could not execute JaCoCoComparison because IOException '"+e.getMessage()+"'", e);
    }


  }

}
