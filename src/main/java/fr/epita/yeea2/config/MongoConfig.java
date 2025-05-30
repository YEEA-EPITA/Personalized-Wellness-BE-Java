package fr.epita.yeea2.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {
        // Replace <username>, <password>, and <cluster-url> with your real values
        String uri = "mongodb+srv://emmydinh:123456a%40@personalized-wellness-p.ruft5zh.mongodb.net/?retryWrites=true&w=majority";
        return MongoClients.create(uri);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        // Replace with your DB name
        return new MongoTemplate(mongoClient, "personalized-wellness");
    }
}
