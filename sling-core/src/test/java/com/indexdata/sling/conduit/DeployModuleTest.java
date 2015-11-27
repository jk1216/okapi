/*
 * Copyright (c) 2015-2015, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package com.indexdata.sling.conduit;

import com.indexdata.sling.MainVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class DeployModuleTest {
  Vertx vertx;
  
  public DeployModuleTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
    
    DeploymentOptions opt = new DeploymentOptions();
    vertx.deployVerticle(MainVerticle.class.getName(),
            opt, context.asyncAssertSuccess());
    
  }
  
  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }
  
  private int port = Integer.parseInt(System.getProperty("port", "9130"));
            
  @Test
  public void test1(TestContext context) {
    final String doc = "{\n"
            + "  \"name\" : \"sample-module\",\n"
            + "  \"descriptor\" : {\n"
            + "    \"cmdlineStart\" : "
            + "\"java -Dport=%p -jar ../sling-sample-module/target/sling-sample-module-fat.jar\",\n"
            + "    \"cmdlineStop\" : null\n"
            + "  },\n"
            + "  \"routingEntries\" : [ {\n"
            + "    \"methods\" : [ \"GET\" ],\n"
            + "    \"path\" : \"/sample\"\n"
            + "  } ]\n"
            + "}";
    final Async async = context.async();
    HttpClient c = vertx.createHttpClient();
    c.post(port, "localhost", "/conduit/modules", response -> {
      context.assertEquals(201, response.statusCode());
      response.endHandler(x -> {
        getIt(context, async, response.getHeader("Location"), doc);
      });
    }).end(doc);
  }

  public void getIt(TestContext context, Async async, String location,
          String doc) {
    HttpClient c = vertx.createHttpClient();
    c.get(port, "localhost", location, response -> {
      response.handler(body -> {
        context.assertEquals(doc, body.toString());
      });
      response.endHandler(x -> {
        vertx.setTimer(300, id -> {
          useIt(context, async, location);
        });
      });
    }).end();
  }
  
  public void useIt(TestContext context, Async async, String location) {
    HttpClient c = vertx.createHttpClient();
    c.get(port, "localhost", "/sample", response -> {
      context.assertEquals(200, response.statusCode());
      System.out.println("Got response in useIt");
      response.bodyHandler(x -> {
         context.assertEquals("It works", x.toString());
      });
      response.endHandler(x -> {
         useNoPath(context, async, location);        
      });
    }).end();
  }

  public void useNoPath(TestContext context, Async async, String location) {
    HttpClient c = vertx.createHttpClient();
    c.get(port, "localhost", "/samplE", response -> {
      context.assertEquals(404, response.statusCode());
      response.endHandler(x -> {
         useNoMethod(context, async, location);        
      });
    }).end();
  }

  public void useNoMethod(TestContext context, Async async, String location) {
    HttpClient c = vertx.createHttpClient();
    c.delete(port, "localhost", "/sample", response -> {
      context.assertEquals(404, response.statusCode());
      response.endHandler(x -> {
         deleteIt(context, async, location);        
      });
    }).end();
  }

  public void deleteIt(TestContext context, Async async, String location) {
    HttpClient c = vertx.createHttpClient();
    c.delete(port, "localhost", location, response -> {
      context.assertEquals(204, response.statusCode());
      response.endHandler(x -> {
        async.complete();
      });
    }).end();
  }
}
