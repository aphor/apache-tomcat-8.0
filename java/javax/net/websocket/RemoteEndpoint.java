/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.net.websocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;


public interface RemoteEndpoint<T> {

    void sendString(String text) throws IOException;

    void sendBytes(ByteBuffer data) throws IOException;

    void sendPartialString(String fragment, boolean isLast) throws IOException;

    void sendPartialBytes(ByteBuffer partialByte, boolean isLast) throws IOException; // or Iterable<byte[]>

    OutputStream getSendStream() throws IOException;

    Writer getSendWriter() throws IOException;

    void sendObject(T o) throws IOException, EncodeException;

    Future<SendResult> sendString(String text, SendHandler completion);

    Future<SendResult> sendBytes(ByteBuffer data, SendHandler completion);

    Future<SendResult> sendObject(T o, SendHandler handler);

    void sendPing(ByteBuffer applicationData);

    void sendPong(ByteBuffer applicationData);
}

