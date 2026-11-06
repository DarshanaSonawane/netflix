package org.example.videoservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.videoservice.event.VideoUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.video-encoded}")
    private String videoEncodedTopic;

    @Bean
    public NewTopic videoEncodedTopic() {
        return TopicBuilder.name(videoEncodedTopic).partitions(3).replicas(1).build();
    }

    @Value("${kafka.topic.video-uploaded}")
    private String videoUploadedTopic;

    @Bean
    public ProducerFactory<String, VideoUploadedEvent> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // Don't send Java class names in headers, because content-service has its own event class
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Created automatically on startup if it doesn't exist
    @Bean
    public NewTopic videoUploadedTopic() {
        return TopicBuilder.name(videoUploadedTopic).partitions(3).replicas(1).build();
    }
}