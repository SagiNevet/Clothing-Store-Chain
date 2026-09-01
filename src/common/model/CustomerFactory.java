package common.model;

import common.util.AppConfig;

public final class CustomerFactory {

    private static final String CONFIG_KEY_VIP_MINIMUM_PURCHASES = "customer.vip.minPurchases";

    private static final String CONFIG_KEY_VIP_MINIMUM_TOTAL_SPENT = "customer.vip.minTotalSpent";

    private static final int DEFAULT_VIP_MINIMUM_PURCHASES = 5;

    private static final double DEFAULT_VIP_MINIMUM_TOTAL_SPENT = 1000.0;

    private static final int NO_PURCHASES_YET = 0;

    private CustomerFactory() {
    }

    public static Customer create(CustomerType customerType, String idNumber,
                                  String fullName, String phone) {
        return create(customerType, idNumber, fullName, phone, NO_PURCHASES_YET, 0.0);
    }

    public static Customer create(CustomerType customerType, String idNumber, String fullName,
                                  String phone, int purchaseCount, double totalSpent) {
        if (customerType == null) {
            throw new IllegalArgumentException("customerType must not be null");
        }
        switch (customerType) {
            case NEW:
                return new NewCustomer(idNumber, fullName, phone, purchaseCount, totalSpent);
            case RETURNING:
                return new ReturningCustomer(idNumber, fullName, phone, purchaseCount, totalSpent);
            case VIP:
                return new VipCustomer(idNumber, fullName, phone, purchaseCount, totalSpent);
            default:
                throw new IllegalArgumentException("Unsupported customer type: " + customerType);
        }
    }

    public static Customer createNewCustomer(String idNumber, String fullName, String phone) {
        return new NewCustomer(idNumber, fullName, phone);
    }

    public static Customer upgradeIfNeeded(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("customer must not be null");
        }
        CustomerType requiredType = determineTypeFor(customer);
        if (requiredType == customer.getCustomerType()) {
            return customer;
        }
        return create(requiredType,
                customer.getIdNumber(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getPurchaseCount(),
                customer.getTotalSpent());
    }

    private static CustomerType determineTypeFor(Customer customer) {
        if (customer.getPurchaseCount() == NO_PURCHASES_YET) {
            return CustomerType.NEW;
        }
        if (qualifiesForVip(customer)) {
            return CustomerType.VIP;
        }
        return CustomerType.RETURNING;
    }

    private static boolean qualifiesForVip(Customer customer) {
        AppConfig configuration = AppConfig.getInstance();
        int minimumPurchases = configuration.getInt(
                CONFIG_KEY_VIP_MINIMUM_PURCHASES, DEFAULT_VIP_MINIMUM_PURCHASES);
        double minimumTotalSpent = configuration.getDouble(
                CONFIG_KEY_VIP_MINIMUM_TOTAL_SPENT, DEFAULT_VIP_MINIMUM_TOTAL_SPENT);
        return customer.getPurchaseCount() >= minimumPurchases
                || customer.getTotalSpent() >= minimumTotalSpent;
    }
}
