package com.gopi.zmart;

import com.gopi.zmart.clients.producer.MockDataProducer;
import com.gopi.zmart.model.Purchase;
import com.gopi.zmart.model.PurchasePattern;
import com.gopi.zmart.model.RewardAccumulator;
import com.gopi.zmart.util.serde.StreamsSerdes;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 *  Author: Gopinathan Munappy
 *  Date : 16/11/2018
 *  Time : 11.45 AM
 *
 */

public class ZMartKafkaStreamsApp {

    private static final Logger LOG = LoggerFactory.getLogger(ZMartKafkaStreamsApp.class);

    public static void main(String[] args) throws Exception {

        Serde<Purchase> purchaseSerde = StreamsSerdes.PurchaseSerde();
        Serde<PurchasePattern> purchasePatternSerde = StreamsSerdes.PurchasePatternSerde();
        Serde<RewardAccumulator> rewardAccumulatorSerde = StreamsSerdes.RewardAccumulatorSerde();
        Serde<String> stringSerde = Serdes.String();



        StreamsBuilder streamsBuilder = new StreamsBuilder();

        KStream<String,Purchase> purchaseKStream = streamsBuilder.stream("transactions", Consumed.with(stringSerde, purchaseSerde))
                .mapValues(p -> Purchase.builder(p).maskCreditCard().build());

        KStream<String, PurchasePattern> patternKStream = purchaseKStream.mapValues(purchase -> PurchasePattern.builder(purchase).build());

        patternKStream.print(Printed.<String, PurchasePattern>toSysOut().withLabel("patterns"));
        patternKStream.to("patterns", Produced.with(stringSerde,purchasePatternSerde));

        KStream<String, RewardAccumulator> rewardsKStream = purchaseKStream.mapValues(purchase -> RewardAccumulator.builder(purchase).build());

        rewardsKStream.print(Printed.<String, RewardAccumulator>toSysOut().withLabel("rewards"));
        rewardsKStream.to("rewards", Produced.with(stringSerde,rewardAccumulatorSerde));

        purchaseKStream.print(Printed.<String, Purchase>toSysOut().withLabel("purchases"));
        purchaseKStream.to("purchases", Produced.with(stringSerde,purchaseSerde));

        // used only to produce data for this application, not typical usage
        MockDataProducer.producePurchaseData();

        KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(), getProperties());
        LOG.info("ZMart First Kafka Streams Application Started");
        kafkaStreams.start();
        Thread.sleep(65000);
        LOG.info("Shutting down the Kafka Streams Application now");
        kafkaStreams.close();
        MockDataProducer.shutdown();
    }

    private static Properties getProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.CLIENT_ID_CONFIG, "FirstZmart-Kafka-Streams-Client");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "zmart-purchases");
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "FirstZmart-Kafka-Streams-App");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class);
        return props;
    }

}