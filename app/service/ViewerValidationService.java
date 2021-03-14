package service;

public interface ViewerValidationService {
    /**
     * Persist the fact that accountID has definitely accessed resourceId on hostID.
     * Invoke this after validating the JWT token on an iframe request.
     *
     * Token will expire after VALIDATION_TOKEN_EXPIRY_SECONDS.
     *
     * @param hostId host being accessed
     * @param resourceId resource being accessed
     * @param accountId user accesssing
     *
     * @return the token to use with verifyToken to later check for evidence that the access occurred. The token is valid for VALIDATION_TOKEN_EXPIRY_SECONDS.
     */
    String setToken(final String hostId, final String resourceId, final String accountId);

    /**
     * @param hostId
     * @param resourceId
     * @param accountId
     * @param token
     * @return whether token represents valid evidence that accountId has accessed resourceId on hostId.
     */
    boolean verifyToken(final String hostId, final String resourceId, final String accountId, String token);
}
