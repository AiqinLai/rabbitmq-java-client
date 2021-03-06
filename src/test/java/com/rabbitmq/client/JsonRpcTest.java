// Copyright (c) 2018 Pivotal Software, Inc.  All rights reserved.
//
// This software, the RabbitMQ Java client library, is triple-licensed under the
// Mozilla Public License 1.1 ("MPL"), the GNU General Public License version 2
// ("GPL") and the Apache License version 2 ("ASL"). For the MPL, please see
// LICENSE-MPL-RabbitMQ. For the GPL, please see LICENSE-GPL2.  For the ASL,
// please see LICENSE-APACHE2.
//
// This software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
// either express or implied. See the LICENSE file for specific language governing
// rights and limitations of this software.
//
// If you have any questions regarding licensing, please contact us at
// info@rabbitmq.com.

package com.rabbitmq.client;

import com.rabbitmq.client.test.TestUtils;
import com.rabbitmq.tools.jsonrpc.JsonRpcClient;
import com.rabbitmq.tools.jsonrpc.JsonRpcServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.UndeclaredThrowableException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JsonRpcTest {

    Connection clientConnection, serverConnection;
    Channel clientChannel, serverChannel;
    String queue = "json.rpc.queue";
    JsonRpcServer server;
    JsonRpcClient client;
    RpcService service;

    @Before
    public void init() throws Exception {
        clientConnection = TestUtils.connectionFactory().newConnection();
        clientChannel = clientConnection.createChannel();
        serverConnection = TestUtils.connectionFactory().newConnection();
        serverChannel = serverConnection.createChannel();
        serverChannel.queueDeclare(queue, false, false, false, null);
        server = new JsonRpcServer(serverChannel, queue, RpcService.class, new DefaultRpcservice());
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    server.mainloop();
                } catch (Exception e) {
                    // safe to ignore when loops ends/server is canceled
                }
            }
        }).start();
        client = new JsonRpcClient(clientChannel, "", queue, 1000);
        service = client.createProxy(RpcService.class);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.terminateMainloop();
        }
        if (client != null) {
            client.close();
        }
        if (serverChannel != null) {
            serverChannel.queueDelete(queue);
        }
        clientConnection.close();
        serverConnection.close();
    }

    @Test
    public void rpc() {
        assertEquals("hello1", service.procedureString("hello"));
        assertEquals(2, service.procedureInteger(1).intValue());
        assertEquals(2, service.procedurePrimitiveInteger(1));
        assertEquals(2, service.procedureDouble(1.0).intValue());
        assertEquals(2, (int) service.procedurePrimitiveDouble(1.0));

        try {
            assertEquals(2, (int) service.procedureLongToInteger(1L));
            fail("Long argument isn't supported");
        } catch (UndeclaredThrowableException e) {
            // OK
        }
        assertEquals(2, service.procedurePrimitiveLongToInteger(1L));

        try {
            assertEquals(2, service.procedurePrimitiveLong(1L));
            fail("Long return type not supported");
        } catch (ClassCastException e) {
            // OK
        }

        try {
            assertEquals(2, service.procedureLong(1L).longValue());
            fail("Long argument isn't supported");
        } catch (UndeclaredThrowableException e) {
            // OK
        }
    }

    public interface RpcService {

        String procedureString(String input);

        int procedurePrimitiveInteger(int input);

        Integer procedureInteger(Integer input);

        Double procedureDouble(Double input);

        double procedurePrimitiveDouble(double input);

        Integer procedureLongToInteger(Long input);

        int procedurePrimitiveLongToInteger(long input);

        Long procedureLong(Long input);

        long procedurePrimitiveLong(long input);
    }

    public class DefaultRpcservice implements RpcService {

        @Override
        public String procedureString(String input) {
            return input + 1;
        }

        @Override
        public int procedurePrimitiveInteger(int input) {
            return input + 1;
        }

        @Override
        public Integer procedureInteger(Integer input) {
            return input + 1;
        }

        @Override
        public Long procedureLong(Long input) {
            return input + 1;
        }

        @Override
        public long procedurePrimitiveLong(long input) {
            return input + 1L;
        }

        @Override
        public Double procedureDouble(Double input) {
            return input + 1;
        }

        @Override
        public double procedurePrimitiveDouble(double input) {
            return input + 1;
        }

        @Override
        public Integer procedureLongToInteger(Long input) {
            return (int) (input + 1);
        }

        @Override
        public int procedurePrimitiveLongToInteger(long input) {
            return (int) input + 1;
        }
    }
}
