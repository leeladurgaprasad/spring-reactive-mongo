package com.example.springreactivemongo;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Log4j2
@SpringBootApplication
public class SpringReactiveMongoApplication {

    @Bean
    public ApplicationRunner applicationRunner(MovieRepository movieRepository) {

        return args -> {
            movieRepository.deleteAll().thenMany(
            Flux.just("Yemmaya Chesave", "Guru", "hmm")
                    .map(e -> Movie.builder().id(UUID.randomUUID().toString()).title(e).build())
                    .flatMap(movieRepository::save))
                    .subscribe(log::info);
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringReactiveMongoApplication.class, args);
    }

}

@Configuration
class MovieReactiveControllerConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public RouterFunction<?> movieConfig(MovieService movieService) {
        return RouterFunctions.route()
                .GET("/movies",
                        request -> ok().body((Flux) movieService.getAllMovies(), Flux.class))
                .GET("/movies/{id}",
                        request -> ok().body((Mono) movieService.findMovieById(request.pathVariable("id")), Mono.class))
                .GET("/movie/{id}/events",
                        request -> ok().body((Flux) movieService.getEvents(request.pathVariable("id")), Flux.class))
                .build();
    }
}


@Controller
@RequestMapping("/web")
class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "Ping Success!!!";
    }

    @GetMapping(value = "/movies", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux all() {
        return movieService.getAllMovies();
    }

    @GetMapping(value = "/movie/{id}", produces="text/event-stream")
    public Mono findMovieById(@PathVariable("id") String id) {
        return movieService.findMovieById(id);
    }

    @GetMapping(value = "/movie/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux getMovieEvents(String movieId) {
        return movieService.getEvents(movieId);
    }
}

@Service
class MovieService {
    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Flux<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public void deleteAllMovies() {
        movieRepository.deleteAll();
    }

    public Mono<Movie> findMovieById(String id) {
        return movieRepository.findById(id);
    }

    public Flux<MovieEvent> getEvents(String movieId) {
        return Flux.<MovieEvent>generate(sink -> sink.next(MovieEvent.builder().movieId(movieId).dateViewed(new Date()).build()))
                .delayElements(Duration.ofSeconds(1));
    }

}


interface MovieRepository extends ReactiveCrudRepository<Movie, String> {
    Flux<Movie> findByTitle(String title);
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {
    private String movieId;
    private Date dateViewed;
}


@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
class Movie {
    @Id
    private String id;
    private String title;
}
