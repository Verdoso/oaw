/*******************************************************************************
* Copyright (C) 2012 INTECO, Instituto Nacional de Tecnologías de la Comunicación, 
* This program is licensed and may be used, modified and redistributed under the terms
* of the European Public License (EUPL), either version 1.2 or (at your option) any later 
* version as soon as they are approved by the European Commission.
* Unless required by applicable law or agreed to in writing, software distributed under the 
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
* ANY KIND, either express or implied. See the License for the specific language governing 
* permissions and more details.
* You should have received a copy of the EUPL1.2 license along with this program; if not, 
* you may find it at http://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017D0863
* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
* Modificaciones: MINHAFP (Ministerio de Hacienda y Función Pública) 
* Email: observ.accesibilidad@correo.gob.es
******************************************************************************/
package es.inteco.rastreador2.servlets;

import es.inteco.common.logging.Logger;
import es.inteco.crawler.common.Constants;
import es.inteco.plugin.dao.DataBaseManager;
import es.inteco.rastreador2.actionform.cuentausuario.PeriodicidadForm;
import es.inteco.rastreador2.actionform.observatorio.ObservatorioForm;
import es.inteco.rastreador2.dao.observatorio.ObservatorioDAO;
import es.inteco.rastreador2.job.ExecuteScheduledObservatory;
import es.inteco.rastreador2.utils.CrawlerUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.List;

public class ScheduleObservatoryServlet extends GenericServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static Scheduler scheduler;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        final SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        try {
            scheduler = schedulerFactory.getScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            Logger.putLog("FALLO al iniciar el scheduler de los observatorios", ScheduleObservatoryServlet.class, Logger.LOG_LEVEL_ERROR);
        }

        Logger.putLog("Programando los observatorios", ScheduleObservatoryServlet.class, Logger.LOG_LEVEL_INFO);

        try (Connection c = DataBaseManager.getConnection()) {
            final List<ObservatorioForm> observatories = ObservatorioDAO.getObservatoryList(c);

            for (ObservatorioForm observatory : observatories) {
                scheduleJob(observatory.getNombre(), observatory.getId(),
                        new Date(observatory.getFecha_inicio().getTime()), observatory.getPeriodicidadForm(), observatory.getCartucho().getId());
            }
        } catch (Exception e) {
            Logger.putLog("FALLO al programar los jobs para los rastreos de observatorios", ScheduleObservatoryServlet.class, Logger.LOG_LEVEL_ERROR, e);
        }
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
    }

    @Override
    public void destroy() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            Logger.putLog("FALLO al desprogramar los jobs para los rastreos de observatorios (puede producir fallo de memoria)", ScheduleObservatoryServlet.class, Logger.LOG_LEVEL_ERROR, e);
        }
        super.destroy();
    }

    public static void scheduleJob(String observatoryName, Long observatoryId, Date observatoryDate, PeriodicidadForm periodicidadForm, Long idCartridge) {
        Logger.putLog("Programando el job para el observatorio " + observatoryName, ScheduleObservatoryServlet.class, Logger.LOG_LEVEL_INFO);

        try {
            JobDetail jobDetail = new JobDetail(Constants.EXECUTE_SCHEDULED_OBSERVATORY + "_" + observatoryId,
                    "ExecuteScheduledObservatory",
                    ExecuteScheduledObservatory.class);

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(Constants.OBSERVATORY_ID, observatoryId);
            jobDataMap.put(Constants.CARTRIDGE_ID, idCartridge);
            jobDetail.setJobDataMap(jobDataMap);

            final Trigger trigger;
            final String triggerName = Constants.EXECUTE_SCHEDULED_OBSERVATORY_TRIGGER + "_" + observatoryId;
            if (StringUtils.isNotEmpty(periodicidadForm.getCronExpression())) {
                String cronExpression = CrawlerUtils.getCronExpression(observatoryDate, periodicidadForm.getCronExpression());
                Logger.putLog("Estableciendo expresión de cron: " + cronExpression, ScheduleObservatoryServlet.class, Logger.LOG_LEVEL_INFO);
                trigger = new CronTrigger(triggerName, Constants.CRAWLER_JOB_TRIGGER_GROUP, cronExpression);
            } else {
                trigger = new SimpleTrigger(triggerName,
                        Constants.CRAWLER_JOB_TRIGGER_GROUP, observatoryDate, new Date(Long.MAX_VALUE), SimpleTrigger.REPEAT_INDEFINITELY,
                        (long) periodicidadForm.getDias() * 24 * 60 * 60 * 1000);
            }

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            Logger.putLog("Error al programar el job", ScheduleObservatoryServlet.class, Logger.LOG_LEVEL_ERROR, e);
        }
    }

    public static void deleteJob(Long observatoryId) throws Exception {
        scheduler.deleteJob(Constants.EXECUTE_SCHEDULED_OBSERVATORY + "_" + observatoryId, "ExecuteScheduledObservatory");
    }

}
