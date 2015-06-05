package org.motechproject.nms.testing.it.utils;


import org.joda.time.DateTime;
import org.motechproject.nms.kilkari.domain.Subscriber;
import org.motechproject.nms.kilkari.domain.Subscription;
import org.motechproject.nms.kilkari.domain.SubscriptionOrigin;
import org.motechproject.nms.kilkari.domain.SubscriptionPack;
import org.motechproject.nms.kilkari.domain.SubscriptionPackMessage;
import org.motechproject.nms.kilkari.domain.SubscriptionPackType;
import org.motechproject.nms.kilkari.domain.SubscriptionStatus;
import org.motechproject.nms.kilkari.repository.SubscriberDataService;
import org.motechproject.nms.kilkari.repository.SubscriptionPackDataService;
import org.motechproject.nms.kilkari.service.SubscriptionService;
import org.motechproject.nms.region.domain.Circle;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.Language;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.repository.CircleDataService;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.motechproject.nms.region.repository.LanguageDataService;
import org.motechproject.nms.region.repository.StateDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.synth.Region;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionHelper.class);
    private static final int PREGNANCY_PACK_WEEKS = 72;
    private static final int CHILD_PACK_WEEKS = 48;
    private static final int TWO_MINUTES = 120;
    private static final int TEN_SECS = 10;

    private SubscriptionService subscriptionService;
    private SubscriberDataService subscriberDataService;
    private SubscriptionPackDataService subscriptionPackDataService;
    private RegionHelper regionHelper;

    public SubscriptionHelper(SubscriptionService subscriptionService,
                              SubscriberDataService subscriberDataService,
                              SubscriptionPackDataService subscriptionPackDataService,
                              LanguageDataService languageDataService,
                              CircleDataService circleDataService,
                              StateDataService stateDataService,
                              DistrictDataService districtDataService) {

        this.subscriptionService = subscriptionService;
        this.subscriberDataService = subscriberDataService;
        this.subscriptionPackDataService = subscriptionPackDataService;

        this.regionHelper = new RegionHelper(languageDataService, circleDataService, stateDataService, districtDataService);
    }

    public SubscriptionPack getChildPack() {
        createSubscriptionPacks();
        return subscriptionService.getSubscriptionPack("childPack");
    }

    public SubscriptionPack getPregnancyPack() {
        createSubscriptionPacks();
        return subscriptionService.getSubscriptionPack("pregnancyPack");
    }

    private void createSubscriptionPacks() {
        if (subscriptionPackDataService.byName("childPack") == null) {
            createSubscriptionPack("childPack", SubscriptionPackType.CHILD, CHILD_PACK_WEEKS, 1);
        }
        if (subscriptionPackDataService.byName("pregnancyPack") == null) {
            createSubscriptionPack("pregnancyPack", SubscriptionPackType.PREGNANCY, PREGNANCY_PACK_WEEKS, 2);
        }
    }

    private void createSubscriptionPack(String name, SubscriptionPackType type, int weeks,
                                        int messagesPerWeek) {
        List<SubscriptionPackMessage> messages = new ArrayList<>();
        for (int week = 1; week <= weeks; week++) {
            messages.add(new SubscriptionPackMessage(week, String.format("w%s_1", week),
                    String.format("w%s_1.wav", week),
                    TWO_MINUTES - TEN_SECS + (int) (Math.random() * 2 * TEN_SECS)));

            if (messagesPerWeek == 2) {
                messages.add(new SubscriptionPackMessage(week, String.format("w%s_2", week),
                        String.format("w%s_2.wav", week),
                        TWO_MINUTES - TEN_SECS + (int) (Math.random() * 2 * TEN_SECS)));
            }
        }

        subscriptionPackDataService.create(new SubscriptionPack(name, type, weeks, messagesPerWeek, messages));
    }


    public Long makeNumber() {
        return (long) (Math.random() * 9000000000L) + 1000000000L;
    }


    public int getRandomMessageIndex(Subscription sub) {
        return (int) (Math.random() * sub.getSubscriptionPack().getMessages().size());
    }


    public int getLastMessageIndex(Subscription sub) {
        return sub.getSubscriptionPack().getMessages().size() - 1;
    }


    public String getWeekId(Subscription sub, int index) {
        return sub.getSubscriptionPack().getMessages().get(index).getWeekId();
    }


    public String getLanguageLocationCode(Subscription sub) {
        return ((Language) subscriberDataService.getDetachedField(
                sub.getSubscriber(),"language")).getCode();
    }


    public String getCircle(Subscription sub) {
        return ((Circle) subscriberDataService.getDetachedField(
                sub.getSubscriber(),"circle")).getName();
    }


    public String getContentMessageFile(Subscription sub, int index) {
        return sub.getSubscriptionPack().getMessages().get(index).getMessageFileName();
    }


    public Subscription mksub(SubscriptionOrigin origin, DateTime startDate, SubscriptionPackType packType) {

        Subscription subscription;
        createSubscriptionPacks();
        Subscriber subscriber = subscriberDataService.create(new Subscriber(
                makeNumber(),
                regionHelper.makeLanguage(),
                regionHelper.makeCircle()
        ));

        if (SubscriptionPackType.PREGNANCY == packType) {
            subscription = new Subscription(subscriber,getPregnancyPack() , origin);
        } else {
            subscription = new Subscription(subscriber, getChildPack(), origin);
        }

        subscription.setStartDate(startDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription = subscriptionService.create(subscription);
        LOGGER.debug("Created subscription {}", subscription.toString());
        return subscription;
    }


    public Subscription mksub(SubscriptionOrigin origin, DateTime startDate) {
        return mksub(origin, startDate, SubscriptionPackType.CHILD);
    }
}
