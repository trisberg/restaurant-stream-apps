/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.scdf.customer;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Corneil du Plessis
 */
@Configuration
public class Customer {
    private static final Logger logger = LoggerFactory.getLogger(Customer.class);
    private final Random random = new Random(System.currentTimeMillis());
    private final List<String> cold = Arrays.asList("water", "coke", "sprite");
    private final List<String> hot = Arrays.asList("coffee", "tea");
    private final List<String> food = Arrays.asList("burger", "pizza", "steak", "pasta");
    private final StreamBridge bridge;
    private boolean placedOrder = false;

    public Customer(StreamBridge bridge) {
        this.bridge = bridge;
    }

    public void placeOrders() {
        if (!placedOrder) {
            logger.info("placeOrder:start");
            placeOrder(cold);
            placeOrder(food);
            placeOrder(hot);
            placedOrder = true;
            logger.info("placeOrder:end");
        } else {
            logger.info("placeOrder:done");
        }
    }

    public void placeOrder(List<String> items) {
        String item = items.get(random.nextInt(items.size()));
        logger.info("placeOrder:send:order:{}", item);
        bridge.send(Events.ORDER, MessageBuilder.withPayload(item).build());
    }


    @Bean(name = Events.RECEIVE)
    public Consumer<String> receive() {
        return (String order) -> {
            String message = "money for " + order;
            logger.info("receive:{}:send:payment:{}", order, message);
            bridge.send(Events.PAYMENT, MessageBuilder.withPayload(message).build());
        };
    }

    @Bean(name = Events.OPEN)
    public Consumer<String> isOpen() {
        return (String message) -> {
            logger.info("isOpen:{}", message);
            placeOrders();
        };
    }

    public interface Events {
        String OPEN = "open";
        String ORDER = "order";
        String RECEIVE = "receive";
        String PAYMENT = "payment";
    }
}
