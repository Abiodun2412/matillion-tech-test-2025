# Matillion Technical Test 2025

A Java Spring Boot REST API developed as part of the Matillion 2025 placement technical assessment.

The service ingests CSV data, analyses its contents, generates profiling information, and persists the results using an H2 database.

## What I Implemented

- CSV ingestion through a REST API
- Analysis of uploaded datasets
- Unique-value counting
- Column profiling and metadata generation
- Persistence of analysis results
- Automated testing using JUnit 5
- API documentation and manual testing through Swagger UI

## Technology

- Java 21
- Spring Boot 3
- Gradle
- H2 Database
- JPA
- Lombok
- JUnit 5
- Swagger / OpenAPI

## API

The main endpoint accepts CSV data for analysis:

```text
POST /api/analysis/ingestCsv
```

The service processes the supplied dataset and produces profiling information based on its contents.

## What I Learned

Completing this technical assessment helped me develop my skills in:

- Building REST APIs with Spring Boot
- Structuring backend services
- Processing and analysing CSV data
- Persisting application data using JPA and H2
- Writing automated tests with JUnit
- Working within an existing codebase and technical specification
- Using Gradle to manage and build a Java application

## Getting Started

### Prerequisites

- Java 21

### Clone the repository

```bash
git clone https://github.com/Abiodun2412/matillion-tech-test-2025.git
cd matillion-tech-test-2025
```

### Build the project

```bash
./gradlew build
```

On Windows:

```bash
gradlew.bat build
```

### Run the application

```bash
./gradlew bootRun
```

On Windows:

```bash
gradlew.bat bootRun
```

The application will run at:

```text
http://localhost:8080
```

## Running Tests

```bash
./gradlew test
```

On Windows:

```bash
gradlew.bat test
```

## Swagger UI

Once the application is running, the API can be explored through Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```
