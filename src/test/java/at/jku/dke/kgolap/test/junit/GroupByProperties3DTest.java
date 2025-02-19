package at.jku.dke.kgolap.test.junit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.jku.dke.kgolap.KGOLAPCubeProperties;
import at.jku.dke.kgolap.repo.Repo;
import at.jku.dke.kgolap.repo.RepoProperties;

public class GroupByProperties3DTest extends OperatorTest {
  private static final Logger logger = LoggerFactory.getLogger(GroupByProperties3DTest.class);
  
  public GroupByProperties3DTest() {
    super();
  }

  @Override
  public KGOLAPCubeProperties getProperties() {
    KGOLAPCubeProperties properties = new KGOLAPCubeProperties();

    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("rulesets/atm-3D-ruleset.ttl")) {
      logger.info("Read the ruleset file.");
      String rulesetTtl = IOUtils.toString(in, StandardCharsets.UTF_8.name());
      properties.setRulesetTtl(rulesetTtl);
    } catch (IOException e) {
      logger.error("Error reading ruleset file.", e);
    }

    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("config/prefixes.properties")) {
      logger.info("Read the prefixes properties from file.");

      Properties prefixProperties = new Properties();
      prefixProperties.load(in);

      for (String prefix : prefixProperties.stringPropertyNames()) {
        properties.addPrefix(prefix, prefixProperties.getProperty(prefix));
      }
    } catch (IOException e) {
      logger.error("Error reading prefixes properties from file.", e);
    }

    properties.setBaseRepoFactoryClass("at.jku.dke.kgolap.repo.sesame.SesameHTTPRepoFactory");
    properties.setTempRepoFactoryClass("at.jku.dke.kgolap.repo.sesame.SesameHTTPRepoFactory");

    RepoProperties baseRepoProperties = new RepoProperties();
    baseRepoProperties.setRepositoryURL("http://localhost:7201/repositories/Base");

    RepoProperties tempRepoProperties = new RepoProperties();
    tempRepoProperties.setRepositoryURL("http://localhost:7201/repositories/Temp");

    properties.setBaseRepoProperties(baseRepoProperties);
    properties.setTempRepoProperties(tempRepoProperties);

    return properties;
  }

  @Override
  public void loadTestdata() {
    Repo baseRepo = this.getKGOLAPCube().getBaseRepository();
    Repo tempRepo = this.getKGOLAPCube().getTempRepository();
    
    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("knowledge/atm-3D-cube.trig")) {
      logger.info("Adding cube knowledge to base repository ...");
      this.getKGOLAPCube().add(in);
      logger.info("Cube knowledge added.");
    } catch (IOException e) {
      logger.error("Error reading file.", e);
    }

    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("knowledge/atm-3D-object-groupByProperties.trig")) {
      logger.info("Adding object knowledge to base repository ...");
      this.getKGOLAPCube().add(in);
      logger.info("Object knowledge added.");
    } catch (IOException e) {
      logger.error("Error reading file.", e);
    }
    
    logger.info("Load all data from the base repository into the temp repository.");
    HashMap<String, String> allSlice = new HashMap<String, String>();
    allSlice.put("cube:hasAircraft", "cube:Level_Aircraft_All-All");
    allSlice.put("cube:hasLocation", "cube:Level_Location_All-All");
    allSlice.put("cube:hasDate", "cube:Level_Date_All-All");

    this.getKGOLAPCube().sliceDice(allSlice);

    assertTrue(repositoriesContainSameNumberOfTriples(baseRepo, tempRepo));
  }
  
  @Test
  public void testGroupBy() {
    Repo tempRepo = this.getKGOLAPCube().getTempRepository();
    
    this.getKGOLAPCube().groupByProperties("cube:Ctx-11", new String[] {"obj:a"}, "obj:grouping");

    assertFalse(this.repositoryContainsQuad(tempRepo, "cube:Ctx-11-mod", "obj:x", "obj:a", "obj:m1"));
    assertFalse(this.repositoryContainsQuad(tempRepo, "cube:Ctx-11-mod", "obj:x", "obj:b", "obj:c1"));
    assertFalse(this.repositoryContainsQuad(tempRepo, "cube:Ctx-11-mod", "obj:y", "obj:a", "obj:m1"));
    assertFalse(this.repositoryContainsQuad(tempRepo, "cube:Ctx-11-mod", "obj:y", "obj:b", "obj:c2"));
    assertFalse(this.repositoryContainsQuad(tempRepo, "cube:Ctx-11-mod", "obj:y", "obj:c", "obj:c2"));
    
    assertFalse(this.repositoryContainsQuad(tempRepo, "cube:Ctx-11-mod", "obj:z", "obj:a", "obj:m3"));
    assertFalse(this.repositoryContainsQuad(tempRepo, "cube:Ctx-11-mod", "obj:t", "obj:u", "obj:x"));
  }
//  
//  @Test
//  public void testGroupByAtModelSegmentDayLevel() {
//    HashMap<String,String> regionLevel = new HashMap<String,String>();
//    regionLevel.put("cube:hasAircraft", "cube:Level_Aircraft_Model");
//    regionLevel.put("cube:hasLocation", "cube:Level_Location_Segment");
//    regionLevel.put("cube:hasDate", "cube:Level_Date_Day");
//    
//    this.getKGOLAPCube().groupByProperties(regionLevel, new String[] {"obj:a"}, "obj:grouping");
//  }
//  
//  @Test
//  public void testGroupByInContextWithMultipleGroupingProperties() {
//    this.getKGOLAPCube().groupByProperties("cube:Ctx-11", new String[] {"obj:a", "obj:b"}, "obj:grouping", "obj:GroupedType");
//  }
//  
//  @Test
//  public void testGroupByWithRestrictionToClass() {
//    HashMap<String,String> regionLevel = new HashMap<String,String>();
//    regionLevel.put("cube:hasAircraft", "cube:Level_Aircraft_Model");
//    regionLevel.put("cube:hasLocation", "cube:Level_Location_Segment");
//    regionLevel.put("cube:hasDate", "cube:Level_Date_Day");
//    
//    this.getKGOLAPCube().groupByProperties(regionLevel, new String[] {"obj:a"}, "obj:grouping", "obj:GroupedType");
//  }
}
