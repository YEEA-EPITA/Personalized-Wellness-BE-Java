package fr.epita.yeea2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "fr.epita.yeea2") // or whatever top-level package
public class Yeea2Application {

    public static void main(String[] args) {
        SpringApplication.run(Yeea2Application.class, args);
    }

}
