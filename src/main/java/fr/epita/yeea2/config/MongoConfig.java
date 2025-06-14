package fr.epita.yeea2.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    @Value("${mongo.connection}")
    private String mongoConnection;

    @Bean
    public MongoClient mongoClient() {
        // Replace <username>, <password>, and <cluster-url> with your real values
        String uri = mongoConnection;
        return MongoClients.create(uri);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        // Replace with your DB name
        return new MongoTemplate(mongoClient, "personalized-wellness");
    }
}
