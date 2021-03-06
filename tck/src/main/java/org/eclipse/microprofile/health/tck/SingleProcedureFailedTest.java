/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.microprofile.health.tck;

import static org.eclipse.microprofile.health.tck.JsonUtils.asJsonObject;
import static org.eclipse.microprofile.health.tck.TCKConfiguration.getHealthURL;

import org.eclipse.microprofile.health.tck.deployment.FailedCheck;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;

/**
 * @author Heiko Braun
 */
@RunWith(Arquillian.class)
public class SingleProcedureFailedTest extends SimpleHttp {

    @Deployment
    public static Archive getDeployment() throws Exception {
        WebArchive deployment = ShrinkWrap.create(WebArchive.class, "tck.war");
        deployment.addClass(FailedCheck.class);
        return deployment;
    }

    /**
     * Verifies the health integration with CDI at the scope of a server runtime
     */
    @Test
    @RunAsClient
    public void testFailureResponsePayload() throws Exception {
        Response response = getUrlContents(getHealthURL());

        // status code
        Assert.assertEquals(503, response.getStatus());

        JsonReader jsonReader = Json.createReader(new StringReader(response.getBody().get()));
        JsonObject json = jsonReader.readObject();
        System.out.println(json);

        // response size
        JsonArray checks = json.getJsonArray("checks");
        Assert.assertEquals("Expected a single check response", 1, checks.size());

        // single procedure response
        Assert.assertEquals(
                "Expected a CDI health check to be invoked, but it was not present in the response",
                "failed-check",
                asJsonObject(checks.get(0)).getString("id")
        );

        Assert.assertEquals(
                "Expected a successful check result",
                "DOWN",
                asJsonObject(checks.get(0)).getString("result")
        );

        // overall outcome
        Assert.assertEquals(
                "Expected overall outcome to be unsuccessful",
                "DOWN",
                json.getString("outcome")
        );
    }
}

