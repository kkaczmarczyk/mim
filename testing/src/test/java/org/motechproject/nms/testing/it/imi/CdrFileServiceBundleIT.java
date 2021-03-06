package org.motechproject.nms.testing.it.imi;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.motechproject.alerts.contract.AlertCriteria;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.Alert;
import org.motechproject.event.MotechEvent;
import org.motechproject.nms.imi.domain.CallDetailRecord;
import org.motechproject.nms.imi.exception.InvalidCdrFileException;
import org.motechproject.nms.imi.repository.CallDetailRecordDataService;
import org.motechproject.nms.imi.repository.FileAuditRecordDataService;
import org.motechproject.nms.imi.service.CdrFileService;
import org.motechproject.nms.imi.service.SettingsService;
import org.motechproject.nms.imi.web.contract.CdrFileNotificationRequest;
import org.motechproject.nms.imi.web.contract.FileInfo;
import org.motechproject.nms.kilkari.domain.CallRetry;
import org.motechproject.nms.kilkari.dto.CallDetailRecordDto;
import org.motechproject.nms.kilkari.repository.CallRetryDataService;
import org.motechproject.nms.kilkari.repository.SubscriberDataService;
import org.motechproject.nms.kilkari.repository.SubscriptionPackDataService;
import org.motechproject.nms.kilkari.service.SubscriptionService;
import org.motechproject.nms.region.repository.CircleDataService;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.LanguageDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.service.DistrictService;
import org.motechproject.nms.testing.it.utils.CdrHelper;
import org.motechproject.nms.testing.service.TestingService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;
import javax.xml.rpc.Call;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class CdrFileServiceBundleIT extends BasePaxIT {

    private static final String PROCESS_FILES_SUBJECT = "nms.imi.kk.process_files";
    private static final String FILE_NOTIFICATION_REQUEST_PARAM_KEY = "request";
    private static final long MAX_MILLISECOND_WAIT = 2000L;

    private static final String INITIAL_RETRY_DELAY = "imi.initial_retry_delay";
    private static final String MAX_CDR_ERROR_COUNT = "imi.max_cdr_error_count";

    @Inject
    SettingsService settingsService;
    @Inject
    SubscriptionService subscriptionService;
    @Inject
    SubscriptionPackDataService subscriptionPackDataService;
    @Inject
    SubscriberDataService subscriberDataService;
    @Inject
    LanguageDataService languageDataService;
    @Inject
    AlertService alertService;
    @Inject
    CdrFileService cdrFileService;
    @Inject
    CircleDataService circleDataService;
    @Inject
    StateDataService stateDataService;
    @Inject
    DistrictDataService districtDataService;
    @Inject
    DistrictService districtService;
    @Inject
    FileAuditRecordDataService fileAuditRecordDataService;
    @Inject
    CallRetryDataService callRetryDataService;
    @Inject
    CallDetailRecordDataService callDetailRecordDataService;


    @Inject
    TestingService testingService;

    @Before
    public void cleanupDatabase() {
        testingService.clearDatabase();
    }


    private String localCdrDirBackup;
    private String remoteCdrDirBackup;
    private String localObdDirBackup;
    private String remoteObdDirBackup;
    private String initialRetryDelay;
    private String maxErrorCountBackup;

    @Before
    public void setupSettings() {
        localCdrDirBackup = ImiTestHelper.setupTestDir(settingsService, ImiTestHelper.LOCAL_CDR_DIR, "cdr-local-dir-it");
        remoteCdrDirBackup = ImiTestHelper.setupTestDir(settingsService, ImiTestHelper.REMOTE_CDR_DIR, "cdr-remote-dir-it");
        localObdDirBackup = ImiTestHelper.setupTestDir(settingsService, ImiTestHelper.LOCAL_OBD_DIR, "obd-local-dir-it");
        remoteObdDirBackup = ImiTestHelper.setupTestDir(settingsService, ImiTestHelper.REMOTE_OBD_DIR, "obd-remote-dir-it");
        initialRetryDelay = settingsService.getSettingsFacade().getProperty(INITIAL_RETRY_DELAY);
        settingsService.getSettingsFacade().setProperty(INITIAL_RETRY_DELAY, "0");
        settingsService.getSettingsFacade().setProperty(INITIAL_RETRY_DELAY, "0");
        maxErrorCountBackup = settingsService.getSettingsFacade().getProperty(MAX_CDR_ERROR_COUNT);
        settingsService.getSettingsFacade().setProperty(MAX_CDR_ERROR_COUNT, "3");
    }


    @After
    public void restoreSettings() {
        settingsService.getSettingsFacade().setProperty(ImiTestHelper.REMOTE_OBD_DIR, remoteObdDirBackup);
        settingsService.getSettingsFacade().setProperty(ImiTestHelper.LOCAL_OBD_DIR, localObdDirBackup);
        settingsService.getSettingsFacade().setProperty(ImiTestHelper.REMOTE_CDR_DIR, remoteCdrDirBackup);
        settingsService.getSettingsFacade().setProperty(ImiTestHelper.LOCAL_CDR_DIR, localCdrDirBackup);
        settingsService.getSettingsFacade().setProperty(INITIAL_RETRY_DELAY, initialRetryDelay);
        settingsService.getSettingsFacade().setProperty(MAX_CDR_ERROR_COUNT, maxErrorCountBackup);
    }


    @Test
    public void testServicePresent() {
        assertTrue(cdrFileService != null);
    }


    @Test
    public void testVerify() throws IOException, NoSuchAlgorithmException {

        CdrHelper helper = new CdrHelper(settingsService, subscriptionService, subscriberDataService,
                subscriptionPackDataService, languageDataService, circleDataService, stateDataService,
                districtDataService, fileAuditRecordDataService, districtService);

        helper.makeCdrs(1,1,1,1);
        helper.makeLocalCdrFile();
        helper.makeCsrs(1);
        helper.makeLocalCsrFile(0);
        cdrFileService.verifyDetailFileChecksumAndCount(helper.cdrFileNotificationRequest());
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testChecksumError() throws IOException, NoSuchAlgorithmException {

        CdrHelper helper = new CdrHelper(settingsService, subscriptionService, subscriberDataService,
                subscriptionPackDataService, languageDataService, circleDataService, stateDataService,
                districtDataService, fileAuditRecordDataService, districtService);

        helper.makeCdrs(1, 1, 1, 1);
        helper.makeLocalCdrFile();
        helper.makeCsrs(1);
        helper.makeLocalCsrFile();
        FileInfo cdrFileInfo = new FileInfo(helper.cdr(), "invalid checksum", helper.cdrCount());
        FileInfo csrFileInfo = new FileInfo(helper.csr(), helper.csrLocalChecksum(), helper.csrCount());

        exception.expect(IllegalStateException.class);
        CdrFileNotificationRequest request = new CdrFileNotificationRequest(
                helper.obd(),
                csrFileInfo,
                cdrFileInfo
        );
        cdrFileService.verifyDetailFileChecksumAndCount(request);
    }


    @Test
    public void testCsvErrors() throws IOException, NoSuchAlgorithmException {

        CdrHelper helper = new CdrHelper(settingsService, subscriptionService, subscriberDataService,
                subscriptionPackDataService, languageDataService, circleDataService, stateDataService,
                districtDataService, fileAuditRecordDataService, districtService);

        helper.makeCdrs(1, 1, 1, 1);
        helper.makeLocalCdrFile(2);
        helper.makeLocalCsrFile();
        try {
            cdrFileService.verifyDetailFileChecksumAndCount(helper.cdrFileNotificationRequest());
        } catch (InvalidCdrFileException e) {
            assertEquals(2, e.getMessages().size());
        }
    }


    @Test
    public void testTooManyErrors() throws IOException, NoSuchAlgorithmException {

        CdrHelper helper = new CdrHelper(settingsService, subscriptionService, subscriberDataService,
                subscriptionPackDataService, languageDataService, circleDataService, stateDataService,
                districtDataService, fileAuditRecordDataService, districtService);

        helper.makeCdrs(5, 0, 0, 0);
        helper.makeLocalCdrFile(5);
        helper.makeLocalCsrFile();
        try {
            cdrFileService.verifyDetailFileChecksumAndCount(helper.cdrFileNotificationRequest());
        } catch (InvalidCdrFileException e) {
            List<String> errors = e.getMessages();
            assertEquals(4, errors.size());
            assertEquals("The maximum number of allowed errors", errors.get(errors.size() - 1).substring(0, 36));
        }
    }


    @Test
    public void testProcess() throws IOException, NoSuchAlgorithmException, InterruptedException {

        CdrHelper helper = new CdrHelper(settingsService, subscriptionService, subscriberDataService,
                subscriptionPackDataService, languageDataService, circleDataService, stateDataService,
                districtDataService, fileAuditRecordDataService, districtService);

        helper.makeCsrs(1);
        helper.makeRemoteCsrFile();
        helper.makeLocalCsrFile();
        helper.makeCdrs(3,1,1,1);
        helper.makeRemoteCdrFile();
        helper.makeLocalCdrFile();
        Map<String, Object> eventParams = new HashMap<>();
        eventParams.put(FILE_NOTIFICATION_REQUEST_PARAM_KEY, helper.cdrFileNotificationRequest());
        MotechEvent motechEvent = new MotechEvent(PROCESS_FILES_SUBJECT, helper.cdrFileNotificationParams());
        List<String> errors = cdrFileService.processDetailFile(motechEvent);
        assertEquals(0, errors.size());

        // This is going to try to send the file processed notification back to IMI, but will fail since we
        // didn't setup a server
        AlertCriteria criteria = new AlertCriteria().byExternalId(
                helper.cdrFileNotificationRequest().getFileName()
        );
        List<Alert> alerts = alertService.search(criteria);
        assertEquals(4, alerts.size()); //three warnings plus one error

        // Fancy code that waits for all 4 CDRs and 1 CSR to be processed
        long start = System.currentTimeMillis();
        while (true) {
            //Now verify that we should be rescheduling two calls (1 CDR and 1 CSR)
            if (callRetryDataService.count() == 2) {
                getLogger().debug("Found retry record in {} ms", System.currentTimeMillis() - start);
                break;
            }

            Thread.sleep(100L);

            if (System.currentTimeMillis() - start > MAX_MILLISECOND_WAIT) {
                assertTrue("Timeout while waiting for CSR processing", false);
            }
        }

        // Verify we have both a failed CDR (weekId="w5_1") and a failed CSR (weekId="w7_1")
        List<CallRetry> retries = callRetryDataService.retrieveAll();
        assertTrue(
                (retries.get(0).getWeekId().equals("w5_1") && retries.get(1).getWeekId().equals("w7_1"))
                ||
                (retries.get(1).getWeekId().equals("w5_1") && retries.get(0).getWeekId().equals("w7_1"))
        );

        // Verify we logged the incoming CDRs in the CallDetailRecord table
        assertEquals(6, callDetailRecordDataService.count());
    }


    @Test
    public void testAggregation() throws IOException, NoSuchAlgorithmException {

        CdrHelper helper = new CdrHelper(settingsService, subscriptionService, subscriberDataService,
                subscriptionPackDataService, languageDataService, circleDataService, stateDataService,
                districtDataService, fileAuditRecordDataService, districtService);

        helper.makeSingleCallCdrs(3, true);
        List<CallDetailRecordDto> cdrs = helper.getCdrs();
        String sid1 = cdrs.get(cdrs.size()-1).getRequestId().getSubscriptionId();

        helper.makeSingleCallCdrs(4, false);
        String sid2 = cdrs.get(cdrs.size()-1).getRequestId().getSubscriptionId();

        File cdrFile = helper.makeLocalCdrFile();

        List<String> errors = cdrFileService.sendAggregatedRecords(cdrFile);

        assertEquals(0, errors.size());
    }
}
