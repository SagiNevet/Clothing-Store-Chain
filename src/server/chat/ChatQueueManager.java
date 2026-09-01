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

public final class ChatQueueManager {

    private static final ChatQueueManager INSTANCE = new ChatQueueManager();

    private static final String CONFIG_KEY_WAIT_WINDOW = "chat.waitForPartnerMillis";

    private static final int DEFAULT_WAIT_WINDOW_MILLIS = 3000;

    private final Set<String> busyEmployeeNumbers = new HashSet<>();

    private final List<PendingChatRequest> waitingRequests = new ArrayList<>();

    private ChatQueueManager() {
    }

    public static ChatQueueManager getInstance() {
        return INSTANCE;
    }

    public synchronized Employee findPartnerOrQueue(Employee requester, Branch targetBranch,
                                                    PendingChatRequest pendingRequest)
            throws InterruptedException {
        Employee partner = findFreeEmployeeOf(targetBranch, requester.getEmployeeNumber());
        if (partner != null) {
            markBusy(requester.getEmployeeNumber());
            markBusy(partner.getEmployeeNumber());
            return partner;
        }

        waitingRequests.add(pendingRequest);

        long waitWindowMillis = AppConfig.getInstance()
                .getInt(CONFIG_KEY_WAIT_WINDOW, DEFAULT_WAIT_WINDOW_MILLIS);
        long deadline = System.currentTimeMillis() + waitWindowMillis;

        while (partner == null) {
            long remainingMillis = deadline - System.currentTimeMillis();
            if (remainingMillis <= 0) {
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

    public synchronized void markFree(String employeeNumber) {
        busyEmployeeNumbers.remove(employeeNumber);

        notifyAll();
    }

    public synchronized void markBusy(String employeeNumber) {
        busyEmployeeNumbers.add(employeeNumber);
    }

    public synchronized boolean isBusy(String employeeNumber) {
        return busyEmployeeNumbers.contains(employeeNumber);
    }

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

    public synchronized void removeRequestsOf(String requesterEmployeeNumber) {
        waitingRequests.removeIf(request ->
                request.getRequesterEmployeeNumber().equals(requesterEmployeeNumber));
    }

    public synchronized List<PendingChatRequest> getWaitingRequests() {
        return new ArrayList<>(waitingRequests);
    }

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
