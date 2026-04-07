package me.thinkcat.opic.practice.entrypoint;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.internal.info.MigrationInfoDumper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
public class FlywayMigrationApplication {

    final static String PWD = System.getProperty("user.dir");
    final static String RESULT_DIRECTORY = PWD + "/build/flyway/";
    final static String RESULT_FILE = "flyway-migration-results.txt";

    public static void main(String[] args) throws IOException {
        SpringApplication app = new SpringApplication(FlywayMigrationApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);

        Flyway flyway = ctx.getBean(Flyway.class);
        String str = getFlyWayResult(flyway);

        Files.createDirectories(Path.of(RESULT_DIRECTORY));
        File file = new File(RESULT_DIRECTORY + RESULT_FILE);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write(str); bw.close();
    }

    public static String getFlyWayResult(Flyway flyway) {
        MigrationInfo[] applied = flyway.info().applied();
        String str = MigrationInfoDumper.dumpToAsciiTable(applied);
        return str;
    }
}