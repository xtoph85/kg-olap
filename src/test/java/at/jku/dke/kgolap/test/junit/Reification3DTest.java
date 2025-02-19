package at.jku.dke.kgolap.test.junit;

import static org.junit.Assert.assertEquals;
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

public class Reification3DTest extends OperatorTest {
  private static final Logger logger = LoggerFactory.getLogger(Reification3DTest.class);
  
  public Reification3DTest() {
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

    try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("knowledge/atm-3D-object-reification.trig")) {
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
  public void testReificationWithGranularity() {
    Repo baseRepo = this.getKGOLAPCube().getBaseRepository();
    Repo tempRepo = this.getKGOLAPCube().getTempRepository();
    
    HashMap<String,String> granularity = new HashMap<String,String>();
    granularity.put("cube:hasAircraft", "cube:Level_Aircraft_Model");
    granularity.put("cube:hasLocation", "cube:Level_Location_Segment");
    granularity.put("cube:hasDate", "cube:Level_Date_Day");

    this.getKGOLAPCube().reify(granularity, "obj:usage");
    
    {
      String sparqlBase = this.getKGOLAPCube().getSparqlPrefixes() +
          "SELECT (COUNT(DISTINCT *) AS ?cnt) WHERE {\r\n" + 
          "    GRAPH ?g {\r\n" + 
          "        ?s obj:usage ?u.\r\n" + 
          "    }\r\n" + 
          "}";
      
      String sparqlTemp = this.getKGOLAPCube().getSparqlPrefixes() +
          "SELECT (COUNT(DISTINCT ?x) AS ?cnt) WHERE {\r\n" + 
          "    GRAPH ?g {\r\n" + 
          "        ?x rdf:type obj:usage-type .\r\n" + 
          "    }\r\n" + 
          "}";
      
      String resultBase = baseRepo.executeTupleQuery(sparqlBase);
      String resultTemp = tempRepo.executeTupleQuery(sparqlTemp);
      
      assertEquals(resultBase, resultTemp);
    }
  }
}
