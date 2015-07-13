package org.motechproject.nms.kilkari.ut;

import org.joda.time.DateTime;
import org.junit.Test;
import org.motechproject.nms.kilkari.domain.Subscriber;
import org.motechproject.nms.kilkari.domain.Subscription;
import org.motechproject.nms.kilkari.domain.SubscriptionOrigin;
import org.motechproject.nms.kilkari.domain.SubscriptionPack;
import org.motechproject.nms.kilkari.domain.SubscriptionPackType;
import org.motechproject.nms.kilkari.domain.SubscriptionStatus;
import org.motechproject.nms.props.domain.DayOfTheWeek;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.motechproject.nms.kilkari.ut.SubscriptionTestHelper.createSubscriptionPack;

public class SubscriptionUnitTest {

    @Test
    public void verifyHasCompleted() {
        Subscription subscription = new Subscription(
                new Subscriber(1111111111L),
                new SubscriptionPack("pack", SubscriptionPackType.CHILD, 10, 1, null),
                SubscriptionOrigin.IVR);
        subscription.setStartDate(DateTime.now().minusDays(5));
        assertFalse(subscription.hasCompleted(DateTime.now()));
        subscription.setStartDate(DateTime.now().minusDays(71));
        assertTrue(subscription.hasCompleted(DateTime.now()));
    }

    @Test
    public void verifyDayOfTheWeek() {
        Subscription subscription = new Subscription(
                new Subscriber(1111111111L),
                new SubscriptionPack("pack", SubscriptionPackType.CHILD, 10, 1, null),
                SubscriptionOrigin.IVR);

        DateTime startDate = DateTime.now().minusDays((int) (Math.random()) * 10);
        DayOfTheWeek startDayOfTheWeek = DayOfTheWeek.fromInt(startDate.getDayOfWeek());
        subscription.setStartDate(startDate);

        assertEquals(startDayOfTheWeek, subscription.getFirstMessageDayOfWeek());
    }

    @Test
    public void verifySetStatusUpdatesEndDate() {
        Subscription s = new Subscription(
                new Subscriber(1111111111L),
                createSubscriptionPack("pack", SubscriptionPackType.CHILD, 10, 1),
                SubscriptionOrigin.IVR);
        s.setStatus(SubscriptionStatus.ACTIVE);

        assertNull(s.getEndDate());

        s.setStatus(SubscriptionStatus.COMPLETED);
        assertNotNull(s.getEndDate());

        s.setStatus(SubscriptionStatus.ACTIVE);
        assertNull(s.getEndDate());

        s.setStatus(SubscriptionStatus.DEACTIVATED);
        assertNotNull(s.getEndDate());
    }


}
