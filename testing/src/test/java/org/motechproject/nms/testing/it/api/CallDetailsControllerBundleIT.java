package org.motechproject.nms.testing.it.api;

import com.google.common.base.Joiner;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.nms.api.web.contract.FlwUserResponse;
import org.motechproject.nms.flw.domain.CallContent;
import org.motechproject.nms.flw.domain.CallDetailRecord;
import org.motechproject.nms.flw.domain.FrontLineWorker;
import org.motechproject.nms.flw.domain.FrontLineWorkerStatus;
import org.motechproject.nms.flw.repository.CallDetailRecordDataService;
import org.motechproject.nms.flw.service.CallDetailRecordService;
import org.motechproject.nms.flw.service.FrontLineWorkerService;
import org.motechproject.nms.props.domain.DeployedService;
import org.motechproject.nms.props.domain.Service;
import org.motechproject.nms.props.repository.DeployedServiceDataService;
import org.motechproject.nms.region.repository.CircleDataService;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.LanguageDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.motechproject.nms.region.service.DistrictService;
import org.motechproject.nms.testing.it.utils.RegionHelper;
import org.motechproject.nms.testing.service.TestingService;
import org.motechproject.testing.osgi.BasePaxIT;
import org.motechproject.testing.osgi.container.MotechNativeTestContainerFactory;
import org.motechproject.testing.osgi.http.SimpleHttpClient;
import org.motechproject.testing.utils.TestContext;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@ExamFactory(MotechNativeTestContainerFactory.class)
public class CallDetailsControllerBundleIT extends BasePaxIT {
    private static final String ADMIN_USERNAME = "motech";
    private static final String ADMIN_PASSWORD = "motech";

    private static final int MILLISECONDS_PER_SECOND = 1000;

    @Inject
    CallDetailRecordService callDetailRecordService;

    @Inject
    CallDetailRecordDataService callDetailRecordDataService;

    @Inject
    FrontLineWorkerService frontLineWorkerService;

    @Inject
    TestingService testingService;

    @Inject
    DeployedServiceDataService deployedServiceDataService;

    @Inject
    LanguageDataService languageDataService;
    @Inject
    CircleDataService circleDataService;
    @Inject
    StateDataService stateDataService;
    @Inject
    DistrictDataService districtDataService;
    @Inject
    DistrictService districtService;

    private RegionHelper rh;


    @Before
    public void clearDatabase() {
        testingService.clearDatabase();
        rh = new RegionHelper(languageDataService, circleDataService, stateDataService, districtDataService,
                districtService);
    }

    /**
     * method to pass all values as per data type mentioned in API
     */
    private String createCallDetailsJson(boolean includeCallingNumber, Long callingNumber,
                                         boolean includeCallId, Long callId,
                                         boolean includeOperator, String operator,
                                         boolean includeCircle, String circle,
                                         boolean includeCallStartTime, Long callStartTime,
                                         boolean includeCallEndTime, Long callEndTime,
                                         boolean includeCallDurationInPulses, Integer callDurationInPulses,
                                         boolean includeEndOfUsagePromptCounter, Integer endOfUsagePromptCounter,
                                         boolean includeWelcomeMessagePromptFlag, Boolean welcomeMessagePromptFlag,
                                         boolean includeCallStatus, Integer callStatus,
                                         boolean includeCallDisconnectReason, Integer callDisconnectReason,
                                         boolean includeContet, String content) {
        StringBuffer contentTemplate = new StringBuffer("{");
        ArrayList<String> array = new ArrayList<>();

        if (includeCallingNumber) {
            array.add(String.format("\"callingNumber\": %s", callingNumber));
        }

        if (includeCallId) {
            array.add(String.format("\"callId\": %s", callId));
        }

        if (includeOperator) {
            array.add(String.format("\"operator\": \"%s\"", operator));
        }

        if (includeCircle) {
            array.add(String.format("\"circle\": \"%s\"", circle));
        }

        if (includeCallStartTime) {
            array.add(String.format("\"callStartTime\": %s", callStartTime));
        }

        if (includeCallEndTime) {
            array.add(String.format("\"callEndTime\": %s", callEndTime));
        }

        if (includeCallDurationInPulses) {
            array.add(String.format("\"callDurationInPulses\": %s", callDurationInPulses));
        }

        if (includeEndOfUsagePromptCounter) {
            array.add(String.format("\"endOfUsagePromptCounter\": %s", endOfUsagePromptCounter));
        }

        if (includeWelcomeMessagePromptFlag) {
            array.add(String.format("\"welcomeMessagePromptFlag\": %s", welcomeMessagePromptFlag));
        }

        if (includeCallStatus) {
            array.add(String.format("\"callStatus\": %s", callStatus));
        }

        if (includeCallDisconnectReason) {
            array.add(String.format("\"callDisconnectReason\": %s", callDisconnectReason));
        }

        if (includeContet) {
            array.add(String.format("\"content\": [%s]", content));
        }

        contentTemplate.append(Joiner.on(",").join(array));
        contentTemplate.append("}");

        return contentTemplate.toString();
    }

