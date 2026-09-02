FROM eclipse-temurin:25-jdk-alpine-3.24 AS builder
WORKDIR /build

COPY mvnw ./
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY src/ src/

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests -B
RUN mv target/*.jar application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted


FROM eclipse-temurin:25-jre-alpine-3.24
WORKDIR /application

RUN addgroup -S app && adduser -S app -G app

COPY --from=builder --chown=app:app /build/extracted/dependencies/ ./
COPY --from=builder --chown=app:app /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /build/extracted/application/ ./

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "application.jar"]
