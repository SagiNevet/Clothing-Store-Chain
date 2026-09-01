package common.model;

import common.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatSessionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sessionId;

    private final String initiatorEmployeeNumber;

    private final String initiatorName;

    private final Branch initiatorBranch;

    private final String partnerEmployeeNumber;

    private final String partnerName;

    private final Branch partnerBranch;

    private final List<String> joinedManagers;

    private final LocalDateTime openedAt;

    public ChatSessionInfo(String sessionId, String initiatorEmployeeNumber,
                           String initiatorName, Branch initiatorBranch,
                           String partnerEmployeeNumber, String partnerName,
                           Branch partnerBranch, List<String> joinedManagers,
                           LocalDateTime openedAt) {
        this.sessionId = sessionId;
        this.initiatorEmployeeNumber = initiatorEmployeeNumber;
        this.initiatorName = initiatorName;
        this.initiatorBranch = initiatorBranch;
        this.partnerEmployeeNumber = partnerEmployeeNumber;
        this.partnerName = partnerName;
        this.partnerBranch = partnerBranch;
        this.joinedManagers = Collections.unmodifiableList(new ArrayList<>(joinedManagers));
        this.openedAt = openedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInitiatorEmployeeNumber() {
        return initiatorEmployeeNumber;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public Branch getInitiatorBranch() {
        return initiatorBranch;
    }

    public String getPartnerEmployeeNumber() {
        return partnerEmployeeNumber;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public Branch getPartnerBranch() {
        return partnerBranch;
    }

    public List<String> getJoinedManagers() {
        return joinedManagers;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public String describeParticipants() {
        return initiatorName + " (" + initiatorBranch.getDisplayName() + ")"
                + " with " + partnerName + " (" + partnerBranch.getDisplayName() + ")";
    }

    @Override
    public String toString() {
        return describeParticipants() + ", opened at " + TimeUtil.formatForDisplay(openedAt)
                + (joinedManagers.isEmpty() ? "" : ", joined by " + joinedManagers);
    }
}
