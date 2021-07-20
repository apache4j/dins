package com.texaspoker.kafka;

import com.ilumi.util.kafka.KafkaSender;
//import org.apache.kafka.clients.producer.Producer;

public class TablesLogKafkaProducer {

    private static String topicName = "ch_tables_normal_ins";

//    private static Producer<String, String> producer = null;

    private static final KafkaSender kafkaSender = KafkaSender.getInstance().init("b-1.hp-test4-msk-primary.bpvksf.c3.kafka.ap-east-1.amazonaws.com:9092,b-2.hp-test4-msk-primary.bpvksf.c3.kafka.ap-east-1.amazonaws.com:9092", 100000000);

    /**
     * Send a set of message via the producer instance.
     *
     * @param stamp a timestamp to pack into a ProducerRecord<>
     * @param key   the key to pack into a ProducerRecord<>, temporarily using Redis.publish.CHANNEL_NAME
     * @param msg   the message to pack into a ProducerRecord<>
     */
    public static void send(long stamp, String key, String msg) {

        try {
//            KafkaSender kafkaSender = KafkaSender.getInstance().init("sati4:9092", 100000000);
            kafkaSender.send(msg, topicName);

//            if (producer == null)
//                producer = new KafkaManager().getKafkaProducer();
//
//            producer.send(new ProducerRecord<>(topicName, 0, stamp, key, msg));

        } catch (Exception e) {
//            if (producer != null)
//                producer.close();
//            producer = null;
            e.printStackTrace();
        }
    }
}  
