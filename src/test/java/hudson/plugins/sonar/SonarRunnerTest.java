/*
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package hudson.plugins.sonar;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SonarRunnerTest {

  private FilePath workDir;
  private AbstractProject project;
  private Launcher launcher;
  private EnvVars envVars;
  private BuildListener listener;

  private SonarRunner runner;

  @Before
  public void setUp() {
    File file = new File("target/test-workspace");
    file.mkdir();
    workDir = new FilePath(file);

    project = mock(AbstractProject.class);
    launcher = mock(Launcher.class);
    listener = mock(BuildListener.class);
    envVars = new EnvVars();

    runner = new SonarRunner(project, launcher, envVars, workDir);
  }

  @After
  public void tearDown() throws Exception {
    workDir.deleteRecursive();
  }

  @Test
  public void shouldBuildCmdLine() throws Exception {
    runner.extract();
    String properties = new StringBuilder()
        .append("# comment\n") // comment should be ignored - see SONARPLUGINS-1461
        .append("sonar.branch = 1.0\n")
        .toString();
    String javaOpts = "-Xmx200m";
    ArgumentListBuilder args = runner.buildCmdLine(listener, new SonarRunnerBuilder(null, "project.properties", properties, javaOpts));

    List<String> cmdLine = args.toList();
    System.out.println(cmdLine);
    // Note that first 5 parameters should have strict order:
    assertThat(cmdLine.get(0), is("java"));
    assertThat(cmdLine.get(1), is("-Xmx200m"));
    assertThat(cmdLine.get(2), is("-cp"));
    assertThat(cmdLine.get(3), containsString(".jar"));
    assertThat(cmdLine.get(4), is("org.sonar.runner.Main"));
    assertThat(cmdLine, hasItem("-Dsonar.branch=1.0"));
    assertThat(cmdLine, hasItem("-Dproject.settings=project.properties"));
  }

  @Test
  public void shouldReturnClasspathDelimiter() {
    when(launcher.isUnix()).thenReturn(true).thenReturn(false);
    assertThat("linux", runner.getClasspathDelimiter(), is(':'));
    assertThat("windows", runner.getClasspathDelimiter(), is(';'));
  }

  @Test
  public void shouldExtract() throws Exception {
    runner.extract();
    assertThat(workDir.list().size(), is(2));
    runner.cleanup();
    assertThat(workDir.list().size(), is(0));
  }

}
