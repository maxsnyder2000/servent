package edu.harvard.servent.tables.table1;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Table1Service {
	@Autowired
	Table1Repository repository;

	public void delete() {
		repository.deleteAll();
	}

	public List<Table1GetDTO> get() {
		return Table1GetDTO.list(repository.findAll());
	}

	public void patch(Table1PatchDTO dto) {
		repository.save(dto.patch(repository.findByIdPublic(dto.id)));
	}

	public void post(Table1PostDTO dto) {
		repository.save(dto.row());
	}
}
