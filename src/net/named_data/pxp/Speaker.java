package net.named_data.pxp;

import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;

/* Manages the speaking of Phrases
 * Note: Intended to help with multi-threading so that multiple Phrases may be handed concurrently
 * from a central mediator.
 * Disclaimer: As some of the jNDN code is not guaranteed to be thread-safe
 * (i.e. accessing the in-memory representation of a ConfigPolicyManager's .conf file)
 * there is additional challenge in implementing and testing concurrent Phrasing beyond PXP-Demo code.
 * In the context of this proof-of-concept, concurrency is not strictly required, however, the
 * design patterns employed should hopefully make a concurrent Speaker implementation require
 * minimal changes to the rest of the PXP-Demo code.
 */
public class Speaker {

    private static PriorityQueue<Phrase> _phraseQueue = new PriorityQueue<Phrase>(new PhraseBeginTimeComparator());

    public static void enqueuePhrase(Phrase phrase){
        _phraseQueue.add(phrase);
    }

    //Begins the single, most urgent Phrase on the callers thread, if its time has come
    public static void speak(){
        Phrase mostUrgent = _phraseQueue.peek();

        if(mostUrgent == null)
            return;

        if(mostUrgent.getBeginTime().getTime() <= new Date().getTime()) {
            _phraseQueue.remove().begin();
        }
    }

    //Compares the begin time of two Phrases to determine a new Phrase's place in the queue
    private static class PhraseBeginTimeComparator implements Comparator<Phrase> {

        @Override
        public int compare(Phrase p1, Phrase p2) {

            long diff = p1.getBeginTime().getTime() - p2.getBeginTime().getTime();

            if (diff > 0)
                return 1;
            else if (diff < 0)
                return -1;
            else
                return 0;
        }
    }
}
