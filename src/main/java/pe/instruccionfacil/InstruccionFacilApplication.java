package pe.instruccionfacil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableScheduling
public class InstruccionFacilApplication {
    public static void main(String[] args) {
        SpringApplication.run(InstruccionFacilApplication.class, args);
    }

    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
