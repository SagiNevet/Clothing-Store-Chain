package server.chat;

import common.model.Branch;
import common.model.Employee;
import common.util.AppConfig;
import server.core.ClientRegistry;
import server.core.ConnectedClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds a free employee for a chat request, and keeps the queue of requests
 * that nobody was free to answer.
 * <p>
 * <b>This class is where {@code wait()} and {@code notify()} are used.</b> The
 * two of them together solve a problem no other tool in the project solves: how
 * does one thread find out that another thread changed something, without
 * asking again and again?
 * </p>
 *
 * <h2>How a request is served</h2>
 * <ol>
 *   <li>Somebody in the target branch is free - the conversation opens
 *       immediately.</li>
 *   <li>Nobody is free - the request is put in the waiting list, and the thread
 *       calls {@code wait()} for a short window. If an employee finishes a
 *       conversation during that window, {@code notifyAll()} wakes this thread
 *       and the conversation opens straight away.</li>
 *   <li>The window passes with nobody becoming free - the request <b>stays in
 *       the waiting list</b> and the requester is told so. Later, when somebody
 *       does become free, the requester receives a
 *       {@code CHAT_PEER_AVAILABLE} notification and may call back.</li>
 * </ol>
 *
 * <h2>Why the wait is short and the queue takes over</h2>
 * <p>
 * A thread sitting in {@code wait()} is a thread of the business pool that
 * nobody else can use. Waiting there for minutes would slowly consume the whole
 * pool while the employees involved do nothing at all. The short window catches
 * the common case - the colleague is about to finish - and the notification
 * mechanism covers everything longer, without holding a thread hostage.
 * </p>
 *
 * <h2>Why {@code wait()} and not a loop that keeps checking</h2>
 * <p>
 * A loop with a {@code sleep} inside would burn processor time doing nothing
 * and would still answer late. {@code wait()} releases the lock and puts the
 * thread to sleep at no cost; {@code notifyAll()} wakes it the instant the
 * state actually changes.
 * </p>
 * <p>
 * <b>The wait is inside a loop and not inside an {@code if}</b>, which is the
 * rule for every use of {@code wait()}: a thread can wake up without anybody
 * having called notify - a "spurious wakeup" - and even a real notify does not
 * promise the condition still holds by the time this thread gets the lock back.
 * The loop re-checks, which is the only correct way to write it.
 * </p>
 */
public final class ChatQueueManager {

    /** The single instance, created when the class is first loaded. */
    private static final ChatQueueManager INSTANCE = new ChatQueueManager();

    /** Configuration key holding how long a request waits before it is queued. */
    private static final String CONFIG_KEY_WAIT_WINDOW = "chat.waitForPartnerMillis";

    /** The waiting window used when the configuration file is missing. */
    private static final int DEFAULT_WAIT_WINDOW_MILLIS = 3000;

    /** The employees who are in a conversation right now. */
    private final Set<String> busyEmployeeNumbers = new HashSet<>();

    /** The requests nobody was free to answer, in the order they arrived. */
    private final List<PendingChatRequest> waitingRequests = new ArrayList<>();

    /**
     * Prevents anybody from building a second queue manager.
     */
    private ChatQueueManager() {
    }

    /**
     * Returns the single queue manager instance.
     *
     * @return the singleton instance, never {@code null}
     */
    public static ChatQueueManager getInstance() {
        return INSTANCE;
    }

