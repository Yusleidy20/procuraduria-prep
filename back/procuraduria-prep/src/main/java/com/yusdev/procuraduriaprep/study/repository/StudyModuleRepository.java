package com.yusdev.procuraduriaprep.study.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yusdev.procuraduriaprep.study.entity.StudyModule;

public interface StudyModuleRepository extends JpaRepository<StudyModule, UUID> {

	boolean existsByNameIgnoreCase(String name);
}
