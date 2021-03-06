/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.transport.http.netty.sender.http2;

import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.contractimpl.DefaultHttpResponseFuture;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;
import org.wso2.transport.http.netty.message.Http2PushPromise;
import org.wso2.transport.http.netty.message.HttpCarbonResponse;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@code OutboundMsgHolder} holds data related to a single outbound invocation.
 */
public class OutboundMsgHolder {

    // Outbound request HTTPCarbonMessage
    private HTTPCarbonMessage requestCarbonMessage;
    private BlockingQueue<Http2PushPromise> promises;
    private ConcurrentHashMap<Integer, HttpCarbonResponse> pushResponsesMap;
    private HttpCarbonResponse response;

    // Future which is used to notify the response listener upon response receive
    private HttpResponseFuture responseFuture;
    private Http2ClientChannel http2ClientChannel;

    private boolean allPromisesReceived = false;

    public OutboundMsgHolder(HTTPCarbonMessage httpCarbonMessage, Http2ClientChannel http2ClientChannel) {
        this.requestCarbonMessage = httpCarbonMessage;
        this.http2ClientChannel = http2ClientChannel;
        promises = new LinkedBlockingQueue<>();
        pushResponsesMap = new ConcurrentHashMap<>();
        responseFuture = new DefaultHttpResponseFuture(this);
    }

    /**
     * Gets the outbound request {@code HTTPCarbonMessage}.
     *
     * @return request HTTPCarbonMessage
     */
    public HTTPCarbonMessage getRequest() {
        return requestCarbonMessage;
    }

    /**
     * Gets the Future which is used to notify the response listener upon response receive.
     *
     * @return the Future used to notify the response listener
     */
    public HttpResponseFuture getResponseFuture() {
        return responseFuture;
    }

    /**
     * Gets the associated {@link Http2ClientChannel}.
     *
     * @return the associated Http2ClientChannel
     */
    public Http2ClientChannel getHttp2ClientChannel() {
        return http2ClientChannel;
    }

    /**
     * Adds a {@link Http2PushPromise} message.
     *
     * @param pushPromise push promise message
     */
    void addPromise(Http2PushPromise pushPromise) {
        promises.add(pushPromise);
        responseFuture.notifyPromiseAvailability();
        responseFuture.notifyPushPromise();
    }

    /**
     * Adds a push response message.
     *
     * @param streamId  id of the stream in which the push response received
     * @param pushResponse  push response message
     */
    void addPushResponse(int streamId, HttpCarbonResponse pushResponse) {
        pushResponsesMap.put(streamId, pushResponse);
        responseFuture.notifyPushResponse(streamId, pushResponse);
    }

    /**
     * Checks whether all push promises received.
     *
     * @return  whether all push promises received
     */
    public boolean isAllPromisesReceived() {
        return allPromisesReceived;
    }

    /**
     * Mark no push promises received.
     */
    public void markNoPromisesReceived() {
        allPromisesReceived = true;
        responseFuture.notifyPromiseAvailability();
    }

    /**
     * Gets a push response received over a particular stream.
     *
     * @param steamId id of the stream in which the push response is received
     * @return the push response
     */
    public HttpCarbonResponse getPushResponse(int steamId) {
        return pushResponsesMap.get(steamId);
    }

    /**
     * Gets the response {@code HttpCarbonResponse} message.
     *
     * @return the response {@code HttpCarbonResponse} message.
     */
    public HttpCarbonResponse getResponse() {
        return response;
    }

    /**
     * Sets the response {@code HttpCarbonResponse} message.
     *
     * @param response the {@code HttpCarbonResponse} message
     */
    public void setResponse(HttpCarbonResponse response) {
        this.response = response;
        allPromisesReceived = true;
        responseFuture.notifyPromiseAvailability();      // Response received so no more promises allowed
        responseFuture.notifyHttpListener(response);
    }

    /**
     * Checks whether a push promise exists.
     *
     * @return whether a push promise exists
     */
    public boolean hasPromise() {
        return !promises.isEmpty();
    }

    /**
     * Gets the next available push promise.
     *
     * @return next available push promise
     */
    public Http2PushPromise getNextPromise() {
        return promises.poll();
    }
}

