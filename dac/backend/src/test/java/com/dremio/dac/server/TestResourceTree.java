/*
 * Copyright (C) 2017 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.dac.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.dremio.dac.model.resourcetree.ResourceList;
import com.dremio.dac.model.resourcetree.ResourceTreeEntity;
import com.dremio.dac.model.resourcetree.ResourceTreeEntity.ResourceType;
import com.dremio.service.namespace.NamespaceService;
import com.dremio.service.namespace.TestNamespaceService;

/**
 * Test resource tree.
 */
public class TestResourceTree extends BaseTestServer {

  @Before
  public void setup() throws Exception {
    clearAllDataExceptUser();
    populateNamespace();
  }

  private void populateNamespace() throws Exception {
    NamespaceService ns = newNamespaceService();

    getPopulator().populateTestUsers();

    TestNamespaceService.addSource(ns, "src1");
    TestNamespaceService.addSource(ns, "src2");
    TestNamespaceService.addSource(ns, "src3");
    TestNamespaceService.addSource(ns, "src4");

    TestNamespaceService.addSpace(ns, "space1");
    TestNamespaceService.addSpace(ns, "space2");
    TestNamespaceService.addSpace(ns, "space3");

    TestNamespaceService.addFolder(ns, "space1.foo1");
    TestNamespaceService.addFolder(ns, "space1.foo1.bar1");
    TestNamespaceService.addFolder(ns, "space1.bar1");
    TestNamespaceService.addDS(ns, "space1.ds1");
    TestNamespaceService.addDS(ns, "space1.foo1.ds2");
    TestNamespaceService.addDS(ns, "space1.foo1.bar1.ds3");

    TestNamespaceService.addFolder(ns, "space2.foo2");
    TestNamespaceService.addFolder(ns, "space2.foo2.bar2");
    TestNamespaceService.addDS(ns, "space2.ds1");
    TestNamespaceService.addDS(ns, "space2.ds2");
    TestNamespaceService.addDS(ns, "space2.ds3");
    TestNamespaceService.addDS(ns, "space2.foo2.ds4");
    TestNamespaceService.addDS(ns, "space2.foo2.bar2.ds5");
  }

