package com.poc.app.frameworks.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DlqMessageRepository extends JpaRepository<DlqMessageEntity, String> {

    List<DlqMessageEntity> findByTopicOrderByReceivedAtAsc(String topic);

    List<DlqMessageEntity> findAllByOrderByReceivedAtAsc();

    @Query("SELECT DISTINCT d.topic FROM DlqMessageEntity d")
    List<String> findDistinctTopics();

    long countByTopic(String topic);

    void deleteByTopicAndId(String topic, String id);
}
