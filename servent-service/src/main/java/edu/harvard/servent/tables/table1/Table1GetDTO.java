package edu.harvard.servent.tables.table1;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Table1GetDTO {
	public UUID id;
	public Long column1;
	public String column2;
	public Integer column3;

	public Table1GetDTO(Table1Row row) {
		this.id = row.idPublic;
		this.column1 = row.column1;
		this.column2 = row.column2;
		this.column3 = row.column3;
	}

	public static List<Table1GetDTO> list(List<Table1Row> rows) {
		return rows.stream().sorted().map((row) -> new Table1GetDTO(row)).collect(Collectors.toList());
	}
}
