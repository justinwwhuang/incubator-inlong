/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tubemq.corerpc.protocol;

import java.util.HashMap;
import java.util.Map;


public class ProtocolFactory {

    private static final Map<Integer, Class<? extends Protocol>> protocols =
            new HashMap<>();

    static {
        registerProtocol(RpcProtocol.RPC_PROTOCOL_TCP, RpcProtocol.class);
        registerProtocol(RpcProtocol.RPC_PROTOCOL_TLS, RpcProtocol.class);
    }

    public static void registerProtocol(int type, Class<? extends Protocol> customProtocol) {
        protocols.put(type, customProtocol);
    }

    public static Class<? extends Protocol> getProtocol(int type) {
        return protocols.get(type);
    }

    public static Protocol getProtocolInstance(int type) throws Exception {
        return protocols.get(type).newInstance();
    }

}
