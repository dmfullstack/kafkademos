package com.gopi.kafka.serialization.custom;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class UserConsumerThread implements Runnable {

  private final KafkaConsumer<String, User> consumer;
  private final String topic;

  public UserConsumerThread(String brokers, String groupId, String topic) {
    Properties prop = createConsumerConfig(brokers, groupId);
    this.consumer = new KafkaConsumer<>(prop);
    this.topic = topic;
    this.consumer.subscribe(Arrays.asList(this.topic));
  }

  private static Properties createConsumerConfig(String brokers, String groupId) {
    Properties props = new Properties();
    props.put("bootstrap.servers", brokers);
    props.put("group.id", groupId);
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms", "1000");
    props.put("session.timeout.ms", "30000");
    props.put("auto.offset.reset", "earliest");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "com.gopi.kafka.serialization.custom.UserDeserializer");
    return props;
  }

  @Override
  public void run() {
    while (true) {
      ConsumerRecords<String, User> records = consumer.poll(100);
      for (final ConsumerRecord<String, User> record : records) {
        System.out.println("Receive: " + record.value().toString());
      }
    }
  }

}