  @Test
  public void testResourceTreeRoot() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree")
      .queryParam("showSources", true)
      .queryParam("showSpaces", true)
      .queryParam("showHomes", true)).buildGet(), ResourceList.class);
    assertEquals(5, resourceList.count(ResourceType.SOURCE));
    assertEquals(3, resourceList.count(ResourceType.SPACE));
    assertEquals(0, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, resourceList.count(ResourceType.FOLDER));
    assertEquals(1, resourceList.count(ResourceType.HOME)); // logged in user home dir
  }

  @Test
  public void testResourceTreeEmptyRoot() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree")).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.SOURCE));
    assertEquals(0, resourceList.count(ResourceType.SPACE));
    assertEquals(0, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, resourceList.count(ResourceType.FOLDER));
    assertEquals(0, resourceList.count(ResourceType.HOME));
  }

  @Test
  public void testResourceTree() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree")).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.SOURCE));
    assertEquals(0, resourceList.count(ResourceType.SPACE));
    assertEquals(0, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, resourceList.count(ResourceType.FOLDER));
    assertEquals(0, resourceList.count(ResourceType.HOME));
  }

  @Test
  public void testResourcesSpace1() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space1")
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(1, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(2, resourceList.count(ResourceType.FOLDER));
  }

  @Test
  public void testResourcesSpace2() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space2")
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(3, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, resourceList.count(ResourceType.FOLDER));
  }

  @Test
  public void testResourcesFoo1() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space1.foo1")
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(1, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, resourceList.count(ResourceType.FOLDER));
  }

  @Test
  public void testResourcesFoo2() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space2.foo2")
      .queryParam("showDatasets", false)).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, resourceList.count(ResourceType.FOLDER));
  }

  @Test
  public void testResourcesBar1() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space1.foo1.bar1")
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(1, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, resourceList.count(ResourceType.FOLDER));
    assertEquals("ds3", resourceList.getResources().get(0).getName());
  }

  @Test
  public void testResourcesBar2() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space2.foo2.bar2")
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(1, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, resourceList.count(ResourceType.FOLDER));
    assertEquals("ds5", resourceList.getResources().get(0).getName());
  }

  @Test
  public void testResourcesDs5() throws Exception {
    NotFoundErrorMessage err = expectError(FamilyExpectation.SERVER_ERROR, getBuilder(getAPIv2().path("resourcetree/space2.foo2.bar2.ds5")
      .queryParam("showDatasets", true)).buildGet(), NotFoundErrorMessage.class);
    assertContains("ds5", err.toString());
  }

  @Test
  public void testResourceTreeExpandSpace1() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space1/expand")
      .queryParam("showSources", true)
      .queryParam("showSpaces", true)
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(5, resourceList.count(ResourceType.SOURCE));
    assertEquals(3, resourceList.count(ResourceType.SPACE));
    assertEquals(0, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, resourceList.count(ResourceType.FOLDER));

    ResourceTreeEntity space1 = resourceList.find("space1", ResourceType.SPACE);
    assertNotNull(space1);
    assertNotNull(space1.getResources());
    assertEquals(1, new ResourceList(space1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(2, new ResourceList(space1.getResources()).count(ResourceType.FOLDER));
  }


  @Test
  public void testResourceTreeExpandSpace2() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space2/expand")
      .queryParam("showSources", false)
      .queryParam("showSpaces", true)
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.SOURCE));
    assertEquals(3, resourceList.count(ResourceType.SPACE));
    assertEquals(0, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, resourceList.count(ResourceType.FOLDER));

    ResourceTreeEntity space2 = resourceList.find("space2", ResourceType.SPACE);
    assertNotNull(space2);
    assertNotNull(space2.getResources());
    assertEquals(3, new ResourceList(space2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(space2.getResources()).count(ResourceType.FOLDER));
  }

  @Test
  public void testResourceTreeExpandFoo1() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space1.foo1/expand")
      .queryParam("showSources", false)
      .queryParam("showSpaces", false)
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.SOURCE));
    assertEquals(1, resourceList.count(ResourceType.SPACE));

    // space1
    ResourceTreeEntity space1 = resourceList.find("space1", ResourceType.SPACE);
    assertNotNull(space1);
    assertNotNull(space1.getResources());
    assertEquals(1, new ResourceList(space1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(2, new ResourceList(space1.getResources()).count(ResourceType.FOLDER));

    /// foo1
    ResourceTreeEntity foo1 = new ResourceList(space1.getResources()).find("foo1", ResourceType.FOLDER);
    assertNotNull(foo1);
    assertNotNull(foo1.getResources());
    assertEquals(1, new ResourceList(foo1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(foo1.getResources()).count(ResourceType.FOLDER));
  }


  @Test
  public void testResourceTreeExpandFoo2() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space2.foo2/expand")
      .queryParam("showSources", true)
      .queryParam("showSpaces", true)
      .queryParam("showDatasets", false)).buildGet(), ResourceList.class);
    assertEquals(5, resourceList.count(ResourceType.SOURCE));
    assertEquals(3, resourceList.count(ResourceType.SPACE));

    // space2
    ResourceTreeEntity space2 = resourceList.find("space2", ResourceType.SPACE);
    assertNotNull(space2);
    assertNotNull(space2.getResources());
    assertEquals(0, new ResourceList(space2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(space2.getResources()).count(ResourceType.FOLDER));

    /// foo2
    ResourceTreeEntity foo2 = new ResourceList(space2.getResources()).find("foo2", ResourceType.FOLDER);
    assertNotNull(foo2);
    assertNotNull(foo2.getResources());
    assertEquals(0, new ResourceList(foo2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(foo2.getResources()).count(ResourceType.FOLDER));
  }

  @Test
  public void testResourceTreeExpandFooBar1() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space1.foo1.bar1/expand")
      .queryParam("showSources", true)
      .queryParam("showSpaces", true)
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(5, resourceList.count(ResourceType.SOURCE));
    assertEquals(3, resourceList.count(ResourceType.SPACE));

    // space1
    ResourceTreeEntity space1 = resourceList.find("space1", ResourceType.SPACE);
    assertNotNull(space1);
    assertNotNull(space1.getResources());
    assertEquals(1, new ResourceList(space1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(2, new ResourceList(space1.getResources()).count(ResourceType.FOLDER));

    /// foo1
    ResourceTreeEntity foo1 = new ResourceList(space1.getResources()).find("foo1", ResourceType.FOLDER);
    assertNotNull(foo1);
    assertNotNull(foo1.getResources());
    assertEquals(1, new ResourceList(foo1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(foo1.getResources()).count(ResourceType.FOLDER));

    // bar1
    ResourceTreeEntity bar1 = new ResourceList(foo1.getResources()).find("bar1", ResourceType.FOLDER);
    assertNotNull(bar1);
    assertEquals(1, new ResourceList(bar1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, new ResourceList(bar1.getResources()).count(ResourceType.FOLDER));
  }

  @Test
  public void testResourceTreeExpandFooBar2() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space2.foo2.bar2/expand")
      .queryParam("showSources", false)
      .queryParam("showSpaces", false)
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.SOURCE));
    assertEquals(1, resourceList.count(ResourceType.SPACE));

    // space2
    ResourceTreeEntity space2 = resourceList.find("space2", ResourceType.SPACE);
    assertNotNull(space2);
    assertNotNull(space2.getResources());
    assertEquals(3, new ResourceList(space2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(space2.getResources()).count(ResourceType.FOLDER));

    /// foo2
    ResourceTreeEntity foo2 = new ResourceList(space2.getResources()).find("foo2", ResourceType.FOLDER);
    assertNotNull(foo2);
    assertNotNull(foo2.getResources());
    assertEquals(1, new ResourceList(foo2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(foo2.getResources()).count(ResourceType.FOLDER));

    // bar2
    ResourceTreeEntity bar2 = new ResourceList(foo2.getResources()).find("bar2", ResourceType.FOLDER);
    assertNotNull(bar2);
    assertEquals(1, new ResourceList(bar2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, new ResourceList(bar2.getResources()).count(ResourceType.FOLDER));
  }

  @Test
  public void testEmptySpace() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space3/expand")
      .queryParam("showSources", false)
      .queryParam("showSpaces", false)
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.SOURCE));
    assertEquals(1, resourceList.count(ResourceType.SPACE));
    assertEquals(0, resourceList.count(ResourceType.HOME));
    assertEquals(0, resourceList.count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, resourceList.count(ResourceType.FOLDER));
  }

  @Test
  public void testInvalidSpace() throws Exception {
    NotFoundErrorMessage err = expectError(FamilyExpectation.SERVER_ERROR, getBuilder(getAPIv2().path("resourcetree/space4/expand")).buildGet(), NotFoundErrorMessage.class);
    assertContains("space4", err.toString());
  }

  @Test
  public void testResourceTreeExpandFooBar1Ds3() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space1.foo1.bar1.ds3/expand")
      .queryParam("showSources", false)
      .queryParam("showSpaces", false)
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.SOURCE));
    assertEquals(1, resourceList.count(ResourceType.SPACE));

    // space1
    ResourceTreeEntity space1 = resourceList.find("space1", ResourceType.SPACE);
    assertNotNull(space1);
    assertNotNull(space1.getResources());
    assertEquals(1, new ResourceList(space1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(2, new ResourceList(space1.getResources()).count(ResourceType.FOLDER));

    /// foo1
    ResourceTreeEntity foo1 = new ResourceList(space1.getResources()).find("foo1", ResourceType.FOLDER);
    assertNotNull(foo1);
    assertNotNull(foo1.getResources());
    assertEquals(1, new ResourceList(foo1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(foo1.getResources()).count(ResourceType.FOLDER));

    // bar1
    ResourceTreeEntity bar1 = new ResourceList(foo1.getResources()).find("bar1", ResourceType.FOLDER);
    assertNotNull(bar1);
    assertEquals(1, new ResourceList(bar1.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, new ResourceList(bar1.getResources()).count(ResourceType.FOLDER));

    // ds3
    ResourceTreeEntity ds3 = new ResourceList(bar1.getResources()).find("ds3", ResourceType.VIRTUAL_DATASET);
    assertNotNull(ds3);
    assertNull(ds3.getResources());
  }

  @Test
  public void testResourceTreeExpandFooBar2Ds5() throws Exception {
    ResourceList resourceList = expectSuccess(getBuilder(getAPIv2().path("resourcetree/space2.foo2.bar2.ds5/expand")
      .queryParam("showSources", false)
      .queryParam("showSpaces", true)
      .queryParam("showDatasets", true)).buildGet(), ResourceList.class);
    assertEquals(0, resourceList.count(ResourceType.SOURCE));
    assertEquals(3, resourceList.count(ResourceType.SPACE));

    // space2
    ResourceTreeEntity space2 = resourceList.find("space2", ResourceType.SPACE);
    assertNotNull(space2);
    assertNotNull(space2.getResources());
    assertEquals(3, new ResourceList(space2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(space2.getResources()).count(ResourceType.FOLDER));

    /// foo2
    ResourceTreeEntity foo2 = new ResourceList(space2.getResources()).find("foo2", ResourceType.FOLDER);
    assertNotNull(foo2);
    assertNotNull(foo2.getResources());
    assertEquals(1, new ResourceList(foo2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(1, new ResourceList(foo2.getResources()).count(ResourceType.FOLDER));

    // bar2
    ResourceTreeEntity bar2 = new ResourceList(foo2.getResources()).find("bar2", ResourceType.FOLDER);
    assertNotNull(bar2);
    assertEquals(1, new ResourceList(bar2.getResources()).count(ResourceType.VIRTUAL_DATASET));
    assertEquals(0, new ResourceList(bar2.getResources()).count(ResourceType.FOLDER));

    // ds5
    ResourceTreeEntity ds5 = new ResourceList(bar2.getResources()).find("ds5", ResourceType.VIRTUAL_DATASET);
    assertNotNull(ds5);
    assertNull(ds5.getResources());
  }

}
