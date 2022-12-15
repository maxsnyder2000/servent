package edu.harvard.servent.tables.table1;

import java.sql.Timestamp;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "Table1")
public class Table1Row implements Comparable<Table1Row> {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public UUID id;

	@Column(name = "idPublic")
	public UUID idPublic;

	@Column(name = "Column1")
	public Long column1;

	@Column(name = "Column2")
	public String column2;

	@Column(name = "Column3")
	public Integer column3;

	@CreationTimestamp
	private Timestamp creationTimestamp;

	@Override
	public int compareTo(Table1Row row) {
		return this.creationTimestamp.compareTo(row.creationTimestamp);
	}
}
