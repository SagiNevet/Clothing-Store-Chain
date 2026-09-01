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

public final class CommandFactory {

    private static final Map<ActionType, Command> COMMANDS = createCommandTable();

    private CommandFactory() {
    }

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

        ReportCommands.BuildReport buildReport = new ReportCommands.BuildReport();
        commandTable.put(ActionType.SALES_BY_BRANCH_REPORT, buildReport);
        commandTable.put(ActionType.SALES_BY_PRODUCT_REPORT, buildReport);
        commandTable.put(ActionType.EXPORT_REPORT, new ReportCommands.ExportReport());

        return commandTable;
    }

    public static Command commandFor(ActionType actionType) throws ChainStoreException {
        Command command = COMMANDS.get(actionType);
        if (command == null) {
            throw new ChainStoreException(
                    "The action " + actionType + " is not supported by this server yet");
        }
        return command;
    }

    public static boolean isSupported(ActionType actionType) {
        return COMMANDS.containsKey(actionType);
    }
}
