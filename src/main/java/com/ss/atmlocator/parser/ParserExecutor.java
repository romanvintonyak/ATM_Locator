package com.ss.atmlocator.parser;

import com.ss.atmlocator.entity.AtmOffice;
import com.ss.atmlocator.service.DbParserService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
@Configurable
public abstract class ParserExecutor implements Job, IParser {
    final static org.slf4j.Logger logger = LoggerFactory.getLogger(ParserExecutor.class);
    protected Properties parserProperties = new Properties();

    private DbParserService parserService;


    public  void execute(JobExecutionContext executionContext){
        parserService = (DbParserService)initAppContext(executionContext).getBean("dbParserService");
        Map parameters = executionContext.getJobDetail().getJobDataMap();
        setParameter(parameters);
        int bankId = Integer.valueOf((String)parameters.get("bankid"));
        List<AtmOffice> atms = null;
        try {
            atms = parse();
            parserService.update(atms, bankId);
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
        }

    }

    /**
     * Setting parameters to parser from property file or given map
     * Properties that are set by admin page override properties from file if the names are same
     * @param parameters that will by set to parser
     */
    @Override
    public abstract void setParameter(Map<String,String> parameters);

    /**
     * Load properties from file
     * @throws IOException if can't load
     */
    public Properties loadProperties(String filename){
        try {
            Properties properties = new Properties();
            String dirPath = new ClassPathResource("parserProperties").getURI().getPath();
            String filePath = dirPath + "/" + filename;
            logger.info("Try to load properties from file " + filePath);
            InputStream propFile = new FileInputStream(filePath);
            InputStreamReader isr = new InputStreamReader(propFile, "UTF8");
            properties.load(isr);
            logger.info("File successfully loaded.");
            return properties;
        }catch (IOException ioe){
            logger.error("Loading file failed. Properties from admin page will be used");
            return new Properties();
        }
    }

    private ApplicationContext initAppContext(JobExecutionContext context){
        ApplicationContext applicationContext;
        try{
            applicationContext = (ApplicationContext)context
                    .getScheduler().getContext().get("applicationContext");
        }
        catch(SchedulerException exp){
            logger.error(exp.getMessage(),exp);
            return null;
        }
        return applicationContext;
    }


}