    /**
     * Looks for a free employee of a branch, waiting a short while if none is
     * available right now.
     * <p>
     * When an employee is found, both sides are marked busy before this method
     * returns, so a third request cannot grab the same partner.
     * </p>
     *
     * @param requester       the employee asking for a conversation
     * @param targetBranch    the branch to find a partner in
     * @param pendingRequest  the entry to keep in the queue if nobody is free
     * @return the employee who will answer, or {@code null} when nobody became
     *         free and the request was left in the queue
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public synchronized Employee findPartnerOrQueue(Employee requester, Branch targetBranch,
                                                    PendingChatRequest pendingRequest)
            throws InterruptedException {
        Employee partner = findFreeEmployeeOf(targetBranch, requester.getEmployeeNumber());
        if (partner != null) {
            markBusy(requester.getEmployeeNumber());
            markBusy(partner.getEmployeeNumber());
            return partner;
        }

        // Nobody is free. The request joins the queue before the wait begins, so
        // that an employee who becomes free during the wait can already see it.
        waitingRequests.add(pendingRequest);

        long waitWindowMillis = AppConfig.getInstance()
                .getInt(CONFIG_KEY_WAIT_WINDOW, DEFAULT_WAIT_WINDOW_MILLIS);
        long deadline = System.currentTimeMillis() + waitWindowMillis;

        // The wait sits inside a loop, never inside an if. A thread may wake up
        // spuriously, and even a genuine notifyAll does not promise the partner
        // is still free once this thread gets the lock back.
        while (partner == null) {
            long remainingMillis = deadline - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                // The window is over. The request stays in the queue and the
                // requester will be notified when somebody frees up.
                return null;
            }
            wait(remainingMillis);
            partner = findFreeEmployeeOf(targetBranch, requester.getEmployeeNumber());
        }

        waitingRequests.remove(pendingRequest);
        markBusy(requester.getEmployeeNumber());
        markBusy(partner.getEmployeeNumber());
        return partner;
    }

    /**
     * Marks an employee as free and wakes everybody waiting for a partner.
     * <p>
     * Called when a conversation is closed and when a client disconnects. The
     * {@code notifyAll} is what turns "somebody finished" into "the thread that
     * was waiting continues".
     * </p>
     *
     * @param employeeNumber the employee who is free again
     */
    public synchronized void markFree(String employeeNumber) {
        busyEmployeeNumbers.remove(employeeNumber);
        // notifyAll and not notify: several employees may be waiting for a
        // partner, and only some of them want the branch that just freed up.
        // Waking one at random could wake exactly the wrong one, which would
        // leave everybody waiting although a partner is available.
        notifyAll();
    }

    /**
     * Marks an employee as busy, so no other request tries to talk to them.
     *
     * @param employeeNumber the employee who entered a conversation
     */
    public synchronized void markBusy(String employeeNumber) {
        busyEmployeeNumbers.add(employeeNumber);
    }

    /**
     * Indicates whether an employee is in a conversation right now.
     *
     * @param employeeNumber the employee to check
     * @return {@code true} if that employee is busy
     */
    public synchronized boolean isBusy(String employeeNumber) {
        return busyEmployeeNumbers.contains(employeeNumber);
    }

    /**
     * Removes and returns the queued requests that an employee of a branch can
     * now answer.
     * <p>
     * Called after an employee becomes free. The requests are removed from the
     * queue as they are returned, so the same request is never announced twice.
     * </p>
     *
     * @param branchThatBecameFree the branch the free employee belongs to
     * @return the requests that were waiting for that branch, oldest first
     */
    public synchronized List<PendingChatRequest> takeRequestsWaitingFor(
            Branch branchThatBecameFree) {
        List<PendingChatRequest> matching = new ArrayList<>();
        waitingRequests.removeIf(request -> {
            if (request.getTargetBranch() == branchThatBecameFree) {
                matching.add(request);
                return true;
            }
            return false;
        });
        return matching;
    }

    /**
     * Removes every request made by one employee, used when that employee
     * disconnects or gives up.
     *
     * @param requesterEmployeeNumber the employee whose requests should be dropped
     */
    public synchronized void removeRequestsOf(String requesterEmployeeNumber) {
        waitingRequests.removeIf(request ->
                request.getRequesterEmployeeNumber().equals(requesterEmployeeNumber));
    }

    /**
     * Returns the requests still waiting in the queue.
     *
     * @return a copy of the waiting list
     */
    public synchronized List<PendingChatRequest> getWaitingRequests() {
        return new ArrayList<>(waitingRequests);
    }

    /**
     * Finds one connected employee of a branch who is not busy.
     * <p>
     * Always called from inside a {@code synchronized} method of this class, so
     * the answer cannot change between the check and the marking that follows it.
     * </p>
     *
     * @param branch                the branch to search in
     * @param employeeNumberToSkip  the requester, who must not be paired with
     *                              themselves
     * @return a free employee of that branch, or {@code null} if there is none
     */
    private Employee findFreeEmployeeOf(Branch branch, String employeeNumberToSkip) {
        for (ConnectedClient client : ClientRegistry.getInstance().getClientsOfBranch(branch)) {
            Employee candidate = client.getEmployee();
            if (candidate == null) {
                continue;
            }
            if (candidate.getEmployeeNumber().equals(employeeNumberToSkip)) {
                continue;
            }
            if (!busyEmployeeNumbers.contains(candidate.getEmployeeNumber())) {
                return candidate;
            }
        }
        return null;
    }
}
