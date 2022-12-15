all: compile server client

compile:
	cd servent-compiler; sbt run

server:
	cd servent-service; mvn spring-boot:run

client:
	cd servent-ui; npm start
