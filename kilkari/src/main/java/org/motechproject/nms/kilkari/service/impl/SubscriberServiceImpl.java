package org.motechproject.nms.kilkari.service.impl;

import org.datanucleus.store.rdbms.query.ForwardQueryResult;
import org.joda.time.DateTime;
import org.motechproject.mds.query.SqlQueryExecution;
import org.motechproject.nms.kilkari.domain.MctsBeneficiary;
import org.motechproject.nms.kilkari.domain.MctsChild;
import org.motechproject.nms.kilkari.domain.MctsMother;
import org.motechproject.nms.kilkari.domain.Subscriber;
import org.motechproject.nms.kilkari.domain.Subscription;
import org.motechproject.nms.kilkari.domain.SubscriptionError;
import org.motechproject.nms.kilkari.domain.SubscriptionOrigin;
import org.motechproject.nms.kilkari.domain.SubscriptionPack;
import org.motechproject.nms.kilkari.domain.SubscriptionPackType;
import org.motechproject.nms.kilkari.domain.SubscriptionRejectionReason;
import org.motechproject.nms.kilkari.repository.SubscriberDataService;
import org.motechproject.nms.kilkari.repository.SubscriptionDataService;
import org.motechproject.nms.kilkari.repository.SubscriptionErrorDataService;
import org.motechproject.nms.kilkari.repository.SubscriptionPackDataService;
import org.motechproject.nms.kilkari.service.SubscriberService;
import org.motechproject.nms.kilkari.service.SubscriptionService;
import org.motechproject.nms.region.domain.Circle;
import org.motechproject.nms.region.domain.District;
import org.motechproject.nms.region.domain.Language;
import org.motechproject.nms.region.domain.State;
import org.motechproject.nms.region.repository.DistrictDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jdo.Query;
import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * Implementation of the {@link SubscriberService} interface.
 */
@Service("subscriberService")
public class SubscriberServiceImpl implements SubscriberService {

