// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.guides.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.kafka.KafkaProducerConfig;

import io.openliberty.guides.inventory.InventoryResource;
import io.openliberty.guides.models.SystemLoad;
import io.openliberty.guides.models.SystemLoad.JsonbSerializer;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
@TestMethodOrder(OrderAnnotation.class)
// tag::InventoryServiceIT[]
public class InventoryServiceIT {

    @RESTClient
    public static InventoryResource inventoryResource;

    // tag::KafkaProducer[]
    // tag::KafkaProducerConfig[]
    @KafkaProducerConfig(valueSerializer = JsonbSerializer.class)
    // end::KafkaProducerConfig[]
    public static KafkaProducer<String, SystemLoad> cpuProducer;
    // end::KafkaProducer[]

    @AfterAll
    public static void cleanup() {
        inventoryResource.resetSystems();
    }

    @Test
    public void testCpuUsage() throws InterruptedException {
        SystemLoad c = new SystemLoad("localhost", 1.1);
        // tag::systemLoadTopic[]
        cpuProducer.send(new ProducerRecord<String, SystemLoad>("systemLoadTopic", c));
        // end::systemLoadTopic[]
        Thread.sleep(5000);
        Response response = inventoryResource.getSystems();
        List<Properties> systems = response.readEntity(new GenericType<List<Properties>>() {});
        // tag::assert[]
        Assertions.assertEquals(200, response.getStatus(),
                "Response should be 200");
        Assertions.assertEquals(systems.size(), 1);
        // end::assert[]
        for (Properties system : systems) {
            // tag::assert[]
            Assertions.assertEquals(c.hostId, system.get("hostname"),
                    "HostId not match!");
            // end::assert[]
            BigDecimal cpu = (BigDecimal) system.get("systemLoad");
            // tag::assert[]
            Assertions.assertEquals(c.cpuUsage, cpu.doubleValue(),
                    "CPU Usage not match!");
            // end::assert[]
        }
    }
}
// end::InventoryServiceIT[]
