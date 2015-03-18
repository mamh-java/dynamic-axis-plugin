package ca.silvermaplesolutions.jenkins.plugins.daxis;

import hudson.matrix.Axis;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Tests for {@link DynamicAxis}.
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
public class DynamicAxisTest extends HudsonTestCase {
    
    MatrixProject p;

    @Override
    protected void setUp() throws Exception {
        super.setUp(); 
        p = createMatrixProject();
        p.getAxes().add(new DynamicAxis("AXIS", "AXIS_VALUES"));     
    }
    
    public @Test void testDefaultInjection() throws Exception {
        p.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("AXIS_VALUES", "1 2 3")));
        
        MatrixBuild run = buildAndAssertSuccess(p);
        assertEquals(3, run.getExactRuns().size());
    }
   
    @Bug(25660)
    public @Test void testInjectionWithEscaping() throws Exception {
        checkAxesSetup(new String[]{"1", "\"2 3\""});
    }
    
    /**
     * Runs the test, when an environment contributor uses axis values
     * to build the environment.
     * @see AxisValuesUserSCM
     */
    @Bug(27243) 
    public @Test void testInjectionWithAxisValuesUser() throws Exception {
        checkAxesSetup(new String[]{"1", "2", "3"});
    }
    
    /**
     * Sets values from the list and then checks injection results.
     * @param values List of values, which should be escaped externally
     */
    private void checkAxesSetup(String[] values) throws Exception  {
        final String valuesString = StringUtils.join(values, " ");         
        p.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("AXIS_VALUES", valuesString)));      
        
        // Inject SCM, which implicitly triggers axes rebuild
        p.setScm(new AxisValuesUserSCM());
        
        final MatrixBuild run = buildAndAssertSuccess(p);
        
        // No additional values have been injected
        assertEquals(values.length, run.getExactRuns().size());
    }
    
    /**
     * Just a stub {@link SCM}, which rebuilds axes list. 
     */
    public static class AxisValuesUserSCM extends FakeChangeLogSCM {
        
        @Override
        public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
            if (build instanceof MatrixBuild) {
                final MatrixProject prj = (MatrixProject) build.getParent();
                for (Axis axis : prj.getAxes()) {
                    // Get values for a opeeration
                    axis.getValues();
                }
            }
        }
    }
}