    private SubscriberDataService subscriberDataService;
    private SubscriptionService subscriptionService;
    private SubscriptionDataService subscriptionDataService;
    private SubscriptionErrorDataService subscriptionErrorDataService;
    private SubscriptionPackDataService subscriptionPackDataService;
    private DistrictDataService districtDataService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberServiceImpl.class);

    private Random random = new Random(System.currentTimeMillis());

    public static class ObjectSizeFetcher {
        private static Instrumentation instrumentation;

        public static void premain(String args, Instrumentation inst) {
            instrumentation = inst;
        }

        public static long getObjectSize(Object o) {
            return instrumentation.getObjectSize(o);
        }
    }

    @Autowired
    public SubscriberServiceImpl(SubscriberDataService subscriberDataService, SubscriptionService subscriptionService,
                                 SubscriptionDataService subscriptionDataService,
                                 SubscriptionErrorDataService subscriptionErrorDataService,
                                 SubscriptionPackDataService subscriptionPackDataService,
                                 DistrictDataService districtDataService) {
        this.subscriberDataService = subscriberDataService;
        this.subscriptionService = subscriptionService;
        this.subscriptionDataService = subscriptionDataService;
        this.subscriptionErrorDataService = subscriptionErrorDataService;
        this.subscriptionPackDataService = subscriptionPackDataService;
        this.districtDataService = districtDataService;
    }


    private Subscriber findByCallingNumber(final long callingNumber) {

        if (random.nextInt(10) > 100) {

            //will never happen
            return subscriberDataService.findByCallingNumberXXX(callingNumber);

        } else {

            SqlQueryExecution<Subscriber> queryExecution = new SqlQueryExecution<Subscriber>() {

                @Override
                public String getSqlQuery() {
                    return "select *  from nms_subscribers where callingNumber = ?";
                }

                @Override
                public Subscriber execute(Query query) {
                    query.setClass(Subscriber.class);
                    ForwardQueryResult fqr = (ForwardQueryResult) query.execute(callingNumber);
                    if (fqr.isEmpty()) {
                        return null;
                    }
                    if (fqr.size() == 1) {
                        return (Subscriber) fqr.get(0);
                    }
                    throw new IllegalStateException(
                            String.format("More than one row returned for callingNumber %d", callingNumber));
                }
            };

            return subscriberDataService.executeSQLQuery(queryExecution);
        }
    }


    @Override
    public Subscriber getSubscriber(long callingNumber) {
        return findByCallingNumber(callingNumber);
    }

    @Override
    public Subscriber getSubscriberByBeneficiary(final MctsBeneficiary beneficiary) {

        SqlQueryExecution<Subscriber> queryExecution = new SqlQueryExecution<Subscriber>() {

            @Override
            public String getSqlQuery() {
                return "select *  from nms_subscribers where mother_id_OID = ? or child_id_OID = ?";
            }

            @Override
            public Subscriber execute(Query query) {
                query.setClass(Subscriber.class);
                Long id = beneficiary.getId();
                ForwardQueryResult fqr = (ForwardQueryResult) query.execute(id, id);
                if (fqr.isEmpty()) {
                    return null;
                }
                if (fqr.size() == 1) {
                    return (Subscriber) fqr.get(0);
                }
                throw new IllegalStateException(String.format("More than one row returned for beneficiary %s", id));
            }
        };

        return subscriberDataService.executeSQLQuery(queryExecution);

    }

    @Override
    public void create(Subscriber subscriber) {
        Subscriber subscriber1 = subscriberDataService.create(subscriber);
        LOGGER.debug("sizeof(subscriber) = {}", ObjectSizeFetcher.getObjectSize(subscriber1));
    }

    @Override
    @Transactional
    public void update(Subscriber subscriber) {

        Subscriber retrievedSubscriber = findByCallingNumber(subscriber.getCallingNumber());

        subscriberDataService.update(subscriber);

        // update start dates for subscriptions if reference date (LMP/DOB) has changed
        Set<Subscription> subscriptions = retrievedSubscriber.getActiveAndPendingSubscriptions();
        Iterator<Subscription> subscriptionIterator = subscriptions.iterator();
        Subscription subscription;

        while (subscriptionIterator.hasNext()) {
            subscription = subscriptionIterator.next();

            if (subscription.getSubscriptionPack().getType() == SubscriptionPackType.PREGNANCY) {

                if (subscriber.getLastMenstrualPeriod() != null) { // Subscribers via IVR will not have LMP
                    subscriptionService.updateStartDate(subscription, subscriber.getLastMenstrualPeriod());
                }

            } else if (subscription.getSubscriptionPack().getType() == SubscriptionPackType.CHILD) {

                subscriptionService.updateStartDate(subscription, subscriber.getDateOfBirth());

            }
        }
    }

    @Override
    public void updateMsisdnForSubscriber(Subscriber subscriber, MctsBeneficiary beneficiary, Long newMsisdn) {
        SubscriptionPackType packType;
        packType = (beneficiary instanceof MctsChild) ? SubscriptionPackType.CHILD : SubscriptionPackType.PREGNANCY;

        Subscriber subscriberWithMsisdn = findByCallingNumber(newMsisdn);
        if (subscriberWithMsisdn != null) {
            // this number is in use
            if (subscriptionService.getActiveSubscription(subscriberWithMsisdn, packType) != null) {
                // in fact, it's in use for this pack -- reject the subscription
                throw new IllegalStateException(
                        String.format("Can't change subscriber's MSISDN (%s) because it is already in use for %s pack",
                                newMsisdn, packType));
            }
        }

        // do the update -- by creating a new Subscriber object and re-linking the beneficiary and subscription to it

        Subscriber newSubscriber = new Subscriber(newMsisdn, subscriber.getLanguage(), subscriber.getCircle());
        Subscription subscription = subscriptionService.getActiveSubscription(subscriber, packType);
        subscriber.getSubscriptions().remove(subscription);
        newSubscriber.getSubscriptions().add(subscription);
        subscription.setSubscriber(newSubscriber);

        if (packType == SubscriptionPackType.CHILD) {
            subscriber.setChild(null);
            newSubscriber.setChild((MctsChild) beneficiary);
            newSubscriber.setDateOfBirth(subscriber.getDateOfBirth());
        } else {
            subscriber.setMother(null);
            newSubscriber.setMother((MctsMother) beneficiary);
            newSubscriber.setLastMenstrualPeriod(subscriber.getLastMenstrualPeriod());
        }

        subscriberDataService.create(newSubscriber);
        subscriberDataService.update(subscriber);
        subscriptionDataService.update(subscription);
    }

    private Circle circleFromDistrict(District district) {
        State state = (State) districtDataService.getDetachedField(district, "state");
        List<Circle> circleList = state.getCircles();

        if (circleList.size() == 1) {
            return circleList.get(0);
        }

        return null;
    }

    @Override
    public Subscription updateOrCreateMctsSubscriber(MctsBeneficiary beneficiary, Long msisdn, DateTime referenceDate,
                                                     SubscriptionPackType packType) {
        District district = beneficiary.getDistrict();
        Circle circle = circleFromDistrict(district);
        Language language = (Language) districtDataService.getDetachedField(district, "language");
        Subscriber subscriber = getSubscriber(msisdn);

        SubscriptionPack pack = subscriptionPackDataService.byType(packType);

        // TODO: #455 Handle the case in which the MCTS beneficiary already exists but with a different phone number

        if (subscriber == null) {
            // there's no subscriber with this MSISDN, create one

            subscriber = new Subscriber(msisdn, language);
            subscriber = setSubscriberFields(subscriber, beneficiary, referenceDate, packType);
            create(subscriber);
            Subscription subscription = subscriptionService.createSubscription(msisdn, language, circle, pack, SubscriptionOrigin.MCTS_IMPORT);
            LOGGER.debug("sizeof(subscription) = {}", ObjectSizeFetcher.getObjectSize(subscription));
            return subscription;
        }

        Subscription subscription = subscriptionService.getActiveSubscription(subscriber, packType);
        if (subscription != null) {
            // subscriber already has an active subscription to this pack

            MctsBeneficiary existingBeneficiary = (packType == SubscriptionPackType.PREGNANCY) ? subscriber.getMother() :
                    subscriber.getChild();

            if (existingBeneficiary == null) {
                // there's already an IVR-originated subscription for this MSISDN
                subscriptionErrorDataService.create(
                        new SubscriptionError(msisdn, SubscriptionRejectionReason.ALREADY_SUBSCRIBED, packType));

                // TODO: Do we just reject the subscription request, or do we update the subscriber record with MCTS data?
                // TODO: Should we change the subscription start date based on the provided LMP/DOB?

            } else if (!existingBeneficiary.getBeneficiaryId().equals(beneficiary.getBeneficiaryId())) {
                // if the MCTS ID doesn't match (i.e. there are two beneficiaries with the same phone number), reject the import
                subscriptionErrorDataService.create(
                        new SubscriptionError(msisdn, SubscriptionRejectionReason.ALREADY_SUBSCRIBED, packType));
            } else {
                // it's the same beneficiary, treat this import as an update
                subscriber = setSubscriberFields(subscriber, beneficiary, referenceDate, packType);
                update(subscriber);
            }

            return subscription;
        }

        // subscriber exists, but doesn't have a subscription to this pack
        subscriber = setSubscriberFields(subscriber, beneficiary, referenceDate, packType);
        update(subscriber);
        return subscriptionService.createSubscription(msisdn, language, circle, pack, SubscriptionOrigin.MCTS_IMPORT);
    }


    private Subscriber setSubscriberFields(Subscriber subscriber, MctsBeneficiary beneficiary, DateTime referenceDate,
                                             SubscriptionPackType packType) {
        if (packType == SubscriptionPackType.PREGNANCY) {
            subscriber.setLastMenstrualPeriod(referenceDate);
            subscriber.setMother((MctsMother) beneficiary);
        } else {
            subscriber.setDateOfBirth(referenceDate);
            subscriber.setChild((MctsChild) beneficiary);
        }
        return subscriber;
    }


    public void deleteAllowed(Subscriber subscriber) {
        for (Subscription subscription: subscriber.getSubscriptions()) {
            subscriptionService.deletePreconditionCheck(subscription);
        }
    }
}