    /**
     * overloaded method to pass all values as String data type i.e in double
     * quotes ""
     */
    private String createCallDetailsJson(boolean includeCallingNumber,
            String callingNumber, boolean includeCallId, String callId,
            boolean includeOperator, String operator, boolean includeCircle,
            String circle, boolean includeCallStartTime, String callStartTime,
            boolean includeCallEndTime, String callEndTime,
            boolean includeCallDurationInPulses, String callDurationInPulses,
            boolean includeEndOfUsagePromptCounter,
            String endOfUsagePromptCounter,
            boolean includeWelcomeMessagePromptFlag,
            String welcomeMessagePromptFlag, boolean includeCallStatus,
            String callStatus, boolean includeCallDisconnectReason,
            String callDisconnectReason, boolean includeContet, String content) {
        StringBuffer contentTemplate = new StringBuffer("{");
        ArrayList<String> array = new ArrayList<>();

        if (includeCallingNumber) {
            array.add(String.format("\"callingNumber\": \"%s\"", callingNumber));
        }

        if (includeCallId) {
            array.add(String.format("\"callId\": \"%s\"", callId));
        }

        if (includeOperator) {
            array.add(String.format("\"operator\": \"%s\"", operator));
        }

        if (includeCircle) {
            array.add(String.format("\"circle\": \"%s\"", circle));
        }

        if (includeCallStartTime) {
            array.add(String.format("\"callStartTime\": \"%s\"", callStartTime));
        }

        if (includeCallEndTime) {
            array.add(String.format("\"callEndTime\": \"%s\"", callEndTime));
        }

        if (includeCallDurationInPulses) {
            array.add(String.format("\"callDurationInPulses\": \"%s\"",
                    callDurationInPulses));
        }

        if (includeEndOfUsagePromptCounter) {
            array.add(String.format("\"endOfUsagePromptCounter\": \"%s\"",
                    endOfUsagePromptCounter));
        }

        if (includeWelcomeMessagePromptFlag) {
            array.add(String.format("\"welcomeMessagePromptFlag\": \"%s\"",
                    welcomeMessagePromptFlag));
        }

        if (includeCallStatus) {
            array.add(String.format("\"callStatus\": \"%s\"", callStatus));
        }

        if (includeCallDisconnectReason) {
            array.add(String.format("\"callDisconnectReason\": \"%s\"",
                    callDisconnectReason));
        }

        if (includeContet) {
            array.add(String.format("\"content\": [%s]", content));
        }

        contentTemplate.append(Joiner.on(",").join(array));
        contentTemplate.append("}");

        return contentTemplate.toString();
    }

    private HttpPost createCallDetailsPost(String serviceName,
                                   boolean includeCallingNumber, Long callingNumber,
                                   boolean includeCallId, Long callId,
                                   boolean includeOperator, String operator,
                                   boolean includeCircle, String circle,
                                   boolean includeCallStartTime, Long callStartTime,
                                   boolean includeCallEndTime, Long callEndTime,
                                   boolean includeCallDurationInPulses, Integer callDurationInPulses,
                                   boolean includeEndOfUsagePromptCounter, Integer endOfUsagePromptCounter,
                                   boolean includeWelcomeMessagePromptFlag, Boolean welcomeMessagePromptFlag,
                                   boolean includeCallStatus, Integer callStatus,
                                   boolean includeCallDisconnectReason, Integer callDisconnectReason,
                                   boolean includeContet, String content) {
        HttpPost httpPost = new HttpPost(String.format("http://localhost:%d/api/%s/callDetails",
                TestContext.getJettyPort(), serviceName));
        String callDetailsJson = createCallDetailsJson(includeCallingNumber, callingNumber, includeCallId,
                callId, includeOperator, operator, includeCircle, circle, includeCallStartTime, callStartTime,
                includeCallEndTime, callEndTime, includeCallDurationInPulses, callDurationInPulses,
                includeEndOfUsagePromptCounter, endOfUsagePromptCounter, includeWelcomeMessagePromptFlag,
                welcomeMessagePromptFlag, includeCallStatus, callStatus, includeCallDisconnectReason,
                callDisconnectReason, includeContet, content);
        StringEntity params;
        try {
            params = new StringEntity(callDetailsJson);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("We're no expecting this kind of exception in ITs!", e);
        }
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");
        return httpPost;
    }

