package es.unizar.tmdad.lab2.domain;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface TweetBDRepository extends CrudRepository<TweetBD, Long> {

	List<TweetBD> findByQueryOrderByIdDesc(String query);
}
