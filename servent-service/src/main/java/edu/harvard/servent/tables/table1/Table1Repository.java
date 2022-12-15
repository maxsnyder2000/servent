package edu.harvard.servent.tables.table1;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface Table1Repository extends JpaRepository<Table1Row, UUID> {
	Table1Row findByIdPublic(UUID idPublic);
}
