package common.protocol;

/**
 * The outcome of a request, as reported back to the client.
 * <p>
 * Only two values are needed. The reason a request failed is carried as text in
 * {@link Response#getMessage()}, because the client always reacts in the same
 * way: it shows the message to the user.
 * </p>
 */
public enum ResponseStatus {

    /** The action completed and the payload holds its result. */
    SUCCESS,

    /** The action was refused or failed, and the message explains why. */
    FAILURE
}
