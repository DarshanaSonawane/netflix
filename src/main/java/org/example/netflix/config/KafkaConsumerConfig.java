package org.example.netflix.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.netflix.event.VideoEncodedEvent;
import org.example.netflix.event.VideoUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private Map<String, Object> consumerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }

    @Bean
    public ConsumerFactory<String, VideoUploadedEvent> uploadedEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProperties(), new StringDeserializer(),
                new JacksonJsonDeserializer<>(VideoUploadedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VideoUploadedEvent> uploadedEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, VideoUploadedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(uploadedEventConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, VideoEncodedEvent> encodedEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProperties(), new StringDeserializer(),
                new JacksonJsonDeserializer<>(VideoEncodedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VideoEncodedEvent> encodedEventListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, VideoEncodedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(encodedEventConsumerFactory());
        return factory;
    }
}
