package com.texaspoker.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.util.Properties;

class KafkaManager {

    private static Properties props = new Properties();

    KafkaManager() {

        props.put("bootstrap.servers", "sati4:9092");// Assign localhost id

        props.put("acks", "all");// Set acknowledgements for producer requests.

        props.put("retries", 0);// If the request fails, the producer can automatically retry,

        props.put("batch.size", 16384);// Specify buffer size in config

        props.put("linger.ms", 1);// Reduce the no of requests less than 0

        props.put("buffer.memory", 33554432);// The buffer.memory controls the total amount of memory available to the producer for buffering.

        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    }

    Producer<String, String> getKafkaProducer() {

        return new KafkaProducer<>(props);
    }
}
