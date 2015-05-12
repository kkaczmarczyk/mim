package org.motechproject.nms.imi.component;

import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.nms.imi.domain.CallDetailRecord;
import org.motechproject.nms.imi.domain.CallRetry;
import org.motechproject.nms.imi.domain.CallStage;
import org.motechproject.nms.imi.repository.CallRetryDataService;
import org.motechproject.nms.imi.service.RequestId;
import org.motechproject.nms.kilkari.domain.Subscription;
import org.motechproject.nms.kilkari.service.SubscriptionService;
import org.motechproject.nms.props.domain.DayOfTheWeek;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Listens to nms.imi.reschedule_call MOTECH message and reschedules a call for the provided subscription which failed
 */
@Component
public class CallRescheduler {

    private static final String RESCHEDULE_CALL = "nms.imi.reschedule_call";

    private static final Logger LOGGER = LoggerFactory.getLogger(CallRescheduler.class);

    private SubscriptionService subscriptionService;
    private CallRetryDataService callRetryDataService;


    @Autowired
    public CallRescheduler(SubscriptionService subscriptionService, CallRetryDataService callRetryDataService) {
        this.subscriptionService = subscriptionService;
        this.callRetryDataService = callRetryDataService;
    }


    @MotechListener(subjects = { RESCHEDULE_CALL })
    public void rescheduleCall(MotechEvent event) {
        LOGGER.debug("rescheduleCall() is handling {}", event.toString());

        try {
            CallDetailRecord cdr = (CallDetailRecord) event.getParameters().get("CDR");
            RequestId requestId = RequestId.fromString(cdr.getRequestId());
            Subscription subscription = subscriptionService.getSubscription(requestId.getSubscriptionId());
            CallRetry callRetry = callRetryDataService.findBySubscriptionId(requestId.getSubscriptionId());

            if (callRetry == null) {
                //first retry day
                LOGGER.debug("rescheduling msisdn {} subscription {}", cdr.getMsisdn(), requestId.getSubscriptionId());
                callRetry = new CallRetry(
                        requestId.getSubscriptionId(),
                        Long.parseLong(cdr.getMsisdn()),
                        DayOfTheWeek.fromInt(subscription.getStartDate().dayOfWeek().get()).nextDay(),
                        CallStage.RETRY_1,
                        subscription.getSubscriber().getLanguage().getCode(),
                        subscription.getSubscriber().getCircle(),
                        subscription.getOrigin().getCode()
                );
                LOGGER.debug("Creating CallRetry {}", callRetry.toString());
                callRetryDataService.create(callRetry);
                return;
            }

            //we've already rescheduled this call, let's see if it needs to be re-rescheduled

            if (subscription.getSubscriptionPack().retryCount() == 1) {
                //This message should only be retried once, so let's delete it from the CallRetry table
                LOGGER.debug("Not re-rescheduling single-retry msisdn {} subscription {}: max retry exceeded",
                        cdr.getMsisdn(), requestId.getSubscriptionId());
                callRetryDataService.delete(callRetry);
                return;
            }

            if (callRetry.getCallStage() == CallStage.RETRY_LAST) {
                //This message has been re-scheduled for the last (3rd) time, let's delete it from the CallRetry
                //table
                LOGGER.debug("Not re-rescheduling multiple-retry msisdn {} subscription {}: max retry exceeded",
                        cdr.getMsisdn(), requestId.getSubscriptionId());
                callRetryDataService.delete(callRetry);
                return;
            }

            //re-reschedule the call
            LOGGER.debug("re-rescheduling msisdn {} subscription {}", cdr.getMsisdn(), requestId.getSubscriptionId());

            //update the callStage
            callRetry.setCallStage((callRetry.getCallStage() == CallStage.RETRY_1) ? CallStage.RETRY_2 :
                    CallStage.RETRY_LAST);
            //increment the day of the week
            callRetry.setDayOfTheWeek(callRetry.getDayOfTheWeek().nextDay());
            //update the CallRetry record
            LOGGER.debug("Updating CallRetry {}", callRetry.toString());
            callRetryDataService.update(callRetry);
        } catch (Exception e) {
            LOGGER.error("********** Unexpected Exception! **********", e);
            throw e;
        }
    }
}
