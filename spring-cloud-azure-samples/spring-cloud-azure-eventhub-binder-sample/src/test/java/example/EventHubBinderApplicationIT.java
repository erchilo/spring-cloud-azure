/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package example;

import com.example.EventHubBinderApplication;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EventHubBinderApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class EventHubBinderApplicationIT {

    private static PrintStream systemOut;
    private static ByteArrayOutputStream baos;
    @Autowired
    private MockMvc mvc;

    @BeforeClass
    public static void captureSystemOut() {
        systemOut = System.out;
        baos = new ByteArrayOutputStream();
        TeeOutputStream out = new TeeOutputStream(systemOut, baos);
        System.setOut(new PrintStream(out));
    }

    @AfterClass
    public static void revertSystemOut() {
        System.setOut(systemOut);
    }

    @Test
    public void testSendAndReceiveMessage() throws Exception {
        String message = UUID.randomUUID().toString();

        mvc.perform(post("/messages?message=" + message)).andExpect(status().isOk())
           .andExpect(content().string(message));

        String messageReceivedLog = String.format("New message received: '%s'", message);
        String messageCheckpointedLog = String.format("Message '%s' successfully checkpointed", message);
        boolean messageReceived = false;
        boolean messageCheckpointed = false;
        for (int i = 0; i < 100; i++) {
            String output = baos.toString();
            if (!messageReceived && output.contains(messageReceivedLog)) {
                messageReceived = true;
            }

            if (!messageCheckpointed && output.contains(messageCheckpointedLog)) {
                messageCheckpointed = true;
            }

            if (messageReceived && messageCheckpointed) {
                break;
            }

            Thread.sleep(1000);
        }
        assertThat(messageReceived).isTrue();
        assertThat(messageCheckpointed).isTrue();
    }
}
