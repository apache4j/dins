package com.texaspoker.kafka;

import com.ilumi.util.kafka.KafkaSender;

/**
 * @author ：piness
 * @date ：Created in 2020-11-23 18:53
 * @description：
 * @modified By：
 * @version: $
 */
public class ButtonLogKafkaProducer {
    private static String topicName = "ch_button_normal_ins";

    private static final KafkaSender kafkaSender = KafkaSender.getInstance().init("b-1.hp-test4-msk-primary.bpvksf.c3.kafka.ap-east-1.amazonaws.com:9092,b-2.hp-test4-msk-primary.bpvksf.c3.kafka.ap-east-1.amazonaws.com:9092", 100000000);

    public static void send(String msg) {

        try {
            kafkaSender.send(msg, topicName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
