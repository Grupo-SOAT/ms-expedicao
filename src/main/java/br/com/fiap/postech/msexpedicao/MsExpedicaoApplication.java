package br.com.fiap.postech.msexpedicao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class MsExpedicaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsExpedicaoApplication.class, args);
    }
}
