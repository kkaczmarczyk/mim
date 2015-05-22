package org.motechproject.nms.imi.it;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.nms.imi.service.CdrFileService;
import org.motechproject.nms.imi.service.SettingsService;
import org.motechproject.nms.imi.service.contract.ParseResults;
import org.motechproject.nms.imi.web.contract.FileInfo;
import org.motechproject.nms.kilkari.domain.Subscription;
import org.motechproject.nms.kilkari.domain.SubscriptionStatus;
import org.motechproject.nms.kilkari.repository.CallRetryDataService;
import org.motechproject.nms.kilkari.repository.SubscriberDataService;
import org.motechproject.nms.kilkari.repository.SubscriptionDataService;
import org.motechproject.nms.kilkari.service.SubscriptionService;
import org.motechproject.nms.region.repository.CircleDataService;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.LanguageDataService;
import org.motechproject.nms.region.repository.LanguageLocationDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class CdrFileServiceBundleIT extends BasePaxIT {

    @Inject
    private SettingsService settingsService;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private SubscriptionDataService subscriptionDataService;

    @Inject
    private SubscriberDataService subscriberDataService;

    @Inject
    private LanguageDataService languageDataService;

    @Inject
    private CallRetryDataService callRetryDataService;

    @Inject
    private AlertService alertService;

    @Inject
    CdrFileService cdrFileService;

    @Inject
    private LanguageLocationDataService languageLocationDataService;

    @Inject
    private CircleDataService circleDataService;

    @Inject
    private StateDataService stateDataService;

    @Inject
    private DistrictDataService districtDataService;

    @Before
    public void cleanupDatabase() {
        for (Subscription subscription: subscriptionDataService.retrieveAll()) {
            subscription.setStatus(SubscriptionStatus.COMPLETED);
            subscription.setEndDate(new DateTime().withDate(2011, 8, 1));

            subscriptionDataService.update(subscription);
        }

        subscriptionService.deleteAll();
        subscriberDataService.deleteAll();
        languageLocationDataService.deleteAll();
        languageDataService.deleteAll();
        districtDataService.deleteAll();
        stateDataService.deleteAll();
        circleDataService.deleteAll();
        callRetryDataService.deleteAll();
    }


    @Test
    public void testServicePresent() {
        getLogger().debug("testServicePresent()");
        assertTrue(cdrFileService != null);
    }


    @Test
    public void testParse() throws IOException, NoSuchAlgorithmException {
        getLogger().debug("testParse()");

        CdrHelper helper = new CdrHelper(settingsService, subscriptionService, subscriberDataService,
                languageDataService, languageLocationDataService, circleDataService, stateDataService,
                districtDataService);

        helper.makeCdrs(1,1,1,1);
        helper.makeCdr();
        FileInfo fileInfo = new FileInfo(helper.cdr(), helper.cdrChecksum(), helper.cdrCount());
        ParseResults result = cdrFileService.parseDetailFile(fileInfo);
        assertEquals(4, result.getRecords().size());
    }


    @Test
    public void testProcess() throws IOException, NoSuchAlgorithmException {
        getLogger().debug("testProcess()");

        CdrHelper helper = new CdrHelper(settingsService, subscriptionService, subscriberDataService,
                languageDataService, languageLocationDataService, circleDataService, stateDataService,
                districtDataService);

        helper.makeCdrs(1,1,1,1);
        helper.makeCdr();
        FileInfo fileInfo = new FileInfo(helper.cdr(), helper.cdrChecksum(), helper.cdrCount());
        ParseResults result = cdrFileService.parseDetailFile(fileInfo);
        assertEquals(4, result.getRecords().size());
    }

    //todo: test how successfully we aggregate detail records into a summary record
}