    /**
     * overloaded method to pass all values as String type
     */
    private HttpPost createCallDetailsPost(String serviceName,
            boolean includeCallingNumber, String callingNumber,
            boolean includeCallId, String callId, boolean includeOperator,
            String operator, boolean includeCircle, String circle,
            boolean includeCallStartTime, String callStartTime,
            boolean includeCallEndTime, String callEndTime,
            boolean includeCallDurationInPulses, String callDurationInPulses,
            boolean includeEndOfUsagePromptCounter,
            String endOfUsagePromptCounter,
            boolean includeWelcomeMessagePromptFlag,
            String welcomeMessagePromptFlag, boolean includeCallStatus,
            String callStatus, boolean includeCallDisconnectReason,
            String callDisconnectReason, boolean includeContet, String content) {
        HttpPost httpPost = new HttpPost(String.format(
                "http://localhost:%d/api/%s/callDetails",
                TestContext.getJettyPort(), serviceName));
        String callDetailsJson = createCallDetailsJson(includeCallingNumber,
                callingNumber, includeCallId, callId, includeOperator,
                operator, includeCircle, circle, includeCallStartTime,
                callStartTime, includeCallEndTime, callEndTime,
                includeCallDurationInPulses, callDurationInPulses,
                includeEndOfUsagePromptCounter, endOfUsagePromptCounter,
                includeWelcomeMessagePromptFlag, welcomeMessagePromptFlag,
                includeCallStatus, callStatus, includeCallDisconnectReason,
                callDisconnectReason, includeContet, content);
        StringEntity params;
        try {
            params = new StringEntity(callDetailsJson);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                    "We're no expecting this kind of exception in ITs!", e);
        }
        httpPost.setEntity(params);
        httpPost.addHeader("content-type", "application/json");
        return httpPost;
    }
    
    /**
     * method to pass all values as per data type mentioned in API
     */
    private String createContentJson(boolean includeType, String type,
                                     boolean includeMkCardCode, String mkCardCode,
                                     boolean includeContentName, String contentName,
                                     boolean includeContentFileName, String contentFileName,
                                     boolean includeStartTime, Long startTime,
                                     boolean includeEndTime, Long endTime,
                                     boolean includeCompletionFlag, Boolean completionFlag,
                                     boolean includeCorrectAnswerEntered, Boolean correctAnswerEntered) {
        StringBuffer contentTemplate = new StringBuffer("{");
        ArrayList<String> array = new ArrayList<>();

        if (includeType) {
            array.add(String.format("\"type\": \"%s\"", type));
        }

        if (includeMkCardCode) {
            array.add(String.format("\"mkCardCode\": \"%s\"", mkCardCode));
        }

        if (includeContentName) {
            array.add(String.format("\"contentName\": \"%s\"", contentName));
        }

        if (includeContentFileName) {
            array.add(String.format("\"contentFileName\": \"%s\"", contentFileName));
        }

        if (includeStartTime) {
            array.add(String.format("\"startTime\": %s", startTime));
        }

        if (includeEndTime) {
            array.add(String.format("\"endTime\": %s", endTime));
        }

        if (includeCompletionFlag) {
            array.add(String.format("\"completionFlag\": %s", completionFlag));
        }

        if (includeCorrectAnswerEntered) {
            array.add(String.format("\"correctAnswerEntered\": %s", correctAnswerEntered));
        }

        contentTemplate.append(Joiner.on(",").join(array));
        contentTemplate.append("}");

        return contentTemplate.toString();
    }

    /**
     * overloaded method to pass all values as String type
     */
    private String createContentJson(boolean includeType, String type,
            boolean includeMkCardCode, String mkCardCode,
            boolean includeContentName, String contentName,
            boolean includeContentFileName, String contentFileName,
            boolean includeStartTime, String startTime, boolean includeEndTime,
            String endTime, boolean includeCompletionFlag,
            String completionFlag, boolean includeCorrectAnswerEntered,
            String correctAnswerEntered) {
        StringBuffer contentTemplate = new StringBuffer("{");
        ArrayList<String> array = new ArrayList<>();

        if (includeType) {
            array.add(String.format("\"type\": \"%s\"", type));
        }

        if (includeMkCardCode) {
            array.add(String.format("\"mkCardCode\": \"%s\"", mkCardCode));
        }

        if (includeContentName) {
            array.add(String.format("\"contentName\": \"%s\"", contentName));
        }

        if (includeContentFileName) {
            array.add(String.format("\"contentFileName\": \"%s\"",
                    contentFileName));
        }

        if (includeStartTime) {
            array.add(String.format("\"startTime\":  \"%s\"", startTime));
        }

        if (includeEndTime) {
            array.add(String.format("\"endTime\":  \"%s\"", endTime));
        }

        if (includeCompletionFlag) {
            array.add(String.format("\"completionFlag\":  \"%s\"",
                    completionFlag));
        }

        if (includeCorrectAnswerEntered) {
            array.add(String.format("\"correctAnswerEntered\":  \"%s\"",
                    correctAnswerEntered));
        }

        contentTemplate.append(Joiner.on(",").join(array));
        contentTemplate.append("}");

        return contentTemplate.toString();
    }

    private String createFlwUserResponseJson(String defaultLanguageLocationCode, String locationCode,
                                             List<String> allowedLanguageLocations,
                                             Long currentUsageInPulses, Long endOfUsagePromptCounter,
                                             Boolean welcomePromptFlag, Integer maxAllowedUsageInPulses,
                                             Integer maxAllowedEndOfUsagePrompt) throws IOException {
        FlwUserResponse userResponse = new FlwUserResponse();
        if (defaultLanguageLocationCode != null) {
            userResponse.setDefaultLanguageLocationCode(defaultLanguageLocationCode);
        }
        if (locationCode != null) {
            userResponse.setLanguageLocationCode(locationCode);
        }
        if (allowedLanguageLocations != null) {
            userResponse.setAllowedLanguageLocationCodes(new TreeSet<String>(allowedLanguageLocations));
        }
        if (currentUsageInPulses != null) {
            userResponse.setCurrentUsageInPulses(currentUsageInPulses);
        }
        if (endOfUsagePromptCounter != null) {
            userResponse.setEndOfUsagePromptCounter(endOfUsagePromptCounter);
        }
        if (welcomePromptFlag != null) {
            userResponse.setWelcomePromptFlag(welcomePromptFlag);
        }
        if (maxAllowedUsageInPulses != null) {
            userResponse.setMaxAllowedUsageInPulses(maxAllowedUsageInPulses);
        }
        if (maxAllowedEndOfUsagePrompt != null) {
            userResponse.setMaxAllowedEndOfUsagePrompt(maxAllowedEndOfUsagePrompt);
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(userResponse);
    }

    // verifyFT366
    @Test
    public void testCallDetailsValidMobileKunji() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);


        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(false, null,                   // type
                true, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "Yellowfever.wav",       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered
        array.add(createContentJson(false, null,                   // type
                true, "b",                     // mkCardCode
                true, "Malaria",               // contentName
                true, "Malaria.wav",           // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                true, 9810320300l,       // callingNumber
                true, 234000011111111l,  // callId
                true, "A",               // operator
                true, "AP",              // circle
                true, 1422879843l,       // callStartTime
                true, 1422879903l,       // callEndTime
                true, 60,                // callDurationInPulses
                true, 0,                 // endOfUsagePromptCounter
                true, true,              // welcomeMessagePromptFlag
                true, 1,                 // callStatus
                true, 1,                 // callDisconnectReason
                true, Joiner.on(",").join(array));          // content

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        CallDetailRecord cdr = callDetailRecordService.getByCallingNumber(9810320300l);

        assertNotNull(cdr);
        assertEquals(234000011111111l, cdr.getCallId());
        assertEquals(2, cdr.getContent().size());

        assertEquals("YellowFever", cdr.getContent().get(0).getContentName());
        assertNull(cdr.getContent().get(0).getType());
        assertNull(cdr.getContent().get(0).getCompletionFlag());
        assertEquals("Malaria", cdr.getContent().get(1).getContentName());
        assertNull(cdr.getContent().get(1).getType());
        assertNull(cdr.getContent().get(1).getCompletionFlag());

        assertEquals(flw.getId(), ((FrontLineWorker) callDetailRecordDataService.getDetachedField(cdr,
                "frontLineWorker")).getId());
    }

    /**
     * verifyFT470, verifyFT489
     */
    @Test
    public void testCallDetailsValidMobileAcademy() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ true, "lesson",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */false, null));
        array.add(createContentJson(/* type */ true, "question",
                /* mkCardCode */ false, null,
                /* contentName */ true, "chapter-01question-01",
                /* contentFile */ true, "ch1_q1.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
        /* correctAnswerEntered */true, false));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
        /* endOfUsagePromptCounter */true, 1,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
        /* callDisconnectReason */true, 2,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        CallDetailRecord cdr = callDetailRecordService.getByCallingNumber(9810320300l);
        // assert call detail record
        assertNotNull(cdr);
        assertEquals(9810320300l, cdr.getCallingNumber());
        assertEquals(234000011111111l, cdr.getCallId());
        assertEquals("A", cdr.getOperator());
        assertEquals("AP", cdr.getCircle());
        assertEquals(1422879843l, cdr.getCallStartTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(1422879903l, cdr.getCallEndTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(60, cdr.getCallDurationInPulses());
        assertEquals(1, cdr.getEndOfUsagePromptCounter());
        assertEquals(1, cdr.getFinalCallStatus().getValue());
        assertEquals(2, cdr.getCallDisconnectReason().getValue());
        assertEquals(2, cdr.getContent().size());

        // assert content data record
        CallContent cc = cdr.getContent().get(1);
        assertEquals("question", cc.getType());
        assertEquals("chapter-01question-01", cc.getContentName());
        assertEquals("ch1_q1.wav", cc.getContentFile());
        assertEquals(1200000000l, cc.getStartTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(1222222221l, cc.getEndTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(true, cc.getCompletionFlag());

        // TODO correctAnswerEntered assertion

        assertNull(cdr.getWelcomePrompt());
        assertNull(cc.getMobileKunjiCardCode());

        assertEquals(flw.getId(), ((FrontLineWorker) callDetailRecordDataService.getDetachedField(cdr,
                "frontLineWorker")).getId());
    }

    /**
     * verifyFT471 To check that call details of user is saved successfully
     * using Save Call Details API when optional parameter "content" is missing.
     */
    @Test
    public void testCallDetailsValidNoContent() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        CallDetailRecord cdr = callDetailRecordService.getByCallingNumber(9810320300l);

        // assert call detail record
        assertNotNull(cdr);
        assertEquals(9810320300l, cdr.getCallingNumber());
        assertEquals(234000011111111l, cdr.getCallId());
        assertEquals("A", cdr.getOperator());
        assertEquals("AP", cdr.getCircle());
        assertEquals(1422879843l, cdr.getCallStartTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(1422879903l, cdr.getCallEndTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(60, cdr.getCallDurationInPulses());
        assertEquals(0, cdr.getEndOfUsagePromptCounter());
        assertEquals(1, cdr.getFinalCallStatus().getValue());
        assertEquals(1, cdr.getCallDisconnectReason().getValue());

        // assert content
        assertEquals(0, cdr.getContent().size());
    }

    /*****************************************************************************************************************
     Test the existence and validity of elements common to MA/MK
     callingNumber, callId, operator, circle, callStartTime, callEndTime, callDurationInPulses, endOfUsagePromptCount,
     callStatus, callDisconnectReason
     ****************************************************************************************************************/
    @Test
    public void testCallDetailsFLWNotFound() throws IOException, InterruptedException {

        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        FrontLineWorker flw = frontLineWorkerService.getByContactNumber(9810320300l);
        assertEquals(FrontLineWorkerStatus.ANONYMOUS, flw.getStatus());
    }

    @Test
    public void testCallDetailsNullCallDisconnectReason() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ false, null,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDisconnectReason: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallStatus() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ false, null,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStatus: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullEndOfUsagePromptCount() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ false, null,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endOfUsagePromptCount: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallDurationInPulses() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ false, null,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDurationInPulses: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallEndTime() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ false, null,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callEndTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallStartTime() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ false, null,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStartTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallingNumber() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ false, null,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsInvalidCallingNumber() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 1l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullCallId() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ false, null,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsInvalidCallId() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 1l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);
        
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallId" is having invalid value i.e. "CallId" is having value greater
     * than 15 digit
     */
    @Test
    public void verifyFT491() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 1234567890123456l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Invalid>\"}", ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallStatus" is having invalid value.
     */
    @Test
    public void verifyFT499() throws IOException, InterruptedException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 4,
        /* callDisconnectReason */true, 1,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"4 is an invalid FinalCallStatus\"}",
                ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallDisconnectReason" is having invalid value.
     */
    @Test
    public void verifyFT500() throws IOException, InterruptedException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 3,
        /* callDisconnectReason */true, 7,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"7 is an invalid CallDisconnectReason\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallStartTime" is having invalid format.
     */
    @Test
    public void verifyFT494() throws IOException,
            InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, "9810320300",
        /* callId */true, "234000011111111",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, "142287984ss",// Invalid
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, "1",
        /* callDisconnectReason */true, "1",
        /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callStartTime.*");
        
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallEndTime" is having invalid format.
     */
    @Test
    public void verifyFT495() throws IOException,
            InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, "9810320300",
        /* callId */true, "234000011111111",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903sasa",//Invalid
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, "1",
        /* callDisconnectReason */true, "1",
        /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callEndTime.*");
        
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }
    
    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallDurationPulses" is having invalid format.
     */
    @Test
     public void verifyFT496() throws IOException,
             InterruptedException {

         FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                 9810320300L);
         frontLineWorkerService.add(flw);

         HttpPost httpPost = createCallDetailsPost("mobileacademy",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "a6",// Invalid
         /* endOfUsagePromptCounter */true, "0",
         /* welcomeMessagePromptFlag */false, null,
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

         Pattern expectedJsonResponse = Pattern
                .compile(".*callDurationInPulses.*");
         
         assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                 expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

     }
    
    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "EndOfUsagePrompt" is having invalid format.
     */
    @Test
     public void verifyFT497() throws IOException,
             InterruptedException {

         FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                 9810320300L);
         frontLineWorkerService.add(flw);

         HttpPost httpPost = createCallDetailsPost("mobileacademy",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "a",// Invalid
         /* welcomeMessagePromptFlag */false, null,
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

         Pattern expectedJsonResponse = Pattern
                .compile(".*endOfUsagePromptCounter.*");
         
         assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                 expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

     }

    /*****************************************************************************************************************
     Test the existence and validity of elements specific to MA
     content.type, content.completionFlag
     *****************************************************************************************************************/
    @Test
    public void testCallDetailsNullContentType() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ false, null,
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */ true, true));
        array.add(createContentJson(/* type */ true, "question",
                /* mkCardCode */ false, null,
                /* contentName */ true, "chapter-01question-01",
                /* contentFile */ true, "ch1_q1.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ true, true,
                /* correctAnswerEntered */ true, true));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<type: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullContentCompletionFlag() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ true, "lesson",
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ false, null,
                /* correctAnswerEntered */ false, null));
        array.add(createContentJson(/* type */ true, "question",
                /* mkCardCode */ false, null,
                /* contentName */ true, "chapter-01question-01",
                /* contentFile */ true, "ch1_q1.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ false, null,
                /* correctAnswerEntered */ false, null));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<completionFlag: Not Present><completionFlag: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>ContentName" is missing
     */
    @Test
    public void verifyFT483() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */false, null,
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, 1200000000l,
        /* endTime */true, 1222222221l,
        /* completionFlag */true, true,
        /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentName: Not Present>\"}", ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>ContentFile" is missing
     */
    @Test
    public void verifyFT484() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "Chapter-01lesson-04",
        /* contentFile */false, null,
        /* startTime */true, 1200000000l,
        /* endTime */true, 1222222221l,
        /* completionFlag */true, true,
        /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentFile: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>StartTime" is missing
     */
    @Test
    public void verifyFT485() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "Chapter-01lesson-04",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */false, null,
        /* endTime */true, 1222222221l,
        /* completionFlag */true, true,
        /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<startTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>EndTime" is missing
     */
    @Test
    public void verifyFT486() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "Chapter-01lesson-04",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, 1200000000l,
        /* endTime */false, null,
        /* completionFlag */true, true,
        /* correctAnswerEntered */true, true));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 1,
        /* content */true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>StartTime" is having invalid format.
     */
    @Test
    public void verifyFT503() throws InterruptedException, IOException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000ss",// invalid
        /* endTime */true, "1222222221",
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*startTime.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }
    
    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>EndTime" is having invalid format.
     */
    @Test
    public void verifyFT504() throws InterruptedException, IOException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "122ss2222221",// Invalid
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*endTime.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }
    
    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>type" is having invalid format.
     */
    // TODO https://applab.atlassian.net/browse/NMS-232
    @Ignore
    @Test
    public void verifyFT505() throws InterruptedException, IOException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "bookmark",// Type can be "lesson", "chapter", "question"
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "1222222221",
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*type.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }
    
    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * callData>> completionFlag" is having invalid format.
     */
    @Test
    public void verifyFT506() throws InterruptedException, IOException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "1222222221",
        /* completionFlag */true, "t1",// Invalid
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*completionFlag.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * callData>>correctAnswerEntered" is having invalid format.
     */
    @Test
    public void verifyFT507() throws InterruptedException, IOException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */false, null,
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "1222222221",
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "10"));//Invalid
        HttpPost httpPost = createCallDetailsPost("mobileacademy",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */false, null,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*correctAnswerEntered.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /*****************************************************************************************************************
     Test the existence and validity of elements specific to MK
     welcomeMessagePromptFlag, content.mkCardCode
     *****************************************************************************************************************/
    @Test
    public void testCallDetailsNullWelcomeMessagePromptFlag() throws IOException, InterruptedException {
        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879903l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<welcomeMessagePromptFlag: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    @Test
    public void testCallDetailsNullContentMkCardNumber() throws IOException, InterruptedException {
        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */ false, null,
                /* mkCardCode */ false, null,
                /* contentName */ true, "Chapter-01lesson-04",
                /* contentFile */ true, "ch1_l4.wav",
                /* startTime */ true, 1200000000l,
                /* endTime */ true, 1222222221l,
                /* completionFlag */ false, null,
                /* correctAnswerEntered */ false, null));
        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, false,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<mkCardCode: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To check that call details of user is saved successfully using Save Call
     * Details API when optional parameter "content" is missing.
     */
    @Test
    public void verifyFT367() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 9810320300l,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        CallDetailRecord cdr = callDetailRecordService.getByCallingNumber(9810320300l);

        // assert call detail record
        assertNotNull(cdr);
        assertEquals(9810320300l, cdr.getCallingNumber());
        assertEquals(234000011111111l, cdr.getCallId());
        assertEquals("A", cdr.getOperator());
        assertEquals("AP", cdr.getCircle());
        assertEquals(1422879843l, cdr.getCallStartTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(1422879903l, cdr.getCallEndTime().getMillis() / MILLISECONDS_PER_SECOND);
        assertEquals(60, cdr.getCallDurationInPulses());
        assertEquals(0, cdr.getEndOfUsagePromptCounter());
        assertEquals(1, cdr.getFinalCallStatus().getValue());
        assertEquals(1, cdr.getCallDisconnectReason().getValue());

        // assert content
        assertEquals(0, cdr.getContent().size());
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallingNumber" is missing
     */
    @Test
    public void verifyFT368() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ false, null,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallId" is missing
     */
    @Test
    public void verifyFT369() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ false, null,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallStartTime" is missing
     */
    @Test
    public void verifyFT372() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ false, null,
                /* callStartTime */ false, null,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStartTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallEndTime" is missing
     */
    @Test
    public void verifyFT373() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ false, null,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callEndTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallDurationPulses" is missing
     */
    @Test
    public void verifyFT374() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ false, null,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDurationInPulses: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "EndOfUsagePrompt" is missing
     */
    @Test
    public void verifyFT375() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ false, null,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endOfUsagePromptCount: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "WelcomeMessageFlag" is missing
     */
    @Test
    public void verifyFT376() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ false, null,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<welcomeMessagePromptFlag: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallStatus" is missing
     */
    @Test
    public void verifyFT377() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ false, null,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callStatus: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallDisconnectReason" is missing
     */
    @Test
    public void verifyFT378() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ false, null,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callDisconnectReason: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>ContentName" is missing
     */
    @Test
    public void verifyFT379() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(false, null,                   // type
                true, "a",                     // mkCardCode
                false, null,           // contentName
                true, "Yellowfever.wav",       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentName: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>ContentFile" is missing
     */
    @Test
    public void verifyFT380() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(false, null,                   // type
                true, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                false, null,       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<contentFile: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>StartTime" is missing
     */
    @Test
    public void verifyFT381() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(false, null,                   // type
                true, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "YellowFever.wav",       // contentFile
                false, null,             // startTime
                true, 1222222221l,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<startTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>EndTime" is missing
     */
    @Test
    public void verifyFT382() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(false, null,                   // type
                true, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "YellowFever.wav",       // contentFile
                true, 1222222220l,             // startTime
                false, null,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<endTime: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter "CallData>>MKCardCode" is missing
     */
    @Test
    public void verifyFT383() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(false, null,                   // type
                false, null,                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "YellowFever.wav",       // contentFile
                true, 1222222220l,             // startTime
                true, 1222222221l,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered


        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ true, Joiner.on(",").join(array));

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<mkCardCode: Not Present>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallingNumber" is having invalid value.
     */
    @Test
    public void verifyFT384() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 12356789L,
                /* callId */ true, 234000011111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callingNumber: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallId" is having invalid value
     */
    @Test
    public void verifyFT385() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                /* callingNumber */ true, 1234567890L,
                /* callId */ true, 234000011111111111l,
                /* operator */ true, "A",
                /* circle */ true, "AP",
                /* callStartTime */ true, 1422879843l,
                /* callEndTime */ true, 1422879903l,
                /* callDurationInPulses */ true, 60,
                /* endOfUsagePromptCounter */ true, 0,
                /* welcomeMessagePromptFlag */ true, true,
                /* callStatus */ true, 1,
                /* callDisconnectReason */ true, 1,
                /* content */ false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"<callId: Invalid>\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallStartTime" is having invalid format.
     */
    @Test
    public void verifyFT388() throws IOException,
            InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
        /* callingNumber */true, "9810320300",
        /* callId */true, "234000011111111",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, "142287984ss",// Invalid
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",
        /* welcomeMessagePromptFlag */true, "true",
        /* callStatus */true, "1",
        /* callDisconnectReason */true, "1",
        /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callStartTime.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallEndTime" is having invalid format.
     */
    @Test
    public void verifyFT389() throws IOException,
            InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
        /* callingNumber */true, "9810320300",
        /* callId */true, "234000011111111",
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903sasa",//Invalid
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",
        /* welcomeMessagePromptFlag */true, "true",
        /* callStatus */true, "1",
        /* callDisconnectReason */true, "1",
        /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callEndTime.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallDurationPulses" is having invalid format.
     */
    @Test
    public void verifyFT390() throws IOException,
            InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "a6",// Invalid
         /* endOfUsagePromptCounter */true, "0",
         /* welcomeMessagePromptFlag */true, "true",
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*callDurationInPulses.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "EndOfUsagePrompt" is having invalid format.
     */
    @Test
    public void verifyFT391() throws IOException,
            InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "a",// Invalid
         /* welcomeMessagePromptFlag */true, "true",
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

        Pattern expectedJsonResponse = Pattern
                .compile(".*endOfUsagePromptCounter.*");

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_BAD_REQUEST,
                expectedJsonResponse, ADMIN_USERNAME, ADMIN_PASSWORD));

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "WelcomeMessageFlag" is having invalid value.
     */
    @Test
    public void verifyFT392() throws IOException, InterruptedException {

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 9810320300L);
        frontLineWorkerService.add(flw);

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
         /* callingNumber */true, "9810320300",
         /* callId */true, "234000011111111",
         /* operator */true, "A",
         /* circle */true, "AP",
         /* callStartTime */true, "142287984",
        /* callEndTime */true, "1422879903",
        /* callDurationInPulses */true, "60",
        /* endOfUsagePromptCounter */true, "0",// Invalid
         /* welcomeMessagePromptFlag */true, "abc",
         /* callStatus */true, "1",
         /* callDisconnectReason */true, "1",
         /* content */false, null);

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpPost, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallStatus" is having invalid value.
     */
    @Test
    public void verifyFT393() throws IOException, InterruptedException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);
        HttpPost httpPost = createCallDetailsPost("mobilekunji",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */true, true,
        /* callStatus */true, 4,
        /* callDisconnectReason */true, 1,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"4 is an invalid FinalCallStatus\"}",
                ADMIN_USERNAME,
                ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallDisconnectReason" is having invalid value.
     */
    @Test
    public void verifyFT394() throws IOException, InterruptedException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);
        HttpPost httpPost = createCallDetailsPost("mobilekunji",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 0,
        /* welcomeMessagePromptFlag */true, true,
        /* callStatus */true, 3,
        /* callDisconnectReason */true, 7,
        /* content */false, null);

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST,
                "{\"failureReason\":\"7 is an invalid CallDisconnectReason\"}",
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>StartTime" is having invalid format.
     */
    @Test
    public void verifyFT397() throws InterruptedException, IOException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */true, "a",
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000ss",// invalid
        /* endTime */true, "1222222221",
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("mobilekunji",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */true, true,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*startTime.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that Save Call Details API if rejected when mandatory parameter
     * "CallData>>EndTime" is having invalid format.
     */
    @Test
    public void verifyFT398() throws InterruptedException, IOException {
        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright",
                9810320300L);
        frontLineWorkerService.add(flw);

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(/* type */true, "question",
        /* mkCardCode */true, "a",
        /* contentName */true, "chapter-01question-01",
        /* contentFile */true, "ch1_q1.wav",
        /* startTime */true, "1200000000",
        /* endTime */true, "122ss2222221",// Invalid
        /* completionFlag */true, "true",
        /* correctAnswerEntered */true, "false"));
        HttpPost httpPost = createCallDetailsPost("mobilekunji",
        /* callingNumber */true, 9810320300l,
        /* callId */true, 234000011111111l,
        /* operator */true, "A",
        /* circle */true, "AP",
        /* callStartTime */true, 1422879843l,
        /* callEndTime */true, 1422879903l,
        /* callDurationInPulses */true, 60,
        /* endOfUsagePromptCounter */true, 1,
        /* welcomeMessagePromptFlag */true, true,
        /* callStatus */true, 1,
        /* callDisconnectReason */true, 2,
        /* content */true, Joiner.on(",").join(array));

        Pattern expectedJsonResponse = Pattern.compile(".*endTime.*");
        assertTrue(SimpleHttpClient.execHttpRequest(httpPost,
                HttpStatus.SC_BAD_REQUEST, expectedJsonResponse,
                ADMIN_USERNAME, ADMIN_PASSWORD));
    }

    /**
     * To verify that current usage pulses is resetted after the end of month.
     */
    @Test
    public void verifyFT522() throws IOException, InterruptedException {
        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.MOBILE_KUNJI));

        // FLW usage
        FrontLineWorker flw = new FrontLineWorker("Frank Llyod Wright", 1200000000l);
        flw.setLanguage(rh.hindiLanguage());
        frontLineWorkerService.add(flw);

        // invoke get user detail API
        StringBuilder sb = new StringBuilder(String.format("http://localhost:%d/api/", TestContext.getJettyPort()));
        String sep = "";
        sb.append(String.format("%s/", "mobilekunji"));
        sb.append("user?");
        sb.append(String.format("callingNumber=%s", "1200000000"));
        sep = "&";
        sb.append(String.format("%scallId=%s", sep, "123456789012345"));
        HttpGet httpGet = new HttpGet(sb.toString());

        String expectedJsonResponse = createFlwUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                0L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses=No capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(
                httpGet, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));


        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(false, null,                   // type
                true, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "Yellowfever.wav",       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                true, 1200000000l,       // callingNumber
                true, 123456789012345l,  // callId
                true, "A",               // operator
                true, "AP",              // circle
                true, DateTime.now().getMillis()/1000,       // callStartTime
                true, 1422879903l,       // callEndTime
                true, 60,                // callDurationInPulses
                true, 0,                 // endOfUsagePromptCounter
                true, true,              // welcomeMessagePromptFlag
                true, 1,                 // callStatus
                true, 1,                 // callDisconnectReason
                true, Joiner.on(",").join(array));          // content

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));


        // invoke get user detail API
        sb = new StringBuilder(String.format("http://localhost:%d/api/", TestContext.getJettyPort()));
        sep = "";
        sb.append(String.format("%s/", "mobilekunji"));
        sb.append("user?");
        sb.append(String.format("callingNumber=%s", "1200000000"));
        sep = "&";
        sb.append(String.format("%scallId=%s", sep, "123456789012345"));
        httpGet = new HttpGet(sb.toString());

        expectedJsonResponse = createFlwUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                null, // allowedLanguageLocationCodes
                60L, // currentUsageInPulses
                0L, // endOfUsagePromptCounter
                true, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses=No capping
                2 // maxAllowedEndOfUsagePrompt
        );

        response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));
    }

    /**
     * To verift that status of flw must be set to "Active" when user call first time and
     * its information exists in NMS DB and status as "Inactive"
     */
    @Test
    public void verifyFT512() throws IOException, InterruptedException {
        rh.delhiState();
        rh.newDelhiDistrict();
        rh.delhiCircle();
        deployedServiceDataService.create(new DeployedService(rh.delhiState(),
                Service.MOBILE_KUNJI));

        FrontLineWorker flw = new FrontLineWorker("Frank Lloyd Wright", 1200000001L);
        flw.setLanguage(rh.hindiLanguage());
        flw.setDistrict(rh.newDelhiDistrict());
        flw.setState(rh.delhiState());
        flw.setStatus(FrontLineWorkerStatus.INACTIVE);
        frontLineWorkerService.add(flw);

        // invoke get user detail API
        StringBuilder sb = new StringBuilder(String.format("http://localhost:%d/api/", TestContext.getJettyPort()));
        String sep = "";
        sb.append(String.format("%s/", "mobilekunji"));
        sb.append("user?");
        sb.append(String.format("callingNumber=%s", "1200000001"));
        sep = "&";
        sb.append(String.format("%scallId=%s", sep, "123456789012345"));
        HttpGet httpGet = new HttpGet(sb.toString());

        String expectedJsonResponse = createFlwUserResponseJson(null, // defaultLanguageLocationCode
                rh.hindiLanguage().getCode(), // locationCode
                new ArrayList<String>(), // allowedLanguageLocationCodes
                0L, // currentUsageInPulses=updated
                0L, // endOfUsagePromptCounter=updated
                false, // welcomePromptFlag
                -1, // maxAllowedUsageInPulses=State capping
                2 // maxAllowedEndOfUsagePrompt
        );

        HttpResponse response = SimpleHttpClient.httpRequestAndResponse(httpGet,
                ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expectedJsonResponse,
                EntityUtils.toString(response.getEntity()));

        ArrayList<String> array = new ArrayList<>();
        array.add(createContentJson(false, null,                   // type
                true, "a",                     // mkCardCode
                true, "YellowFever",           // contentName
                true, "Yellowfever.wav",       // contentFile
                true, 1200000000l,             // startTime
                true, 1222222221l,             // endTime
                false, null,                   // completionFlag
                false, null));                 // correctAnswerEntered

        HttpPost httpPost = createCallDetailsPost("mobilekunji",
                true, 1200000001l,       // callingNumber
                true, 123456789012345l,  // callId
                true, "A",               // operator
                true, "AP",              // circle
                true, DateTime.now().getMillis()/1000,       // callStartTime
                true, 1422879903l,       // callEndTime
                true, 60,                // callDurationInPulses
                true, 0,                 // endOfUsagePromptCounter
                true, true,              // welcomeMessagePromptFlag
                true, 1,                 // callStatus
                true, 1,                 // callDisconnectReason
                true, Joiner.on(",").join(array));          // content

        assertTrue(SimpleHttpClient.execHttpRequest(httpPost, HttpStatus.SC_OK, ADMIN_USERNAME, ADMIN_PASSWORD));

        flw = frontLineWorkerService.getByContactNumber(1200000001l);
        assertEquals(FrontLineWorkerStatus.ACTIVE, flw.getStatus());
    }

    // Test with no content
    // Test with empty content
    // Test witn invalid callingNumber
    // Test with null callingNumber
    // Test with no flw for callingNumber
    // Test with invalid callId
    // Test with missing callId
}
