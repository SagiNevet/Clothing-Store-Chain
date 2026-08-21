package server.command;

import common.exception.ChainStoreException;
import common.protocol.ActionType;
import server.command.impl.AddCustomerCommand;
import server.command.impl.ChatCommands;
import server.command.impl.AddEmployeeCommand;
import server.command.impl.AddProductCommand;
import server.command.impl.GetCustomersCommand;
import server.command.impl.GetEmployeesCommand;
import server.command.impl.GetInventoryCommand;
import server.command.impl.GetPasswordPolicyCommand;
import server.command.impl.LoginCommand;
import server.command.impl.ReportCommands;
import server.command.impl.LogoutCommand;
import server.command.impl.RestockProductCommand;
import server.command.impl.SellProductCommand;
import server.command.impl.UpdateCustomerCommand;
import server.command.impl.UpdatePasswordPolicyCommand;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds the table that maps every {@link ActionType} to the command that
 * performs it.
 * <p>
 * This class is the <b>Factory</b> pattern of the server side. The handler asks
 * it for "the command of this action" and receives a ready object; it never
 * mentions a concrete command class, and it contains no {@code switch} on the
 * action type.
 * </p>
 * <p>
 * The table is an {@link EnumMap}, which is the natural structure when the key
 * is an enum: internally it is a plain array indexed by the position of the
 * constant, so a lookup costs an array access and no hashing at all.
 * </p>
 * <p>
 * The commands are created once, when the class is loaded, and shared by every
 * client thread. That is safe because a command holds no state of its own - all
 * the data it needs arrives in the {@code Request} of the caller. A command
 * that kept a field per request would have to be created per request instead.
 * </p>
 */
public final class CommandFactory {

    /** The single table of commands, filled once when the class is loaded. */
    private static final Map<ActionType, Command> COMMANDS = createCommandTable();

    /**
     * Prevents instantiation. This class only exposes static methods.
     */
    private CommandFactory() {
    }

    /**
     * Builds the table of commands.
     * <p>
     * Actions that belong to later stages of the project are not in the table
     * yet, and asking for one produces a clear business failure instead of a
     * {@code NullPointerException}.
     * </p>
     *
     * @return the table mapping each supported action to its command
     */
    private static Map<ActionType, Command> createCommandTable() {
        Map<ActionType, Command> commandTable = new EnumMap<>(ActionType.class);

        commandTable.put(ActionType.LOGIN, new LoginCommand());
        commandTable.put(ActionType.LOGOUT, new LogoutCommand());

        commandTable.put(ActionType.GET_INVENTORY, new GetInventoryCommand());
        commandTable.put(ActionType.SELL_PRODUCT, new SellProductCommand());
        commandTable.put(ActionType.RESTOCK_PRODUCT, new RestockProductCommand());
        commandTable.put(ActionType.ADD_PRODUCT, new AddProductCommand());

        commandTable.put(ActionType.GET_CUSTOMERS, new GetCustomersCommand());
        commandTable.put(ActionType.ADD_CUSTOMER, new AddCustomerCommand());
        commandTable.put(ActionType.UPDATE_CUSTOMER, new UpdateCustomerCommand());

        commandTable.put(ActionType.GET_EMPLOYEES, new GetEmployeesCommand());
        commandTable.put(ActionType.ADD_EMPLOYEE, new AddEmployeeCommand());
        commandTable.put(ActionType.GET_PASSWORD_POLICY, new GetPasswordPolicyCommand());
        commandTable.put(ActionType.UPDATE_PASSWORD_POLICY, new UpdatePasswordPolicyCommand());

        commandTable.put(ActionType.CHAT_REQUEST, new ChatCommands.RequestChat());
        commandTable.put(ActionType.CHAT_SEND, new ChatCommands.SendMessage());
        commandTable.put(ActionType.CHAT_JOIN, new ChatCommands.JoinChat());
        commandTable.put(ActionType.CHAT_CLOSE, new ChatCommands.CloseChat());
        commandTable.put(ActionType.GET_ACTIVE_CHATS, new ChatCommands.ListOpenChats());
        commandTable.put(ActionType.CHAT_CALLBACK, new ChatCommands.RequestChat());

        // One command builds every kind of report, and one exports it. The kind
        // of report travels as a parameter, so a new report costs one enum value.
        ReportCommands.BuildReport buildReport = new ReportCommands.BuildReport();
        commandTable.put(ActionType.SALES_BY_BRANCH_REPORT, buildReport);
        commandTable.put(ActionType.SALES_BY_PRODUCT_REPORT, buildReport);
        commandTable.put(ActionType.EXPORT_REPORT, new ReportCommands.ExportReport());

        return commandTable;
    }

    /**
     * Returns the command that performs an action.
     *
     * @param actionType the action requested by the client
     * @return the command object that performs it
     * @throws ChainStoreException if the server does not support that action
     */
    public static Command commandFor(ActionType actionType) throws ChainStoreException {
        Command command = COMMANDS.get(actionType);
        if (command == null) {
            throw new ChainStoreException(
                    "The action " + actionType + " is not supported by this server yet");
        }
        return command;
    }

    /**
     * Indicates whether an action is already implemented.
     *
     * @param actionType the action to check
     * @return {@code true} if a command exists for that action
     */
    public static boolean isSupported(ActionType actionType) {
        return COMMANDS.containsKey(actionType);
    }
}
