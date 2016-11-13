package com.yet.spring.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.yet.spring.core.beans.Client;
import com.yet.spring.core.beans.Event;
import com.yet.spring.core.loggers.CacheFileEventLogger;
import com.yet.spring.core.loggers.CombinedEventLogger;
import com.yet.spring.core.loggers.EventLogger;
import com.yet.spring.core.loggers.FileEventLogger;
import com.yet.spring.core.spring.AppConfig;
import com.yet.spring.core.spring.DBConfig;
import com.yet.spring.core.spring.LoggerConfig;

public class TestContext {
    
    @BeforeClass
    public static void initTestDbProps() {
        System.setProperty("DB_PROPS", "classpath:db_for_test.properties");
    }

    @Test
    public void testPropertyPlaceholderSystemOverride() {
        System.setProperty("id", "35");

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(AppConfig.class);
        ctx.refresh();

        Client client = ctx.getBean(Client.class);
        ctx.close();

        assertEquals("35", client.getId());
    }

    @Test
    public void testFileEventLoggerEventsFileSysPropValue() throws IOException {
        File file = File.createTempFile("test", "FileEventLogger");
        file.deleteOnExit();

        assertFileEventLogger(file);
    }

    @Test
    public void testFileEventLoggerEventsFileDefaultValue() throws IOException {
        File file = new File("target/events_log.txt");
        assertFileEventLogger(file);
    }

    private void assertFileEventLogger(File file) throws IOException {
        System.setProperty("events.file", file.getAbsolutePath());

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(LoggerConfig.class, DBConfig.class);
        ctx.scan(FileEventLogger.class.getPackage().getName());
        ctx.refresh();

        EventLogger logger = ctx.getBean("fileEventLogger",
                FileEventLogger.class);
        Event event = new Event();
        String uuid = UUID.randomUUID().toString();
        event.setMsg(uuid);
        logger.logEvent(event);

        ctx.close();

        String str = FileUtils.readFileToString(file);
        assertTrue(str.contains(uuid));
    }
    
    @Test
    public void testLoggersNames() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(LoggerConfig.class, DBConfig.class);
        ctx.scan(FileEventLogger.class.getPackage().getName());
        ctx.refresh();
        
        EventLogger fileLogger = ctx.getBean("fileEventLogger", FileEventLogger.class);
        EventLogger cacheLogger = ctx.getBean("cacheFileEventLogger", CacheFileEventLogger.class);
        CombinedEventLogger combinedLogger = ctx.getBean(CombinedEventLogger.class);
        
        assertEquals(fileLogger.getName() + " with cache", cacheLogger.getName());
        
        Collection<String> combinedNames = combinedLogger.getLoggers().stream()
                .map(v -> v.getName()).collect(Collectors.toList());
        
        assertEquals("Combined " + combinedNames, combinedLogger.getName());
        
        ctx.close();
    }

}
