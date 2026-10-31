package com.yusdev.procuraduriaprep.study.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yusdev.procuraduriaprep.study.entity.Topic;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

	Page<Topic> findAllByModuleId(UUID moduleId, Pageable pageable);

	boolean existsByModuleIdAndNameIgnoreCase(UUID moduleId, String name);
}
